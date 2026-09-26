package net.jadenxgamer.elysium_api.processor;

import net.jadenxgamer.elysium_api.api.charon.*;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.tools.Diagnostic;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@SupportedAnnotationTypes("net.jadenxgamer.elysium_api.api.charon.CharonEventBoat")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class CharonProcessor extends AbstractProcessor {

    private static final String CONTEXT_FQN = "net.jadenxgamer.elysium_api.api.charon.CharonContext";
    private static final String OPTION_OUTPUT_DIR = "charon.outputDir";
    private static final String OPTION_REFMAP = "charon.refmap";

    @Override
    public Set<String> getSupportedOptions() {
        return Set.of(OPTION_OUTPUT_DIR, OPTION_REFMAP);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Map<String, List<CharonData>> charonsByModId = new LinkedHashMap<>();

        for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(CharonEventBoat.class)) {
            if (annotatedElement.getKind() != ElementKind.CLASS) {
                error("@CharonEventBoat can only be applied to classes", annotatedElement);
                continue;
            }
            TypeElement boatClass = (TypeElement) annotatedElement;
            CharonEventBoat boatAnnotation = boatClass.getAnnotation(CharonEventBoat.class);

            for (Element enclosedElement : boatClass.getEnclosedElements()) {
                if (enclosedElement.getKind() != ElementKind.METHOD) continue;
                CharonEvent eventAnnotation = enclosedElement.getAnnotation(CharonEvent.class);
                if (eventAnnotation == null) continue;
                ExecutableElement handlerMethod = (ExecutableElement) enclosedElement;

                if (!handlerMethod.getModifiers().contains(Modifier.STATIC) || handlerMethod.getModifiers().contains(Modifier.PRIVATE)) {
                    error("@CharonEvent methods must be static and non-private", enclosedElement);
                    continue;
                }

                String targetFQN = resolveTargetFQN(eventAnnotation);
                if (targetFQN == null || targetFQN.isEmpty() || targetFQN.equals("void")) {
                    error("@CharonEvent must specify target or targetName", enclosedElement);
                    continue;
                }

                TypeElement targetClassElement = processingEnv.getElementUtils().getTypeElement(targetFQN);
                ExecutableElement targetMethodElement = targetClassElement != null ? findMethod(targetClassElement, eventAnnotation.method(), handlerMethod) : null;

                if (targetMethodElement == null) {
                    error("@CharonEvent target method '%s' could not be resolved on '%s'.".formatted(eventAnnotation.method(), targetFQN), enclosedElement);
                    continue;
                }

                boolean requiresContext = handlerMethod.getParameters().stream()
                        .anyMatch(p -> processingEnv.getTypeUtils().erasure(p.asType()).toString().equals(CONTEXT_FQN));
                int priority = Optional.ofNullable(handlerMethod.getAnnotation(CharonPriority.class)).map(CharonPriority::value).orElse(1000);

                charonsByModId.computeIfAbsent(boatAnnotation.modid(), k -> new ArrayList<>())
                        .add(new CharonData(boatClass, handlerMethod, targetFQN, targetClassElement, targetMethodElement, eventAnnotation, boatAnnotation.dist(), priority, requiresContext));
            }
        }

        charonsByModId.forEach((modId, data) -> {
            try { generateModResources(modId, data); }
            catch (IOException e) { error("Failed to generate mixins for mod " + modId + ": " + e.getMessage(), null); }
        });

        return true;
    }

    private String resolveTargetFQN(CharonEvent eventAnnotation) {
        if (!eventAnnotation.targetName().isEmpty()) return eventAnnotation.targetName();
        try { return eventAnnotation.target().getCanonicalName(); }
        catch (MirroredTypeException e) { return e.getTypeMirror().getKind() == TypeKind.VOID ? null : e.getTypeMirror().toString(); }
    }

    private ExecutableElement findMethod(TypeElement targetClass, String methodName, ExecutableElement handlerMethod) {
        List<ExecutableElement> candidates = processingEnv.getElementUtils().getAllMembers(targetClass).stream()
                .filter(m -> m.getKind() == ElementKind.METHOD || m.getKind() == ElementKind.CONSTRUCTOR)
                .filter(m -> m.getSimpleName().contentEquals(methodName))
                .map(m -> (ExecutableElement) m).toList();

        if (candidates.isEmpty()) {
            candidates = targetClass.getEnclosedElements().stream()
                    .filter(m -> m.getKind() == ElementKind.CONSTRUCTOR && m.getSimpleName().contentEquals(methodName))
                    .map(m -> (ExecutableElement) m).toList();
        }

        if (candidates.isEmpty()) return null;
        if (candidates.size() == 1) return candidates.getFirst();

        long targetParamCount = handlerMethod.getParameters().stream()
                .filter(p -> !processingEnv.getTypeUtils().erasure(p.asType()).toString().equals(CONTEXT_FQN)).count();
        return candidates.stream().filter(c -> c.getParameters().size() == targetParamCount).findFirst().orElse(candidates.getFirst());
    }

    private void generateModResources(String modId, List<CharonData> charonDataList) throws IOException {
        String packageName = sanitize(modId) + ".charon.mixins";
        Map<CharonDist, List<String>> mixinsByDist = new LinkedHashMap<>();

        charonDataList.stream().collect(Collectors.groupingBy(d -> d.dist, Collectors.groupingBy(d -> d.targetFQN)))
                .forEach((dist, targets) -> targets.forEach((targetFQN, dataList) -> {
                    String mixinName = sanitize(targetFQN.replace('.', '_').replace('$', '_')) + "CharonMixin_" + dist.name();
                    try {
                        writeSource(packageName + "." + mixinName, buildMixinSource(packageName, mixinName, targetFQN, dataList));
                        mixinsByDist.computeIfAbsent(dist, k -> new ArrayList<>()).add(mixinName);
                    } catch (IOException e) { error("Failed writing source", null); }
                }));

        writeResource(modId + ".charon.mixins.json", buildMixinJson(packageName, mixinsByDist));
    }

    private String buildMixinSource(String packageName, String mixinClassName, String targetFQN, List<CharonData> dataList) {
        StringBuilder source = new StringBuilder();
        source.append("package %s;\n\nimport net.jadenxgamer.elysium_api.api.charon.CharonContext;\n".formatted(packageName));
        source.append("import org.spongepowered.asm.mixin.Mixin;\nimport org.spongepowered.asm.mixin.injection.*;\n");
        source.append("import org.spongepowered.asm.mixin.injection.callback.*;\n\n");
        source.append("@SuppressWarnings({\"rawtypes\", \"unchecked\", \"unused\"})\n");

        boolean hasClassConstant = dataList.getFirst().targetClassElement != null;
        source.append(hasClassConstant ? "@Mixin(%s.class)\n".formatted(targetFQN) : "@Mixin(targets = \"%s\")\n".formatted(escape(targetFQN)));
        source.append("public abstract class %s {\n\n".formatted(mixinClassName));

        if (!hasClassConstant) {
            source.append("    private static final Class<?> CHARON$TARGET_CLASS;\n    static {\n        Class<?> clazz = null;\n");
            source.append("        try { clazz = Class.forName(\"%s\"); } catch (Throwable ignored) {}\n".formatted(escape(targetFQN)));
            source.append("        CHARON$TARGET_CLASS = clazz;\n    }\n\n");
        }

        List<List<CharonData>> groups = new ArrayList<>(dataList.stream()
                .collect(Collectors.groupingBy(d -> d.targetFQN + "|" + d.targetMethodElement.toString() + "|" + atToString(d.eventAnnotation.at())))
                .values());

        for (int i = 0; i < groups.size(); i++) {
            List<CharonData> group = groups.get(i);
            group.sort(Comparator.comparingInt((CharonData c) -> c.priority).reversed());

            ExecutableElement targetMethod = group.getFirst().targetMethodElement;
            boolean isConstructor = targetMethod.getKind() == ElementKind.CONSTRUCTOR;
            boolean isVoid = isConstructor || targetMethod.getReturnType().getKind() == TypeKind.VOID;
            boolean isStatic = !isConstructor && targetMethod.getModifiers().contains(Modifier.STATIC);
            boolean needsContext = group.stream().anyMatch(CharonData::requiresContext);
            String descriptor = group.getFirst().eventAnnotation.method() + methodDescriptor(targetMethod);

            source.append("    @Inject(method = \"%s\", at = %s%s)\n".formatted(
                    escape(descriptor), atToString(group.getFirst().eventAnnotation.at()), isConstructor ? "" : ", cancellable = true"
            ));
            source.append("    private %svoid charon$%s$%d(".formatted(isStatic ? "static " : "", sanitize(group.getFirst().eventAnnotation.method()), i));

            for (int p = 0; p < targetMethod.getParameters().size(); p++) {
                source.append("%s p%d, ".formatted(processingEnv.getTypeUtils().erasure(targetMethod.getParameters().get(p).asType()), p));
            }
            source.append(isVoid ? "CallbackInfo ci) {\n" : "CallbackInfoReturnable<%s> cir) {\n".formatted(boxedTypeName(processingEnv.getTypeUtils().erasure(targetMethod.getReturnType()))));

            if (needsContext) {
                String classExpr = hasClassConstant ? targetFQN + ".class" : "CHARON$TARGET_CLASS";
                source.append("        CharonContext ctx = new CharonContext(%s, %s, %s, %s);\n".formatted(
                        classExpr, isStatic ? "null" : "this", !isVoid, isVoid ? "null" : "cir.getReturnValue()"
                ));
            }

            for (CharonData data : group) {
                String callArgs = String.join(", ", resolveHandlerCallArguments(data, targetMethod));
                String call = "%s.%s(%s);\n".formatted(data.boatClass.getQualifiedName(), data.handlerMethod.getSimpleName(), callArgs);
                source.append(needsContext ? "        if (!ctx.isCancelled()) " + call : "        " + call);
            }

            if (needsContext && !isConstructor) {
                source.append(isVoid ? "        if (ctx.isVanillaCancelled()) ci.cancel();\n"
                        : "        if (ctx.isVanillaCancelled()) cir.setReturnValue((%s) ctx.getReturn());\n".formatted(boxedTypeName(processingEnv.getTypeUtils().erasure(targetMethod.getReturnType()))));
            }
            source.append("    }\n\n");
        }
        source.append("}\n");
        return source.toString();
    }

    private List<String> resolveHandlerCallArguments(CharonData data, ExecutableElement targetMethod) {
        List<String> callArguments = new ArrayList<>();
        boolean[] consumedTargetIndices = new boolean[targetMethod.getParameters().size()];

        for (VariableElement handlerParam : data.handlerMethod.getParameters()) {
            TypeMirror paramType = processingEnv.getTypeUtils().erasure(handlerParam.asType());
            if (paramType.toString().equals(CONTEXT_FQN)) {
                callArguments.add("ctx");
                continue;
            }

            int matchedIndex = -1;
            for (int i = 0; i < targetMethod.getParameters().size(); i++) {
                if (!consumedTargetIndices[i] && processingEnv.getTypeUtils().isSameType(paramType, processingEnv.getTypeUtils().erasure(targetMethod.getParameters().get(i).asType()))) {
                    matchedIndex = i; break;
                }
            }
            if (matchedIndex == -1) {
                for (int i = 0; i < targetMethod.getParameters().size(); i++) {
                    if (!consumedTargetIndices[i] && processingEnv.getTypeUtils().isAssignable(processingEnv.getTypeUtils().erasure(targetMethod.getParameters().get(i).asType()), paramType)) {
                        matchedIndex = i; break;
                    }
                }
            }

            if (matchedIndex != -1) {
                consumedTargetIndices[matchedIndex] = true;
                callArguments.add("p" + matchedIndex);
            } else {
                error("Could not map parameter " + paramType + " to target method", data.handlerMethod);
                callArguments.add("null");
            }
        }
        return callArguments;
    }

    private String atToString(Toll at) {
        List<String> attrs = new ArrayList<>();
        if (!at.value().isEmpty()) attrs.add("value = \"" + escape(at.value()) + "\"");
        if (!at.target().isEmpty()) attrs.add("target = \"" + escape(at.target()) + "\"");
        if (at.ordinal() != -1) attrs.add("ordinal = " + at.ordinal());
        if (at.shift() != Toll.Shift.NONE) attrs.add("shift = At.Shift." + at.shift().name());
        if (at.by() != 0) attrs.add("by = " + at.by());
        if (at.opcode() != -1) attrs.add("opcode = " + at.opcode());
        if (at.args().length > 0) attrs.add("args = {" + Arrays.stream(at.args()).map(a -> "\"" + escape(a) + "\"").collect(Collectors.joining(", ")) + "}");
        return "@At(" + String.join(", ", attrs) + ")";
    }

    private String methodDescriptor(ExecutableElement method) {
        return "(" + method.getParameters().stream().map(p -> jvmType(p.asType())).collect(Collectors.joining()) + ")" +
                (method.getKind() == ElementKind.CONSTRUCTOR ? "V" : jvmType(method.getReturnType()));
    }

    private String jvmType(TypeMirror type) {
        TypeMirror e = processingEnv.getTypeUtils().erasure(type);
        return switch (e.getKind()) {
            case BOOLEAN -> "Z"; case BYTE -> "B"; case SHORT -> "S"; case INT -> "I";
            case LONG -> "J"; case CHAR -> "C"; case FLOAT -> "F"; case DOUBLE -> "D"; case VOID -> "V";
            case ARRAY -> "[" + jvmType(((ArrayType) e).getComponentType());
            case DECLARED -> "L" + processingEnv.getElementUtils().getBinaryName((TypeElement) ((DeclaredType) e).asElement()).toString().replace('.', '/') + ";";
            default -> "L" + e.toString().replace('.', '/') + ";";
        };
    }

    private String boxedTypeName(TypeMirror type) {
        return switch (type.getKind()) {
            case BOOLEAN -> "java.lang.Boolean"; case BYTE -> "java.lang.Byte"; case SHORT -> "java.lang.Short";
            case INT -> "java.lang.Integer"; case LONG -> "java.lang.Long"; case CHAR -> "java.lang.Character";
            case FLOAT -> "java.lang.Float"; case DOUBLE -> "java.lang.Double"; default -> type.toString();
        };
    }

    private String buildMixinJson(String packageName, Map<CharonDist, List<String>> mixinsByDist) {
        StringBuilder json = new StringBuilder("{\n  \"required\": true,\n  \"minVersion\": \"0.8\",\n  \"package\": \"").append(packageName).append("\",\n  \"compatibilityLevel\": \"JAVA_21\"");
        Optional.ofNullable(processingEnv.getOptions().get(OPTION_REFMAP)).filter(s -> !s.isEmpty()).ifPresent(s -> json.append(",\n  \"refmap\": \"").append(s).append("\""));

        mixinsByDist.forEach((dist, mixins) -> {
            String key = dist == CharonDist.CLIENT ? "client" : dist == CharonDist.SERVER ? "server" : "mixins";
            json.append(",\n  \"").append(key).append("\": [").append(mixins.stream().map(m -> "\"" + m + "\"").collect(Collectors.joining(", "))).append("]");
        });
        return json.append("\n}\n").toString();
    }

    private void writeSource(String fqn, String content) throws IOException {
        try (Writer writer = processingEnv.getFiler().createSourceFile(fqn).openWriter()) { writer.write(content); }
    }

    private void writeResource(String fileName, String content) throws IOException {
        String outDir = processingEnv.getOptions().get(OPTION_OUTPUT_DIR);
        if (outDir != null && !outDir.isEmpty()) {
            Path dir = Paths.get(outDir);
            Files.createDirectories(dir);
            Files.writeString(dir.resolve(fileName), content);
            return;
        }
        try (Writer writer = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", fileName).openWriter()) { writer.write(content); }
    }

    private void error(String msg, Element element) { processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, msg, element); }
    private String sanitize(String s) { return s.replaceAll("[^A-Za-z0-9_]", "_"); }
    private String escape(String s) { return s.replace("\\", "\\\\").replace("\"", "\\\""); }

    private record CharonData(TypeElement boatClass, ExecutableElement handlerMethod, String targetFQN, TypeElement targetClassElement, ExecutableElement targetMethodElement, CharonEvent eventAnnotation, CharonDist dist, int priority, boolean requiresContext) {}
}