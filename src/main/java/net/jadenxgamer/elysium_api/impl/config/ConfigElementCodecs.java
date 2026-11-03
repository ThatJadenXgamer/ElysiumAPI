package net.jadenxgamer.elysium_api.impl.config;

import net.jadenxgamer.elysium_api.api.config.ConfigParseException;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@ApiStatus.Internal
public final class ConfigElementCodecs {

    private ConfigElementCodecs() {
    }

    public static final ConfigElementCodec<Boolean> BOOLEAN = new ConfigElementCodec<>() {
        public Boolean parse(String token) throws ConfigParseException {
            String trimmed = token.trim();
            if ("true".equalsIgnoreCase(trimmed)) return true;
            if ("false".equalsIgnoreCase(trimmed)) return false;
            throw new ConfigParseException("Expected 'true' or 'false', got '" + token + "'");
        }
        public String emit(Boolean value) { return Boolean.toString(value); }
    };

    public static final ConfigElementCodec<Integer> INT = new ConfigElementCodec<>() {
        public Integer parse(String token) throws ConfigParseException {
            try { return Integer.parseInt(token.trim()); }
            catch (NumberFormatException e) { throw new ConfigParseException("Expected integer, got '" + token + "'"); }
        }
        public String emit(Integer value) { return Integer.toString(value); }
    };

    public static final ConfigElementCodec<Long> LONG = new ConfigElementCodec<>() {
        public Long parse(String token) throws ConfigParseException {
            try { return Long.parseLong(token.trim()); }
            catch (NumberFormatException e) { throw new ConfigParseException("Expected long, got '" + token + "'"); }
        }
        public String emit(Long value) { return Long.toString(value); }
    };

    public static final ConfigElementCodec<Float> FLOAT = new ConfigElementCodec<>() {
        public Float parse(String token) throws ConfigParseException {
            try { return Float.parseFloat(token.trim()); }
            catch (NumberFormatException e) { throw new ConfigParseException("Expected float, got '" + token + "'"); }
        }
        public String emit(Float value) { return Float.toString(value); }
    };

    public static final ConfigElementCodec<Double> DOUBLE = new ConfigElementCodec<>() {
        public Double parse(String token) throws ConfigParseException {
            try { return Double.parseDouble(token.trim()); }
            catch (NumberFormatException e) { throw new ConfigParseException("Expected double, got '" + token + "'"); }
        }
        public String emit(Double value) { return Double.toString(value); }
    };

    public static final ConfigElementCodec<String> STRING = new ConfigElementCodec<>() {
        public String parse(String token) throws ConfigParseException {
            String trimmed = token.trim();
            if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
                return ConfigElementCodec.unescape(trimmed.substring(1, trimmed.length() - 1));
            }
            return trimmed;
        }
        public String emit(String value) { return "\"" + ConfigElementCodec.escape(value) + "\""; }
    };

    public static <E extends Enum<E>> ConfigElementCodec<E> enumType(Class<E> enumClass) {
        return new ConfigElementCodec<>() {
            public E parse(String token) throws ConfigParseException {
                try { return Enum.valueOf(enumClass, token.trim()); }
                catch (IllegalArgumentException e) { throw new ConfigParseException("Invalid enum value '" + token + "' for " + enumClass.getSimpleName()); }
            }
            public String emit(E value) { return value.name(); }
        };
    }

    public static ConfigElementCodec<String> constrainedString(List<String> allowed) {
        return new ConfigElementCodec<>() {
            public String parse(String token) throws ConfigParseException {
                String trimmed = token.trim();
                String value = trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")
                        ? ConfigElementCodec.unescape(trimmed.substring(1, trimmed.length() - 1))
                        : trimmed;
                if (!allowed.contains(value)) {
                    throw new ConfigParseException("Value '" + value + "' is not allowed; must be one of " + allowed);
                }
                return value;
            }
            public String emit(String value) { return "\"" + ConfigElementCodec.escape(value) + "\""; }
        };
    }

    @SuppressWarnings("unchecked")
    public static <E> ConfigElementCodec<E> codecFor(Class<E> elementType) {
        if (elementType == Boolean.class) return (ConfigElementCodec<E>) BOOLEAN;
        if (elementType == Integer.class) return (ConfigElementCodec<E>) INT;
        if (elementType == Long.class) return (ConfigElementCodec<E>) LONG;
        if (elementType == Float.class) return (ConfigElementCodec<E>) FLOAT;
        if (elementType == Double.class) return (ConfigElementCodec<E>) DOUBLE;
        if (elementType == String.class) return (ConfigElementCodec<E>) STRING;
        if (elementType.isEnum()) return (ConfigElementCodec<E>) enumType((Class<? extends Enum>) elementType);
        throw new IllegalArgumentException("Unsupported element type: " + elementType.getName() + ". Supported types: Boolean, Integer, Long, Float, Double, String, and any enum.");
    }

    public static <E> ConfigElementCodec<E> constrained(ConfigElementCodec<E> delegate, Collection<E> allowed) {
        return new ConfigElementCodec<>() {
            public E parse(String token) throws ConfigParseException {
                E value = delegate.parse(token);
                if (!allowed.contains(value)) {
                    throw new ConfigParseException("Value '" + value + "' is not allowed; must be one of " + allowed);
                }
                return value;
            }
            public String emit(E value) { return delegate.emit(value); }
        };
    }

    public static <E> ConfigElementCodec<List<E>> listCodec(ConfigElementCodec<E> scalar) {
        return new ConfigElementCodec<>() {
            public List<E> parse(String raw) throws ConfigParseException {
                String trimmed = raw.trim();
                if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
                    throw new ConfigParseException("Expected list, got '" + raw + "'");
                }
                String inner = trimmed.substring(1, trimmed.length() - 1);
                List<String> tokens = splitElements(inner);
                List<E> result = new ArrayList<>(tokens.size());
                for (String token : tokens) {
                    result.add(scalar.parse(token));
                }
                return result;
            }
            public String emit(List<E> list) {
                if (list.isEmpty()) return "[]";
                StringBuilder builder = new StringBuilder("[");
                for (int i = 0; i < list.size(); i++) {
                    if (i > 0) builder.append(", ");
                    builder.append(scalar.emit(list.get(i)));
                }
                return builder.append("]").toString();
            }
        };
    }

    private static List<String> splitElements(String inner) throws ConfigParseException {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        boolean escaped = false;
        for (int i = 0; i < inner.length(); i++) {
            char c = inner.charAt(i);
            if (escaped) {
                current.append(c);
                escaped = false;
            } else if (inQuotes) {
                if (c == '\\') { current.append(c); escaped = true; }
                else if (c == '"') { current.append(c); inQuotes = false; }
                else current.append(c);
            } else {
                if (c == '"') { current.append(c); inQuotes = true; }
                else if (c == ',') {
                    String token = current.toString().trim();
                    if (!token.isEmpty()) tokens.add(token);
                    current.setLength(0);
                } else current.append(c);
            }
        }
        if (inQuotes) throw new ConfigParseException("Unterminated string in list");
        String lastToken = current.toString().trim();
        if (!lastToken.isEmpty()) tokens.add(lastToken);
        return tokens;
    }

    public static interface ConfigElementCodec<E> {

        E parse(String token) throws ConfigParseException;

        String emit(E value);

        @ApiStatus.Internal
        static String escape(String s) {
            StringBuilder builder = new StringBuilder(s.length());
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                switch (c) {
                    case '"' -> builder.append("\\\"");
                    case '\\' -> builder.append("\\\\");
                    case '\n' -> builder.append("\\n");
                    case '\t' -> builder.append("\\t");
                    case '\r' -> builder.append("\\r");
                    default -> builder.append(c);
                }
            }
            return builder.toString();
        }

        @ApiStatus.Internal
        static String unescape(String s) throws ConfigParseException {
            StringBuilder builder = new StringBuilder(s.length());
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c == '\\' && i + 1 < s.length()) {
                    char next = s.charAt(++i);
                    switch (next) {
                        case '"' -> builder.append('"');
                        case '\\' -> builder.append('\\');
                        case 'n' -> builder.append('\n');
                        case 't' -> builder.append('\t');
                        case 'r' -> builder.append('\r');
                        default -> throw new ConfigParseException("Invalid escape sequence '\\" + next + "'");
                    }
                } else {
                    builder.append(c);
                }
            }
            return builder.toString();
        }
    }
}