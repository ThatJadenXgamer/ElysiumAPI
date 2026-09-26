package asphodel.gradle;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public final class CompilationUnitMerger {

    private static final Pattern EXCLUDE_PATTERN = Pattern.compile("asphodel::exclude\\b");

    public CompilationUnit merge(SourceFile primary, List<SourceFile> contributions) {
        CompilationUnit out = new CompilationUnit();
        primary.unit().getPackageDeclaration().ifPresent(pd -> out.setPackageDeclaration(pd.clone()));
        out.setImports(mergeImports(primary, contributions));

        List<TypeDeclaration<?>> contributionTypes = new ArrayList<>();
        for (SourceFile c : contributions) {
            if (!c.unit().getTypes().isEmpty()) contributionTypes.add(c.unit().getTypes().get(0));
        }

        NodeList<TypeDeclaration<?>> mergedTypes = new NodeList<>();
        for (TypeDeclaration<?> t : primary.unit().getTypes()) mergedTypes.add(mergeType(t, contributionTypes));
        out.setTypes(mergedTypes);
        return out;
    }

    private TypeDeclaration<?> mergeType(TypeDeclaration<?> primary, List<TypeDeclaration<?>> contributions) {
        TypeDeclaration<?> out = primary.clone();

        Map<String, AnnotationExpr> annotations = new LinkedHashMap<>();
        for (AnnotationExpr a : out.getAnnotations()) annotations.putIfAbsent(a.getNameAsString(), a);
        for (TypeDeclaration<?> c : contributions) {
            if (isExcluded(c)) continue;
            for (AnnotationExpr a : c.getAnnotations()) annotations.putIfAbsent(a.getNameAsString(), a.clone());
        }
        out.setAnnotations(new NodeList<>(annotations.values()));

        Map<String, BodyDeclaration<?>> members = new LinkedHashMap<>();
        Map<String, String> signatures = new LinkedHashMap<>();
        for (BodyDeclaration<?> m : out.getMembers()) registerMember(members, signatures, m, false);
        for (TypeDeclaration<?> c : contributions) {
            if (isExcluded(c)) continue;
            for (BodyDeclaration<?> m : c.getMembers()) {
                if (isExcluded(m)) continue;
                registerMember(members, signatures, m, true);
            }
        }
        out.setMembers(new NodeList<>(members.values()));
        return out;
    }

    private static boolean isExcluded(BodyDeclaration<?> member) {
        Optional<Comment> comment = member.getComment();
        return comment.isPresent() && EXCLUDE_PATTERN.matcher(comment.get().getContent()).find();
    }

    private void registerMember(Map<String, BodyDeclaration<?>> members, Map<String, String> signatures, BodyDeclaration<?> member, boolean override) {
        MemberKey mk = keyOf(member);
        String existing = signatures.get(mk.key());
        if (existing != null && !existing.equals(mk.signature())) {
            throw new GradleException("Conflicting declarations for '" + mk.key() + "': " + existing + " vs " + mk.signature());
        }
        if (override || !members.containsKey(mk.key())) {
            members.put(mk.key(), member.clone());
            signatures.put(mk.key(), mk.signature());
        }
    }

    private MemberKey keyOf(BodyDeclaration<?> member) {
        return switch (member) {
            case FieldDeclaration field -> {
                List<VariableDeclarator> vars = field.getVariables();
                if (vars.size() == 1) {
                    VariableDeclarator v = vars.get(0);
                    yield new MemberKey("field:" + v.getNameAsString(), v.getType().asString());
                }
                StringBuilder key = new StringBuilder("field-multi:");
                StringBuilder sig = new StringBuilder();
                for (int i = 0; i < vars.size(); i++) {
                    VariableDeclarator v = vars.get(i);
                    if (i > 0) {
                        key.append(',');
                        sig.append(';');
                    }
                    key.append(v.getNameAsString());
                    sig.append(v.getType().asString());
                }
                yield new MemberKey(key.toString(), sig.toString());
            }
            case MethodDeclaration m -> new MemberKey(
                    "method:" + m.getNameAsString() + parameterSignature(m.getParameters()),
                    m.getType().asString());
            case ConstructorDeclaration c -> new MemberKey(
                    "ctor:" + parameterSignature(c.getParameters()),
                    c.getNameAsString());
            case InitializerDeclaration i -> new MemberKey(
                    i.isStatic() ? "init:static" : "init:instance",
                    i.isStatic() ? "static" : "instance");
            case TypeDeclaration<?> t -> new MemberKey(
                    "type:" + t.getNameAsString(),
                    t.getNameAsString());
            default -> throw new GradleException(
                    "Unsupported member: " + member.getClass().getSimpleName());
        };
    }

    private static String parameterSignature(NodeList<Parameter> parameters) {
        StringBuilder builder = new StringBuilder("(");
        for (int i = 0; i < parameters.size(); i++) {
            if (i > 0) builder.append(',');
            builder.append(parameters.get(i).getType().asString());
        }
        return builder.append(')').toString();
    }

    private NodeList<ImportDeclaration> mergeImports(SourceFile primary, List<SourceFile> contributions) {
        Map<String, ImportDeclaration> typeImports = new LinkedHashMap<>();
        Map<String, File> typeImportSources = new LinkedHashMap<>();
        Map<String, ImportDeclaration> staticImports = new LinkedHashMap<>();
        Map<String, ImportDeclaration> starImports = new LinkedHashMap<>();

        addImports(primary, typeImports, typeImportSources, staticImports, starImports);
        for (SourceFile c : contributions) addImports(c, typeImports, typeImportSources, staticImports, starImports);

        NodeList<ImportDeclaration> out = new NodeList<>();
        out.addAll(typeImports.values());
        out.addAll(staticImports.values());
        out.addAll(starImports.values());
        return out;
    }

    private void addImports(SourceFile source, Map<String, ImportDeclaration> typeImports, Map<String, File> typeImportSources,
                            Map<String, ImportDeclaration> staticImports, Map<String, ImportDeclaration> starImports) {
        File file = source.path().toFile();
        for (ImportDeclaration declaration : source.unit().getImports()) {
            if (declaration.isStatic()) {
                staticImports.putIfAbsent(declaration.getNameAsString(), declaration.clone());
            } else if (declaration.isAsterisk()) {
                starImports.putIfAbsent(declaration.getNameAsString(), declaration.clone());
            } else {
                String simple = declaration.getName().getIdentifier();
                String qualified = declaration.getNameAsString();
                ImportDeclaration existing = typeImports.get(simple);
                if (existing == null) {
                    typeImports.put(simple, declaration.clone());
                    typeImportSources.put(simple, file);
                } else if (!existing.getNameAsString().equals(qualified)) {
                    throw new GradleException(
                            "Import conflict for simple name '" + simple + "': "
                                    + existing.getNameAsString() + " (from " + typeImportSources.get(simple) + ") vs "
                                    + qualified + " (from " + file + ")");
                }
            }
        }
    }

    private record MemberKey(String key, String signature) {}

    public record SourceFile(Path path, CompilationUnit unit, List<MergeDirective> directives, String fqn, String rawSource) {}
}