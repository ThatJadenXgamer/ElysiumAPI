package asphodel.gradle.access;

import org.gradle.api.GradleException;
import org.gradle.api.logging.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public final class AccessAgentProcessor {

    private static final String ACCESS_AGENT_EXTENSION = ".accessagent";
    private static final String ACCESS_WIDENER_SUFFIX = ".accesswidener";
    private static final String ACCESS_TRANSFORMER_SUFFIX = ".accesstransformer.cfg";
    private static final String META_INF_DIRECTORY = "META-INF";
    private static final String GENERATED_BEGIN_MARKER = "# asphodel-generated-begin";
    private static final String GENERATED_END_MARKER = "# asphodel-generated-end";
    private static final String ACCESS_WIDENER_HEADER = "accessWidener\tv1\tnamed";
    private static final String MERGED_ACCESS_WIDENER_NAME = "merged.accesswidener";
    private static final String MERGED_ACCESS_TRANSFORMER_NAME = "merged.accesstransformer.cfg";
    private static final String VANILLA_LOADER = "vanilla";
    private static final String FABRIC_LOADER = "fabric";
    private static final String FORGE_LOADER = "forge";
    private static final String NEOFORGE_LOADER = "neoforge";

    private AccessAgentProcessor() {
    }

    public static void process(Path projectBasePath, String targetLoader, String targetVersion,
                               Path generatedAccessDirectory, Logger logger) {
        boolean usesAccessWidener = targetLoader.equals(VANILLA_LOADER) || targetLoader.equals(FABRIC_LOADER);
        boolean usesAccessTransformer = targetLoader.equals(FORGE_LOADER) || targetLoader.equals(NEOFORGE_LOADER);

        if (!usesAccessWidener && !usesAccessTransformer) {
            logger.warn("Asphodel Access Agent: unrecognized loader '{}'; nothing will be emitted", targetLoader);
            return;
        }

        Path accessAgentDirectory = projectBasePath.resolve("src/main/accessagent");
        Set<String> baseNames = collectAccessAgentNames(accessAgentDirectory);

        for (String baseName : baseNames) {
            List<AccessDirective> allDirectives = parse(accessAgentDirectory.resolve(baseName), logger);
            List<AccessDirective> matching = dedupe(allDirectives.stream()
                    .filter(directive -> directive.matches(targetLoader, targetVersion))
                    .toList());

            if (usesAccessWidener) routeAccessWidener(projectBasePath, targetLoader, targetVersion, baseName, matching, logger);
            else routeAccessTransformer(projectBasePath, targetLoader, targetVersion, baseName, matching, logger);
        }

        mergeAll(projectBasePath, targetLoader, targetVersion, generatedAccessDirectory, usesAccessWidener, logger);
    }

    private static void routeAccessWidener(Path projectBasePath, String targetLoader, String targetVersion,
                                           String baseName, List<AccessDirective> matching, Logger logger) {
        List<AccessDirective> universal = matching.stream().filter(AccessDirective::hasNoConditions).toList();
        List<AccessDirective> versionScoped = matching.stream().filter(d -> !d.hasLoaderCondition()).toList();
        String fileName = baseName + ACCESS_WIDENER_SUFFIX;

        writeTargetFile(projectBasePath.resolve("src/main/resources").resolve(fileName), universal, true, logger);
        writeTargetFile(projectBasePath.resolve("src/overlays/" + targetVersion + "/resources").resolve(fileName), versionScoped, true, logger);
        if (!targetLoader.equals(VANILLA_LOADER)) {
            writeTargetFile(projectBasePath.resolve("src/overlays/" + targetVersion + "-" + targetLoader + "/resources").resolve(fileName), matching, true, logger);
        }
    }

    private static void routeAccessTransformer(Path projectBasePath, String targetLoader, String targetVersion,
                                               String baseName, List<AccessDirective> matching, Logger logger) {
        Path targetFile = projectBasePath
                .resolve("src/overlays/" + targetVersion + "-" + targetLoader + "/resources")
                .resolve(META_INF_DIRECTORY)
                .resolve(baseName + ACCESS_TRANSFORMER_SUFFIX);
        writeTargetFile(targetFile, matching, false, logger);
    }

    private static Set<String> collectAccessAgentNames(Path directory) {
        Set<String> names = new LinkedHashSet<>();
        if (!Files.isDirectory(directory)) return names;
        try (Stream<Path> stream = Files.list(directory)) {
            stream.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(ACCESS_AGENT_EXTENSION))
                    .forEach(names::add);
        } catch (IOException listException) {
            throw new GradleException("Asphodel Access Agent: failed to list " + directory + ": " + listException.getMessage(), listException);
        }
        return names;
    }

    private static List<AccessDirective> parse(Path file, Logger logger) {
        List<AccessDirective> directives = new ArrayList<>();
        List<String> lines = readAllLinesOrEmpty(file);
        for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
            String raw = lines.get(lineIndex).strip();
            if (raw.isEmpty() || raw.startsWith("#")) continue;

            AccessDirective directive = parseLine(raw);
            if (directive == null) {
                logger.warn("Asphodel Access Agent: malformed entry in {}:{}: {}", file, lineIndex + 1, raw);
                continue;
            }
            directives.add(directive);
        }
        return directives;
    }

    private static AccessDirective parseLine(String raw) {
        int firstHash = raw.indexOf('#');
        String directivePart = firstHash < 0 ? raw : raw.substring(0, firstHash).strip();
        String conditionPart = firstHash < 0 ? null : raw.substring(firstHash + 1).strip();
        if (conditionPart != null) {
            int secondHash = conditionPart.indexOf('#');
            if (secondHash >= 0) conditionPart = conditionPart.substring(0, secondHash).strip();
        }

        AccessDirective base = parseDirectivePart(directivePart);
        if (base == null) return null;

        Map<String, String> conditions = parseConditions(conditionPart);
        return new AccessDirective(base.kind(), base.targetType(), base.fqn(), base.memberName(), base.descriptor(),
                conditions == null ? null : conditions.get("loader"),
                conditions == null ? null : conditions.get("version"));
    }

    private static AccessDirective parseDirectivePart(String directivePart) {
        if (directivePart.isEmpty()) return null;

        String[] tokens = directivePart.split("\\s+");
        if (tokens.length < 3) return null;

        Set<AccessDirective.Kind> kinds = new LinkedHashSet<>();
        int index = 0;
        while (index < tokens.length) {
            AccessDirective.Kind kind = AccessDirective.Kind.fromWord(tokens[index]);
            if (kind == null) break;
            kinds.add(kind);
            index++;
        }
        if (kinds.isEmpty() || index >= tokens.length) return null;

        AccessDirective.TargetType targetType = AccessDirective.TargetType.fromWord(tokens[index]);
        if (targetType == null) return null;
        index++;

        if (index >= tokens.length) return null;
        String fqn = tokens[index++].replace('/', '.');

        String memberName = null;
        String descriptor = null;
        if (targetType != AccessDirective.TargetType.CLASS) {
            if (index + 1 >= tokens.length) return null;
            memberName = tokens[index++];
            descriptor = tokens[index++];
        }
        if (index != tokens.length) return null;

        AccessDirective.Kind canonicalKind = canonicalize(kinds, targetType);
        if (canonicalKind == null) return null;

        return new AccessDirective(canonicalKind, targetType, fqn, memberName, descriptor, null, null);
    }

    private static Map<String, String> parseConditions(String conditionPart) {
        if (conditionPart == null || conditionPart.isEmpty()) return null;

        Map<String, String> conditions = new LinkedHashMap<>();
        for (String token : conditionPart.split(",")) {
            String trimmed = token.strip();
            if (trimmed.isEmpty()) continue;

            int equalsIndex = trimmed.indexOf('=');
            if (equalsIndex <= 0 || equalsIndex >= trimmed.length() - 1) return null;

            String key = trimmed.substring(0, equalsIndex).strip().toLowerCase();
            String value = trimmed.substring(equalsIndex + 1).strip();
            if (value.isEmpty() || value.contains(" ")) return null;
            if (!key.equals("loader") && !key.equals("version")) return null;
            if (!value.equals("*")) conditions.put(key, value);
        }

        return conditions.isEmpty() ? null : conditions;
    }

    private static AccessDirective.Kind canonicalize(Set<AccessDirective.Kind> kinds, AccessDirective.TargetType targetType) {
        for (AccessDirective.Kind kind : kinds) {
            if (!kind.validFor(targetType)) return null;
        }
        if (kinds.contains(AccessDirective.Kind.EXTENDABLE)) return AccessDirective.Kind.EXTENDABLE;
        if (kinds.contains(AccessDirective.Kind.MUTABLE)) return AccessDirective.Kind.MUTABLE;
        return kinds.contains(AccessDirective.Kind.ACCESSIBLE) ? AccessDirective.Kind.ACCESSIBLE : null;
    }

    private static List<AccessDirective> dedupe(List<AccessDirective> directives) {
        Map<String, AccessDirective> byKey = new LinkedHashMap<>();
        for (AccessDirective directive : directives) {
            byKey.merge(directive.dedupKey(), directive, (existing, replacement) ->
                    existing.strength() >= replacement.strength() ? existing : replacement);
        }
        return new ArrayList<>(byKey.values());
    }

    private static void writeTargetFile(Path targetFile, List<AccessDirective> directives, boolean useAccessWidener, Logger logger) {
        MarkerSplit split = splitOnMarker(readAllLinesOrEmpty(targetFile), targetFile, logger);
        List<String> before = removeAccessWidenerHeader(split.before(), useAccessWidener);

        StringBuilder builder = new StringBuilder();
        if (useAccessWidener) builder.append(ACCESS_WIDENER_HEADER).append('\n');
        for (String line : before) builder.append(line).append('\n');
        if (!directives.isEmpty()) {
            builder.append(GENERATED_BEGIN_MARKER).append('\n');
            for (AccessDirective directive : directives) {
                builder.append(useAccessWidener ? directive.toAccessWidenerLine() : directive.toAccessTransformerLine()).append('\n');
            }
            builder.append(GENERATED_END_MARKER).append('\n');
        }
        for (String line : split.after()) builder.append(line).append('\n');

        writeIfChanged(targetFile, normalizeTrailing(builder.toString()));
    }

    private static List<String> removeAccessWidenerHeader(List<String> lines, boolean useAccessWidener) {
        if (!useAccessWidener) return lines;
        List<String> filtered = new ArrayList<>(lines.size());
        for (String line : lines) if (!line.strip().startsWith("accessWidener")) filtered.add(line);
        return filtered;
    }

    private record MarkerSplit(List<String> before, List<String> after) {}

    private static MarkerSplit splitOnMarker(List<String> lines, Path targetFile, Logger logger) {
        List<String> before = new ArrayList<>();
        List<String> after = new ArrayList<>();
        boolean inside = false;
        boolean sawBegin = false;
        boolean sawEnd = false;

        for (String line : lines) {
            String stripped = line.strip();
            if (!inside && stripped.equals(GENERATED_BEGIN_MARKER)) { inside = true; sawBegin = true; continue; }
            if (inside && stripped.equals(GENERATED_END_MARKER)) { inside = false; sawEnd = true; continue; }
            if (!inside) {
                if (sawBegin) after.add(line);
                else before.add(line);
            }
        }

        if (sawBegin && !sawEnd) {
            logger.warn("Asphodel Access Agent: '{}' has a malformed asphodel-generated block (begin without end); treating whole file as user content", targetFile);
            return new MarkerSplit(List.copyOf(lines), List.of());
        }
        return new MarkerSplit(List.copyOf(before), List.copyOf(after));
    }

    private static List<String> readAllLinesOrEmpty(Path file) {
        if (!Files.exists(file)) return List.of();
        try {
            return Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException readException) {
            throw new GradleException("Asphodel Access Agent: failed to read " + file + ": " + readException.getMessage(), readException);
        }
    }

    private static String normalizeTrailing(String content) {
        if (content.isEmpty()) return content;
        String result = content;
        while (result.endsWith("\n\n")) result = result.substring(0, result.length() - 1);
        return result.endsWith("\n") ? result : result + "\n";
    }

    private static void writeIfChanged(Path file, String content) {
        try {
            Files.createDirectories(file.getParent());
            String existing = Files.exists(file) ? Files.readString(file) : null;
            if (existing == null || !existing.equals(content)) Files.writeString(file, content);
        } catch (IOException writeException) {
            throw new GradleException("Asphodel Access Agent: failed to write " + file + ": " + writeException.getMessage(), writeException);
        }
    }

    private static void mergeAll(Path projectBasePath, String targetLoader, String targetVersion,
                                 Path generatedAccessDirectory, boolean useAccessWidener, Logger logger) {
        List<Path> scanDirectories = new ArrayList<>();
        String suffix;
        String mergedName;

        if (useAccessWidener) {
            scanDirectories.add(projectBasePath.resolve("src/main/resources"));
            scanDirectories.add(projectBasePath.resolve("src/overlays/" + targetVersion + "/resources"));
            if (!targetLoader.equals(VANILLA_LOADER)) scanDirectories.add(projectBasePath.resolve("src/overlays/" + targetVersion + "-" + targetLoader + "/resources"));
            suffix = ACCESS_WIDENER_SUFFIX;
            mergedName = MERGED_ACCESS_WIDENER_NAME;
        } else {
            scanDirectories.add(projectBasePath.resolve("src/main/resources").resolve(META_INF_DIRECTORY));
            scanDirectories.add(projectBasePath.resolve("src/overlays/" + targetVersion + "/resources").resolve(META_INF_DIRECTORY));
            scanDirectories.add(projectBasePath.resolve("src/overlays/" + targetVersion + "-" + targetLoader + "/resources").resolve(META_INF_DIRECTORY));
            suffix = ".cfg";
            mergedName = MERGED_ACCESS_TRANSFORMER_NAME;
        }

        Set<String> entries = new LinkedHashSet<>();
        for (Path directory : scanDirectories) collectEntriesFrom(directory, suffix, mergedName, useAccessWidener, entries);

        StringBuilder builder = new StringBuilder();
        if (useAccessWidener) builder.append(ACCESS_WIDENER_HEADER).append('\n');
        for (String entry : entries) builder.append(entry).append('\n');

        writeIfChanged(generatedAccessDirectory.resolve(mergedName), builder.toString());
    }

    private static void collectEntriesFrom(Path directory, String suffix, String mergedName,
                                           boolean useAccessWidener, Set<String> entries) {
        if (!Files.isDirectory(directory)) return;
        try (Stream<Path> stream = Files.list(directory)) {
            for (Path file : stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(suffix))
                    .filter(path -> !path.getFileName().toString().equals(mergedName))
                    .toList()) {
                for (String line : readAllLinesOrEmpty(file)) {
                    String stripped = line.strip();
                    if (stripped.isEmpty() || stripped.startsWith("#")) continue;
                    if (useAccessWidener && stripped.startsWith("accessWidener")) continue;
                    entries.add(stripped);
                }
            }
        } catch (IOException listException) {
            throw new GradleException("Asphodel Access Agent: failed to scan " + directory + ": " + listException.getMessage(), listException);
        }
    }
}