package net.jadenxgamer.elysium_api.api.network.pipeline;

import com.mojang.datafixers.util.Function3;
import com.mojang.datafixers.util.Function4;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A cross-version equivalent of 1.21.1's {@code StreamCodec}.
 * <p>
 * On 1.21.1, this interface's methods are powered by vanilla's own codec system.
 * On 1.20.1, it serves as a backport, allowing you to write your network
 * serialization once and have it compile and run identically on both versions.
 * <p>
 * You construct codecs using the static factory methods provided here. For simple
 * payloads with a single field, use {@link #of}. For records with multiple fields,
 * use the various {@link #composite} overloads.
 *
 * @param <B> the byte buffer type (usually {@code FriendlyByteBuf})
 * @param <V> the value type being serialized
 */
public interface ElysiumStreamCodec<B extends ByteBuf, V> {

    /**
     * Decodes a value from the buffer.
     *
     * @param buf the buffer to read from
     * @return the decoded value
     * @throws DecoderException if the data is malformed
     */
    V decode(B buf) throws DecoderException;

    /**
     * Encodes a value into the buffer.
     *
     * @param buf the buffer to write to
     * @param value the value to encode
     * @throws EncoderException if the value cannot be encoded
     */
    void encode(B buf, V value) throws EncoderException;

    /**
     * Creates a codec from a separate encoder and decoder.
     *
     * @param encoder the encoder function
     * @param decoder the decoder function
     * @param <B> the byte buffer type
     * @param <V> the value type
     * @return the codec
     */
    static <B extends ByteBuf, V> ElysiumStreamCodec<B, V> of(Encoder<B, V> encoder, Decoder<B, V> decoder) {
        return new ElysiumStreamCodec<B, V>() {
            @Override
            public V decode(B buf) throws DecoderException {
                return decoder.decode(buf);
            }

            @Override
            public void encode(B buf, V value) throws EncoderException {
                encoder.encode(buf, value);
            }
        };
    }

    /**
     * Creates a codec that doesn't write or read anything, and always returns
     * the expected value.
     *
     * @param value the expected value
     * @param <B> the byte buffer type
     * @param <V> the value type
     * @return the unit codec
     */
    static <B extends ByteBuf, V> ElysiumStreamCodec<B, V> unit(V value) {
        return new ElysiumStreamCodec<B, V>() {
            @Override
            public V decode(B buf) throws DecoderException {
                return value;
            }

            @Override
            public void encode(B buf, V value2) throws EncoderException {
            }
        };
    }

    /**
     * Creates a composite codec from a single sub-codec.
     *
     * @param codec1 the field codec
     * @param getter1 extracts the field from the value
     * @param constructor constructs the value from the field
     */
    static <B extends ByteBuf, C, T1> ElysiumStreamCodec<B, C> composite(
            ElysiumStreamCodec<? super B, T1> codec1, Function<C, T1> getter1,
            Function<T1, C> constructor) {
        return new ElysiumStreamCodec<B, C>() {
            @Override
            public C decode(B buf) throws DecoderException {
                T1 t1 = codec1.decode(buf);
                return constructor.apply(t1);
            }

            @Override
            public void encode(B buf, C value) throws EncoderException {
                codec1.encode(buf, getter1.apply(value));
            }
        };
    }

    /**
     * Creates a composite codec from two sub-codecs.
     */
    static <B extends ByteBuf, C, T1, T2> ElysiumStreamCodec<B, C> composite(
            ElysiumStreamCodec<? super B, T1> codec1, Function<C, T1> getter1,
            ElysiumStreamCodec<? super B, T2> codec2, Function<C, T2> getter2,
            BiFunction<T1, T2, C> constructor) {
        return new ElysiumStreamCodec<B, C>() {
            @Override
            public C decode(B buf) throws DecoderException {
                T1 t1 = codec1.decode(buf);
                T2 t2 = codec2.decode(buf);
                return constructor.apply(t1, t2);
            }

            @Override
            public void encode(B buf, C value) throws EncoderException {
                codec1.encode(buf, getter1.apply(value));
                codec2.encode(buf, getter2.apply(value));
            }
        };
    }

    /**
     * Creates a composite codec from three sub-codecs.
     */
    static <B extends ByteBuf, C, T1, T2, T3> ElysiumStreamCodec<B, C> composite(
            ElysiumStreamCodec<? super B, T1> codec1, Function<C, T1> getter1,
            ElysiumStreamCodec<? super B, T2> codec2, Function<C, T2> getter2,
            ElysiumStreamCodec<? super B, T3> codec3, Function<C, T3> getter3,
            Function3<T1, T2, T3, C> constructor) {
        return new ElysiumStreamCodec<B, C>() {
            @Override
            public C decode(B buf) throws DecoderException {
                T1 t1 = codec1.decode(buf);
                T2 t2 = codec2.decode(buf);
                T3 t3 = codec3.decode(buf);
                return constructor.apply(t1, t2, t3);
            }

            @Override
            public void encode(B buf, C value) throws EncoderException {
                codec1.encode(buf, getter1.apply(value));
                codec2.encode(buf, getter2.apply(value));
                codec3.encode(buf, getter3.apply(value));
            }
        };
    }

    /**
     * Creates a composite codec from four sub-codecs.
     */
    static <B extends ByteBuf, C, T1, T2, T3, T4> ElysiumStreamCodec<B, C> composite(
            ElysiumStreamCodec<? super B, T1> codec1, Function<C, T1> getter1,
            ElysiumStreamCodec<? super B, T2> codec2, Function<C, T2> getter2,
            ElysiumStreamCodec<? super B, T3> codec3, Function<C, T3> getter3,
            ElysiumStreamCodec<? super B, T4> codec4, Function<C, T4> getter4,
            Function4<T1, T2, T3, T4, C> constructor) {
        return new ElysiumStreamCodec<B, C>() {
            @Override
            public C decode(B buf) throws DecoderException {
                T1 t1 = codec1.decode(buf);
                T2 t2 = codec2.decode(buf);
                T3 t3 = codec3.decode(buf);
                T4 t4 = codec4.decode(buf);
                return constructor.apply(t1, t2, t3, t4);
            }

            @Override
            public void encode(B buf, C value) throws EncoderException {
                codec1.encode(buf, getter1.apply(value));
                codec2.encode(buf, getter2.apply(value));
                codec3.encode(buf, getter3.apply(value));
                codec4.encode(buf, getter4.apply(value));
            }
        };
    }

    /**
     * A functional interface for writing a value to a buffer.
     */
    @FunctionalInterface
    interface Encoder<B extends ByteBuf, V> {
        void encode(B buf, V value) throws EncoderException;
    }

    /**
     * A functional interface for reading a value from a buffer.
     */
    @FunctionalInterface
    interface Decoder<B extends ByteBuf, V> {
        V decode(B buf) throws DecoderException;
    }
}