package asphodel.gradle.transformers;

import asphodel.gradle.directives.ImportDirective;

import java.util.ArrayList;
import java.util.List;

public final class ImportTransformer {

    private static final String FQN_PATTERN = "[A-Za-z_][A-Za-z0-9_.]*(?:\\.\\*)?";
    private static final String STATIC_PREFIX = "static ";
    private static final String ADD_MARKER = "// asphodel::import*add->";
    private static final String SUFFIX_MARKER = "// asphodel::import*";

    private ImportTransformer() {
    }

    public static Result transform(String rawSource, String currentLoader, String currentVersion) {
        String normalized = rawSource.replace("\r\n", "\n").replace('\r', '\n');
        String[] lines = normalized.split("\n", -1);
        StringBuilder output = new StringBuilder(normalized.length());
        List<String> warnings = new ArrayList<>();

        for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
            String line = lines[lineIndex];
            if (lineIndex > 0) output.append('\n');

            String trimmed = line.stripLeading();
            String leadingWhitespace = line.substring(0, line.length() - trimmed.length());

            if (trimmed.startsWith(ADD_MARKER)) {
                String restOfIt = trimmed.substring(ADD_MARKER.length()).strip();
                ImportDirective directive = parseConditionsAndTarget(restOfIt, true, false);
                if (directive == null) {
                    warnings.add("Malformed import add directive at line " + (lineIndex + 1) + "; line skipped.");
                    continue;
                }
                if (directive.matches(currentLoader, currentVersion)) output.append(emitImport(leadingWhitespace, directive));
                continue;
            }

            if (!trimmed.startsWith("import ")) {
                output.append(line);
                continue;
            }

            int markerIndex = line.indexOf(SUFFIX_MARKER);
            if (markerIndex < 0) {
                output.append(line);
                continue;
            }

            String codeBeforeMarker = line.substring(0, markerIndex).stripTrailing();
            String afterMarker = line.substring(markerIndex + SUFFIX_MARKER.length());

            int arrowIndex = afterMarker.indexOf("->");
            if (arrowIndex < 0) {
                output.append(line);
                continue;
            }

            String action = afterMarker.substring(0, arrowIndex).strip();
            String rest = afterMarker.substring(arrowIndex + 2).strip();
            boolean originalIsStatic = trimmed.startsWith("import static ");

            if (action.equals("replace")) {
                ImportDirective directive = parseConditionsAndTarget(rest, true, originalIsStatic);
                if (directive == null) {
                    warnings.add("Malformed import replace directive at line " + (lineIndex + 1) + "; line left unchanged.");
                    output.append(line);
                    continue;
                }
                if (directive.matches(currentLoader, currentVersion)) output.append(emitImport(leadingWhitespace, directive));
                else output.append(codeBeforeMarker);

                continue;
            }

            if (action.equals("remove")) {
                ImportDirective directive = parseConditionsAndTarget(rest, false, originalIsStatic);
                if (directive == null) {
                    warnings.add("Malformed import remove directive at line " + (lineIndex + 1) + "; line left unchanged.");
                    output.append(line);
                    continue;
                }
                if (!directive.matches(currentLoader, currentVersion)) output.append(codeBeforeMarker);

                continue;
            }

            output.append(line);
        }

        return new Result(output.toString(), warnings);
    }

    private static String emitImport(String indent, ImportDirective directive) {
        StringBuilder builder = new StringBuilder(indent);
        builder.append("import ");
        if (directive.isStatic()) builder.append("static ");
        builder.append(directive.targetFqn());
        builder.append(';');
        return builder.toString();
    }

    private static ImportDirective parseConditionsAndTarget(String body, boolean hasTarget, boolean originalIsStatic) {
        String conditions;
        String targetFqn = null;
        boolean isStatic = originalIsStatic;

        if (hasTarget) {
            int lastArrowIndex = body.lastIndexOf("->");
            if (lastArrowIndex < 0) return null;
            conditions = body.substring(0, lastArrowIndex).strip();
            String targetText = body.substring(lastArrowIndex + 2).strip();
            if (targetText.startsWith(STATIC_PREFIX)) {
                isStatic = true;
                targetText = targetText.substring(STATIC_PREFIX.length()).strip();
            }
            if (!targetText.matches(FQN_PATTERN)) return null;
            targetFqn = targetText;
        } else conditions = body;

        String loader = null;
        String version = null;
        for (String token : conditions.split(",")) {
            int equalsIndex = token.indexOf('=');
            if (equalsIndex < 0) continue;
            String key = token.substring(0, equalsIndex).trim().toLowerCase();
            String value = token.substring(equalsIndex + 1).trim();
            if (value.isEmpty()) continue;
            switch (key) {
                case "loader" -> loader = value;
                case "version" -> version = value;
            }
        }

        return new ImportDirective(loader, version, targetFqn, isStatic);
    }

    public record Result(String source, List<String> warnings) {}
}