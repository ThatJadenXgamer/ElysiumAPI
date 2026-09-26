package asphodel.gradle;

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
public abstract class MergeSourcesTask extends DefaultTask {

    private static final Pattern DIRECTIVE_PATTERN = Pattern.compile(
            "^\\s*//\\s*asphodel::merge\\s*->\\s*loader\\s*=\\s*([A-Za-z_*]+)\\s*,\\s*version\\s*=\\s*([A-Za-z0-9._-]+|\\*)\\s*->\\s*([A-Za-z_][A-Za-z0-9_.]*)\\s*$",
            Pattern.MULTILINE);

    private static final Pattern PACKAGE_PATTERN = Pattern.compile(
            "^\\s*package\\s+([A-Za-z_][A-Za-z0-9_.]*)\\s*;",
            Pattern.MULTILINE);

    private static final int STATE_VERSION = 2;

    @InputFiles
    @Incremental
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getJavaSourceDirs();

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getResourceSourceDirs();

    @Input
    public abstract Property<String> getTargetLoader();

    @Input
    public abstract Property<String> getTargetVersion();

    @Internal
    public abstract DirectoryProperty getProjectDirectory();

    @OutputDirectory
    public abstract DirectoryProperty getOutputJavaDir();

    @OutputDirectory
    public abstract DirectoryProperty getOutputResourceDir();

    @OutputFile
    public abstract RegularFileProperty getStateFile();

    @TaskAction
    public void execute(InputChanges inputChanges) throws IOException {
        Path outJava = getOutputJavaDir().get().getAsFile().toPath();
        Path outRes = getOutputResourceDir().get().getAsFile().toPath();
        Path statePath = getStateFile().get().getAsFile().toPath();
        Path base = getProjectDirectory().get().getAsFile().toPath().toAbsolutePath().normalize();

        State previous = inputChanges.isIncremental() ? State.load(statePath, base) : null;
        State current;

        if (previous == null) {
            clearDirectory(outJava);
            clearDirectory(outRes);
            Files.createDirectories(outJava);
            Files.createDirectories(outRes);
            current = scanAll();
            applyResources(outRes);
            rebuildAll(current, outJava);
        } else {
            Files.createDirectories(outJava);
            Files.createDirectories(outRes);
            clearDirectory(outRes);
            applyResources(outRes);
            current = rebuildIncremental(previous, outJava, inputChanges);
        }

        current.save(statePath, base);
    }

    private State scanAll() throws IOException {
        State state = new State();
        for (File root : getJavaSourceDirs().getFiles()) {
            if (!root.isDirectory()) continue;
            Path rootPath = root.toPath();
            try (Stream<Path> stream = Files.walk(rootPath)) {
                List<Path> files = stream
                        .filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".java"))
                        .sorted()
                        .toList();
                for (Path path : files) {
                    Entry entry = scan(path);
                    Entry existing = state.get(entry.fqn);
                    if (existing != null && !existing.path.equals(entry.path)) {
                        throw new GradleException("Duplicate class '" + entry.fqn + "' declared in:\n  "
                                + existing.path + "\n  " + path);
                    }
                    state.put(entry);
                }
            }
        }
        return state;
    }

    private Entry scan(Path path) throws IOException {
        Path normalized = path.toAbsolutePath().normalize();
        String raw = Files.readString(normalized);
        List<MergeDirective> directives = parseDirectives(raw);
        Matcher m = PACKAGE_PATTERN.matcher(raw);
        String pkg = m.find() ? m.group(1) : "";
        String filename = normalized.getFileName().toString();
        String simple = filename.substring(0, filename.length() - ".java".length());
        String fqn = pkg.isEmpty() ? simple : pkg + "." + simple;
        return new Entry(normalized, fqn, directives);
    }

    private void rebuildAll(State state, Path outJava) throws IOException {
        Map<String, List<String>> contributions = resolveContributions(state);
        Set<String> consumed = collectTargets(contributions, state);
        for (String c : consumed) contributions.remove(c);

        Set<String> parseSet = new HashSet<>(contributions.keySet());
        for (List<String> targets : contributions.values()) parseSet.addAll(targets);
        parseAll(state, parseSet);

        CompilationUnitMerger merger = new CompilationUnitMerger();
        for (Entry entry : state.all()) {
            if (consumed.contains(entry.fqn)) continue;
            writeOutput(merger, state, entry, contributions.get(entry.fqn), outJava);
        }
    }

    private State rebuildIncremental(State previous, Path outJava, InputChanges inputChanges) throws IOException {
        State current = new State();
        for (Entry entry : previous.all()) current.put(new Entry(entry.path, entry.fqn, entry.directives));

        Set<String> changedFQNs = new HashSet<>();
        Set<String> removedFQNs = new HashSet<>();

        List<FileChange> changes = new ArrayList<>();
        for (FileChange c : inputChanges.getFileChanges(getJavaSourceDirs())) {
            changes.add(c);
        }

        for (FileChange change : changes) {
            File file = change.getFile();
            if (!file.getName().endsWith(".java")) continue;
            Path path = file.toPath().toAbsolutePath().normalize();

            if (change.getChangeType() == ChangeType.REMOVED) {
                Entry prev = current.getByPath(path);
                if (prev != null) {
                    current.remove(prev.fqn);
                    removedFQNs.add(prev.fqn);
                }
            } else {
                Entry old = current.getByPath(path);
                if (old != null) {
                    current.remove(old.fqn);
                    changedFQNs.add(old.fqn);
                }
                Entry entry = scan(path);
                current.put(entry);
                changedFQNs.add(entry.fqn);
            }
        }

        Map<String, List<String>> prevContrib = resolveContributions(previous);
        Map<String, List<String>> currContrib = resolveContributions(current);
        Set<String> prevConsumed = collectTargets(prevContrib, previous);
        Set<String> currConsumed = collectTargets(currContrib, current);

        Set<String> affected = new HashSet<>(changedFQNs);
        for (String fqn : changedFQNs) {
            affected.addAll(dependentsOf(prevContrib, fqn));
            affected.addAll(dependentsOf(currContrib, fqn));
        }
        for (String fqn : removedFQNs) {
            affected.addAll(dependentsOf(prevContrib, fqn));
            affected.addAll(dependentsOf(currContrib, fqn));
        }
        for (String fqn : prevConsumed) {
            if (!currConsumed.contains(fqn)) affected.add(fqn);
        }

        Set<String> prevOutputs = new HashSet<>(previous.fqns());
        prevOutputs.removeAll(prevConsumed);
        Set<String> currOutputs = new HashSet<>(current.fqns());
        currOutputs.removeAll(currConsumed);
        for (String fqn : prevOutputs) {
            if (!currOutputs.contains(fqn)) deleteOutput(outJava, fqn);
        }

        for (String fqn : currOutputs) {
            Path dest = outJava.resolve(fqn.replace('.', '/') + ".java");
            if (!Files.exists(dest)) affected.add(fqn);
        }

        Set<String> parseSet = new HashSet<>();
        for (String fqn : affected) {
            List<String> targets = currContrib.get(fqn);
            if (targets != null) {
                parseSet.add(fqn);
                parseSet.addAll(targets);
            }
        }
        parseAll(current, parseSet);

        CompilationUnitMerger merger = new CompilationUnitMerger();
        for (String fqn : affected) {
            if (currConsumed.contains(fqn)) continue;
            Entry entry = current.get(fqn);
            if (entry == null) continue;
            writeOutput(merger, current, entry, currContrib.get(fqn), outJava);
        }

        return current;
    }

    private void writeOutput(CompilationUnitMerger merger, State state, Entry entry, List<String> targets, Path outJava) throws IOException {
        Path dest = outJava.resolve(entry.fqn.replace('.', '/') + ".java");
        Files.createDirectories(dest.getParent());
        if (targets == null || targets.isEmpty()) {
            Files.copy(entry.path, dest, StandardCopyOption.REPLACE_EXISTING);
            return;
        }
        CompilationUnitMerger.SourceFile primary = new CompilationUnitMerger.SourceFile(entry.path, entry.unit, entry.directives, entry.fqn, null);
        List<CompilationUnitMerger.SourceFile> parts = new ArrayList<>(targets.size());
        for (String target : targets) {
            Entry targetEntry = state.get(target);
            parts.add(new CompilationUnitMerger.SourceFile(targetEntry.path, targetEntry.unit, targetEntry.directives, targetEntry.fqn, null));
        }
        Files.writeString(dest, merger.merge(primary, parts).toString());
    }

    private void parseAll(State state, Set<String> parseSet) {
        for (String fqn : parseSet) {
            Entry entry = state.get(fqn);
            if (entry == null || entry.unit != null) continue;
            try {
                entry.unit = StaticJavaParser.parse(Files.readString(entry.path));
            } catch (Exception ex) {
                throw new GradleException("Failed to parse " + entry.path + ": " + ex.getMessage(), ex);
            }
        }
    }

    private Map<String, List<String>> resolveContributions(State state) {
        String loader = getTargetLoader().get();
        String version = getTargetVersion().get();
        Map<String, List<String>> out = new LinkedHashMap<>();
        for (Entry entry : state.all()) {
            if (entry.directives.isEmpty()) continue;
            List<String> matches = new ArrayList<>();
            for (MergeDirective d : entry.directives) if (d.matches(loader, version)) matches.add(d.targetFqn());
            if (!matches.isEmpty()) out.put(entry.fqn, matches);
        }
        return out;
    }

    private static Set<String> collectTargets(Map<String, List<String>> contributions, State state) {
        Set<String> out = new HashSet<>();
        for (Map.Entry<String, List<String>> entry : contributions.entrySet()) {
            for (String target : entry.getValue()) {
                if (!state.contains(target)) throw new GradleException("Merge directive in '" + entry.getKey()
                        + "' references unknown class '" + target + "'");
                out.add(target);
            }
        }
        return out;
    }

    private static Set<String> dependentsOf(Map<String, List<String>> contributions, String fqn) {
        Set<String> out = new HashSet<>();
        for (Map.Entry<String, List<String>> e : contributions.entrySet()) if (e.getValue().contains(fqn)) out.add(e.getKey());
        return out;
    }

    private static List<MergeDirective> parseDirectives(String source) {
        List<MergeDirective> out = new ArrayList<>();
        Matcher matcher = DIRECTIVE_PATTERN.matcher(source);
        while (matcher.find()) out.add(new MergeDirective(matcher.group(1), matcher.group(2), matcher.group(3)));
        return out;
    }

    private void applyResources(Path outRes) throws IOException {
        Set<String> copied = new HashSet<>();
        for (File root : getResourceSourceDirs().getFiles()) {
            if (!root.isDirectory()) continue;
            Path rootPath = root.toPath();
            try (Stream<Path> stream = Files.walk(rootPath)) {
                for (Path path : stream.filter(Files::isRegularFile).sorted().toList()) {
                    String relative = rootPath.relativize(path).toString().replace(File.separatorChar, '/');
                    if (!copied.add(relative)) continue;
                    Path destination = outRes.resolve(relative);
                    Files.createDirectories(destination.getParent());
                    Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static void deleteOutput(Path outJava, String fqn) throws IOException {
        Files.deleteIfExists(outJava.resolve(fqn.replace('.', '/') + ".java"));
    }

    private static void clearDirectory(Path root) throws IOException {
        if (!Files.exists(root)) return;
        try (Stream<Path> stream = Files.walk(root)) {
            for (Path path : stream.filter(p -> !p.equals(root))
                    .sorted(Comparator.reverseOrder())
                    .toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    private static final class Entry {
        final Path path;
        final String fqn;
        final List<MergeDirective> directives;
        CompilationUnit unit;

        Entry(Path path, String fqn, List<MergeDirective> directives) {
            this.path = path;
            this.fqn = fqn;
            this.directives = directives;
        }
    }

    private static final class State {
        private final Map<String, Entry> byFqn = new LinkedHashMap<>();
        private final Map<String, Entry> byPath = new LinkedHashMap<>();

        Entry get(String fqn) {
            return byFqn.get(fqn);
        }

        Entry getByPath(Path p) {
            return byPath.get(p.toString());
        }

        boolean contains(String fqn) {
            return byFqn.containsKey(fqn);
        }

        Iterable<Entry> all() {
            return byFqn.values();
        }

        Set<String> fqns() {
            return byFqn.keySet();
        }

        void put(Entry entry) {
            Entry old = byFqn.get(entry.fqn);
            if (old != null) byPath.remove(old.path.toString());
            byFqn.put(entry.fqn, entry);
            byPath.put(entry.path.toString(), entry);
        }

        void remove(String fqn) {
            Entry entry = byFqn.remove(fqn);
            if (entry != null) byPath.remove(entry.path.toString());
        }

        void save(Path path, Path base) throws IOException {
            StringBuilder sb = new StringBuilder();
            sb.append("v\t").append(STATE_VERSION).append('\n');
            for (Entry entry : byFqn.values()) {
                sb.append("entry\t").append(entry.fqn).append('\t')
                        .append(escape(toStatePath(base, entry.path))).append('\t')
                        .append(entry.directives.size());
                for (MergeDirective d : entry.directives) {
                    sb.append('\t').append(d.loader()).append('\t')
                            .append(d.version()).append('\t').append(d.targetFqn());
                }
                sb.append('\n');
            }
            Files.createDirectories(path.getParent());
            Files.writeString(path, sb.toString());
        }

        static State load(Path path, Path base) {
            if (!Files.exists(path)) return null;
            try {
                List<String> lines = Files.readAllLines(path);
                if (lines.isEmpty()) return null;
                String[] header = lines.getFirst().split("\t", -1);
                if (header.length != 2 || !header[0].equals("v")
                        || Integer.parseInt(header[1]) != STATE_VERSION) return null;
                State s = new State();
                for (int i = 1; i < lines.size(); i++) {
                    String[] parts = lines.get(i).split("\t", -1);
                    if (parts.length < 4 || !parts[0].equals("entry")) continue;
                    Path entryPath = fromStatePath(base, unescape(parts[2]));
                    int n = Integer.parseInt(parts[3]);
                    List<MergeDirective> directives = new ArrayList<>(n);
                    int idx = 4;
                    for (int j = 0; j < n; j++) {
                        directives.add(new MergeDirective(parts[idx], parts[idx + 1], parts[idx + 2]));
                        idx += 3;
                    }
                    s.put(new Entry(entryPath, parts[1], directives));
                }
                return s;
            } catch (Exception ex) { return null; }
        }

        private static String toStatePath(Path base, Path absolute) {
            Path rel = base.relativize(absolute.toAbsolutePath().normalize());
            return rel.toString().replace(File.separatorChar, '/');
        }

        private static Path fromStatePath(Path base, String stored) {
            String platformPath = stored.replace('/', File.separatorChar);
            return base.resolve(platformPath).normalize();
        }

        private static String escape(String s) {
            StringBuilder sb = new StringBuilder(s.length());
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                switch (c) {
                    case '\\' -> sb.append("\\\\");
                    case '\t' -> sb.append("\\t");
                    case '\n' -> sb.append("\\n");
                    case '\r' -> sb.append("\\r");
                    default -> sb.append(c);
                }
            }
            return sb.toString();
        }

        private static String unescape(String s) {
            StringBuilder sb = new StringBuilder(s.length());
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c == '\\' && i + 1 < s.length()) {
                    char n = s.charAt(++i);
                    switch (n) {
                        case '\\' -> sb.append('\\');
                        case 't' -> sb.append('\t');
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        default -> {
                            sb.append('\\');
                            sb.append(n);
                        }
                    }
                } else sb.append(c);
            }
            return sb.toString();
        }
    }
}