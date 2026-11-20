package net.jadenxgamer.elysium_api.api.core.attachment.pipeline;

import net.jadenxgamer.elysium_api.api.network.ElysiumNetwork;
import net.jadenxgamer.elysium_api.api.network.ElysiumNetworkContext;
import net.jadenxgamer.elysium_api.mod.network.payload.client.ElysiumBlockEntityAttachmentSyncPayload;
import net.jadenxgamer.elysium_api.mod.network.payload.client.ElysiumChunkAttachmentSyncPayload;
import net.jadenxgamer.elysium_api.mod.network.payload.client.ElysiumEntityAttachmentSyncPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public final class ElysiumAttachmentSyncDispatcher {

    @Nullable private static EntityClientApplier entityApplier;
    @Nullable private static BlockEntityClientApplier blockEntityApplier;
    @Nullable private static ChunkClientApplier chunkApplier;

    private ElysiumAttachmentSyncDispatcher() {
    }

    public static void setEntityApplier(@Nullable EntityClientApplier applier) {
        entityApplier = applier;
    }

    public static void setBlockEntityApplier(@Nullable BlockEntityClientApplier applier) {
        blockEntityApplier = applier;
    }

    public static void setChunkApplier(@Nullable ChunkClientApplier applier) {
        chunkApplier = applier;
    }

    @Deprecated
    public static void setClientApplier(@Nullable EntityClientApplier applier) {
        setEntityApplier(applier);
    }

    public static void sendToTrackingAndSelf(Entity entity, ResourceLocation attachmentId, byte @Nullable [] data) {
        ElysiumNetwork.sendToPlayersTrackingAndSelf(entity, new ElysiumEntityAttachmentSyncPayload(entity.getId(), attachmentId, data));
    }

    public static void sendToPlayer(ServerPlayer player, Entity entity, ResourceLocation attachmentId, byte @Nullable [] data) {
        ElysiumNetwork.sendToPlayer(player, new ElysiumEntityAttachmentSyncPayload(entity.getId(), attachmentId, data));
    }

    public static void sendToTrackingBlockEntity(BlockEntity blockEntity, ResourceLocation attachmentId, byte @Nullable [] data) {
        Level level = blockEntity.getLevel();
        if (level == null) return;
        ElysiumNetwork.sendToPlayersTrackingBlockEntity(blockEntity,
                new ElysiumBlockEntityAttachmentSyncPayload(level.dimension().location(), blockEntity.getBlockPos(), attachmentId, data));
    }

    public static void sendToPlayer(ServerPlayer player, BlockEntity blockEntity, ResourceLocation attachmentId, byte @Nullable [] data) {
        Level level = blockEntity.getLevel();
        if (level == null) return;
        ElysiumNetwork.sendToPlayer(player,
                new ElysiumBlockEntityAttachmentSyncPayload(level.dimension().location(), blockEntity.getBlockPos(), attachmentId, data));
    }

    public static void sendToTrackingChunk(LevelChunk chunk, ResourceLocation attachmentId, byte @Nullable [] data) {
        if (!(chunk.getLevel() instanceof ServerLevel serverLevel)) return;
        ElysiumNetwork.sendToPlayersTrackingChunk(serverLevel, chunk.getPos(),
                new ElysiumChunkAttachmentSyncPayload(serverLevel.dimension().location(), chunk.getPos(), attachmentId, data));
    }

    public static void sendToPlayer(ServerPlayer player, LevelChunk chunk, ResourceLocation attachmentId, byte @Nullable [] data) {
        Level level = chunk.getLevel();
        ElysiumNetwork.sendToPlayer(player,
                new ElysiumChunkAttachmentSyncPayload(level.dimension().location(), chunk.getPos(), attachmentId, data));
    }

    public static void onClientReceiveEntity(ElysiumEntityAttachmentSyncPayload payload, ElysiumNetworkContext context) {
        EntityClientApplier applier = entityApplier;
        if (applier == null) return;
        applier.apply(payload.entityId(), payload.attachmentId(), payload.data());
    }

    public static void onClientReceiveBlockEntity(ElysiumBlockEntityAttachmentSyncPayload payload, ElysiumNetworkContext context) {
        BlockEntityClientApplier applier = blockEntityApplier;
        if (applier == null) return;
        applier.apply(payload.dimension(), payload.pos(), payload.attachmentId(), payload.data());
    }

    public static void onClientReceiveChunk(ElysiumChunkAttachmentSyncPayload payload, ElysiumNetworkContext context) {
        ChunkClientApplier applier = chunkApplier;
        if (applier == null) return;
        applier.apply(payload.dimension(), payload.pos(), payload.attachmentId(), payload.data());
    }

    @FunctionalInterface
    public interface EntityClientApplier {
        void apply(int entityId, ResourceLocation attachmentId, byte @Nullable [] data);
    }

    @FunctionalInterface
    public interface BlockEntityClientApplier {
        void apply(ResourceLocation dimension, BlockPos pos, ResourceLocation attachmentId, byte @Nullable [] data);
    }

    @FunctionalInterface
    public interface ChunkClientApplier {
        void apply(ResourceLocation dimension, ChunkPos pos, ResourceLocation attachmentId, byte @Nullable [] data);
    }
}