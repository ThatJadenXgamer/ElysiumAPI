package net.jadenxgamer.elysium_api.impl.core.attachment;

import io.netty.buffer.Unpooled;
import net.jadenxgamer.elysium_api.ElysiumAPI;
import net.jadenxgamer.elysium_api.api.core.attachment.ElysiumAttachmentRegistry;
import net.jadenxgamer.elysium_api.api.core.attachment.ElysiumAttachmentType;
import net.jadenxgamer.elysium_api.api.core.attachment.ElysiumAttachments;
import net.jadenxgamer.elysium_api.api.core.attachment.pipeline.ElysiumAttachmentBackend;
import net.jadenxgamer.elysium_api.api.core.attachment.pipeline.ElysiumAttachmentSyncDispatcher;
import net.jadenxgamer.elysium_api.api.network.pipeline.ElysiumStreamCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.ChunkWatchEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import org.jetbrains.annotations.ApiStatus;
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

@ApiStatus.Internal
public final class ElysiumAttachmentBackendL20 implements ElysiumAttachmentBackend {

    static final ResourceLocation CAPABILITY_KEY = ElysiumAPI.idPath("elysium_api", "attachment_holder");
    public static final Capability<IElysiumAttachmentHolder> CAPABILITY = CapabilityManager.get(new CapabilityToken<IElysiumAttachmentHolder>() {});

    private static final ElysiumAttachmentBackendL20 INSTANCE = new ElysiumAttachmentBackendL20();

    private final Map<ResourceLocation, ElysiumAttachmentType<?>> attachmentTypes = new HashMap<>();
    private final List<ElysiumAttachmentType<?>> syncedTypes = new ArrayList<>();
    private final Map<UUID, PendingCopy> pendingCopies = new ConcurrentHashMap<>();
    private final Set<UUID> pendingSelfSyncPlayers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Map<ResourceLocation, byte[]>> lastSentEntityBytes = new ConcurrentHashMap<>();
    private final Map<ChunkKey, Map<BlockPos, Map<ResourceLocation, byte[]>>> lastSentBlockEntityBytes = new ConcurrentHashMap<>();
    private final Map<ChunkKey, Map<ResourceLocation, byte[]>> lastSentChunkBytes = new ConcurrentHashMap<>();
    private final Map<ChunkKey, Map<BlockPos, Map<ResourceLocation, byte[]>>> pendingBlockEntitySyncs = new ConcurrentHashMap<>();
    private final Map<ChunkKey, Map<ResourceLocation, byte[]>> pendingChunkSyncs = new ConcurrentHashMap<>();

    private ElysiumAttachmentBackendL20() {}

    public static void installBackend(IEventBus modBus) {
        ElysiumAttachments.installBackend(INSTANCE);
        ElysiumAttachmentSyncDispatcher.setEntityApplier(INSTANCE::applyClientEntitySync);
        ElysiumAttachmentSyncDispatcher.setBlockEntityApplier(INSTANCE::applyClientBlockEntitySync);
        ElysiumAttachmentSyncDispatcher.setChunkApplier(INSTANCE::applyClientChunkSync);

        modBus.addListener((RegisterCapabilitiesEvent event) -> event.register(IElysiumAttachmentHolder.class));

        MinecraftForge.EVENT_BUS.addGenericListener(Entity.class, (AttachCapabilitiesEvent<Entity> event) ->
                event.addCapability(CAPABILITY_KEY, new ElysiumAttachmentProvider(INSTANCE)));
        MinecraftForge.EVENT_BUS.addGenericListener(BlockEntity.class, (AttachCapabilitiesEvent<BlockEntity> event) ->
                event.addCapability(CAPABILITY_KEY, new ElysiumAttachmentProvider(INSTANCE)));
        MinecraftForge.EVENT_BUS.addGenericListener(LevelChunk.class, (AttachCapabilitiesEvent<LevelChunk> event) ->
                event.addCapability(CAPABILITY_KEY, new ElysiumAttachmentProvider(INSTANCE)));

        MinecraftForge.EVENT_BUS.addListener((LivingDeathEvent event) -> {
            if (!(event.getEntity() instanceof ServerPlayer player)) return;
            INSTANCE.captureAttachments(player, false);
        });

        MinecraftForge.EVENT_BUS.addListener((TickEvent.PlayerTickEvent event) -> {
            if (event.phase != TickEvent.Phase.END) return;
            if (!(event.player instanceof ServerPlayer sp)) return;
            INSTANCE.tryDeferredApply(sp);
            INSTANCE.trySelfSync(sp);
        });

        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
            UUID uuid = event.getEntity().getUUID();
            INSTANCE.pendingCopies.remove(uuid);
            INSTANCE.pendingSelfSyncPlayers.remove(uuid);
        });

        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            if (!(event.getEntity() instanceof ServerPlayer player)) return;
            INSTANCE.sendAllSyncedEntityTypes(player, player);
        });

        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.StartTracking event) -> {
            if (!(event.getEntity() instanceof ServerPlayer player)) return;
            INSTANCE.sendAllSyncedEntityTypes(player, event.getTarget());
        });

        MinecraftForge.EVENT_BUS.addListener((EntityLeaveLevelEvent event) -> INSTANCE.lastSentEntityBytes.remove(event.getEntity().getUUID()));

        MinecraftForge.EVENT_BUS.addListener((ChunkWatchEvent.Watch event) ->
                INSTANCE.onChunkWatch(event.getPlayer(), event.getPos(), event.getLevel()));

        MinecraftForge.EVENT_BUS.addListener((ChunkEvent.Load event) -> {
            if (!(event.getLevel() instanceof Level level)) return;
            if (!(event.getChunk() instanceof LevelChunk chunk)) return;
            INSTANCE.onChunkLoad(level, chunk);
        });

        MinecraftForge.EVENT_BUS.addListener((ChunkEvent.Unload event) -> {
            if (!(event.getLevel() instanceof Level level)) return;
            if (!(event.getChunk() instanceof LevelChunk chunk)) return;
            INSTANCE.onChunkUnload(level, chunk);
        });

        ElysiumAPI.LOGGER.info("Successfully installed Backend for Data Attachments Facade");
    }

    public static void captureBeforeRestore(Player oldPlayer, boolean keepEverything) {
        INSTANCE.captureAttachments(oldPlayer, keepEverything);
    }

    public static void applyAfterRestore(Player newPlayer) {
        UUID uuid = newPlayer.getUUID();
        PendingCopy pending = INSTANCE.pendingCopies.get(uuid);
        if (pending == null) return;
        IElysiumAttachmentHolder holder = INSTANCE.resolveHolder(newPlayer);
        if (holder == null) return;
        INSTANCE.pendingCopies.remove(uuid);
        INSTANCE.applyStash(holder, pending);
        INSTANCE.pendingSelfSyncPlayers.add(uuid);
    }

    private void captureAttachments(Player player, boolean keepEverything) {
        UUID uuid = player.getUUID();
        if (this.pendingCopies.containsKey(uuid)) return;
        IElysiumAttachmentHolder holder = this.resolveHolder(player);
        if (holder == null) return;
        Map<ResourceLocation, Object> stash = new HashMap<>();
        for (ElysiumAttachmentType<?> attachmentType : this.attachmentTypes.values()) {
            if (!attachmentType.isPersistent() && !attachmentType.isCopyOnDeath()) continue;
            Object value = holder.getData(attachmentType);
            if (value != null) stash.put(attachmentType.id(), value);
        }
        if (!stash.isEmpty()) this.pendingCopies.put(uuid, new PendingCopy(stash, keepEverything));
    }

    private void tryDeferredApply(ServerPlayer player) {
        UUID uuid = player.getUUID();
        PendingCopy pending = this.pendingCopies.get(uuid);
        if (pending == null) return;
        if (!player.isAlive() || player.isRemoved()) return;
        IElysiumAttachmentHolder holder = this.resolveHolder(player);
        if (holder == null) return;
        this.pendingCopies.remove(uuid);
        this.applyStash(holder, pending);
        this.pendingSelfSyncPlayers.add(uuid);
    }

    private void trySelfSync(ServerPlayer player) {
        UUID uuid = player.getUUID();
        if (!this.pendingSelfSyncPlayers.remove(uuid)) return;
        this.sendAllSyncedEntityTypes(player, player);
    }

    @SuppressWarnings("unchecked")
    private void applyStash(IElysiumAttachmentHolder holder, PendingCopy pending) {
        for (Map.Entry<ResourceLocation, Object> entry : pending.data().entrySet()) {
            ElysiumAttachmentType<?> attachmentType = this.attachmentTypes.get(entry.getKey());
            if (attachmentType == null) continue;
            if (pending.keepEverything() ? !attachmentType.isPersistent() : !attachmentType.isCopyOnDeath()) continue;
            holder.setData((ElysiumAttachmentType<Object>) attachmentType, entry.getValue());
        }
    }

    @Override
    public void register(String modId, ElysiumAttachmentRegistry registry) {
        for (ElysiumAttachmentType<?> attachmentType : registry.types()) {
            this.attachmentTypes.put(attachmentType.id(), attachmentType);
            if (attachmentType.isSynced()) this.syncedTypes.add(attachmentType);
        }
    }

    @Nullable
    public ElysiumAttachmentType<?> findType(ResourceLocation id) {
        return this.attachmentTypes.get(id);
    }

    public Map<ResourceLocation, ElysiumAttachmentType<?>> registeredTypes() {
        return this.attachmentTypes;
    }

    @Nullable
    private IElysiumAttachmentHolder resolveHolder(Object holderObject) {
        if (!(holderObject instanceof ICapabilityProvider provider)) return null;
        return provider.getCapability(CAPABILITY).resolve().orElse(null);
    }

    private IElysiumAttachmentHolder requireHolder(Object holderObject) {
        IElysiumAttachmentHolder holder = this.resolveHolder(holderObject);
        return holder != null ? holder : NoOpHolder.INSTANCE;
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

    private <T> T readAttachment(Object holderObject, ElysiumAttachmentType<T> type) {
        T value = this.requireHolder(holderObject).getData(type);
        return value != null ? value : type.defaultValue().get();
    }

    @Override
    public <T> void setData(Entity holder, ElysiumAttachmentType<T> type, @Nullable T value) {
        if (value == null) { this.removeData(holder, type); return; }
        this.requireHolder(holder).setData(type, value);
        this.broadcastEntityUpdate(holder, type, value);
    }

    @Override
    public <T> void setData(BlockEntity holder, ElysiumAttachmentType<T> type, @Nullable T value) {
        if (value == null) { this.removeData(holder, type); return; }
        this.requireHolder(holder).setData(type, value);
        holder.setChanged();
        this.broadcastBlockEntityUpdate(holder, type, value);
    }

    @Override
    public <T> void setData(LevelChunk holder, ElysiumAttachmentType<T> type, @Nullable T value) {
        if (value == null) { this.removeData(holder, type); return; }
        this.requireHolder(holder).setData(type, value);
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

    private <T> boolean hasAttachment(Object holderObject, ElysiumAttachmentType<T> type) {
        return this.requireHolder(holderObject).hasData(type);
    }

    @Override
    public <T> void removeData(Entity holder, ElysiumAttachmentType<T> type) {
        IElysiumAttachmentHolder nativeHolder = this.requireHolder(holder);
        boolean hadValue = nativeHolder.hasData(type);
        nativeHolder.setData(type, null);
        if (hadValue) this.broadcastEntityRemoval(holder, type);
    }

    @Override
    public <T> void removeData(BlockEntity holder, ElysiumAttachmentType<T> type) {
        IElysiumAttachmentHolder nativeHolder = this.requireHolder(holder);
        boolean hadValue = nativeHolder.hasData(type);
        nativeHolder.setData(type, null);
        holder.setChanged();
        if (hadValue) this.broadcastBlockEntityRemoval(holder, type);
    }

    @Override
    public <T> void removeData(LevelChunk holder, ElysiumAttachmentType<T> type) {
        IElysiumAttachmentHolder nativeHolder = this.requireHolder(holder);
        boolean hadValue = nativeHolder.hasData(type);
        nativeHolder.setData(type, null);
        holder.setUnsaved(true);
        if (hadValue) this.broadcastChunkRemoval(holder, type);
    }

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

    private <T> void sendCurrentTo(Object holderObject, ElysiumAttachmentType<T> type, BiConsumer<ResourceLocation, byte[]> dispatcher) {
        IElysiumAttachmentHolder holder = this.resolveHolder(holderObject);
        if (holder == null) return;
        T value = holder.getData(type);
        if (value == null) return;
        byte[] bytes = this.encodeSyncData(type, value);
        if (bytes == null) return;
        dispatcher.accept(type.id(), bytes);
    }

    private void onChunkWatch(ServerPlayer player, ChunkPos pos, ServerLevel level) {
        LevelChunk chunk = level.getChunk(pos.x, pos.z);

        for (ElysiumAttachmentType<?> attachmentType : this.syncedTypes)
            this.sendCurrentTo(chunk, attachmentType, (attachmentId, bytes) -> ElysiumAttachmentSyncDispatcher.sendToPlayer(player, chunk, attachmentId, bytes));

        for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
            for (ElysiumAttachmentType<?> attachmentType : this.syncedTypes)
                this.sendCurrentTo(blockEntity, attachmentType, (attachmentId, bytes) -> ElysiumAttachmentSyncDispatcher.sendToPlayer(player, blockEntity, attachmentId, bytes));
        }
    }

    private void onChunkLoad(Level level, LevelChunk chunk) {
        if (!level.isClientSide()) return;

        ChunkKey chunkKey = new ChunkKey(level.dimension().location(), chunk.getPos());

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
            BlockEntity blockEntity = level.getBlockEntity(blockEntityEntry.getKey());
            if (blockEntity == null) continue;
            for (Map.Entry<ResourceLocation, byte[]> attachmentEntry : blockEntityEntry.getValue().entrySet()) {
                ElysiumAttachmentType<?> attachmentType = this.attachmentTypes.get(attachmentEntry.getKey());
                if (attachmentType == null || !attachmentType.isSynced()) continue;
                this.decodeAndSet(blockEntity, attachmentType, attachmentEntry.getValue());
            }
        }
    }

    private void onChunkUnload(Level level, LevelChunk chunk) {
        ChunkKey chunkKey = new ChunkKey(level.dimension().location(), chunk.getPos());
        if (level.isClientSide()) {
            this.pendingBlockEntitySyncs.remove(chunkKey);
            this.pendingChunkSyncs.remove(chunkKey);
        } else {
            this.lastSentBlockEntityBytes.remove(chunkKey);
            this.lastSentChunkBytes.remove(chunkKey);
        }
    }

    private void applyClientEntitySync(int entityId, ResourceLocation attachmentId, byte @Nullable [] bytes) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;
        Entity entity = client.level.getEntity(entityId);
        if (entity == null && client.player != null && client.player.getId() == entityId) entity = client.player;
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
    private <T> void decodeAndSet(Object holderObject, ElysiumAttachmentType<T> type, byte @Nullable [] bytes) {
        IElysiumAttachmentHolder holder = this.resolveHolder(holderObject);
        if (holder == null) return;
        if (bytes == null) { holder.setData(type, null); return; }
        ElysiumStreamCodec<? super FriendlyByteBuf, T> codec = type.syncCodec();
        if (codec == null) return;
        try {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));
            holder.setData(type, codec.decode(buf));
        } catch (Exception ignored) {}
    }

    private record ChunkKey(ResourceLocation dimension, ChunkPos pos) {}

    private record PendingCopy(Map<ResourceLocation, Object> data, boolean keepEverything) {}

    private static final class NoOpHolder implements IElysiumAttachmentHolder {

        static final NoOpHolder INSTANCE = new NoOpHolder();

        private NoOpHolder() {}

        @Override
        public <T> @Nullable T getData(ElysiumAttachmentType<T> type) {
            return null;
        }

        @Override
        public <T> void setData(ElysiumAttachmentType<T> type, @Nullable T value) {}

        @Override
        public <T> boolean hasData(ElysiumAttachmentType<T> type) {
            return false;
        }
    }
}