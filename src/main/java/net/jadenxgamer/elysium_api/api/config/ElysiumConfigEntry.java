package net.jadenxgamer.elysium_api.api.config;

import net.jadenxgamer.elysium_api.impl.config.ConfigElementCodecs;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A declarative, typed handle for a single config value.
 * <p>
 * An entry is created via one of the static {@code define*}, {@code defineInRange}, or
 * {@code defineInList} factory methods, then enriched with metadata through the builder
 * methods before being frozen and registered. The typical pattern is a single class of
 * {@code public static final} fields, one per entry, passed to
 * {@link ElysiumConfigManager#register(String, Class)} during mod initialization.
 * <p>
 * At callsites, {@link #get()} returns the current value with full type safety.
 * The factory methods shape goes as following:
 * <ul>
 * <li>{@code define(name, defaultValue)} - an unconstrained value of the inferred type
 * <li>{@code defineInRange(name, defaultValue, min, max)} - a numeric value bounded to an inclusive range
 * <li>{@code defineInList(name, defaultValue, allowedValues)} - a scalar value restricted to a fixed set
 * <li>{@code defineInList(name, defaultValue, elementType)} - a list of values
 * <li>{@code defineInList(name, defaultValue, elementType, allowedValues)} - a list whose elements are restricted to a fixed set
 * </ul>
 * Every factory method returns the entry itself so metadata can be chained directly:
 * <pre>{@code
 * public static final ElysiumConfigEntry<Integer> AMOUNT =
 *     ElysiumConfigEntry.defineInRange("amount", 10, 1, 100)
 *         .category("general")
 *         .comment("How much of the thing to do");
 * }</pre>
 * Once an entry has been registered it becomes frozen, and any further builder call throws.
 *
 * @param <T> the Java type of the entry's value
 */
public final class ElysiumConfigEntry<T> {

    private interface Parser<T> { T parse(String raw) throws ConfigParseException; }
    private interface Emitter<T> { String emit(T value); }

    private final String name;
    private final ConfigValueType type;
    @Nullable private final Class<? extends Enum<?>> enumClass;
    private final T defaultValue;
    private final Parser<T> parser;
    private final Emitter<T> emitter;

    private @Nullable String category;
    private @Nullable String comment;
    private ConfigLoadType loadType = ConfigLoadType.COMMON;
    @Nullable private Number rangeMin;
    @Nullable private Number rangeMax;
    @Nullable private List<?> allowedValues;
    private boolean gameRestart;
    private boolean worldRestart;

    private volatile T currentValue;
    private volatile boolean serverSynced;
    private boolean frozen;

    private ElysiumConfigEntry(String name, ConfigValueType type, @Nullable Class<? extends Enum<?>> enumClass,
                               T defaultValue, Parser<T> parser, Emitter<T> emitter) {
        this.name = name;
        this.type = type;
        this.enumClass = enumClass;
        this.defaultValue = defaultValue;
        this.parser = parser;
        this.emitter = emitter;
        this.currentValue = defaultValue;
    }

    /**
     * Creates an unconstrained boolean entry.
     *
     * @param name the entry's name
     * @param defaultValue the default value
     * @return the entry
     */
    public static ElysiumConfigEntry<Boolean> define(String name, boolean defaultValue) {
        return new ElysiumConfigEntry<>(
                name, ConfigValueType.BOOLEAN, null, defaultValue,
                raw -> {
                    String trimmed = raw.trim();
                    if ("true".equalsIgnoreCase(trimmed)) return true;
                    if ("false".equalsIgnoreCase(trimmed)) return false;
                    throw new ConfigParseException("Expected 'true' or 'false', got '" + raw + "'");
                },
                String::valueOf
        );
    }

    /**
     * Creates an unconstrained integer entry.
     *
     * @param name the entry's name
     * @param defaultValue the default value
     * @return the entry
     */
    public static ElysiumConfigEntry<Integer> define(String name, int defaultValue) {
        return defineNumber(name, ConfigValueType.INT, "integer", defaultValue, Integer::parseInt);
    }

    /**
     * Creates an unconstrained long entry.
     *
     * @param name the entry's name
     * @param defaultValue the default value
     * @return the entry
     */
    public static ElysiumConfigEntry<Long> define(String name, long defaultValue) {
        return defineNumber(name, ConfigValueType.LONG, "long", defaultValue, Long::parseLong);
    }

    /**
     * Creates an unconstrained float entry.
     *
     * @param name the entry's name
     * @param defaultValue the default value
     * @return the entry
     */
    public static ElysiumConfigEntry<Float> define(String name, float defaultValue) {
        return defineNumber(name, ConfigValueType.FLOAT, "float", defaultValue, Float::parseFloat);
    }

    /**
     * Creates an unconstrained double entry.
     *
     * @param name the entry's name
     * @param defaultValue the default value
     * @return the entry
     */
    public static ElysiumConfigEntry<Double> define(String name, double defaultValue) {
        return defineNumber(name, ConfigValueType.DOUBLE, "double", defaultValue, Double::parseDouble);
    }

    /**
     * Creates an unconstrained string entry.
     *
     * @param name the entry's name
     * @param defaultValue the default value
     * @return the entry
     */
    public static ElysiumConfigEntry<String> define(String name, String defaultValue) {
        return new ElysiumConfigEntry<>(name, ConfigValueType.STRING, null, defaultValue,
                raw -> raw, value -> "\"" + ConfigElementCodecs.ConfigElementCodec.escape(value) + "\"");
    }

    /**
     * Creates an unconstrained enum entry.
     *
     * @param name the entry's name
     * @param defaultValue the default enum constant
     * @param enumClass the enum class
     * @param <E> the enum type
     * @return the entry
     */
    public static <E extends Enum<E>> ElysiumConfigEntry<E> define(String name, E defaultValue, Class<E> enumClass) {
        ConfigElementCodecs.ConfigElementCodec<E> codec = ConfigElementCodecs.enumType(enumClass);
        ElysiumConfigEntry<E> entry = new ElysiumConfigEntry<>(name, ConfigValueType.ENUM, enumClass, defaultValue, codec::parse, codec::emit);
        E[] constants = enumClass.getEnumConstants();
        if (constants != null) entry.allowedValues = List.of(constants);
        return entry;
    }

    /**
     * Creates an integer entry bounded to the inclusive range {@code [min, max]}.
     *
     * @param name the entry's name
     * @param defaultValue the default value
     * @param min the inclusive lower bound
     * @param max the inclusive upper bound
     * @return the entry
     */
    public static ElysiumConfigEntry<Integer> defineInRange(String name, int defaultValue, int min, int max) {
        return bounded(define(name, defaultValue), min, max);
    }

    /**
     * Creates a long entry bounded to the inclusive range {@code [min, max]}.
     *
     * @param name the entry's name
     * @param defaultValue the default value
     * @param min the inclusive lower bound
     * @param max the inclusive upper bound
     * @return the entry
     * @see #defineInRange(String, int, int, int)
     */
    public static ElysiumConfigEntry<Long> defineInRange(String name, long defaultValue, long min, long max) {
        return bounded(define(name, defaultValue), min, max);
    }

    /**
     * Creates a float entry bounded to the inclusive range {@code [min, max]}.
     *
     * @param name the entry's name
     * @param defaultValue the default value
     * @param min the inclusive lower bound
     * @param max the inclusive upper bound
     * @return the entry
     * @see #defineInRange(String, int, int, int)
     */
    public static ElysiumConfigEntry<Float> defineInRange(String name, float defaultValue, float min, float max) {
        return bounded(define(name, defaultValue), min, max);
    }

    /**
     * Creates a double entry bounded to the inclusive range {@code [min, max]}.
     *
     * @param name the entry's name
     * @param defaultValue the default value
     * @param min the inclusive lower bound
     * @param max the inclusive upper bound
     * @return the entry
     * @see #defineInRange(String, int, int, int)
     */
    public static ElysiumConfigEntry<Double> defineInRange(String name, double defaultValue, double min, double max) {
        return bounded(define(name, defaultValue), min, max);
    }

    /**
     * Creates a boolean entry restricted to the given allowed set.
     * <p>
     * Values read from the file that are not in {@code allowedValues} are rejected with a
     * warning and the default is used. The allowed set is rendered in the
     * {@code ALLOWED VALUES:} metadata comment.
     *
     * @param name the entry's name
     * @param defaultValue the default value; must be present in {@code allowedValues}
     * @param allowedValues the complete set of values the entry may hold
     * @return the entry
     */
    public static ElysiumConfigEntry<Boolean> defineInList(String name, boolean defaultValue, Collection<Boolean> allowedValues) {
        List<Boolean> allowed = List.copyOf(allowedValues);
        return withValidator(define(name, defaultValue), allowed::contains, allowed);
    }

    /**
     * Creates an integer entry restricted to the given allowed set.
     *
     * @param name the entry's name
     * @param defaultValue the default value
     * @param allowedValues the complete set of values the entry may hold
     * @return the entry
     * @see #defineInList(String, boolean, Collection)
     */
    public static ElysiumConfigEntry<Integer> defineInList(String name, int defaultValue, Collection<Integer> allowedValues) {
        List<Integer> allowed = List.copyOf(allowedValues);
        return withValidator(define(name, defaultValue), allowed::contains, allowed);
    }

    /**
     * Creates a long entry restricted to the given allowed set.
     *
     * @param name the entry's name
     * @param defaultValue the default value
     * @param allowedValues the complete set of values the entry may hold
     * @return the entry
     * @see #defineInList(String, boolean, Collection)
     */
    public static ElysiumConfigEntry<Long> defineInList(String name, long defaultValue, Collection<Long> allowedValues) {
        List<Long> allowed = List.copyOf(allowedValues);
        return withValidator(define(name, defaultValue), allowed::contains, allowed);
    }

    /**
     * Creates a float entry restricted to the given allowed set.
     *
     * @param name the entry's name
     * @param defaultValue the default value
     * @param allowedValues the complete set of values the entry may hold
     * @return the entry
     * @see #defineInList(String, boolean, Collection)
     */
    public static ElysiumConfigEntry<Float> defineInList(String name, float defaultValue, Collection<Float> allowedValues) {
        List<Float> allowed = List.copyOf(allowedValues);
        return withValidator(define(name, defaultValue), allowed::contains, allowed);
    }

    /**
     * Creates a double entry restricted to the given allowed set.
     *
     * @param name the entry's name
     * @param defaultValue the default value
     * @param allowedValues the complete set of values the entry may hold
     * @return the entry
     * @see #defineInList(String, boolean, Collection)
     */
    public static ElysiumConfigEntry<Double> defineInList(String name, double defaultValue, Collection<Double> allowedValues) {
        List<Double> allowed = List.copyOf(allowedValues);
        return withValidator(define(name, defaultValue), allowed::contains, allowed);
    }

    /**
     * Creates a string entry restricted to the given allowed set.
     *
     * @param name the entry's name
     * @param defaultValue the default value
     * @param allowedValues the complete set of values the entry may hold
     * @return the entry
     * @see #defineInList(String, boolean, Collection)
     */
    public static ElysiumConfigEntry<String> defineInList(String name, String defaultValue, Collection<String> allowedValues) {
        List<String> allowed = List.copyOf(allowedValues);
        return withValidator(define(name, defaultValue), allowed::contains, allowed);
    }

    /**
     * Creates an enum entry restricted to the given allowed set.
     *
     * @param name the entry's name
     * @param defaultValue the default enum constant
     * @param allowedValues the permitted constants; must include {@code defaultValue}
     * @param <E> the enum type
     * @return the entry
     */
    public static <E extends Enum<E>> ElysiumConfigEntry<E> defineInList(String name, E defaultValue, Collection<E> allowedValues) {
        List<E> allowed = List.copyOf(allowedValues);
        Class<E> enumClass = defaultValue.getDeclaringClass();
        return withValidator(define(name, defaultValue, enumClass), allowed::contains, allowed);
    }

    /**
     * Creates a list of the given element type with the given default contents.
     * <p>
     * The element type must be one of {@link Boolean}, {@link Integer}, {@link Long},
     * {@link Float}, {@link Double}, {@link String}, or any Enum class.
     *
     * @param name the entry's name
     * @param defaultValue the default list
     * @param elementType the element class, like {@code Integer.class} or {@code SomeEnum.class}
     * @param <E> the element type
     * @return the entry
     */
    public static <E> ElysiumConfigEntry<List<E>> defineInList(String name, List<E> defaultValue, Class<E> elementType) {
        return newListGeneric(name, defaultValue, elementType, null);
    }

    /**
     * Creates a list of the given element type whose elements are restricted to the given
     * allowed set.
     * <p>
     * On load, any element that is not in {@code allowedValues} causes the entire list to
     * be rejected with a warning and the default list is used.
     *
     * @param name the entry's name
     * @param defaultValue the default list; every element must be present in {@code allowedValues}
     * @param elementType the element class, like {@code Integer.class} or {@code SomeEnum.class}
     * @param allowedValues the complete set of values each element may hold
     * @param <E> the element type
     * @return the entry
     * @see #defineInList(String, List, Class)
     */
    public static <E> ElysiumConfigEntry<List<E>> defineInList(String name, List<E> defaultValue, Class<E> elementType, Collection<E> allowedValues) {
        return newListGeneric(name, defaultValue, elementType, List.copyOf(allowedValues));
    }

    private static <N extends Number> ElysiumConfigEntry<N> defineNumber(
            String name, ConfigValueType type, String label, N defaultValue, Function<String, N> parse) {
        return new ElysiumConfigEntry<>(
                name, type, null, defaultValue,
                raw -> {
                    try { return parse.apply(raw.trim()); }
                    catch (NumberFormatException e) { throw new ConfigParseException("Expected " + label + ", got '" + raw + "'"); }
                },
                String::valueOf
        );
    }

    private static <N extends Number> ElysiumConfigEntry<N> bounded(ElysiumConfigEntry<N> entry, N min, N max) {
        entry.rangeMin = min;
        entry.rangeMax = max;
        return entry;
    }

    private static <T> ElysiumConfigEntry<T> withValidator(ElysiumConfigEntry<T> base, Predicate<T> validator, Collection<?> allowed) {
        ElysiumConfigEntry<T> entry = new ElysiumConfigEntry<>(
                base.name, base.type, base.enumClass, base.defaultValue,
                raw -> {
                    T parsed = base.parser.parse(raw);
                    if (!validator.test(parsed)) {
                        throw new ConfigParseException("Value '" + parsed + "' is not allowed");
                    }
                    return parsed;
                },
                base.emitter
        );
        entry.allowedValues = List.copyOf(allowed);
        return entry;
    }

    @SuppressWarnings("unchecked")
    private static <E> ElysiumConfigEntry<List<E>> newListGeneric(
            String name, List<E> defaultValue, Class<E> elementType, @Nullable List<E> userAllowedValues) {
        ConfigElementCodecs.ConfigElementCodec<E> scalar = ConfigElementCodecs.codecFor(elementType);
        ConfigElementCodecs.ConfigElementCodec<List<E>> listCodec = ConfigElementCodecs.listCodec(
                userAllowedValues == null ? scalar : ConfigElementCodecs.constrained(scalar, userAllowedValues)
        );

        List<?> displayAllowedValues = userAllowedValues;
        if (displayAllowedValues == null && elementType.isEnum()) {
            Object[] constants = elementType.getEnumConstants();
            displayAllowedValues = constants != null ? List.of(constants) : null;
        }

        Class<? extends Enum<?>> enumClass = elementType.isEnum() ? (Class<? extends Enum<?>>) elementType : null;
        ElysiumConfigEntry<List<E>> entry = new ElysiumConfigEntry<>(
                name, listTypeFor(elementType), enumClass, List.copyOf(defaultValue),
                listCodec::parse, listCodec::emit
        );
        entry.allowedValues = displayAllowedValues;
        return entry;
    }

    private static ConfigValueType listTypeFor(Class<?> elementType) {
        if (elementType == Boolean.class) return ConfigValueType.LIST_OF_BOOLEAN;
        if (elementType == Integer.class) return ConfigValueType.LIST_OF_INT;
        if (elementType == Long.class) return ConfigValueType.LIST_OF_LONG;
        if (elementType == Float.class) return ConfigValueType.LIST_OF_FLOAT;
        if (elementType == Double.class) return ConfigValueType.LIST_OF_DOUBLE;
        if (elementType == String.class) return ConfigValueType.LIST_OF_STRING;
        if (elementType.isEnum()) return ConfigValueType.LIST_OF_ENUM;
        throw new IllegalArgumentException("Unsupported list element type: " + elementType.getName() + ". Supported types: Boolean, Integer, Long, Float, Double, String, and any enum.");
    }

    /**
     * Assigns the category path the entry belongs to.
     * <p>
     * Categories are dotted paths such as {@code "general"} or {@code "general.other"}.
     * Each segment becomes a nested section in the emitted config file. This method is
     * required; an entry without a category, it will throw at registration time.
     *
     * @param category the dotted category path
     * @return this entry, for chaining
     */
    public ElysiumConfigEntry<T> category(String category) {
        checkNotFrozen();
        this.category = category;
        return this;
    }

    /**
     * Attaches a description to the entry.
     * <p>
     * The text appears as a Javadoc-style comment block above the entry in the emitted
     * config file, before any parameter guidelines lines. It is regenerated on every save.
     * Multiline comments are supported via embedded newlines.
     *
     * @param comment the description text
     * @return this entry, for chaining
     */
    public ElysiumConfigEntry<T> comment(String comment) {
        checkNotFrozen();
        this.comment = comment;
        return this;
    }

    /**
     * Assigns which load stage this entry belongs to.
     * <p>
     * Entries of different load types are grouped into separate files. The default is
     * {@link ConfigLoadType#COMMON} when this method is not called.
     *
     * @param loadType the load type this entry belongs to
     * @return this entry, for chaining
     */
    public ElysiumConfigEntry<T> loadType(ConfigLoadType loadType) {
        checkNotFrozen();
        this.loadType = loadType;
        return this;
    }

    /**
     * Marks the entry as requiring a full game restart for value changes to take effect.
     * <p>
     * This is a hint for the configuration screen and for the reload command; use it for
     * entries that cannot change values dynamically on runtime.
     *
     * @return this entry, for chaining
     */
    public ElysiumConfigEntry<T> gameRestart() {
        checkNotFrozen();
        this.gameRestart = true;
        return this;
    }

    /**
     * Marks the entry as requiring a world exit and re-entry for value changes to take effect.
     * <p>
     * This is a hint for the configuration screen and for the reload command; use it for
     * entries that are read once when a world is loaded.
     *
     * @return this entry, for chaining
     */
    public ElysiumConfigEntry<T> worldRestart() {
        checkNotFrozen();
        this.worldRestart = true;
        return this;
    }

    /**
     * Returns the current value of the entry.
     * <p>
     * The returned reference is the live in-memory value, populated from the on-disk file
     * at registration time and refreshed by reload or server sync. Callsites should read
     * this every time they need the value rather than caching the result.
     *
     * @return the current value
     */
    public T get() { return currentValue; }

    /**
     * {@return the entry's name, used as its key inside its category}
     */
    public String name() { return name; }

    /**
     * {@return the entry's value type}
     */
    public ConfigValueType type() { return type; }

    /**
     * {@return the entry's default value, as provided at declaration time}
     */
    public T defaultValue() { return defaultValue; }

    /**
     * {@return the entry's category path, or {@code null} if not yet assigned}
     */
    @Nullable public String category() { return category; }

    /**
     * {@return the entry's comment, or {@code null} if none was provided}
     */
    @Nullable public String comment() { return comment; }

    /**
     * {@return the entry's load type}
     */
    public ConfigLoadType loadType() { return loadType; }

    /**
     * {@return true if this entry was marked with {@link #gameRestart()}}
     */
    public boolean requiresGameRestart() { return gameRestart; }

    /**
     * {@return true if this entry was marked with {@link #worldRestart()}}
     */
    public boolean requiresWorldRestart() { return worldRestart; }

    /**
     * {@return true if the current value originated from a server sync rather than from disk}
     * <p>
     * Server-synced values are treated as read-only by the config screen, and are reverted
     * the next time the entry is reloaded from disk, such as when the client disconnects.
     * This flag is cleared automatically by any file-driven load path.
     */
    public boolean isServerSynced() { return serverSynced; }

    /**
     * {@return the element type if this entry is a list, or {@code null} otherwise}
     */
    @Nullable
    public ConfigValueType elementType() {
        return type.elementType();
    }

    @ApiStatus.Internal
    public String emitValue(T value) {
        return emitter.emit(value);
    }

    @ApiStatus.Internal
    public String emitForSync() {
        if (type == ConfigValueType.STRING) {
            return (String) currentValue;
        }
        return emitter.emit(currentValue);
    }

    @ApiStatus.Internal
    public void applyServerSync(String raw) throws ConfigParseException {
        this.currentValue = parser.parse(raw);
        this.serverSynced = true;
    }

    @ApiStatus.Internal
    public void setCurrentFromRaw(String raw) throws ConfigParseException {
        this.currentValue = parser.parse(raw);
        this.serverSynced = false;
    }

    @ApiStatus.Internal
    public void setCurrent(T value) {
        this.currentValue = value;
    }

    @ApiStatus.Internal
    public void resetToDefault() {
        this.currentValue = defaultValue;
        this.serverSynced = false;
    }

    @ApiStatus.Internal
    public void freeze() {
        if (name == null || name.isEmpty()) throw new IllegalStateException("Config entry has an empty name");
        if (!name.matches("[a-zA-Z_][a-zA-Z0-9_-]*")) throw new IllegalStateException("Config entry '" + name + "' has an invalid name; must match [a-zA-Z_][a-zA-Z0-9_-]*");
        if (category == null) throw new IllegalStateException("Config entry '" + name + "' has no category");
        if (!category.matches("[a-zA-Z_][a-zA-Z0-9_-]*(\\.[a-zA-Z_][a-zA-Z0-9_-]*)*")) throw new IllegalStateException("Config entry '" + name + "' has an invalid category path '" + category + "'");

        this.frozen = true;
    }

    @ApiStatus.Internal
    @Nullable
    public Class<? extends Enum<?>> enumClass() {
        return enumClass;
    }

    @ApiStatus.Internal
    @Nullable
    public String describeRange() {
        if (rangeMin == null || rangeMax == null) return null;

        boolean minAtSentinel = isSentinel(rangeMin, true);
        boolean maxAtSentinel = isSentinel(rangeMax, false);
        if (minAtSentinel && maxAtSentinel) return null;
        if (minAtSentinel) return "< " + format(rangeMax, 1);
        if (maxAtSentinel) return "> " + format(rangeMin, -1);
        return format(rangeMin, 0) + " to " + format(rangeMax, 0);
    }

    @ApiStatus.Internal
    @Nullable
    public String describeAllowedValues() {
        if (allowedValues == null || allowedValues.isEmpty()) return null;
        ConfigValueType effective = type.isList() ? type.elementType() : type;
        boolean needsQuotes = effective == ConfigValueType.STRING || effective == ConfigValueType.ENUM;
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < allowedValues.size(); i++) {
            if (i > 0) builder.append(", ");
            if (needsQuotes) {
                Object element = allowedValues.get(i);
                String text = element instanceof Enum<?> e ? e.name() : String.valueOf(element);
                builder.append('"').append(text).append('"');
            } else {
                builder.append(allowedValues.get(i));
            }
        }
        return builder.toString();
    }

    private boolean isSentinel(Number n, boolean minimum) {
        return switch (type) {
            case INT -> minimum ? n.intValue() == Integer.MIN_VALUE : n.intValue() == Integer.MAX_VALUE;
            case LONG -> minimum ? n.longValue() == Long.MIN_VALUE : n.longValue() == Long.MAX_VALUE;
            case FLOAT -> minimum ? n.floatValue() == Float.NEGATIVE_INFINITY : n.floatValue() == Float.POSITIVE_INFINITY;
            case DOUBLE -> minimum ? n.doubleValue() == Double.NEGATIVE_INFINITY : n.doubleValue() == Double.POSITIVE_INFINITY;
            default -> false;
        };
    }

    private String format(Number n, long offset) {
        return switch (type) {
            case INT -> Long.toString(n.intValue() + offset);
            case LONG -> Long.toString(n.longValue() + offset);
            case FLOAT -> Float.toString(n.floatValue());
            case DOUBLE -> Double.toString(n.doubleValue());
            default -> String.valueOf(n);
        };
    }

    private void checkNotFrozen() {
        if (frozen) throw new IllegalStateException("Config entry '" + name + "' is already registered and cannot be modified");
    }

    /**
     * The set of value shapes an entry can hold.
     */
    public enum ConfigValueType {
        BOOLEAN, INT, LONG, FLOAT, DOUBLE, STRING, ENUM,
        LIST_OF_BOOLEAN, LIST_OF_INT, LIST_OF_LONG, LIST_OF_FLOAT, LIST_OF_DOUBLE, LIST_OF_STRING, LIST_OF_ENUM;

        /**
         * {@return true if this type represents a list}
         */
        public boolean isList() {
            return switch (this) {
                case LIST_OF_BOOLEAN, LIST_OF_INT, LIST_OF_LONG, LIST_OF_FLOAT, LIST_OF_DOUBLE, LIST_OF_STRING, LIST_OF_ENUM -> true;
                default -> false;
            };
        }

        /**
         * {@return the scalar element type for a list constant, or {@code null} for scalars}
         */
        @Nullable
        public ConfigValueType elementType() {
            return switch (this) {
                case LIST_OF_BOOLEAN -> BOOLEAN;
                case LIST_OF_INT -> INT;
                case LIST_OF_LONG -> LONG;
                case LIST_OF_FLOAT -> FLOAT;
                case LIST_OF_DOUBLE -> DOUBLE;
                case LIST_OF_STRING -> STRING;
                case LIST_OF_ENUM -> ENUM;
                default -> null;
            };
        }
    }
}