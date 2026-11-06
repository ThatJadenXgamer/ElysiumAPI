package net.jadenxgamer.elysium_api.api.core.attachment;

import net.jadenxgamer.elysium_api.api.core.attachment.pipeline.ElysiumAttachmentBackend;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * The central facade for all data attachment operations in Elysium API.
 * <p>
 * This class provides a unified, cross-loader, and cross-version entry point
 * for reading and writing attachments on entities, block entities, and level
 * chunks. Whether you are running on 1.20.1 or 1.21.1, and regardless of
 * whether the underlying mod loader is Fabric, Forge, or NeoForge, the methods
 * here behave identically.
 * <p>
 * To register your attachment types, call {@link #register(String, Consumer)}
 * from your mod's common initialization phase. Once registered, use the various
 * {@code getData}, {@code setData}, {@code hasData}, and {@code removeData}
 * methods to work with attachment values on live holders.
 */
public final class ElysiumAttachments {

    private static ElysiumAttachmentBackend backend;

    private ElysiumAttachments() {
    }

    /**
     * Registers a set of attachment types for a specific mod ID.
     * <p>
     * Every type passed to the configurator must have an id namespaced under
     * {@code modId}. Duplicate ids are rejected.
     *
     * @param modId the mod ID registering these attachments
     * @param configurator the lambda that populates the registry
     */
    public static void register(String modId, Consumer<ElysiumAttachmentRegistry> configurator) {
        ElysiumAttachmentRegistry registry = new ElysiumAttachmentRegistry(modId);
        configurator.accept(registry);
        requireBackend().register(modId, registry);
    }

    /**
     * Returns the current value of an attachment on an entity.
     * <p>
     * If the attachment has never been set on this holder, the type's default
     * value supplier is invoked, the result is stored, and it is returned.
     *
     * @param holder the entity
     * @param type the attachment type
     * @param <T> the attachment value type
     * @return the current value
     */
    public static <T> T getData(Entity holder, ElysiumAttachmentType<T> type) {
        return requireBackend().getData(holder, type);
    }

    /**
     * Returns the current value of an attachment on a block entity.
     *
     * @param holder the block entity
     * @param type the attachment type
     * @param <T> the attachment value type
     * @return the current value
     */
    public static <T> T getData(BlockEntity holder, ElysiumAttachmentType<T> type) {
        return requireBackend().getData(holder, type);
    }

    /**
     * Returns the current value of an attachment on a level chunk.
     *
     * @param holder the level chunk
     * @param type the attachment type
     * @param <T> the attachment value type
     * @return the current value
     */
    public static <T> T getData(LevelChunk holder, ElysiumAttachmentType<T> type) {
        return requireBackend().getData(holder, type);
    }

    /**
     * Sets the value of an attachment on an entity.
     * <p>
     * Passing null is equivalent to {@link #removeData(Entity, ElysiumAttachmentType)}
     * and causes subsequent reads to fall back to the default value.
     *
     * @param holder the entity
     * @param type the attachment type
     * @param value the new value, or null to remove
     * @param <T> the attachment value type
     */
    public static <T> void setData(Entity holder, ElysiumAttachmentType<T> type, @Nullable T value) {
        requireBackend().setData(holder, type, value);
    }

    /**
     * Sets the value of an attachment on a block entity.
     *
     * @param holder the block entity
     * @param type the attachment type
     * @param value the new value, or null to remove
     * @param <T> the attachment value type
     */
    public static <T> void setData(BlockEntity holder, ElysiumAttachmentType<T> type, @Nullable T value) {
        requireBackend().setData(holder, type, value);
    }

    /**
     * Sets the value of an attachment on a level chunk.
     *
     * @param holder the level chunk
     * @param type the attachment type
     * @param value the new value, or null to remove
     * @param <T> the attachment value type
     */
    public static <T> void setData(LevelChunk holder, ElysiumAttachmentType<T> type, @Nullable T value) {
        requireBackend().setData(holder, type, value);
    }

    /**
     * Returns whether an attachment has been explicitly set on an entity.
     * <p>
     * This returns true only when a value has been set through
     * {@link #setData(Entity, ElysiumAttachmentType, Object)} or the equivalent
     * backend path, and does not return true simply because the attachment has
     * a default value available.
     *
     * @param holder the entity
     * @param type the attachment type
     * @param <T> the attachment value type
     * @return true if a value is present
     */
    public static <T> boolean hasData(Entity holder, ElysiumAttachmentType<T> type) {
        return requireBackend().hasData(holder, type);
    }

    /**
     * Returns whether an attachment has been explicitly set on a block entity.
     *
     * @param holder the block entity
     * @param type the attachment type
     * @param <T> the attachment value type
     * @return true if a value is present
     */
    public static <T> boolean hasData(BlockEntity holder, ElysiumAttachmentType<T> type) {
        return requireBackend().hasData(holder, type);
    }

    /**
     * Returns whether an attachment has been explicitly set on a level chunk.
     *
     * @param holder the level chunk
     * @param type the attachment type
     * @param <T> the attachment value type
     * @return true if a value is present
     */
    public static <T> boolean hasData(LevelChunk holder, ElysiumAttachmentType<T> type) {
        return requireBackend().hasData(holder, type);
    }

    /**
     * Removes an attachment from an entity.
     * <p>
     * After this call, {@link #hasData(Entity, ElysiumAttachmentType)} returns
     * false and {@link #getData(Entity, ElysiumAttachmentType)} returns the
     * default value.
     *
     * @param holder the entity
     * @param type the attachment type
     * @param <T> the attachment value type
     */
    public static <T> void removeData(Entity holder, ElysiumAttachmentType<T> type) {
        requireBackend().removeData(holder, type);
    }

    /**
     * Removes an attachment from a block entity.
     *
     * @param holder the block entity
     * @param type the attachment type
     * @param <T> the attachment value type
     */
    public static <T> void removeData(BlockEntity holder, ElysiumAttachmentType<T> type) {
        requireBackend().removeData(holder, type);
    }

    /**
     * Removes an attachment from a level chunk.
     *
     * @param holder the level chunk
     * @param type the attachment type
     * @param <T> the attachment value type
     */
    public static <T> void removeData(LevelChunk holder, ElysiumAttachmentType<T> type) {
        requireBackend().removeData(holder, type);
    }

    @ApiStatus.Internal
    public static void installBackend(ElysiumAttachmentBackend backend) {
        if (ElysiumAttachments.backend != null) throw new IllegalStateException("Elysium attachment backend has already been installed; refusing to replace.");
        ElysiumAttachments.backend = backend;
    }

    private static ElysiumAttachmentBackend requireBackend() {
        if (backend == null) throw new IllegalStateException("Elysium attachment backend has not been installed. A loader adapter has seemingly failed to install a backend on the Elysium API entrypoint.");
        return backend;
    }
}