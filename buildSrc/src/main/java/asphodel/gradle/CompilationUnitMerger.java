package asphodel.gradle;

import com.github.javaparser.Range;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.InitializerDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.expr.AnnotationExpr;
import org.gradle.api.GradleException;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public final class CompilationUnitMerger {

    private static final Pattern EXCLUDE_PATTERN = Pattern.compile("asphodel::exclude\\b");

    public CompilationUnit merge(SourceFile primarySourceFile, List<SourceFile> contributionSourceFiles) {
        CompilationUnit mergedUnit = new CompilationUnit();
        primarySourceFile.unit().getPackageDeclaration().ifPresent(packageDeclaration -> mergedUnit.setPackageDeclaration(packageDeclaration.clone()));
        mergedUnit.setImports(mergeImports(primarySourceFile, contributionSourceFiles));

        NodeList<TypeDeclaration<?>> mergedTypes = new NodeList<>();
        for (TypeDeclaration<?> primaryType : primarySourceFile.unit().getTypes()) mergedTypes.add(mergeType(primaryType, contributionSourceFiles));
        mergedUnit.setTypes(mergedTypes);
        stripComments(mergedUnit);
        return mergedUnit;
    }

    private void stripComments(CompilationUnit compilationUnit) {
        new ArrayList<>(compilationUnit.getAllContainedComments()).forEach(Comment::remove);
        compilationUnit.getComment().ifPresent(Comment::remove);
    }

    private TypeDeclaration<?> mergeType(TypeDeclaration<?> primaryType, List<SourceFile> contributionSourceFiles) {
        TypeDeclaration<?> mergedType = primaryType.clone();

        Map<String, AnnotationExpr> mergedAnnotations = new LinkedHashMap<>();
        for (AnnotationExpr annotation : mergedType.getAnnotations()) {
            mergedAnnotations.putIfAbsent(annotation.getNameAsString(), annotation);
        }
        for (SourceFile contributionSourceFile : contributionSourceFiles) {
            if (contributionSourceFile.unit().getTypes().isEmpty()) continue;
            TypeDeclaration<?> contributionType = contributionSourceFile.unit().getTypes().get(0);
            if (isExcluded(contributionType, contributionSourceFile.sourceLines())) continue;

            for (AnnotationExpr annotation : contributionType.getAnnotations()) mergedAnnotations.putIfAbsent(annotation.getNameAsString(), annotation.clone());
        }
        mergedType.setAnnotations(new NodeList<>(mergedAnnotations.values()));

        Map<String, BodyDeclaration<?>> mergedMembers = new LinkedHashMap<>();
        Map<String, String> memberSignatures = new LinkedHashMap<>();
        for (BodyDeclaration<?> primaryMember : mergedType.getMembers())
            registerMember(mergedMembers, memberSignatures, primaryMember, false);

        for (SourceFile contributionSourceFile : contributionSourceFiles) {
            if (contributionSourceFile.unit().getTypes().isEmpty()) continue;

            TypeDeclaration<?> contributionType = contributionSourceFile.unit().getTypes().get(0);

            if (isExcluded(contributionType, contributionSourceFile.sourceLines())) continue;

            for (BodyDeclaration<?> contributionMember : contributionType.getMembers()) {
                if (isExcluded(contributionMember, contributionSourceFile.sourceLines())) continue;
                if (contributionMember instanceof ConstructorDeclaration contributionConstructor) {
                    ConstructorDeclaration renamedConstructor = contributionConstructor.clone();
                    renamedConstructor.setName(mergedType.getNameAsString());
                    registerMember(mergedMembers, memberSignatures, renamedConstructor, true);
                } else {
                    registerMember(mergedMembers, memberSignatures, contributionMember, true);
                }
            }
        }
        mergedType.setMembers(new NodeList<>(mergedMembers.values()));
        return mergedType;
    }

    private static boolean isExcluded(BodyDeclaration<?> memberDeclaration, List<String> sourceLines) {
        Optional<Comment> attachedComment = memberDeclaration.getComment();
        if (attachedComment.isPresent() && EXCLUDE_PATTERN.matcher(attachedComment.get().getContent()).find()) return true;
        if (sourceLines == null) return false;
        Optional<Range> memberRange = memberDeclaration.getRange();
        if (memberRange.isEmpty()) return false;

        int beginLineNumber = memberRange.get().begin.line;
        int endLineNumber = memberRange.get().end.line;

        int previousLineIndex = beginLineNumber - 2;
        if (previousLineIndex >= 0 && previousLineIndex < sourceLines.size()) {
            String previousLine = sourceLines.get(previousLineIndex).strip();
            if ((previousLine.startsWith("//") || previousLine.startsWith("/*")) && EXCLUDE_PATTERN.matcher(previousLine).find()) return true;
        }

        int endLineIndex = endLineNumber - 1;
        if (endLineIndex >= 0 && endLineIndex < sourceLines.size()) {
            String endLine = sourceLines.get(endLineIndex);
            return EXCLUDE_PATTERN.matcher(endLine).find();
        }

        return false;
    }

    private void registerMember(Map<String, BodyDeclaration<?>> mergedMembers, Map<String, String> memberSignatures, BodyDeclaration<?> memberDeclaration, boolean overrideExisting) {
        MemberKey memberKey = keyOf(memberDeclaration);
        String existingSignature = memberSignatures.get(memberKey.key());
        if (existingSignature != null && !existingSignature.equals(memberKey.signature())) {
            throw new GradleException("Conflicting declarations for '" + memberKey.key() + "': " + existingSignature + " vs " + memberKey.signature());
        }
        if (overrideExisting || !mergedMembers.containsKey(memberKey.key())) {
            mergedMembers.put(memberKey.key(), memberDeclaration.clone());
            memberSignatures.put(memberKey.key(), memberKey.signature());
        }
    }

    private MemberKey keyOf(BodyDeclaration<?> memberDeclaration) {
        return switch (memberDeclaration) {
            case FieldDeclaration fieldDeclaration -> {
                List<VariableDeclarator> variables = fieldDeclaration.getVariables();
                if (variables.size() == 1) {
                    VariableDeclarator singleVariable = variables.get(0);
                    yield new MemberKey("field:" + singleVariable.getNameAsString(), singleVariable.getType().asString());
                }
                StringBuilder combinedKey = new StringBuilder("field-multi:");
                StringBuilder combinedSignature = new StringBuilder();
                for (int variableIndex = 0; variableIndex < variables.size(); variableIndex++) {
                    VariableDeclarator variable = variables.get(variableIndex);
                    if (variableIndex > 0) {
                        combinedKey.append(',');
                        combinedSignature.append(';');
                    }
                    combinedKey.append(variable.getNameAsString());
                    combinedSignature.append(variable.getType().asString());
                }
                yield new MemberKey(combinedKey.toString(), combinedSignature.toString());
            }
            case MethodDeclaration methodDeclaration -> new MemberKey(
                    "method:" + methodDeclaration.getNameAsString()
                            + parameterSignature(methodDeclaration.getParameters()),
                    methodDeclaration.getType().asString());
            case ConstructorDeclaration constructorDeclaration -> new MemberKey(
                    "ctor:" + parameterSignature(constructorDeclaration.getParameters()),
                    "<ctor>");
            case InitializerDeclaration initializerDeclaration -> new MemberKey(
                    initializerDeclaration.isStatic() ? "init:static" : "init:instance",
                    initializerDeclaration.isStatic() ? "static" : "instance");
            case TypeDeclaration<?> nestedType -> new MemberKey("type:" + nestedType.getNameAsString(), nestedType.getNameAsString());
            default -> throw new GradleException("Unsupported member: " + memberDeclaration.getClass().getSimpleName());
        };
    }

    private static String parameterSignature(NodeList<Parameter> parameters) {
        StringBuilder signatureBuilder = new StringBuilder("(");
        for (int parameterIndex = 0; parameterIndex < parameters.size(); parameterIndex++) {
            if (parameterIndex > 0) signatureBuilder.append(',');
            signatureBuilder.append(parameters.get(parameterIndex).getType().asString());
        }
        return signatureBuilder.append(')').toString();
    }

    private NodeList<ImportDeclaration> mergeImports(SourceFile primarySourceFile, List<SourceFile> contributionSourceFiles) {
        Map<String, ImportDeclaration> typeImports = new LinkedHashMap<>();
        Map<String, File> typeImportOrigins = new LinkedHashMap<>();
        Map<String, ImportDeclaration> staticImports = new LinkedHashMap<>();
        Map<String, ImportDeclaration> starImports = new LinkedHashMap<>();

        addImports(primarySourceFile, typeImports, typeImportOrigins, staticImports, starImports);
        for (SourceFile contributionSourceFile : contributionSourceFiles)
            addImports(contributionSourceFile, typeImports, typeImportOrigins, staticImports, starImports);

        NodeList<ImportDeclaration> mergedImports = new NodeList<>();
        mergedImports.addAll(typeImports.values());
        mergedImports.addAll(staticImports.values());
        mergedImports.addAll(starImports.values());
        return mergedImports;
    }

    private void addImports(SourceFile sourceFile, Map<String, ImportDeclaration> typeImports, Map<String, File> typeImportOrigins, Map<String, ImportDeclaration> staticImports, Map<String, ImportDeclaration> starImports) {
        File sourceFileHandle = sourceFile.path().toFile();
        for (ImportDeclaration importDeclaration : sourceFile.unit().getImports()) {

            if (importDeclaration.isStatic()) {
                staticImports.putIfAbsent(importDeclaration.getNameAsString(), importDeclaration.clone());
            } else if (importDeclaration.isAsterisk()) {
                starImports.putIfAbsent(importDeclaration.getNameAsString(), importDeclaration.clone());
            } else {
                String simpleName = importDeclaration.getName().getIdentifier();
                String qualifiedName = importDeclaration.getNameAsString();
                ImportDeclaration existingImport = typeImports.get(simpleName);
                if (existingImport == null) {
                    typeImports.put(simpleName, importDeclaration.clone());
                    typeImportOrigins.put(simpleName, sourceFileHandle);
                } else if (!existingImport.getNameAsString().equals(qualifiedName)) {
                    throw new GradleException("Import conflict for simple name '" + simpleName + "': "
                            + existingImport.getNameAsString() + " (from "
                            + typeImportOrigins.get(simpleName) + ") vs "
                            + qualifiedName + " (from " + sourceFileHandle + ")");
                }
            }
        }
    }

    private record MemberKey(String key, String signature) {}

    public record SourceFile(Path path, CompilationUnit unit, String patchedSource) {
        public List<String> sourceLines() {
            if (patchedSource == null) return null;
            return Arrays.asList(patchedSource.split("\n", -1));
        }
    }
}