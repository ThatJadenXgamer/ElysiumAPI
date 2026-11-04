package net.jadenxgamer.elysium_api.api.unification;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Keyable;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;

/**
 * A version-agnostic backport of 1.21.1's {@link StringRepresentable} that works across versions
 * <p>
 * Extends vanilla's {@link StringRepresentable} so any {@link ElysiumStringRepresentable} instance can be
 * used wherever a vanilla {@code StringRepresentable} is expected, while exposing the full 1.21.1 codec
 * API surface ({@link #fromEnum}, {@link #fromEnumWithMapping}, {@link #fromValues}, {@link #createNameLookup},
 * {@link #keys}) as static helpers plus the codec classes they construct. Use this instead of
 * {@link StringRepresentable} throughout Elysium and any downstream mods.
 */
public interface ElysiumStringRepresentable extends StringRepresentable {

    int PRE_BUILT_MAP_THRESHOLD = 16;

    /**
     * Creates an enum-based codec that has {@link EnumCodec#byName(String)} lookups available.
     * <br>
     * Equivalent to 1.21.1's {@code StringRepresentable.fromEnum}.
     *
     * @param elementsSupplier supplier of the enum's values array
     * @param <E> the enum type
     * @return the enum codec
     */
    static <E extends Enum<E> & ElysiumStringRepresentable> EnumCodec<E> fromEnum(Supplier<E[]> elementsSupplier) {
        return fromEnumWithMapping(elementsSupplier, s -> s);
    }

    /**
     * Creates an enum-based codec with a custom name mapping applied to each serialized name.
     * <br>
     * Equivalent to 1.21.1's {@code StringRepresentable.fromEnumWithMapping}.
     *
     * @param enumValues supplier of the enum's values array
     * @param keyFunction mapping applied to each serialized name
     * @param <E> the enum type
     * @return the enum codec
     */
    static <E extends Enum<E> & ElysiumStringRepresentable> EnumCodec<E> fromEnumWithMapping(Supplier<E[]> enumValues, Function<String, String> keyFunction) {
        E[] enums = enumValues.get();
        Function<String, E> lookup = createNameLookup(enums, keyFunction);
        return new EnumCodec<>(enums, lookup);
    }

    /**
     * Creates a codec from an array of string-representable values without requiring them to be enums.
     * <br>
     * Equivalent to 1.21.1's {@code StringRepresentable.fromValues}.
     *
     * @param valuesSupplier supplier of the values array
     * @param <T> the value type
     * @return the codec
     */
    static <T extends ElysiumStringRepresentable> Codec<T> fromValues(Supplier<T[]> valuesSupplier) {
        T[] values = valuesSupplier.get();
        Function<String, T> lookup = createNameLookup(values, s -> s);
        ToIntFunction<T> indexLookup = Arrays.asList(values)::indexOf;
        return new StringRepresentableCodec<>(values, lookup, indexLookup);
    }

    /**
     * Creates a lookup function from a serialized name to its corresponding value, applying the provided key mapping to each value's name before comparison.
     *
     * @param values the values to look up from
     * @param keyFunction mapping applied to each serialized name
     * @param <T> the value type
     * @return the lookup function
     */
    static <T extends ElysiumStringRepresentable> Function<String, T> createNameLookup(T[] values, Function<String, String> keyFunction) {
        if (values.length > PRE_BUILT_MAP_THRESHOLD) {
            Map<String, T> map = new HashMap<>();
            for (T value : values) {
                map.put(keyFunction.apply(value.getSerializedName()), value);
            }
            return name -> name == null ? null : map.get(name);
        }
        return name -> {
            for (T value : values) {
                if (keyFunction.apply(value.getSerializedName()).equals(name)) {
                    return value;
                }
            }
            return null;
        };
    }

    /**
     * Creates a {@link Keyable} over the serialized names of the given values.
     *
     * @param serializable the values to enumerate
     * @return the keyable
     */
    static Keyable keys(StringRepresentable[] serializable) {
        return new Keyable() {
            @Override
            public <T> Stream<T> keys(DynamicOps<T> ops) {
                return Arrays.stream(serializable).map(StringRepresentable::getSerializedName).map(ops::createString);
            }
        };
    }

    /**
     * A codec for any {@link ElysiumStringRepresentable} value.
     *
     * @param <S> the value type
     */
    class StringRepresentableCodec<S extends ElysiumStringRepresentable> implements Codec<S> {

        private final Codec<S> codec;

        public StringRepresentableCodec(S[] values, Function<String, S> nameLookup, ToIntFunction<S> indexLookup) {
            Codec<S> nameCodec = Codec.STRING.comapFlatMap(
                name -> {
                    S value = nameLookup.apply(name);
                    return value != null ? DataResult.success(value) : DataResult.error(() -> "Unknown name: " + name);
                },
                ElysiumStringRepresentable::getSerializedName
            );
            Codec<S> idCodec = Codec.INT.comapFlatMap(
                id -> {
                    S value = id >= 0 && id < values.length ? values[id] : null;
                    return value != null ? DataResult.success(value) : DataResult.error(() -> "Unknown id: " + id);
                }, indexLookup::applyAsInt
            );
            this.codec = ExtraCodecs.orCompressed(nameCodec, idCodec);
        }

        @Override
        public <T> DataResult<Pair<S, T>> decode(DynamicOps<T> ops, T input) {
            return this.codec.decode(ops, input);
        }

        @Override
        public <T> DataResult<T> encode(S input, DynamicOps<T> ops, T prefix) {
            return this.codec.encode(input, ops, prefix);
        }
    }

    /**
     * A codec for {@link Enum} types that also implement {@link ElysiumStringRepresentable}.
     * <br>
     * Adds {@link #byName} lookups on top of {@link StringRepresentableCodec}.
     *
     * @param <E> the enum type
     */
    class EnumCodec<E extends Enum<E> & ElysiumStringRepresentable> extends StringRepresentableCodec<E> {

        private final Function<String, E> resolver;

        public EnumCodec(E[] values, Function<String, E> resolver) {
            super(values, resolver, e -> ((Enum<?>) e).ordinal());
            this.resolver = resolver;
        }

        @Nullable
        public E byName(@Nullable String name) {
            return this.resolver.apply(name);
        }

        public E byName(@Nullable String name, E defaultValue) {
            return Objects.requireNonNullElse(this.byName(name), defaultValue);
        }
    }
}