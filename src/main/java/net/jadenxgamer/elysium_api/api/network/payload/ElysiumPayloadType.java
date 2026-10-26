package net.jadenxgamer.elysium_api.api.network.payload;

import net.jadenxgamer.elysium_api.api.network.pipeline.ElysiumStreamCodec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * A pair of a unique {@link ResourceLocation} identifier and the {@link ElysiumStreamCodec}
 * used to serialize the payload.
 * <p>
 * This acts as the identity of a payload. It is analogous to 1.21.1's {@code CustomPacketPayload.Type},
 * but is bundled with its codec for convenience. You should declare an instance of this
 * as a {@code public static final} field in your payload class.
 *
 * @param <T> the payload class
 */
public record ElysiumPayloadType<T extends ElysiumPayload>(ResourceLocation id, ElysiumStreamCodec<? super FriendlyByteBuf, T> codec) {

    /**
     * Creates a new payload type.
     *
     * @param id the unique identifier (should be in your mod's namespace)
     * @param codec the stream codec used to read and write the payload
     * @param <T> the payload class
     * @return the payload type
     */
    public static <T extends ElysiumPayload> ElysiumPayloadType<T> of(ResourceLocation id, ElysiumStreamCodec<? super FriendlyByteBuf, T> codec) {
        return new ElysiumPayloadType<>(id, codec);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ElysiumPayloadType<?> type)) return false;
        return this.id.equals(type.id);
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public @NotNull String toString() {
        return "ElysiumPayloadType[" + this.id + "]";
    }
}