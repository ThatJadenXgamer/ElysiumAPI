package net.jadenxgamer.elysium_api.impl.config;

import net.jadenxgamer.elysium_api.api.config.*;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ConfigFile {

    private static final Logger LOGGER = LoggerFactory.getLogger("Elysium-API");

    private ConfigFile() {
    }

    static void loadFrom(Spec spec, Path file) {
        try {
            if (!Files.exists(file)) {
                writeTo(spec, file);
                spec.setActiveFile(file);
                return;
            }

            String content = Files.readString(file);

            Map<String, List<Parser.ParsedEntry>> parsed = parseOrResetOnFailure(spec, content, file);
            if (parsed == null) return;

            for (ElysiumConfigEntry<?> entry : spec.entries()) {
                applyEntryFromParsed(entry, parsed);
            }

            spec.setActiveFile(file);

            String regenerated = Writer.write(spec);
            if (!content.equals(regenerated)) Files.writeString(file, regenerated);
        } catch (IOException e) {
            LOGGER.error("Failed to load config file {}", file, e);
        }
    }

    static void save(Spec spec) {
        Path target = spec.activeFile();
        if (target == null) {
            LOGGER.warn("Refusing to save SERVER config for mod '{}': no active file is set (values are currently synced from a remote server)", spec.modId());
            return;
        }
        writeTo(spec, target);
    }

    @Nullable
    private static Map<String, List<Parser.ParsedEntry>> parseOrResetOnFailure(Spec spec, String content, Path file) {
        try {
            return Parser.parse(content);
        } catch (ConfigParseException e) {
            LOGGER.warn("Malformed config file {}: {} (using defaults, file left untouched)", file, e.getMessage());
            for (ElysiumConfigEntry<?> entry : spec.entries()) entry.resetToDefault();
            return null;
        }
    }

    private static void applyEntryFromParsed(ElysiumConfigEntry<?> entry, Map<String, List<Parser.ParsedEntry>> parsed) {
        List<Parser.ParsedEntry> section = parsed.get(entry.category());
        if (section == null) {
            entry.resetToDefault();
            return;
        }

        Parser.ParsedEntry match = null;
        for (Parser.ParsedEntry candidate : section) {
            if (candidate.name().equals(entry.name())) {
                match = candidate;
                break;
            }
        }
        if (match == null) {
            entry.resetToDefault();
            return;
        }

        try {
            entry.setCurrentFromRaw(match.rawValue());
        } catch (ConfigParseException e) {
            LOGGER.warn("Invalid value for {}.{}: {} (using default)", entry.category(), entry.name(), e.getMessage());
            entry.resetToDefault();
        }
    }

    private static void writeTo(Spec spec, Path file) {
        try {
            Path parent = file.getParent();
            if (parent != null) Files.createDirectories(parent);
            Files.writeString(file, Writer.write(spec));
        } catch (IOException e) {
            LOGGER.error("Failed to save config file {}", file, e);
        }
    }

    static final class Spec {

        private final String modId;
        private final ConfigLoadType loadType;
        private final List<ElysiumConfigEntry<?>> entries;
        private final Path globalFile;
        private Path activeFile;

        private Spec(String modId, ConfigLoadType loadType, List<ElysiumConfigEntry<?>> entries, Path globalFile) {
            this.modId = modId;
            this.loadType = loadType;
            this.entries = List.copyOf(entries);
            this.globalFile = globalFile;
            this.activeFile = globalFile;
        }

        static List<Spec> scan(String modId, Class<?> configClass, Path configRoot) {
            List<ElysiumConfigEntry<?>> allEntries = collectEntries(configClass);

            Map<ConfigLoadType, List<ElysiumConfigEntry<?>>> grouped = new LinkedHashMap<>();
            for (ElysiumConfigEntry<?> entry : allEntries) {
                grouped.computeIfAbsent(entry.loadType(), ignored -> new ArrayList<>()).add(entry);
            }

            List<Spec> specs = new ArrayList<>();
            for (Map.Entry<ConfigLoadType, List<ElysiumConfigEntry<?>>> group : grouped.entrySet()) {
                ConfigLoadType type = group.getKey();
                List<ElysiumConfigEntry<?>> entries = group.getValue();

                Set<String> keys = new HashSet<>();
                for (ElysiumConfigEntry<?> entry : entries) {
                    String key = entry.category() + "." + entry.name();
                    if (!keys.add(key)) {
                        throw new IllegalStateException("Duplicate config key '" + key + "' for mod '" + modId + "' (load type " + type + ")");
                    }
                }

                Path globalFile = configRoot.resolve(modId).resolve(type.fileName() + ".css");
                specs.add(new Spec(modId, type, entries, globalFile));
            }

            return specs;
        }

        private static List<ElysiumConfigEntry<?>> collectEntries(Class<?> configClass) {
            List<ElysiumConfigEntry<?>> allEntries = new ArrayList<>();
            for (Field field : configClass.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers())) continue;
                if (!Modifier.isPublic(field.getModifiers())) continue;
                if (field.getType() != ElysiumConfigEntry.class) continue;

                try {
                    ElysiumConfigEntry<?> entry = (ElysiumConfigEntry<?>) field.get(null);
                    if (entry == null) continue;
                    entry.freeze();
                    allEntries.add(entry);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException("Failed to read config field " + field.getName() + " in " + configClass.getName(), e);
                }
            }
            return allEntries;
        }

        String modId() {
            return modId;
        }

        ConfigLoadType loadType() {
            return loadType;
        }

        List<ElysiumConfigEntry<?>> entries() {
            return entries;
        }

        Path globalFile() {
            return globalFile;
        }

        Path activeFile() {
            return activeFile;
        }

        void setActiveFile(Path file) {
            this.activeFile = file;
        }

        void clearActiveFile() {
            this.activeFile = null;
        }
    }

    private static final class Parser {

        record ParsedEntry(String name, String rawValue) {}

        private final String source;
        private int pos;
        private int line = 1;

        private Parser(String source) {
            this.source = source;
        }

        static Map<String, List<ParsedEntry>> parse(String source) throws ConfigParseException {
            return new Parser(source).doParse();
        }

        private Map<String, List<ParsedEntry>> doParse() throws ConfigParseException {
            Map<String, List<ParsedEntry>> sections = new LinkedHashMap<>();
            while (true) {
                skipWhitespaceAndComments();
                if (atEnd()) return sections;
                if (peek() != '#') throw error("Expected section header starting with '#'");

                consume();
                String name = readIdentifier();
                skipWhitespaceAndComments();
                expect('{');
                sections.computeIfAbsent(name, ignored -> new ArrayList<>());
                parseSectionBody(sections, name);
            }
        }

        private void parseSectionBody(Map<String, List<ParsedEntry>> sections, String path) throws ConfigParseException {
            while (true) {
                skipWhitespaceAndComments();
                if (atEnd()) throw error("Unterminated section '" + path + "'");
                char c = peek();
                if (c == '}') {
                    consume();
                    return;
                }
                if (c == '#') {
                    consume();
                    String name = readIdentifier();
                    skipWhitespaceAndComments();
                    expect('{');
                    String childPath = path + "." + name;
                    sections.computeIfAbsent(childPath, ignored -> new ArrayList<>());
                    parseSectionBody(sections, childPath);
                } else if (isIdentifierStart(c)) {
                    String name = readIdentifier();
                    skipWhitespaceAndComments();
                    expect(':');
                    skipWhitespaceAndComments();
                    String value = readValue();
                    skipWhitespaceAndComments();
                    expect(';');
                    sections.get(path).add(new ParsedEntry(name, value));
                } else {
                    throw error("Unexpected character '" + c + "' in section '" + path + "'");
                }
            }
        }

        private String readValue() throws ConfigParseException {
            if (peek() == '"') return readString();
            if (peek() == '[') return readListValue();

            StringBuilder value = new StringBuilder();
            while (!atEnd()) {
                char c = peek();
                if (c == ';') return value.toString().trim();
                value.append(consume());
            }
            throw error("Unterminated value (missing ';')");
        }

        private String readListValue() throws ConfigParseException {
            int start = pos;
            expect('[');
            boolean inQuotes = false;
            boolean escaped = false;
            while (!atEnd()) {
                char c = consume();
                if (escaped) {
                    escaped = false;
                } else if (inQuotes) {
                    if (c == '\\') escaped = true;
                    else if (c == '"') inQuotes = false;
                } else {
                    if (c == '"') inQuotes = true;
                    else if (c == ']') return source.substring(start, pos);
                }
            }
            throw error("Unterminated list value (missing ']')");
        }

        private String readString() throws ConfigParseException {
            expect('"');
            StringBuilder value = new StringBuilder();
            while (!atEnd()) {
                char c = consume();
                if (c == '"') return value.toString();
                if (c == '\\') {
                    if (atEnd()) throw error("Unterminated escape sequence");
                    char esc = consume();
                    switch (esc) {
                        case '"' -> value.append('"');
                        case '\\' -> value.append('\\');
                        case 'n' -> value.append('\n');
                        case 't' -> value.append('\t');
                        case 'r' -> value.append('\r');
                        case 'u' -> {
                            if (pos + 4 > source.length()) throw error("Incomplete unicode escape");
                            String hex = source.substring(pos, pos + 4);
                            pos += 4;
                            try {
                                value.append((char) Integer.parseInt(hex, 16));
                            } catch (NumberFormatException e) {
                                throw error("Invalid unicode escape '\\u" + hex + "'");
                            }
                        }
                        default -> throw error("Invalid escape sequence '\\" + esc + "'");
                    }
                } else if (c == '\n') {
                    throw error("Newline inside string literal");
                } else {
                    value.append(c);
                }
            }
            throw error("Unterminated string literal");
        }

        private String readIdentifier() throws ConfigParseException {
            if (atEnd() || !isIdentifierStart(peek())) throw error("Expected identifier");
            int start = pos;
            while (!atEnd() && isIdentifierPart(peek())) consume();
            return source.substring(start, pos);
        }

        private void skipWhitespaceAndComments() throws ConfigParseException {
            while (!atEnd()) {
                char c = peek();
                if (Character.isWhitespace(c)) {
                    consume();
                } else if (c == '/') {
                    if (pos + 1 >= source.length()) return;

                    char next = source.charAt(pos + 1);
                    if (next == '*') {
                        consume();
                        consume();
                        boolean closed = false;
                        while (!atEnd()) {
                            if (peek() == '*' && pos + 1 < source.length() && source.charAt(pos + 1) == '/') {
                                consume();
                                consume();
                                closed = true;
                                break;
                            }
                            consume();
                        }
                        if (!closed) throw error("Unterminated block comment");
                    } else if (next == '/') {
                        consume();
                        do { consume(); } while (!atEnd() && peek() != '\n');
                    } else {
                        return;
                    }
                } else {
                    return;
                }
            }
        }

        private void expect(char expected) throws ConfigParseException {
            if (atEnd() || peek() != expected) {
                throw error("Expected '" + expected + "'" + (atEnd() ? " but reached end of file" : " but found '" + peek() + "'"));
            }
            consume();
        }

        private char peek() {
            return source.charAt(pos);
        }

        private char consume() {
            char c = source.charAt(pos++);
            if (c == '\n') line++;
            return c;
        }

        private boolean atEnd() {
            return pos >= source.length();
        }

        private boolean isIdentifierStart(char c) {
            return Character.isLetter(c) || c == '_';
        }

        private boolean isIdentifierPart(char c) {
            return Character.isLetterOrDigit(c) || c == '_' || c == '-';
        }

        private ConfigParseException error(String message) {
            return new ConfigParseException(message + " at line " + line);
        }
    }

    private static final class Writer {

        static String write(Spec spec) {
            StringBuilder builder = new StringBuilder();
            builder.append("/**\n");
            builder.append(" * === Elysium API Config ===\n");
            builder.append(" * Mod: ").append(spec.modId()).append("\n");
            builder.append(" * Load type: ").append(spec.loadType().name()).append("\n");
            builder.append(" */\n");

            CategoryNode root = buildTree(spec.entries());
            boolean first = true;
            for (CategoryNode child : root.children.values()) {
                if (!first) builder.append("\n");
                writeSection(builder, child, 0);
                first = false;
            }
            return builder.toString();
        }

        private static CategoryNode buildTree(List<ElysiumConfigEntry<?>> entries) {
            CategoryNode root = new CategoryNode("");
            for (ElysiumConfigEntry<?> entry : entries) {
                if (entry.category() == null) continue;
                CategoryNode node = root;
                for (String part : entry.category().split("\\.")) {
                    node = node.children.computeIfAbsent(part, CategoryNode::new);
                }
                node.entries.add(entry);
            }
            return root;
        }

        private static void writeSection(StringBuilder builder, CategoryNode node, int depth) {
            String indent = "\t".repeat(depth);
            builder.append(indent).append("#").append(node.name).append(" {\n");

            boolean needsBlank = false;
            for (ElysiumConfigEntry<?> entry : node.entries) {
                if (needsBlank) builder.append("\n");
                writeEntry(builder, entry, depth + 1);
                needsBlank = true;
            }

            for (CategoryNode child : node.children.values()) {
                if (needsBlank) builder.append("\n");
                writeSection(builder, child, depth + 1);
                needsBlank = true;
            }

            builder.append(indent).append("}\n");
        }

        private static <T> void writeEntry(StringBuilder builder, ElysiumConfigEntry<T> entry, int depth) {
            String indent = "\t".repeat(depth);
            boolean isEnum = entry.type() == ElysiumConfigEntry.ConfigValueType.ENUM;
            boolean isList = entry.type().isList();

            builder.append(indent).append("/**\n");

            String comment = entry.comment();
            if (comment != null && !comment.isEmpty()) {
                for (String line : comment.split("\n", -1)) {
                    builder.append(indent).append(" * ").append(line).append("\n");
                }
            }

            if (!isList) {
                builder.append(indent).append(" * DEFAULT: ");
                String defaultValue = entry.emitValue(entry.defaultValue());
                if (isEnum) defaultValue = "\"" + defaultValue + "\"";
                builder.append(defaultValue).append("\n");
            }

            String range = entry.describeRange();
            if (range != null) builder.append(indent).append(" * RANGE: ").append(range).append("\n");

            String allowed = entry.describeAllowedValues();
            if (allowed != null) builder.append(indent).append(" * ALLOWED VALUES: ").append(allowed).append("\n");

            builder.append(indent).append(" */\n");
            builder.append(indent).append(entry.name()).append(": ").append(entry.emitValue(entry.get())).append(";\n");
        }

        private static final class CategoryNode {
            final String name;
            final List<ElysiumConfigEntry<?>> entries = new ArrayList<>();
            final Map<String, CategoryNode> children = new LinkedHashMap<>();

            CategoryNode(String name) {
                this.name = name;
            }
        }
    }
}