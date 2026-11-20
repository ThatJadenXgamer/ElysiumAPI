package net.jadenxgamer.elysium_api.impl.core.attachment;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.jadenxgamer.elysium_api.ElysiumAPI;
import net.jadenxgamer.elysium_api.api.core.attachment.ElysiumAttachmentRegistry;
import net.jadenxgamer.elysium_api.api.core.attachment.ElysiumAttachmentType;
import net.jadenxgamer.elysium_api.api.core.attachment.ElysiumAttachments;
import net.jadenxgamer.elysium_api.api.core.attachment.pipeline.ElysiumAttachmentBackend;
import net.jadenxgamer.elysium_api.api.core.attachment.pipeline.ElysiumAttachmentSyncDispatcher;
import net.jadenxgamer.elysium_api.api.core.attachment.pipeline.ElysiumChunkTrackingHooks;
import net.jadenxgamer.elysium_api.api.core.attachment.pipeline.ElysiumEntityTrackingHooks;
import net.jadenxgamer.elysium_api.api.network.pipeline.ElysiumStreamCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public final class ElysiumAttachmentBackendF21 implements ElysiumAttachmentBackend,
        ElysiumEntityTrackingHooks.Tracker,
        ElysiumChunkTrackingHooks.Tracker {

    private static final ElysiumAttachmentBackendF21 INSTANCE = new ElysiumAttachmentBackendF21();

    private final Map<ResourceLocation, AttachmentType<?>> nativeTypes = new HashMap<>();
    private final Map<ResourceLocation, ElysiumAttachmentType<?>> attachmentTypes = new HashMap<>();
    private final List<ElysiumAttachmentType<?>> syncedTypes = new ArrayList<>();
    private final Map<UUID, Set<UUID>> syncedPlayersByEntity = new ConcurrentHashMap<>();
    private final Map<UUID, Map<ResourceLocation, byte[]>> lastSentEntityBytes = new ConcurrentHashMap<>();
    private final Map<ChunkKey, Map<BlockPos, Map<ResourceLocation, byte[]>>> lastSentBlockEntityBytes = new ConcurrentHashMap<>();
    private final Map<ChunkKey, Map<ResourceLocation, byte[]>> lastSentChunkBytes = new ConcurrentHashMap<>();
    private final Map<ChunkKey, Map<BlockPos, Map<ResourceLocation, byte[]>>> pendingBlockEntitySyncs = new ConcurrentHashMap<>();
    private final Map<ChunkKey, Map<ResourceLocation, byte[]>> pendingChunkSyncs = new ConcurrentHashMap<>();

    private ElysiumAttachmentBackendF21() {}

    public static void installBackend() {
        ElysiumAttachments.installBackend(INSTANCE);
        ElysiumAttachmentSyncDispatcher.setEntityApplier(INSTANCE::applyClientEntitySync);
        ElysiumAttachmentSyncDispatcher.setBlockEntityApplier(INSTANCE::applyClientBlockEntitySync);
        ElysiumAttachmentSyncDispatcher.setChunkApplier(INSTANCE::applyClientChunkSync);
        ElysiumEntityTrackingHooks.setTracker(INSTANCE);
        ElysiumChunkTrackingHooks.setTracker(INSTANCE);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> INSTANCE.onPlayerJoin(handler.getPlayer()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> INSTANCE.onPlayerDisconnect(handler.getPlayer()));
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> INSTANCE.onEntityUnload(entity));
        ServerChunkEvents.CHUNK_UNLOAD.register(INSTANCE::onChunkUnload);
        ClientChunkEvents.CHUNK_LOAD.register(INSTANCE::onClientChunkLoad);
        ClientChunkEvents.CHUNK_UNLOAD.register(INSTANCE::onClientChunkUnload);
        ElysiumAPI.LOGGER.info("Successfully installed Backend for Data Attachments Facade");
    }

    @Override
    public void register(String modId, ElysiumAttachmentRegistry registry) {
        for (ElysiumAttachmentType<?> attachmentType : registry.types()) {
            this.registerType(attachmentType);
            this.attachmentTypes.put(attachmentType.id(), attachmentType);
            if (attachmentType.isSynced()) this.syncedTypes.add(attachmentType);
        }
    }

    private <T> void registerType(ElysiumAttachmentType<T> type) {
        AttachmentRegistry.Builder<T> builder = AttachmentRegistry.builder();
        builder.initializer(type.defaultValue());

        if (type.codec() != null) builder.persistent(type.codec());
        if (type.isCopyOnDeath()) builder.copyOnDeath();

        this.nativeTypes.put(type.id(), builder.buildAndRegister(type.id()));
    }

    @SuppressWarnings("unchecked")
    private <T> AttachmentType<T> lookup(ElysiumAttachmentType<T> type) {
        AttachmentType<T> nativeType = (AttachmentType<T>) this.nativeTypes.get(type.id());
        if (nativeType == null) throw new IllegalStateException("Attachment type '" + type.id() + "' has not been registered; call ElysiumAttachments.register before accessing it.");
        return nativeType;
    }

    @Override
    public <T> T getData(Entity holder, ElysiumAttachmentType<T> type) {
        return this.readAttachment(holder, type);
    }

    @Override
    public <T> T getData(BlockEntity holder, ElysiumAttachmentType<T> type) {
        return this.readAttachment(holder, type);
    }

    @Override
    public <T> T getData(LevelChunk holder, ElysiumAttachmentType<T> type) {
        return this.readAttachment(holder, type);
    }

    private <T> T readAttachment(AttachmentTarget target, ElysiumAttachmentType<T> type) {
        return target.getAttachedOrGet(this.lookup(type), type.defaultValue());
    }

    @Override
    public <T> void setData(Entity holder, ElysiumAttachmentType<T> type, @Nullable T value) {
        if (value == null) { this.removeData(holder, type); return; }
        holder.setAttached(this.lookup(type), value);
        this.broadcastEntityUpdate(holder, type, value);
    }

    @Override
    public <T> void setData(BlockEntity holder, ElysiumAttachmentType<T> type, @Nullable T value) {
        if (value == null) { this.removeData(holder, type); return; }
        holder.setAttached(this.lookup(type), value);
        holder.setChanged();
        this.broadcastBlockEntityUpdate(holder, type, value);
    }

    @Override
    public <T> void setData(LevelChunk holder, ElysiumAttachmentType<T> type, @Nullable T value) {
        if (value == null) { this.removeData(holder, type); return; }
        holder.setAttached(this.lookup(type), value);
        holder.setUnsaved(true);
        this.broadcastChunkUpdate(holder, type, value);
    }

    @Override
    public <T> boolean hasData(Entity holder, ElysiumAttachmentType<T> type) {
        return this.hasAttachment(holder, type);
    }

    @Override
    public <T> boolean hasData(BlockEntity holder, ElysiumAttachmentType<T> type) {
        return this.hasAttachment(holder, type);
    }

    @Override
    public <T> boolean hasData(LevelChunk holder, ElysiumAttachmentType<T> type) {
        return this.hasAttachment(holder, type);
    }

    private <T> boolean hasAttachment(AttachmentTarget target, ElysiumAttachmentType<T> type) {
        return target.hasAttached(this.lookup(type));
    }

    @Override
    public <T> void removeData(Entity holder, ElysiumAttachmentType<T> type) {
        if (this.clearAttachment(holder, type)) this.broadcastEntityRemoval(holder, type);
    }

    @Override
    public <T> void removeData(BlockEntity holder, ElysiumAttachmentType<T> type) {
        if (this.clearAttachment(holder, type)) this.broadcastBlockEntityRemoval(holder, type);
        holder.setChanged();
    }

    @Override
    public <T> void removeData(LevelChunk holder, ElysiumAttachmentType<T> type) {
        if (this.clearAttachment(holder, type)) this.broadcastChunkRemoval(holder, type);
        holder.setUnsaved(true);
    }

    private <T> boolean clearAttachment(AttachmentTarget target, ElysiumAttachmentType<T> type) {
        AttachmentType<T> nativeType = this.lookup(type);
        boolean hadValue = target.hasAttached(nativeType);
        target.setAttached(nativeType, null);
        return hadValue;
    }

    @Override
    public void onStartTracking(ServerPlayer player, Entity entity) {
        Set<UUID> syncedTo = this.syncedPlayersByEntity.computeIfAbsent(entity.getUUID(), k -> ConcurrentHashMap.newKeySet());
        if (!syncedTo.add(player.getUUID())) return;
        for (ElysiumAttachmentType<?> attachmentType : this.syncedTypes) {
            this.sendCurrentTo(entity, attachmentType, (attachmentId, bytes) -> ElysiumAttachmentSyncDispatcher.sendToPlayer(player, entity, attachmentId, bytes));
        }
    }

    @Override
    public void onStopTracking(ServerPlayer player, Entity entity) {
        Set<UUID> syncedTo = this.syncedPlayersByEntity.get(entity.getUUID());
        if (syncedTo != null) syncedTo.remove(player.getUUID());
    }

    @Override
    public void onStartTracking(ServerPlayer player, ChunkPos pos) {
        if (!(player.level() instanceof ServerLevel level)) return;
        LevelChunk chunk = level.getChunk(pos.x, pos.z);

        for (ElysiumAttachmentType<?> attachmentType : this.syncedTypes)
            this.sendCurrentTo(chunk, attachmentType, (attachmentId, bytes) -> ElysiumAttachmentSyncDispatcher.sendToPlayer(player, chunk, attachmentId, bytes));

        for (BlockEntity be : chunk.getBlockEntities().values()) {
            for (ElysiumAttachmentType<?> attachmentType : this.syncedTypes)
                this.sendCurrentTo(be, attachmentType, (attachmentId, bytes) -> ElysiumAttachmentSyncDispatcher.sendToPlayer(player, be, attachmentId, bytes));
        }
    }

    @Override
    public void onStopTracking(ServerPlayer player, ChunkPos pos) {}

    private <T> void broadcastEntityUpdate(Entity holder, ElysiumAttachmentType<T> type, T value) {
        byte[] bytes = this.prepareSyncBytes(holder.level(), type, value);
        if (bytes == null) return;
        Map<ResourceLocation, byte[]> perEntity = this.lastSentEntityBytes.computeIfAbsent(holder.getUUID(), k -> new ConcurrentHashMap<>());
        if (!this.markSentIfChanged(perEntity, type, bytes)) return;
        ElysiumAttachmentSyncDispatcher.sendToTrackingAndSelf(holder, type.id(), bytes);
    }

    private <T> void broadcastBlockEntityUpdate(BlockEntity holder, ElysiumAttachmentType<T> type, T value) {
        Level level = holder.getLevel();
        byte[] bytes = this.prepareSyncBytes(level, type, value);
        if (bytes == null) return;
        ChunkKey chunkKey = new ChunkKey(level.dimension().location(), new ChunkPos(holder.getBlockPos()));
        Map<ResourceLocation, byte[]> perBlockEntity = this.lastSentBlockEntityBytes
                .computeIfAbsent(chunkKey, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(holder.getBlockPos(), k -> new ConcurrentHashMap<>());
        if (!this.markSentIfChanged(perBlockEntity, type, bytes)) return;
        ElysiumAttachmentSyncDispatcher.sendToTrackingBlockEntity(holder, type.id(), bytes);
    }

    private <T> void broadcastChunkUpdate(LevelChunk holder, ElysiumAttachmentType<T> type, T value) {
        Level level = holder.getLevel();
        byte[] bytes = this.prepareSyncBytes(level, type, value);
        if (bytes == null) return;
        ChunkKey chunkKey = new ChunkKey(level.dimension().location(), holder.getPos());
        Map<ResourceLocation, byte[]> perChunk = this.lastSentChunkBytes.computeIfAbsent(chunkKey, k -> new ConcurrentHashMap<>());
        if (!this.markSentIfChanged(perChunk, type, bytes)) return;
        ElysiumAttachmentSyncDispatcher.sendToTrackingChunk(holder, type.id(), bytes);
    }

    private <T> void broadcastEntityRemoval(Entity holder, ElysiumAttachmentType<T> type) {
        if (holder.level().isClientSide() || !type.isSynced()) return;
        this.clearLastSent(this.lastSentEntityBytes.get(holder.getUUID()), type.id());
        ElysiumAttachmentSyncDispatcher.sendToTrackingAndSelf(holder, type.id(), null);
    }

    private <T> void broadcastBlockEntityRemoval(BlockEntity holder, ElysiumAttachmentType<T> type) {
        Level level = holder.getLevel();
        if (level == null || level.isClientSide() || !type.isSynced()) return;
        ChunkKey chunkKey = new ChunkKey(level.dimension().location(), new ChunkPos(holder.getBlockPos()));
        Map<BlockPos, Map<ResourceLocation, byte[]>> perChunk = this.lastSentBlockEntityBytes.get(chunkKey);
        if (perChunk != null) this.clearLastSent(perChunk.get(holder.getBlockPos()), type.id());
        ElysiumAttachmentSyncDispatcher.sendToTrackingBlockEntity(holder, type.id(), null);
    }

    private <T> void broadcastChunkRemoval(LevelChunk holder, ElysiumAttachmentType<T> type) {
        Level level = holder.getLevel();
        if (level == null || level.isClientSide() || !type.isSynced()) return;
        ChunkKey chunkKey = new ChunkKey(level.dimension().location(), holder.getPos());
        this.clearLastSent(this.lastSentChunkBytes.get(chunkKey), type.id());
        ElysiumAttachmentSyncDispatcher.sendToTrackingChunk(holder, type.id(), null);
    }

    private <T> byte[] prepareSyncBytes(Level level, ElysiumAttachmentType<T> type, T value) {
        if (level == null || level.isClientSide() || !type.isSynced()) return null;
        return this.encodeSyncData(type, value);
    }

    private boolean markSentIfChanged(Map<ResourceLocation, byte[]> perHolderBytes, ElysiumAttachmentType<?> type, byte[] bytes) {
        byte[] previous = perHolderBytes.get(type.id());
        if (previous != null && Arrays.equals(previous, bytes)) return false;
        perHolderBytes.put(type.id(), bytes);
        return true;
    }

    private void clearLastSent(@Nullable Map<ResourceLocation, byte[]> perHolderBytes, ResourceLocation attachmentId) {
        if (perHolderBytes != null) perHolderBytes.remove(attachmentId);
    }

    @SuppressWarnings("unchecked")
    private <T> byte[] encodeSyncData(ElysiumAttachmentType<T> type, T value) {
        ElysiumStreamCodec<? super FriendlyByteBuf, T> codec = type.syncCodec();
        if (codec == null) return null;
        try {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            codec.encode(buf, value);
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            return bytes;
        } catch (Exception ignored) { return null; }
    }

    private void sendAllSyncedEntityTypes(ServerPlayer player, Entity entity) {
        for (ElysiumAttachmentType<?> attachmentType : this.syncedTypes) {
            this.sendCurrentTo(entity, attachmentType, (attachmentId, bytes) -> ElysiumAttachmentSyncDispatcher.sendToPlayer(player, entity, attachmentId, bytes));
        }
    }

    private <T> void sendCurrentTo(AttachmentTarget target, ElysiumAttachmentType<T> type, BiConsumer<ResourceLocation, byte[]> dispatcher) {
        T value = target.getAttached(this.lookup(type));
        if (value == null) return;
        byte[] bytes = this.encodeSyncData(type, value);
        if (bytes == null) return;
        dispatcher.accept(type.id(), bytes);
    }

    private void onPlayerJoin(ServerPlayer player) {
        this.sendAllSyncedEntityTypes(player, player);
    }

    private void onPlayerDisconnect(ServerPlayer player) {
        UUID playerId = player.getUUID();
        for (Set<UUID> syncedTo : this.syncedPlayersByEntity.values()) syncedTo.remove(playerId);
    }

    private void onEntityUnload(Entity entity) {
        this.syncedPlayersByEntity.remove(entity.getUUID());
        this.lastSentEntityBytes.remove(entity.getUUID());
    }

    private void onChunkUnload(ServerLevel world, LevelChunk chunk) {
        ChunkKey chunkKey = new ChunkKey(world.dimension().location(), chunk.getPos());
        this.lastSentBlockEntityBytes.remove(chunkKey);
        this.lastSentChunkBytes.remove(chunkKey);
    }

    private void onClientChunkLoad(ClientLevel world, LevelChunk chunk) {
        ChunkKey chunkKey = new ChunkKey(world.dimension().location(), chunk.getPos());

        Map<ResourceLocation, byte[]> pendingChunk = this.pendingChunkSyncs.remove(chunkKey);
        if (pendingChunk != null) {
            for (Map.Entry<ResourceLocation, byte[]> entry : pendingChunk.entrySet()) {
                ElysiumAttachmentType<?> attachmentType = this.attachmentTypes.get(entry.getKey());
                if (attachmentType == null || !attachmentType.isSynced()) continue;
                this.decodeAndSet(chunk, attachmentType, entry.getValue());
            }
        }

        Map<BlockPos, Map<ResourceLocation, byte[]>> pendingBlockEntities = this.pendingBlockEntitySyncs.remove(chunkKey);
        if (pendingBlockEntities == null) return;
        for (Map.Entry<BlockPos, Map<ResourceLocation, byte[]>> blockEntityEntry : pendingBlockEntities.entrySet()) {
            BlockEntity blockEntity = world.getBlockEntity(blockEntityEntry.getKey());
            if (blockEntity == null) continue;
            for (Map.Entry<ResourceLocation, byte[]> attachmentEntry : blockEntityEntry.getValue().entrySet()) {
                ElysiumAttachmentType<?> attachmentType = this.attachmentTypes.get(attachmentEntry.getKey());
                if (attachmentType == null || !attachmentType.isSynced()) continue;
                this.decodeAndSet(blockEntity, attachmentType, attachmentEntry.getValue());
            }
        }
    }

    private void onClientChunkUnload(ClientLevel world, LevelChunk chunk) {
        ChunkKey chunkKey = new ChunkKey(world.dimension().location(), chunk.getPos());
        this.pendingBlockEntitySyncs.remove(chunkKey);
        this.pendingChunkSyncs.remove(chunkKey);
    }

    private void applyClientEntitySync(int entityId, ResourceLocation attachmentId, byte @Nullable [] bytes) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;
        Entity entity = client.level.getEntity(entityId);
        if (entity == null) return;
        ElysiumAttachmentType<?> attachmentType = this.attachmentTypes.get(attachmentId);
        if (attachmentType == null || !attachmentType.isSynced()) return;
        this.decodeAndSet(entity, attachmentType, bytes);
    }

    private void applyClientBlockEntitySync(ResourceLocation dimension, BlockPos pos, ResourceLocation attachmentId, byte @Nullable [] bytes) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || !client.level.dimension().location().equals(dimension)) return;
        ElysiumAttachmentType<?> attachmentType = this.attachmentTypes.get(attachmentId);
        if (attachmentType == null || !attachmentType.isSynced()) return;

        BlockEntity blockEntity = client.level.getBlockEntity(pos);
        if (blockEntity == null) {
            if (bytes == null) return;
            this.pendingBlockEntitySyncs
                    .computeIfAbsent(new ChunkKey(dimension, new ChunkPos(pos)), k -> new ConcurrentHashMap<>())
                    .computeIfAbsent(pos, k -> new ConcurrentHashMap<>())
                    .put(attachmentId, bytes);
            return;
        }
        this.decodeAndSet(blockEntity, attachmentType, bytes);
    }

    private void applyClientChunkSync(ResourceLocation dimension, ChunkPos pos, ResourceLocation attachmentId, byte @Nullable [] bytes) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || !client.level.dimension().location().equals(dimension)) return;
        ElysiumAttachmentType<?> attachmentType = this.attachmentTypes.get(attachmentId);
        if (attachmentType == null || !attachmentType.isSynced()) return;

        LevelChunk chunk = client.level.getChunkSource().getChunkNow(pos.x, pos.z);
        if (chunk == null) {
            if (bytes == null) return;
            this.pendingChunkSyncs
                    .computeIfAbsent(new ChunkKey(dimension, pos), k -> new ConcurrentHashMap<>())
                    .put(attachmentId, bytes);
            return;
        }
        this.decodeAndSet(chunk, attachmentType, bytes);
    }

    @SuppressWarnings("unchecked")
    private <T> void decodeAndSet(AttachmentTarget target, ElysiumAttachmentType<T> type, byte @Nullable [] bytes) {
        AttachmentType<T> nativeType = this.lookup(type);
        if (bytes == null) { target.setAttached(nativeType, null); return; }
        ElysiumStreamCodec<? super FriendlyByteBuf, T> codec = type.syncCodec();
        if (codec == null) return;
        try {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));
            target.setAttached(nativeType, codec.decode(buf));
        } catch (Exception ignored) {}
    }

    private record ChunkKey(ResourceLocation dimension, ChunkPos pos) {}
}