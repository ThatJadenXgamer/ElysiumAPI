package asphodel.gradle;

import asphodel.gradle.directives.EjectDirective;
import asphodel.gradle.directives.MergeDirective;
import asphodel.gradle.directives.MixinDirective;
import asphodel.gradle.directives.PatchDirective;
import asphodel.gradle.transformers.ImportTransformer;
import asphodel.gradle.transformers.PatchTransformer;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.Range;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@CacheableTask
public abstract class AssembleSourcesTask extends DefaultTask {

    private static final ParserConfiguration PARSER_CONFIGURATION;

    static {
        PARSER_CONFIGURATION = new ParserConfiguration();
        PARSER_CONFIGURATION.setLanguageLevel(ParserConfiguration.LanguageLevel.BLEEDING_EDGE);
    }

    private static final Pattern MERGE_DIRECTIVE_PATTERN = Pattern.compile(
            "^\\s*//\\s*asphodel::merge\\s*->\\s*(.+?)\\s*$");

    private static final Pattern EJECT_DIRECTIVE_PATTERN = Pattern.compile(
            "^\\s*//\\s*asphodel::eject\\s*->\\s*(.*?)\\s*$");

    private static final Pattern PACKAGE_PATTERN = Pattern.compile(
            "^\\s*package\\s+([A-Za-z_][A-Za-z0-9_.]*)\\s*;",
            Pattern.MULTILINE);

    private static final Pattern FULLY_QUALIFIED_NAME_PATTERN = Pattern.compile(
            "^[A-Za-z_][A-Za-z0-9_.]*$");

    private static final int STATE_VERSION = 6;

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
        verifyParserCapability();

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
            applyMixinDirectives(currentState, outputResourcesDirectory);
            rebuildAllOutputs(currentState, outputJavaDirectory);
        } else {
            Files.createDirectories(outputJavaDirectory);
            Files.createDirectories(outputResourcesDirectory);
            clearDirectory(outputResourcesDirectory);
            applyResources(outputResourcesDirectory);
            currentState = rebuildIncremental(previousState, outputJavaDirectory, inputChanges);
            applyMixinDirectives(currentState, outputResourcesDirectory);
        }

        currentState.save(stateFilePath, projectBasePath);
    }

    private void verifyParserCapability() {
        String probeSource = "class AsphodelParserCapabilityProbe { boolean probe(Object candidate) { return candidate instanceof String name; } }";
        if (!new JavaParser(PARSER_CONFIGURATION).parse(probeSource).isSuccessful()) {
            throw new GradleException("Asphodel requires com.github.javaparser:javaparser-core 3.28.2 on the buildSrc classpath; "
                    + "the currently loaded parser cannot handle Java 16+ syntax. Check buildSrc/build.gradle and verify "
                    + "no plugin is shadowing the parser artifact.");
        }
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
        List<MixinDirective> mixinDirectives = parseMixinDirectives(raw);

        String normalizedSource = raw.replace("\r\n", "\n").replace('\r', '\n');
        int packageLine = findPackageLine(normalizedSource);
        List<EjectDirective> ejectDirectives = parseEjectDirectives(normalizedSource, packageLine, normalized.toString());
        boolean ejected = isEjected(ejectDirectives, getTargetLoader().get(), getTargetVersion().get());

        boolean hasAsphodelMarkers = raw.contains("asphodel::merge")
                || raw.contains("asphodel::patch")
                || raw.contains("asphodel::import")
                || raw.contains("asphodel::mixin")
                || raw.contains("asphodel::eject");

        Matcher packageMatcher = PACKAGE_PATTERN.matcher(raw);
        String packageName = packageMatcher.find() ? packageMatcher.group(1) : "";
        String fileName = normalized.getFileName().toString();
        String simpleClassName = fileName.substring(0, fileName.length() - ".java".length());
        String fqn = packageName.isEmpty() ? simpleClassName : packageName + "." + simpleClassName;
        return new SourceEntry(normalized, fqn, mergeDirectives, patchDirectives, mixinDirectives, hasAsphodelMarkers, ejected);
    }

    private static int findPackageLine(String normalizedSource) {
        String[] lines = normalizedSource.split("\n", -1);
        for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
            String stripped = lines[lineIndex].strip();
            if (stripped.startsWith("package ") && stripped.endsWith(";")) return lineIndex + 1;
            if (!stripped.isEmpty() && !stripped.startsWith("//") && !stripped.startsWith("/*") && !stripped.startsWith("*")) {
                break;
            }
        }
        return -1;
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
            currentState.put(new SourceEntry(previousEntry.path, previousEntry.fqn, previousEntry.mergeDirectives, previousEntry.patchDirectives, previousEntry.mixinDirectives, previousEntry.hasAsphodelMarkers, previousEntry.ejected));
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
        previousOutputClassNames.removeIf(fqn -> {
            SourceEntry entry = previousState.get(fqn);
            return entry != null && entry.ejected;
        });
        Set<String> currentOutputClassNames = new HashSet<>(currentState.fqns());
        currentOutputClassNames.removeAll(currentConsumedClassNames);
        currentOutputClassNames.removeIf(fqn -> {
            SourceEntry entry = currentState.get(fqn);
            return entry != null && entry.ejected;
        });

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
        if (entry.ejected) return;

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
        entry.parsedUnit = parseSource(entry.patchedSource, entry.path.toString());
    }

    private CompilationUnit parseSource(String source, String description) {
        ParseResult<CompilationUnit> result = new JavaParser(PARSER_CONFIGURATION).parse(source);
        if (!result.isSuccessful()) {
            throw new GradleException("Failed to parse " + description + ": " + result.getProblems());
        }
        CompilationUnit parsedUnit = result.getResult().orElse(null);
        if (parsedUnit == null) {
            throw new GradleException("Parser returned no compilation unit for " + description);
        }
        return parsedUnit;
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

    private static List<MixinDirective> parseMixinDirectives(String source) {
        List<MixinDirective> mixinDirectives = new ArrayList<>();
        String normalizedSource = source.replace("\r\n", "\n").replace('\r', '\n');
        String[] lines = normalizedSource.split("\n", -1);
        for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
            MixinDirective mixinDirective = parseMixinDirective(lines[lineIndex], lineIndex + 1);
            if (mixinDirective != null) mixinDirectives.add(mixinDirective);
        }
        return mixinDirectives;
    }

    private static MixinDirective parseMixinDirective(String line, int lineNumber) {
        String trimmed = line.stripLeading();
        if (!trimmed.startsWith("//")) return null;
        String body = trimmed.substring(2).stripLeading();
        if (!body.startsWith("asphodel::mixin")) return null;
        String afterKeyword = body.substring("asphodel::mixin".length()).stripLeading();
        if (!afterKeyword.startsWith("->")) return null;
        String directiveBody = afterKeyword.substring(2).strip();

        String loader = null;
        String version = null;
        String file = null;
        String env = "common";
        for (String token : directiveBody.replace("->", ",").split(",")) {
            int equalsIndex = token.indexOf('=');
            if (equalsIndex < 0) continue;
            String key = token.substring(0, equalsIndex).trim().toLowerCase();
            String value = token.substring(equalsIndex + 1).trim();
            if (value.isEmpty()) continue;
            switch (key) {
                case "loader" -> loader = value;
                case "version" -> version = value;
                case "file" -> file = value;
                case "env" -> env = value.toLowerCase();
            }
        }
        if (file == null) return null;
        if (!env.equals("common") && !env.equals("client") && !env.equals("server")) return null;
        return new MixinDirective(loader, version, file, env, lineNumber);
    }

    private List<EjectDirective> parseEjectDirectives(String normalizedSource, int packageLine, String sourcePath) {
        List<EjectDirective> ejectDirectives = new ArrayList<>();
        String[] lines = normalizedSource.split("\n", -1);
        for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
            EjectDirective ejectDirective = parseEjectDirective(lines[lineIndex]);
            if (ejectDirective == null) continue;

            int lineNumber = lineIndex + 1;
            if (packageLine > 0 && lineNumber >= packageLine) {
                getLogger().warn("[{}] asphodel::eject at line {} must be placed above the package declaration at line {}; ignoring",
                        sourcePath, lineNumber, packageLine);
                continue;
            }
            ejectDirectives.add(ejectDirective);
        }
        return ejectDirectives;
    }

    private static EjectDirective parseEjectDirective(String line) {
        Matcher ejectMatcher = EJECT_DIRECTIVE_PATTERN.matcher(line);
        if (!ejectMatcher.matches()) return null;

        String conditions = ejectMatcher.group(1);
        String loaderPattern = null;
        String versionPattern = null;
        for (String token : conditions.replace("->", ",").split(",")) {
            int equalsIndex = token.indexOf('=');
            if (equalsIndex < 0) continue;
            String key = token.substring(0, equalsIndex).trim().toLowerCase();
            String value = token.substring(equalsIndex + 1).trim();
            if (value.isEmpty()) continue;
            switch (key) {
                case "loader" -> loaderPattern = value;
                case "version" -> versionPattern = value;
                default -> {}
            }
        }
        return new EjectDirective(loaderPattern, versionPattern);
    }

    private static boolean isEjected(List<EjectDirective> ejectDirectives, String loader, String version) {
        for (EjectDirective ejectDirective : ejectDirectives) {
            if (ejectDirective.matches(loader, version)) return true;
        }
        return false;
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

    private void applyMixinDirectives(SourceState state, Path outputResourcesDirectory) {
        Path resourcesRoot = outputResourcesDirectory.toAbsolutePath().normalize();
        Map<Path, List<PendingMixinAddition>> additionsByFile = new LinkedHashMap<>();

        for (SourceEntry entry : state.all()) {
            if (entry.ejected) continue;
            if (entry.mixinDirectives.isEmpty()) continue;

            try {
                ensurePatchedSource(entry);
            } catch (IOException readException) {
                throw new GradleException("Failed to read " + entry.path + ": " + readException.getMessage(), readException);
            }
            parseEntryIfNeeded(entry);

            CompilationUnit compilationUnit = entry.parsedUnit;
            if (compilationUnit.getTypes().isEmpty()) {
                getLogger().warn("[{}] asphodel::mixin directive found but the file declares no types; ignoring", entry.fqn);
                continue;
            }

            TypeDeclaration<?> mixinType = null;
            for (TypeDeclaration<?> type : compilationUnit.getTypes()) {
                if (type.getAnnotationByName("Mixin").isPresent()) {
                    mixinType = type;
                    break;
                }
            }
            if (mixinType == null) {
                getLogger().warn("[{}] class is not a mixin file; ignoring asphodel::mixin directives", entry.fqn);
                continue;
            }

            Optional<AnnotationExpr> mixinAnnotation = mixinType.getAnnotationByName("Mixin");
            Optional<Range> mixinRange = mixinAnnotation.get().getRange();
            if (mixinRange.isEmpty()) {
                getLogger().warn("[{}] asphodel::mixin directive found but the @Mixin annotation location is unknown; ignoring", entry.fqn);
                continue;
            }
            int mixinLine = mixinRange.get().begin.line;

            for (MixinDirective directive : entry.mixinDirectives) {
                if (directive.line() >= mixinLine) {
                    getLogger().warn("[{}] asphodel::mixin directive on line {} must be placed above the @Mixin annotation on line {}; ignoring",
                            entry.fqn, directive.line(), mixinLine);
                    continue;
                }
                if (!directive.matches(getTargetLoader().get(), getTargetVersion().get())) continue;

                Path targetFile = resourcesRoot.resolve(directive.file()).normalize();
                if (!targetFile.startsWith(resourcesRoot)) {
                    getLogger().warn("[{}] asphodel::mixin targets '{}' which escapes the resources root; ignoring", entry.fqn, directive.file());
                    continue;
                }
                if (!Files.isRegularFile(targetFile)) {
                    getLogger().warn("[{}] asphodel::mixin targets '{}' but the file does not exist in resources; ignoring", entry.fqn, directive.file());
                    continue;
                }
                additionsByFile.computeIfAbsent(targetFile, path -> new ArrayList<>())
                        .add(new PendingMixinAddition(entry.fqn, directive.env()));
            }
        }

        for (Map.Entry<Path, List<PendingMixinAddition>> additionsEntry : additionsByFile.entrySet()) {
            applyMixinAdditions(additionsEntry.getKey(), additionsEntry.getValue());
        }
    }

    private void applyMixinAdditions(Path targetFile, List<PendingMixinAddition> additions) {
        JsonObject root;
        try {
            String content = Files.readString(targetFile);
            JsonElement parsed = JsonParser.parseString(content);
            if (!parsed.isJsonObject()) {
                getLogger().warn("Mixin file '{}' is not a JSON object; skipping asphodel::mixin additions", targetFile.getFileName());
                return;
            }
            root = parsed.getAsJsonObject();
        } catch (IOException readException) {
            getLogger().warn("Failed to read mixin file '{}': {}; skipping asphodel::mixin additions", targetFile.getFileName(), readException.getMessage());
            return;
        } catch (RuntimeException parseException) {
            getLogger().warn("Failed to parse mixin file '{}': {}; skipping asphodel::mixin additions", targetFile.getFileName(), parseException.getMessage());
            return;
        }

        if (!root.has("package") || !root.get("package").isJsonPrimitive() || !root.getAsJsonPrimitive("package").isString()) {
            getLogger().warn("Mixin file '{}' has no string 'package' field; skipping asphodel::mixin additions", targetFile.getFileName());
            return;
        }
        String packageName = root.get("package").getAsString();
        if (packageName.isEmpty()) {
            getLogger().warn("Mixin file '{}' has an empty 'package' field; skipping asphodel::mixin additions", targetFile.getFileName());
            return;
        }
        String packagePrefix = packageName.endsWith(".") ? packageName : packageName + ".";

        boolean changed = false;
        for (PendingMixinAddition addition : additions) {
            if (!addition.classFqn().startsWith(packagePrefix)) {
                getLogger().warn("[{}] class is not within the mixin file's package '{}'; skipping addition to '{}'",
                        addition.classFqn(), packageName, targetFile.getFileName());
                continue;
            }
            String relativeName = addition.classFqn().substring(packagePrefix.length());
            String arrayName = switch (addition.env()) {
                case "common" -> "mixins";
                case "client" -> "client";
                case "server" -> "server";
                default -> null;
            };
            if (arrayName == null) continue;

            JsonArray array;
            if (root.has(arrayName)) {
                JsonElement existing = root.get(arrayName);
                if (!existing.isJsonArray()) {
                    getLogger().warn("Mixin file '{}' has a non-array '{}' field; skipping", targetFile.getFileName(), arrayName);
                    continue;
                }
                array = existing.getAsJsonArray();
            } else {
                array = new JsonArray();
                root.add(arrayName, array);
            }

            boolean alreadyPresent = false;
            for (JsonElement element : array) {
                if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString() && element.getAsString().equals(relativeName)) {
                    alreadyPresent = true;
                    break;
                }
            }
            if (alreadyPresent) {
                getLogger().warn("[{}] mixin entry '{}' is already present in the '{}' array of '{}'; skipping",
                        addition.classFqn(), relativeName, arrayName, targetFile.getFileName());
                continue;
            }
            array.add(relativeName);
            changed = true;
        }

        if (!changed) return;

        JsonObject reordered = new JsonObject();
        for (Map.Entry<String, JsonElement> field : root.entrySet()) {
            String key = field.getKey();
            if (key.equals("mixins") || key.equals("client") || key.equals("server")) continue;
            reordered.add(key, field.getValue());
        }
        for (String key : new String[]{"mixins", "client", "server"}) {
            if (root.has(key)) reordered.add(key, root.get(key));
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String output = gson.toJson(reordered) + "\n";
        try {
            Files.writeString(targetFile, output);
        } catch (IOException writeException) {
            throw new GradleException("Failed to write mixin file " + targetFile + ": " + writeException.getMessage(), writeException);
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

    private record PendingMixinAddition(String classFqn, String env) {}

    private static final class SourceEntry {
        final Path path;
        final String fqn;
        final List<MergeDirective> mergeDirectives;
        final List<PatchDirective> patchDirectives;
        final List<MixinDirective> mixinDirectives;
        final boolean hasAsphodelMarkers;
        final boolean ejected;
        CompilationUnit parsedUnit;
        String patchedSource;

        SourceEntry(Path path, String fqn, List<MergeDirective> mergeDirectives, List<PatchDirective> patchDirectives, List<MixinDirective> mixinDirectives, boolean hasAsphodelMarkers, boolean ejected) {
            this.path = path;
            this.fqn = fqn;
            this.mergeDirectives = mergeDirectives;
            this.patchDirectives = patchDirectives;
            this.mixinDirectives = mixinDirectives;
            this.hasAsphodelMarkers = hasAsphodelMarkers;
            this.ejected = ejected;
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
                stateBuilder.append('\t').append(entry.mixinDirectives.size());
                for (MixinDirective mixinDirective : entry.mixinDirectives) {
                    stateBuilder.append('\t')
                            .append(mixinDirective.loader() == null ? "" : mixinDirective.loader())
                            .append('\t')
                            .append(mixinDirective.version() == null ? "" : mixinDirective.version())
                            .append('\t')
                            .append(mixinDirective.file())
                            .append('\t')
                            .append(mixinDirective.env())
                            .append('\t')
                            .append(mixinDirective.line());
                }
                stateBuilder.append('\t').append(entry.ejected ? "true" : "false");
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
                    if (fieldIndex >= fields.length) continue;

                    int mixinDirectiveCount = Integer.parseInt(fields[fieldIndex++]);
                    List<MixinDirective> mixinDirectives = new ArrayList<>(mixinDirectiveCount);
                    for (int mixinIndex = 0; mixinIndex < mixinDirectiveCount; mixinIndex++) {
                        String loaderPattern = fields[fieldIndex].isEmpty() ? null : fields[fieldIndex];
                        String versionPattern = fields[fieldIndex + 1].isEmpty() ? null : fields[fieldIndex + 1];
                        String file = fields[fieldIndex + 2];
                        String env = fields[fieldIndex + 3];
                        int line = Integer.parseInt(fields[fieldIndex + 4]);
                        mixinDirectives.add(new MixinDirective(loaderPattern, versionPattern, file, env, line));
                        fieldIndex += 5;
                    }
                    if (fieldIndex >= fields.length) continue;

                    boolean ejected = Boolean.parseBoolean(fields[fieldIndex]);

                    loadedState.put(new SourceEntry(entrypath, fields[1], mergeDirectives, patchDirectives, mixinDirectives, hasAsphodelMarkers, ejected));
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