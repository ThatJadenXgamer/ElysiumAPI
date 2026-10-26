package asphodel.gradle.transformers;

import asphodel.gradle.directives.PatchDirective;
import org.gradle.api.GradleException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PatchTransformer {

    private static final String ID = "[A-Za-z_][A-Za-z0-9_]*";

    private PatchTransformer() {
    }

    public static PatchDirective parseDirective(String line) {
        String[] directive = asphodelDirective(line);
        if (directive == null || !directive[0].equals("patch")) return null;

        String loader = null;
        String version = null;
        String id = null;
        for (String token : directive[1].replace("->", ",").split(",")) {
            int equalsIndex = token.indexOf('=');
            if (equalsIndex < 0) continue;
            String key = token.substring(0, equalsIndex).trim().toLowerCase();
            String value = token.substring(equalsIndex + 1).trim();
            if (value.isEmpty()) continue;
            switch (key) {
                case "loader" -> loader = value;
                case "version" -> version = value;
                case "id" -> id = value;
            }
        }
        if (id == null || !id.matches(ID)) return null;
        return new PatchDirective(loader, version, id);
    }

    public static Result transform(String rawSource, List<PatchDirective> patchDirectives, String currentLoader, String currentVersion) {
        Set<String> activeIds = new HashSet<>();
        Set<String> knownIds = new HashSet<>();
        List<String> warnings = new ArrayList<>();
        for (PatchDirective directive : patchDirectives) {
            if (!knownIds.add(directive.id())) {
                warnings.add("Duplicate patch id '" + directive.id() + "'; first declaration wins.");
                continue;
            }
            if (directive.matches(currentLoader, currentVersion)) activeIds.add(directive.id());
        }

        String normalized = rawSource.replace("\r\n", "\n").replace('\r', '\n');
        String[] lines = normalized.split("\n", -1);
        StringBuilder output = new StringBuilder(normalized.length());
        List<String> massIds = null;
        int massStartLine = -1;
        boolean inBlockComment = false;

        for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
            String line = lines[lineIndex];
            if (lineIndex > 0) output.append('\n');

            if (asphodelDirective(line) != null) continue;

            MassAction mass = massAction(line);
            if (mass != null) {
                if (mass.play()) {
                    if (massIds != null) throw new GradleException("Nested mass play directive for id(s) '" + joinIds(mass.ids())
                            + "' at line " + (lineIndex + 1)
                            + " while mass id(s) '" + joinIds(massIds)
                            + "' (opened at line " + massStartLine + ") was still active");
                    for (String id : mass.ids()) {
                        if (!knownIds.contains(id)) warnings.add("Unknown patch id '" + id
                                + "' referenced in mass play marker at line " + (lineIndex + 1)
                                + "; content will be dropped on all configurations.");
                    }
                    massIds = mass.ids();
                    massStartLine = lineIndex + 1;
                } else {
                    if (massIds == null) throw new GradleException("Mass pause directive for id(s) '" + joinIds(mass.ids())
                            + "' at line " + (lineIndex + 1)
                            + " without a matching mass play directive");
                    if (!new HashSet<>(massIds).equals(new HashSet<>(mass.ids()))) throw new GradleException("Mass pause directive for id(s) '" + joinIds(mass.ids())
                            + "' at line " + (lineIndex + 1)
                            + " while mass id(s) '" + joinIds(massIds)
                            + "' (opened at line " + massStartLine + ") was active");
                    if (inBlockComment) throw new GradleException("Mass pause directive for id(s) '" + joinIds(mass.ids())
                            + "' at line " + (lineIndex + 1)
                            + " while a block comment was still open");
                    massIds = null;
                    massStartLine = -1;
                }
                continue;
            }

            if (massIds != null) {
                boolean active = anyActive(massIds, activeIds);

                if (inBlockComment) {
                    String trimmedLine = line.stripTrailing();
                    if (trimmedLine.endsWith("*/")) {
                        String content = trimmedLine.substring(0, trimmedLine.length() - 2).stripTrailing();
                        if (active && !content.isBlank()) output.append(content);
                        inBlockComment = false;
                        continue;
                    }
                    String rest = line.stripLeading();
                    if (rest.startsWith("*")) {
                        if (active) {
                            output.append(line, 0, line.length() - rest.length());
                            output.append(stripOneLeadingSpace(rest.substring(1)));
                        }
                    } else if (active) output.append(line);
                    continue;
                }

                String rest = line.stripLeading();
                int indentLength = line.length() - rest.length();

                if (rest.startsWith("//")) {
                    if (active) {
                        output.append(line, 0, indentLength);
                        output.append(stripOneLeadingSpace(rest.substring(2)));
                    }
                    continue;
                }

                if (rest.startsWith("/*")) {
                    String after = rest.substring(2);
                    String afterTrailing = after.stripTrailing();
                    if (afterTrailing.endsWith("*/")) {
                        String content = afterTrailing.substring(0, afterTrailing.length() - 2);
                        content = stripOneTrailingSpace(stripOneLeadingSpace(content));
                        if (active) {
                            output.append(line, 0, indentLength);
                            output.append(content);
                        }
                    } else {
                        inBlockComment = true;
                        if (active) {
                            output.append(line, 0, indentLength);
                            output.append(stripOneLeadingSpace(after));
                        }
                    }
                    continue;
                }
            }

            String strippedLine = line.strip();
            if (strippedLine.isEmpty()) {
                output.append(line);
                continue;
            }

            boolean isCommentLine = strippedLine.startsWith("//") || strippedLine.startsWith("/*");

            if (isCommentLine) {
                if (endsWithRemoveMarker(strippedLine)) {
                    output.append(line);
                    continue;
                }
                IncludeMarker include = parseIncludeMarker(line);
                if (include != null) {
                    List<String> ids = include.ids();
                    boolean anyUnknown = false;
                    for (String id : ids) {
                        if (!knownIds.contains(id)) {
                            warnings.add("Unknown patch id '" + id + "' referenced in include marker; dropping line.");
                            anyUnknown = true;
                        }
                    }
                    if (anyUnknown) continue;
                    if (anyActive(ids, activeIds)) output.append(include.code());
                    continue;
                }
                output.append(line);
                continue;
            }

            RemoveMarker remove = parseRemoveMarker(line);
            if (remove != null) {
                List<String> ids = remove.ids();
                boolean anyUnknown = false;
                for (String id : ids) {
                    if (!knownIds.contains(id)) {
                        warnings.add("Unknown patch id '" + id + "' referenced in remove marker; leaving line unchanged.");
                        anyUnknown = true;
                    }
                }
                if (anyUnknown) {
                    output.append(line);
                    continue;
                }
                if (anyActive(ids, activeIds)) continue;
                output.append(remove.code());
                continue;
            }

            output.append(line);
        }

        if (inBlockComment) throw new GradleException("Block comment was never closed inside the last mass block");
        if (massIds != null) throw new GradleException("Mass play directive for id(s) '" + joinIds(massIds)
                + "' (opened at line " + massStartLine + ") was never paused");

        return new Result(output.toString(), warnings);
    }

    private static String joinIds(List<String> ids) {
        return String.join(" && ", ids);
    }

    private static boolean anyActive(List<String> ids, Set<String> activeIds) {
        for (String id : ids) if (activeIds.contains(id)) return true;
        return false;
    }

    private static String[] asphodelDirective(String line) {
        String trimmed = line.stripLeading();
        if (!trimmed.startsWith("//")) return null;
        String body = trimmed.substring(2).stripLeading();
        if (!body.startsWith("asphodel::")) return null;
        body = body.substring("asphodel::".length()).stripLeading();

        for (String keyword : new String[]{"patch", "merge", "mixin", "eject"}) {
            if (body.startsWith(keyword)) {
                String rest = body.substring(keyword.length()).stripLeading();
                if (rest.startsWith("->")) return new String[]{keyword, rest.substring(2).strip()};
            }
        }
        return null;
    }

    private static MassAction massAction(String line) {
        String trimmed = line.stripLeading();
        if (!trimmed.startsWith("//")) return null;
        String body = trimmed.substring(2).stripLeading();
        if (!body.startsWith("mass*")) return null;
        body = body.substring("mass*".length()).strip();

        int lastSpace = body.lastIndexOf(' ');
        if (lastSpace < 0) return null;
        String action = body.substring(lastSpace + 1);
        boolean play;
        if (action.equals("play")) play = true;
        else if (action.equals("pause")) play = false;
        else return null;

        String idPart = body.substring(0, lastSpace);
        List<String> ids = parseIdList(idPart);
        if (ids == null) return null;
        return new MassAction(ids, play);
    }

    private static boolean endsWithRemoveMarker(String line) {
        String trimmed = line.stripTrailing();
        int removeIndex = trimmed.lastIndexOf("remove*");
        if (removeIndex < 0) return false;
        String idList = trimmed.substring(removeIndex + "remove*".length());
        return parseIdList(idList) != null;
    }

    private static IncludeMarker parseIncludeMarker(String line) {
        String trimmed = line.stripTrailing();
        if (trimmed.endsWith("*/")) trimmed = trimmed.substring(0, trimmed.length() - 2).stripTrailing();

        int starIndex = trimmed.lastIndexOf('*');
        if (starIndex < 0) return null;
        String idList = trimmed.substring(starIndex + 1);
        List<String> ids = parseIdList(idList);
        if (ids == null) return null;

        String prefix = trimmed.substring(0, starIndex).stripTrailing();
        String prefixLeading = prefix.stripLeading();
        int indentLength = prefix.length() - prefixLeading.length();
        String indent = prefix.substring(0, indentLength);

        if (!prefixLeading.startsWith("//") && !prefixLeading.startsWith("/*")) return null;
        String content = stripOneLeadingSpace(prefixLeading.substring(2));

        return new IncludeMarker(indent + content, ids);
    }

    private static RemoveMarker parseRemoveMarker(String line) {
        String trimmed = line.stripTrailing();
        int removeIndex = trimmed.lastIndexOf("remove*");
        if (removeIndex < 0) return null;
        String idList = trimmed.substring(removeIndex + "remove*".length());
        List<String> ids = parseIdList(idList);
        if (ids == null) return null;

        String prefix = trimmed.substring(0, removeIndex);
        String prefixTrimmed = prefix.stripTrailing();

        if (!prefixTrimmed.endsWith("//")) return null;
        String code = prefixTrimmed.substring(0, prefixTrimmed.length() - 2).stripTrailing();
        return new RemoveMarker(code, ids);
    }

    private static List<String> parseIdList(String value) {
        if (value == null || value.isEmpty()) return null;
        String[] parts = value.split("&&");
        if (parts.length == 0) return null;
        List<String> ids = new ArrayList<>(parts.length);
        for (String part : parts) {
            String trimmed = part.strip();
            if (!trimmed.matches(ID)) return null;
            ids.add(trimmed);
        }
        return ids;
    }

    private static String stripOneLeadingSpace(String value) {
        return value.startsWith(" ") ? value.substring(1) : value;
    }

    private static String stripOneTrailingSpace(String value) {
        return value.endsWith(" ") ? value.substring(0, value.length() - 1) : value;
    }

    public record Result(String source, List<String> warnings) {}

    private record RemoveMarker(String code, List<String> ids) {}
    private record IncludeMarker(String code, List<String> ids) {}
    private record MassAction(List<String> ids, boolean play) {}
}