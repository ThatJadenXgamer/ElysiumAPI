package net.jadenxgamer.elysium_api.api.core.attachment;

import com.mojang.serialization.Codec;
import net.jadenxgamer.elysium_api.api.network.pipeline.ElysiumStreamCodec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * The identity and configuration of a data attachment.
 * <p>
 * An attachment type pairs a namespaced {@link ResourceLocation} with a default value
 * supplier, and optionally declares a {@link Codec} for persistence, a
 * {@link ElysiumStreamCodec} for network sync, and a copy-on-death flag for
 * player respawn. Instances are immutable and built through
 * {@link #builder(ResourceLocation, Supplier)}.
 * <p>
 * Two type instances with the same id are considered equal, so an attachment type
 * can be reconstructed from its id without needing the original instance.
 *
 * @param <T> the type of value stored by this attachment
 */
public final class ElysiumAttachmentType<T> {

    private final ResourceLocation id;
    private final Supplier<T> defaultValue;
    @Nullable private final Codec<T> codec;
    @Nullable private final ElysiumStreamCodec<? super FriendlyByteBuf, T> syncCodec;
    private final boolean copyOnDeath;

    private ElysiumAttachmentType(ResourceLocation id, Supplier<T> defaultValue, @Nullable Codec<T> codec, @Nullable ElysiumStreamCodec<? super FriendlyByteBuf, T> syncCodec, boolean copyOnDeath) {
        this.id = id;
        this.defaultValue = defaultValue;
        this.codec = codec;
        this.syncCodec = syncCodec;
        this.copyOnDeath = copyOnDeath;
    }

    /**
     * Creates a new builder for an attachment type.
     *
     * @param id the unique namespaced identifier for this attachment
     * @param defaultValue a supplier invoked to produce the default value when none is set
     * @param <T> the type of value stored by this attachment
     * @return the builder
     */
    public static <T> Builder<T> builder(ResourceLocation id, Supplier<T> defaultValue) {
        return new Builder<>(id, defaultValue);
    }

    /**
     * Returns the unique identifier of this attachment.
     *
     * @return the id
     */
    public ResourceLocation id() {
        return id;
    }

    /**
     * Returns the supplier used to produce the default value of this attachment.
     *
     * @return the default value supplier
     */
    public Supplier<T> defaultValue() {
        return defaultValue;
    }

    /**
     * Returns the codec used to persist this attachment, or null if this
     * attachment is not persisted.
     *
     * @return the codec, or null
     */
    @Nullable
    public Codec<T> codec() {
        return codec;
    }

    /**
     * Returns the codec used to sync this attachment to clients, or null if
     * this attachment is not synced.
     *
     * @return the sync codec, or null
     */
    @Nullable
    public ElysiumStreamCodec<? super FriendlyByteBuf, T> syncCodec() {
        return syncCodec;
    }

    /**
     * Returns whether this attachment has a codec and will therefore be
     * serialized to and from disk.
     *
     * @return true if a codec was provided
     */
    public boolean isPersistent() {
        return codec != null;
    }

    /**
     * Returns whether this attachment has a sync codec and will therefore be
     * sent to tracking clients whenever its value changes.
     *
     * @return true if a sync codec was provided
     */
    public boolean isSynced() {
        return syncCodec != null;
    }

    /**
     * Returns whether this attachment is copied from the old player entity to
     * the new one on respawn.
     *
     * @return true if copy-on-death was requested
     */
    public boolean isCopyOnDeath() {
        return copyOnDeath;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ElysiumAttachmentType<?> type)) return false;
        return this.id.equals(type.id);
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public @NotNull String toString() {
        return "ElysiumAttachmentType[" + this.id + "]";
    }

    /**
     * A builder for {@link ElysiumAttachmentType} instances.
     *
     * @param <T> the type of value stored by the attachment being built
     */
    public static final class Builder<T> {

        private final ResourceLocation id;
        private final Supplier<T> defaultValue;
        @Nullable private Codec<T> codec;
        @Nullable private ElysiumStreamCodec<? super FriendlyByteBuf, T> syncCodec;
        private boolean copyOnDeath;

        private Builder(ResourceLocation id, Supplier<T> defaultValue) {
            this.id = id;
            this.defaultValue = defaultValue;
        }

        /**
         * Marks this attachment as persistent using the given codec.
         * <p>
         * If this method is never called, the attachment will not be serialized
         * to disk and will reset to its default value on world reload.
         *
         * @param codec the codec used to serialize and deserialize this attachment
         * @return this builder
         */
        public Builder<T> serialize(Codec<T> codec) {
            this.codec = codec;
            return this;
        }

        /**
         * Marks this attachment as synced to tracking clients using the given
         * stream codec.
         * <p>
         * The value is sent to every player tracking the holder whenever it is
         * set on the server, and to newly tracking players on their first send.
         * This behaviour matches NeoForge's {@code AttachmentType.Builder#sync}
         * and is emulated on Fabric and Forge via Elysium API's networking
         * layer.
         * <p>
         * If this method is never called, the attachment stays server-only and
         * client reads of it will always return the default value.
         * <p>
         * Sync and persistence are independent: a type may declare either,
         * both, or neither.
         *
         * @param codec the stream codec used to serialize this attachment over the network
         * @return this builder
         */
        public Builder<T> sync(ElysiumStreamCodec<? super FriendlyByteBuf, T> codec) {
            this.syncCodec = codec;
            return this;
        }

        /**
         * Marks this attachment as being copied from the old player to the new
         * player when the player respawns.
         * <p>
         * This flag has no effect on non-player entities, since they do not
         * respawn. It also has no effect on block entities or chunks.
         *
         * @return this builder
         */
        public Builder<T> copyOnDeath() {
            this.copyOnDeath = true;
            return this;
        }

        /**
         * Builds the attachment type.
         *
         * @return the attachment type
         */
        public ElysiumAttachmentType<T> build() {
            return new ElysiumAttachmentType<>(this.id, this.defaultValue, this.codec, this.syncCodec, this.copyOnDeath);
        }
    }
}