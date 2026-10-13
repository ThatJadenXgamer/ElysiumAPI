package asphodel.gradle;

import asphodel.gradle.directives.MergeDirective;
import asphodel.gradle.directives.PatchDirective;
import asphodel.gradle.transformers.ImportTransformer;
import asphodel.gradle.transformers.PatchTransformer;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.ChangeType;
import org.gradle.work.FileChange;
import org.gradle.work.Incremental;
import org.gradle.work.InputChanges;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@CacheableTask
public abstract class AssembleSourcesTask extends DefaultTask {

    private static final Pattern MERGE_DIRECTIVE_PATTERN = Pattern.compile(
            "^\\s*//\\s*asphodel::merge\\s*->\\s*(.+?)\\s*$");

    private static final Pattern PACKAGE_PATTERN = Pattern.compile(
            "^\\s*package\\s+([A-Za-z_][A-Za-z0-9_.]*)\\s*;",
            Pattern.MULTILINE);

    private static final Pattern FULLY_QUALIFIED_NAME_PATTERN = Pattern.compile(
            "^[A-Za-z_][A-Za-z0-9_.]*$");

    private static final int STATE_VERSION = 4;

    @InputFiles
    @Incremental
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getJavaSourceDirs();

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getMainResourcesDir();

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getVersionResourcesDir();

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getLoaderResourcesDir();

    @Input
    public abstract Property<String> getTargetLoader();

    @Input
    public abstract Property<String> getTargetVersion();

    @Internal
    public abstract DirectoryProperty getProjectDirectory();

    @OutputDirectory
    public abstract DirectoryProperty getOutputJavaDir();

    @OutputDirectory
    public abstract DirectoryProperty getOutputResourcesDir();

    @OutputFile
    public abstract RegularFileProperty getStateFile();

    @TaskAction
    public void execute(InputChanges inputChanges) throws IOException {
        Path outputJavaDirectory = getOutputJavaDir().get().getAsFile().toPath();
        Path outputResourcesDirectory = getOutputResourcesDir().get().getAsFile().toPath();
        Path stateFilePath = getStateFile().get().getAsFile().toPath();
        Path projectBasePath = getProjectDirectory().get().getAsFile().toPath().toAbsolutePath().normalize();

        SourceState previousState = inputChanges.isIncremental() ? SourceState.load(stateFilePath, projectBasePath) : null;
        SourceState currentState;

        if (previousState == null) {
            clearDirectory(outputJavaDirectory);
            clearDirectory(outputResourcesDirectory);
            Files.createDirectories(outputJavaDirectory);
            Files.createDirectories(outputResourcesDirectory);
            currentState = scanAllSources();
            applyResources(outputResourcesDirectory);
            rebuildAllOutputs(currentState, outputJavaDirectory);
        } else {
            Files.createDirectories(outputJavaDirectory);
            Files.createDirectories(outputResourcesDirectory);
            clearDirectory(outputResourcesDirectory);
            applyResources(outputResourcesDirectory);
            currentState = rebuildIncremental(previousState, outputJavaDirectory, inputChanges);
        }

        currentState.save(stateFilePath, projectBasePath);
    }

    private SourceState scanAllSources() throws IOException {
        SourceState state = new SourceState();
        for (File sourceRoot : getJavaSourceDirs().getFiles()) {
            if (!sourceRoot.isDirectory()) continue;

            Path sourceRootPath = sourceRoot.toPath();
            try (Stream<Path> pathStream = Files.walk(sourceRootPath)) {
                List<Path> javaFiles = pathStream
                        .filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".java")).sorted().toList();
                for (Path javaFile : javaFiles) {
                    SourceEntry entry = scanSourceFile(javaFile);
                    SourceEntry existingEntry = state.get(entry.fqn);
                    if (existingEntry != null && !existingEntry.path.equals(entry.path)) {
                        throw new GradleException("Duplicate class '" + entry.fqn + "' declared in:\n  " + existingEntry.path + "\n  " + javaFile);
                    }
                    state.put(entry);
                }
            }
        }
        return state;
    }

    private SourceEntry scanSourceFile(Path path) throws IOException {
        Path normalized = path.toAbsolutePath().normalize();
        String raw = Files.readString(normalized);
        List<MergeDirective> mergeDirectives = parseMergeDirectives(raw);
        List<PatchDirective> patchDirectives = parsePatchDirectives(raw);
        boolean hasAsphodelMarkers = raw.contains("asphodel::merge")
                || raw.contains("asphodel::patch")
                || raw.contains("asphodel::import");
        Matcher packageMatcher = PACKAGE_PATTERN.matcher(raw);
        String packageName = packageMatcher.find() ? packageMatcher.group(1) : "";
        String fileName = normalized.getFileName().toString();
        String simpleClassName = fileName.substring(0, fileName.length() - ".java".length());
        String fqn = packageName.isEmpty() ? simpleClassName : packageName + "." + simpleClassName;
        return new SourceEntry(normalized, fqn, mergeDirectives, patchDirectives, hasAsphodelMarkers);
    }

    private void rebuildAllOutputs(SourceState state, Path outputJavaDirectory) throws IOException {
        Map<String, List<String>> contributions = resolveContributions(state);
        Set<String> consumedClassNames = collectTargets(contributions, state);
        for (String consumedClassName : consumedClassNames) contributions.remove(consumedClassName);

        Set<String> classesToParse = new HashSet<>(contributions.keySet());
        for (List<String> targetClassNames : contributions.values()) classesToParse.addAll(targetClassNames);
        parseEntries(state, classesToParse);

        CompilationUnitMerger unitMerger = new CompilationUnitMerger();
        for (SourceEntry entry : state.all()) {
            if (consumedClassNames.contains(entry.fqn)) continue;
            writeOutput(unitMerger, state, entry, contributions.get(entry.fqn), outputJavaDirectory);
        }
    }

    private SourceState rebuildIncremental(SourceState previousState, Path outputJavaDirectory, InputChanges inputChanges) throws IOException {
        SourceState currentState = new SourceState();
        for (SourceEntry previousEntry : previousState.all()) {
            currentState.put(new SourceEntry(previousEntry.path, previousEntry.fqn, previousEntry.mergeDirectives, previousEntry.patchDirectives, previousEntry.hasAsphodelMarkers));
        }

        Set<String> changedClassNames = new HashSet<>();
        Set<String> removedClassNames = new HashSet<>();

        for (FileChange fileChange : inputChanges.getFileChanges(getJavaSourceDirs())) {
            File changedFile = fileChange.getFile();
            if (!changedFile.getName().endsWith(".java")) continue;

            Path changedpath = changedFile.toPath().toAbsolutePath().normalize();

            if (fileChange.getChangeType() == ChangeType.REMOVED) {
                SourceEntry removedEntry = currentState.getByPath(changedpath);
                if (removedEntry != null) {
                    currentState.remove(removedEntry.fqn);
                    removedClassNames.add(removedEntry.fqn);
                }
            } else {
                SourceEntry previousEntry = currentState.getByPath(changedpath);
                if (previousEntry != null) {
                    currentState.remove(previousEntry.fqn);
                    changedClassNames.add(previousEntry.fqn);
                }
                SourceEntry newEntry = scanSourceFile(changedpath);
                currentState.put(newEntry);
                changedClassNames.add(newEntry.fqn);
            }
        }

        Map<String, List<String>> previousContributions = resolveContributions(previousState);
        Map<String, List<String>> currentContributions = resolveContributions(currentState);
        Set<String> previousConsumedClassNames = collectTargets(previousContributions, previousState);
        Set<String> currentConsumedClassNames = collectTargets(currentContributions, currentState);

        Set<String> affectedClassNames = new HashSet<>(changedClassNames);
        for (String changedClassName : changedClassNames) {
            affectedClassNames.addAll(dependentsOf(previousContributions, changedClassName));
            affectedClassNames.addAll(dependentsOf(currentContributions, changedClassName));
        }
        for (String removedClassName : removedClassNames) {
            affectedClassNames.addAll(dependentsOf(previousContributions, removedClassName));
            affectedClassNames.addAll(dependentsOf(currentContributions, removedClassName));
        }
        for (String previouslyConsumedClassName : previousConsumedClassNames) {
            if (!currentConsumedClassNames.contains(previouslyConsumedClassName)) affectedClassNames.add(previouslyConsumedClassName);
        }

        Set<String> previousOutputClassNames = new HashSet<>(previousState.fqns());
        previousOutputClassNames.removeAll(previousConsumedClassNames);
        Set<String> currentOutputClassNames = new HashSet<>(currentState.fqns());
        currentOutputClassNames.removeAll(currentConsumedClassNames);

        for (String previousOutputClassName : previousOutputClassNames) {
            if (!currentOutputClassNames.contains(previousOutputClassName)) deleteOutput(outputJavaDirectory, previousOutputClassName);
        }

        for (String currentOutputClassName : currentOutputClassNames) {
            Path destinationFile = outputJavaDirectory.resolve(currentOutputClassName.replace('.', '/') + ".java");
            if (!Files.exists(destinationFile)) affectedClassNames.add(currentOutputClassName);
        }

        Set<String> classesToParse = new HashSet<>();
        for (String affectedClassName : affectedClassNames) {
            List<String> targetClassNames = currentContributions.get(affectedClassName);
            if (targetClassNames != null) {
                classesToParse.add(affectedClassName);
                classesToParse.addAll(targetClassNames);
            }
        }
        parseEntries(currentState, classesToParse);

        CompilationUnitMerger unitMerger = new CompilationUnitMerger();
        for (String affectedClassName : affectedClassNames) {
            if (currentConsumedClassNames.contains(affectedClassName)) continue;
            SourceEntry entry = currentState.get(affectedClassName);
            if (entry == null) continue;
            writeOutput(unitMerger, currentState, entry, currentContributions.get(affectedClassName), outputJavaDirectory);
        }

        return currentState;
    }

    private void writeOutput(CompilationUnitMerger unitMerger, SourceState state, SourceEntry entry,
                             List<String> targetClassNames, Path outputJavaDirectory) throws IOException {
        Path destinationFile = outputJavaDirectory.resolve(entry.fqn.replace('.', '/') + ".java");
        Files.createDirectories(destinationFile.getParent());
        if (targetClassNames == null || targetClassNames.isEmpty()) {
            if (entry.hasAnyDirectives()) {
                ensurePatchedSource(entry);
                Files.writeString(destinationFile, entry.patchedSource);
            } else {
                Files.copy(entry.path, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }
            return;
        }
        ensurePatchedSource(entry);
        parseEntryIfNeeded(entry);
        CompilationUnitMerger.SourceFile primarySourceFile = new CompilationUnitMerger.SourceFile(entry.path, entry.parsedUnit, entry.patchedSource);

        List<CompilationUnitMerger.SourceFile> contributionSourceFiles = new ArrayList<>(targetClassNames.size());
        for (String targetClassName : targetClassNames) {
            SourceEntry targetEntry = state.get(targetClassName);
            ensurePatchedSource(targetEntry);
            parseEntryIfNeeded(targetEntry);
            contributionSourceFiles.add(new CompilationUnitMerger.SourceFile(targetEntry.path, targetEntry.parsedUnit, targetEntry.patchedSource));
        }
        Files.writeString(destinationFile, unitMerger.merge(primarySourceFile, contributionSourceFiles).toString());
    }

    private void ensurePatchedSource(SourceEntry entry) throws IOException {
        if (entry.patchedSource != null) return;

        String rawSource = Files.readString(entry.path);
        PatchTransformer.Result patchResult = PatchTransformer.transform(rawSource, entry.patchDirectives, getTargetLoader().get(), getTargetVersion().get());
        for (String warning : patchResult.warnings()) getLogger().warn("[{}] {}", entry.fqn, warning);

        ImportTransformer.Result importResult = ImportTransformer.transform(patchResult.source(), getTargetLoader().get(), getTargetVersion().get());
        for (String warning : importResult.warnings()) getLogger().warn("[{}] {}", entry.fqn, warning);

        entry.patchedSource = importResult.source();
    }

    private void parseEntryIfNeeded(SourceEntry entry) {
        if (entry.parsedUnit != null) return;

        try {
            entry.parsedUnit = StaticJavaParser.parse(entry.patchedSource);
        } catch (Exception parseException) {
            throw new GradleException("Failed to parse " + entry.path + ": " + parseException.getMessage(), parseException);
        }
    }

    private void parseEntries(SourceState state, Set<String> classNamesToParse) {
        for (String className : classNamesToParse) {
            SourceEntry entry = state.get(className);
            if (entry == null) continue;

            try {
                ensurePatchedSource(entry);
            } catch (IOException readException) {
                throw new GradleException("Failed to read " + entry.path + ": "
                        + readException.getMessage(), readException);
            }
            parseEntryIfNeeded(entry);
        }
    }

    private Map<String, List<String>> resolveContributions(SourceState state) {
        String loader = getTargetLoader().get();
        String version = getTargetVersion().get();
        Map<String, List<String>> contributions = new LinkedHashMap<>();
        for (SourceEntry entry : state.all()) {
            if (entry.mergeDirectives.isEmpty()) continue;

            List<String> matchingTargetClassNames = new ArrayList<>();
            for (MergeDirective mergeDirective : entry.mergeDirectives) {
                if (mergeDirective.matches(loader, version)) matchingTargetClassNames.add(mergeDirective.targetFqn());
            }
            if (!matchingTargetClassNames.isEmpty()) contributions.put(entry.fqn, matchingTargetClassNames);
        }
        return contributions;
    }

    private static Set<String> collectTargets(Map<String, List<String>> contributions, SourceState state) {
        Set<String> targetClassNames = new HashSet<>();
        for (Map.Entry<String, List<String>> contributionEntry : contributions.entrySet()) {
            for (String targetClassName : contributionEntry.getValue()) {
                if (!state.contains(targetClassName))
                    throw new GradleException("Merge directive in '" + contributionEntry.getKey()
                            + "' references unknown class '" + targetClassName + "'");
                targetClassNames.add(targetClassName);
            }
        }
        return targetClassNames;
    }

    private static Set<String> dependentsOf(Map<String, List<String>> contributions, String targetClassName) {
        Set<String> dependentClassNames = new HashSet<>();
        for (Map.Entry<String, List<String>> contributionEntry : contributions.entrySet()) {
            if (contributionEntry.getValue().contains(targetClassName)) dependentClassNames.add(contributionEntry.getKey());
        }
        return dependentClassNames;
    }

    private static List<MergeDirective> parseMergeDirectives(String source) {
        List<MergeDirective> mergeDirectives = new ArrayList<>();
        String normalizedSource = source.replace("\r\n", "\n").replace('\r', '\n');
        for (String line : normalizedSource.split("\n")) {
            MergeDirective mergeDirective = parseMergeDirective(line);
            if (mergeDirective != null) mergeDirectives.add(mergeDirective);
        }
        return mergeDirectives;
    }

    private static MergeDirective parseMergeDirective(String line) {
        Matcher directiveMatcher = MERGE_DIRECTIVE_PATTERN.matcher(line);
        if (!directiveMatcher.matches()) return null;

        String directiveBody = directiveMatcher.group(1);
        String conditionSegment = null;
        String targetClassName = directiveBody;
        int lastArrowIndex = directiveBody.lastIndexOf("->");
        if (lastArrowIndex >= 0) {
            conditionSegment = directiveBody.substring(0, lastArrowIndex).trim();
            targetClassName = directiveBody.substring(lastArrowIndex + 2).trim();
        }
        if (!FULLY_QUALIFIED_NAME_PATTERN.matcher(targetClassName).matches()) return null;

        String loaderPattern = null;
        String versionPattern = null;
        if (conditionSegment != null && !conditionSegment.isEmpty()) {
            for (String rawCondition : conditionSegment.split(",")) {
                String trimmedCondition = rawCondition.trim();
                if (trimmedCondition.isEmpty()) continue;

                int equalsIndex = trimmedCondition.indexOf('=');
                if (equalsIndex < 0) continue;

                String key = trimmedCondition.substring(0, equalsIndex).trim().toLowerCase();
                String value = trimmedCondition.substring(equalsIndex + 1).trim();
                if (value.isEmpty()) continue;

                switch (key) {
                    case "loader" -> loaderPattern = value;
                    case "version" -> versionPattern = value;
                    default -> {}
                }
            }
        }
        return new MergeDirective(loaderPattern, versionPattern, targetClassName);
    }

    private static List<PatchDirective> parsePatchDirectives(String source) {
        List<PatchDirective> patchDirectives = new ArrayList<>();
        String normalizedSource = source.replace("\r\n", "\n").replace('\r', '\n');
        for (String line : normalizedSource.split("\n")) {
            PatchDirective patchDirective = PatchTransformer.parseDirective(line);
            if (patchDirective != null) patchDirectives.add(patchDirective);
        }
        return patchDirectives;
    }

    private void applyResources(Path outputResourcesDirectory) throws IOException {
        Set<String> copiedRelativePaths = new HashSet<>();
        copyDirectoryTree(getLoaderResourcesDir(), outputResourcesDirectory, copiedRelativePaths);
        copyDirectoryTree(getVersionResourcesDir(), outputResourcesDirectory, copiedRelativePaths);
        copyDirectoryTree(getMainResourcesDir(), outputResourcesDirectory, copiedRelativePaths);
    }

    private void copyDirectoryTree(ConfigurableFileCollection sourceRoots, Path outputResourcesDirectory, Set<String> copiedRelativePaths) throws IOException {
        for (File sourceRoot : sourceRoots.getFiles()) {
            if (!sourceRoot.isDirectory()) continue;

            Path sourceRootPath = sourceRoot.toPath();
            try (Stream<Path> pathStream = Files.walk(sourceRootPath)) {
                for (Path sourceFile : pathStream.filter(Files::isRegularFile).sorted().toList()) {
                    String relativePath = sourceRootPath.relativize(sourceFile).toString().replace(File.separatorChar, '/');
                    if (!copiedRelativePaths.add(relativePath)) continue;

                    Path destinationFile = outputResourcesDirectory.resolve(relativePath);
                    Files.createDirectories(destinationFile.getParent());
                    Files.copy(sourceFile, destinationFile, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static void deleteOutput(Path outputJavaDirectory, String fullyQualifiedClassName) throws IOException {
        Files.deleteIfExists(outputJavaDirectory.resolve(
                fullyQualifiedClassName.replace('.', '/') + ".java"));
    }

    private static void clearDirectory(Path directoryRoot) throws IOException {
        if (!Files.exists(directoryRoot)) return;

        try (Stream<Path> pathStream = Files.walk(directoryRoot)) {
            for (Path path : pathStream.filter(currentPath -> !currentPath.equals(directoryRoot))
                    .sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        }
    }

    private static final class SourceEntry {
        final Path path;
        final String fqn;
        final List<MergeDirective> mergeDirectives;
        final List<PatchDirective> patchDirectives;
        final boolean hasAsphodelMarkers;
        CompilationUnit parsedUnit;
        String patchedSource;

        SourceEntry(Path path, String fqn, List<MergeDirective> mergeDirectives, List<PatchDirective> patchDirectives, boolean hasAsphodelMarkers) {
            this.path = path;
            this.fqn = fqn;
            this.mergeDirectives = mergeDirectives;
            this.patchDirectives = patchDirectives;
            this.hasAsphodelMarkers = hasAsphodelMarkers;
        }

        boolean hasAnyDirectives() {
            return hasAsphodelMarkers;
        }
    }

    private static final class SourceState {
        private final Map<String, SourceEntry> entriesByClassName = new LinkedHashMap<>();
        private final Map<String, SourceEntry> entriesBypath = new LinkedHashMap<>();

        SourceEntry get(String fqn) {
            return entriesByClassName.get(fqn);
        }

        SourceEntry getByPath(Path path) {
            return entriesBypath.get(path.toString());
        }

        boolean contains(String fqn) {
            return entriesByClassName.containsKey(fqn);
        }

        Iterable<SourceEntry> all() {
            return entriesByClassName.values();
        }

        Set<String> fqns() {
            return entriesByClassName.keySet();
        }

        void put(SourceEntry entry) {
            SourceEntry previousEntry = entriesByClassName.get(entry.fqn);
            if (previousEntry != null) entriesBypath.remove(previousEntry.path.toString());

            entriesByClassName.put(entry.fqn, entry);
            entriesBypath.put(entry.path.toString(), entry);
        }

        void remove(String fqn) {
            SourceEntry removedEntry = entriesByClassName.remove(fqn);
            if (removedEntry != null) entriesBypath.remove(removedEntry.path.toString());
        }

        void save(Path stateFilePath, Path projectBasePath) throws IOException {
            StringBuilder stateBuilder = new StringBuilder();
            stateBuilder.append("v\t").append(STATE_VERSION).append('\n');
            for (SourceEntry entry : entriesByClassName.values()) {
                stateBuilder.append("entry\t").append(entry.fqn).append('\t')
                        .append(escapeStateField(toRelativeStatePath(projectBasePath, entry.path)))
                        .append('\t')
                        .append(entry.hasAsphodelMarkers ? "true" : "false")
                        .append('\t')
                        .append(entry.mergeDirectives.size());
                for (MergeDirective mergeDirective : entry.mergeDirectives) {
                    stateBuilder.append('\t')
                            .append(mergeDirective.loader() == null ? "" : mergeDirective.loader())
                            .append('\t')
                            .append(mergeDirective.version() == null ? "" : mergeDirective.version())
                            .append('\t')
                            .append(mergeDirective.targetFqn());
                }
                stateBuilder.append('\t').append(entry.patchDirectives.size());
                for (PatchDirective patchDirective : entry.patchDirectives) {
                    stateBuilder.append('\t')
                            .append(patchDirective.loader() == null ? "" : patchDirective.loader())
                            .append('\t')
                            .append(patchDirective.version() == null ? "" : patchDirective.version())
                            .append('\t')
                            .append(patchDirective.id());
                }
                stateBuilder.append('\n');
            }
            Files.createDirectories(stateFilePath.getParent());
            Files.writeString(stateFilePath, stateBuilder.toString());
        }

        static SourceState load(Path stateFilePath, Path projectBasePath) {
            if (!Files.exists(stateFilePath)) return null;

            try {
                List<String> stateLines = Files.readAllLines(stateFilePath);
                if (stateLines.isEmpty()) return null;

                String[] headerFields = stateLines.get(0).split("\t", -1);
                if (headerFields.length != 2 || !headerFields[0].equals("v") || Integer.parseInt(headerFields[1]) != STATE_VERSION) return null;

                SourceState loadedState = new SourceState();
                for (int lineIndex = 1; lineIndex < stateLines.size(); lineIndex++) {
                    String[] fields = stateLines.get(lineIndex).split("\t", -1);
                    if (fields.length < 6 || !fields[0].equals("entry")) continue;

                    Path entrypath = fromRelativeStatePath(projectBasePath, unescapeStateField(fields[2]));
                    boolean hasAsphodelMarkers = Boolean.parseBoolean(fields[3]);
                    int mergeDirectiveCount = Integer.parseInt(fields[4]);
                    List<MergeDirective> mergeDirectives = new ArrayList<>(mergeDirectiveCount);
                    int fieldIndex = 5;
                    for (int mergeIndex = 0; mergeIndex < mergeDirectiveCount; mergeIndex++) {
                        String loaderPattern = fields[fieldIndex].isEmpty() ? null : fields[fieldIndex];
                        String versionPattern = fields[fieldIndex + 1].isEmpty() ? null : fields[fieldIndex + 1];
                        mergeDirectives.add(new MergeDirective(loaderPattern, versionPattern, fields[fieldIndex + 2]));
                        fieldIndex += 3;
                    }
                    if (fieldIndex >= fields.length) continue;

                    int patchDirectiveCount = Integer.parseInt(fields[fieldIndex++]);
                    List<PatchDirective> patchDirectives = new ArrayList<>(patchDirectiveCount);
                    for (int patchIndex = 0; patchIndex < patchDirectiveCount; patchIndex++) {
                        String loaderPattern = fields[fieldIndex].isEmpty() ? null : fields[fieldIndex];
                        String versionPattern = fields[fieldIndex + 1].isEmpty() ? null : fields[fieldIndex + 1];
                        patchDirectives.add(new PatchDirective(loaderPattern, versionPattern, fields[fieldIndex + 2]));
                        fieldIndex += 3;
                    }
                    loadedState.put(new SourceEntry(entrypath, fields[1], mergeDirectives, patchDirectives, hasAsphodelMarkers));
                }
                return loadedState;
            } catch (Exception loadException) { return null; }
        }

        private static String toRelativeStatePath(Path projectBasePath, Path absolutepath) {
            Path relativePath = projectBasePath.relativize(absolutepath.toAbsolutePath().normalize());
            return relativePath.toString().replace(File.separatorChar, '/');
        }

        private static Path fromRelativeStatePath(Path projectBasePath, String storedRelativePath) {
            String platformPath = storedRelativePath.replace('/', File.separatorChar);
            return projectBasePath.resolve(platformPath).normalize();
        }

        private static String escapeStateField(String rawValue) {
            StringBuilder escapedBuilder = new StringBuilder(rawValue.length());
            for (int characterIndex = 0; characterIndex < rawValue.length(); characterIndex++) {
                char currentCharacter = rawValue.charAt(characterIndex);
                switch (currentCharacter) {
                    case '\\' -> escapedBuilder.append("\\\\");
                    case '\t' -> escapedBuilder.append("\\t");
                    case '\n' -> escapedBuilder.append("\\n");
                    case '\r' -> escapedBuilder.append("\\r");
                    default -> escapedBuilder.append(currentCharacter);
                }
            }
            return escapedBuilder.toString();
        }

        private static String unescapeStateField(String escapedValue) {
            StringBuilder unescapedBuilder = new StringBuilder(escapedValue.length());
            for (int characterIndex = 0; characterIndex < escapedValue.length(); characterIndex++) {
                char currentCharacter = escapedValue.charAt(characterIndex);
                if (currentCharacter == '\\' && characterIndex + 1 < escapedValue.length()) {
                    char escapedCharacter = escapedValue.charAt(++characterIndex);
                    switch (escapedCharacter) {
                        case '\\' -> unescapedBuilder.append('\\');
                        case 't' -> unescapedBuilder.append('\t');
                        case 'n' -> unescapedBuilder.append('\n');
                        case 'r' -> unescapedBuilder.append('\r');
                        default -> {
                            unescapedBuilder.append('\\');
                            unescapedBuilder.append(escapedCharacter);
                        }
                    }
                } else unescapedBuilder.append(currentCharacter);
            }
            return unescapedBuilder.toString();
        }
    }
}