package net.jadenxgamer.elysium_api.api.core.attachment.pipeline;

import net.jadenxgamer.elysium_api.api.core.attachment.ElysiumAttachmentRegistry;
import net.jadenxgamer.elysium_api.api.core.attachment.ElysiumAttachmentType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * The internal bridge between the cross-loader attachment implementations.
 * <p>
 * This interface should not be implemented or called by downstream mods. It is
 * used exclusively by Elysium API's internal loader adapters to install the
 * active attachment backend into {@link net.jadenxgamer.elysium_api.api.core.attachment.ElysiumAttachments}.
 */
@ApiStatus.Internal
public interface ElysiumAttachmentBackend {

    void register(String modId, ElysiumAttachmentRegistry registry);

    <T> T getData(Entity holder, ElysiumAttachmentType<T> type);

    <T> T getData(BlockEntity holder, ElysiumAttachmentType<T> type);

    <T> T getData(LevelChunk holder, ElysiumAttachmentType<T> type);

    <T> void setData(Entity holder, ElysiumAttachmentType<T> type, @Nullable T value);

    <T> void setData(BlockEntity holder, ElysiumAttachmentType<T> type, @Nullable T value);

    <T> void setData(LevelChunk holder, ElysiumAttachmentType<T> type, @Nullable T value);

    <T> boolean hasData(Entity holder, ElysiumAttachmentType<T> type);

    <T> boolean hasData(BlockEntity holder, ElysiumAttachmentType<T> type);

    <T> boolean hasData(LevelChunk holder, ElysiumAttachmentType<T> type);

    <T> void removeData(Entity holder, ElysiumAttachmentType<T> type);

    <T> void removeData(BlockEntity holder, ElysiumAttachmentType<T> type);

    <T> void removeData(LevelChunk holder, ElysiumAttachmentType<T> type);
}