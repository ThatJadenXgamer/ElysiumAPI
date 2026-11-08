package net.jadenxgamer.elysium_api.impl.core.attachment;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.jadenxgamer.elysium_api.api.core.attachment.ElysiumAttachmentRegistry;
import net.jadenxgamer.elysium_api.api.core.attachment.ElysiumAttachmentType;
import net.jadenxgamer.elysium_api.api.core.attachment.ElysiumAttachments;
import net.jadenxgamer.elysium_api.api.core.attachment.pipeline.ElysiumAttachmentBackend;
import net.jadenxgamer.elysium_api.api.core.attachment.pipeline.ElysiumAttachmentSyncDispatcher;
import net.jadenxgamer.elysium_api.api.network.pipeline.ElysiumStreamCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ElysiumAttachmentBackendF21 implements ElysiumAttachmentBackend {

    private static final ElysiumAttachmentBackendF21 INSTANCE = new ElysiumAttachmentBackendF21();
    private static final int POLL_INTERVAL_TICKS = 10;

    private final Map<ResourceLocation, AttachmentType<?>> fabricTypes = new HashMap<>();
    private final Map<ResourceLocation, ElysiumAttachmentType<?>> elysiumTypes = new HashMap<>();
    private final Map<UUID, Set<UUID>> initialSyncedTo = new ConcurrentHashMap<>();
    private final Map<UUID, Map<ResourceLocation, byte[]>> lastSent = new ConcurrentHashMap<>();

    private int tickCounter = 0;

    private ElysiumAttachmentBackendF21() {}

    public static void installBackend() {
        ElysiumAttachments.installBackend(INSTANCE);
        ElysiumAttachmentSyncDispatcher.setClientApplier(INSTANCE::applyClientSync);

        ServerTickEvents.END_SERVER_TICK.register(INSTANCE::onServerTick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> INSTANCE.onPlayerJoin(handler.getPlayer()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> INSTANCE.onPlayerDisconnect(handler.getPlayer()));
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> INSTANCE.onEntityUnload(entity));
    }

    @Override
    public void register(String modId, ElysiumAttachmentRegistry registry) {
        for (ElysiumAttachmentType<?> type : registry.types()) {
            this.registerType(type);
            this.elysiumTypes.put(type.id(), type);
        }
    }

    private <T> void registerType(ElysiumAttachmentType<T> type) {
        AttachmentRegistry.Builder<T> builder = AttachmentRegistry.builder();
        builder.initializer(type.defaultValue());

        if (type.codec() != null) builder.persistent(type.codec());
        if (type.isCopyOnDeath()) builder.copyOnDeath();

        AttachmentType<T> fabricType = builder.buildAndRegister(type.id());
        this.fabricTypes.put(type.id(), fabricType);
    }

    @SuppressWarnings("unchecked")
    private <T> AttachmentType<T> lookup(ElysiumAttachmentType<T> type) {
        AttachmentType<T> fabricType = (AttachmentType<T>) this.fabricTypes.get(type.id());
        if (fabricType == null) throw new IllegalStateException("Attachment type '" + type.id() + "' has not been registered; call ElysiumAttachments.register before accessing it.");
        return fabricType;
    }

    @Override
    public <T> T getData(Entity holder, ElysiumAttachmentType<T> type) {
        return holder.getAttachedOrGet(this.lookup(type), type.defaultValue());
    }

    @Override
    public <T> T getData(BlockEntity holder, ElysiumAttachmentType<T> type) {
        return holder.getAttachedOrGet(this.lookup(type), type.defaultValue());
    }

    @Override
    public <T> T getData(LevelChunk holder, ElysiumAttachmentType<T> type) {
        return holder.getAttachedOrGet(this.lookup(type), type.defaultValue());
    }

    @Override
    public <T> void setData(Entity holder, ElysiumAttachmentType<T> type, @Nullable T value) {
        if (value == null) {
            this.removeData(holder, type);
        } else {
            holder.setAttached(this.lookup(type), value);
            this.broadcastUpdate(holder, type, value);
        }
    }

    @Override
    public <T> void setData(BlockEntity holder, ElysiumAttachmentType<T> type, @Nullable T value) {
        if (value == null) this.removeData(holder, type);
        else holder.setAttached(this.lookup(type), value);
    }

    @Override
    public <T> void setData(LevelChunk holder, ElysiumAttachmentType<T> type, @Nullable T value) {
        if (value == null) this.removeData(holder, type);
        else holder.setAttached(this.lookup(type), value);
    }

    @Override
    public <T> boolean hasData(Entity holder, ElysiumAttachmentType<T> type) {
        return holder.hasAttached(this.lookup(type));
    }

    @Override
    public <T> boolean hasData(BlockEntity holder, ElysiumAttachmentType<T> type) {
        return holder.hasAttached(this.lookup(type));
    }

    @Override
    public <T> boolean hasData(LevelChunk holder, ElysiumAttachmentType<T> type) {
        return holder.hasAttached(this.lookup(type));
    }

    @Override
    public <T> void removeData(Entity holder, ElysiumAttachmentType<T> type) {
        boolean had = holder.hasAttached(this.lookup(type));
        holder.setAttached(this.lookup(type), null);
        if (had) this.broadcastRemoval(holder, type);
    }

    @Override
    public <T> void removeData(BlockEntity holder, ElysiumAttachmentType<T> type) {
        holder.setAttached(this.lookup(type), null);
    }

    @Override
    public <T> void removeData(LevelChunk holder, ElysiumAttachmentType<T> type) {
        holder.setAttached(this.lookup(type), null);
    }

    private <T> void broadcastUpdate(Entity holder, ElysiumAttachmentType<T> type, T value) {
        if (holder.level().isClientSide()) return;
        if (!type.isSynced()) return;
        byte[] data = this.encodeSyncData(type, value);
        if (data == null) return;
        if (!this.shouldBroadcast(holder, type, data)) return;
        ElysiumAttachmentSyncDispatcher.sendToTrackingAndSelf(holder, type.id(), data);
    }

    private <T> void broadcastRemoval(Entity holder, ElysiumAttachmentType<T> type) {
        if (holder.level().isClientSide()) return;
        if (!type.isSynced()) return;
        Map<ResourceLocation, byte[]> perEntity = this.lastSent.get(holder.getUUID());
        if (perEntity != null) perEntity.remove(type.id());
        ElysiumAttachmentSyncDispatcher.sendToTrackingAndSelf(holder, type.id(), null);
    }

    @SuppressWarnings("unchecked")
    private <T> byte[] encodeSyncData(ElysiumAttachmentType<T> type, T value) {
        ElysiumStreamCodec<? super FriendlyByteBuf, T> codec = type.syncCodec();
        if (codec == null) return null;
        try {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            codec.encode(buf, value);
            byte[] data = new byte[buf.readableBytes()];
            buf.readBytes(data);
            return data;
        } catch (Exception e) { return null; }
    }

    private boolean shouldBroadcast(Entity holder, ElysiumAttachmentType<?> type, byte[] data) {
        Map<ResourceLocation, byte[]> perEntity = this.lastSent.computeIfAbsent(holder.getUUID(), k -> new ConcurrentHashMap<>());
        byte[] last = perEntity.get(type.id());
        if (last != null && Arrays.equals(last, data)) return false;
        perEntity.put(type.id(), data);
        return true;
    }

    private void onServerTick(MinecraftServer server) {
        if (++this.tickCounter < POLL_INTERVAL_TICKS) return;
        this.tickCounter = 0;
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) this.pollEntitySync(entity);
        }
    }

    private void pollEntitySync(Entity entity) {
        UUID entityId = entity.getUUID();
        boolean anySyncable = false;
        for (ElysiumAttachmentType<?> type : this.elysiumTypes.values()) {
            if (!type.isSynced()) continue;
            if (entity.hasAttached(this.lookup(type))) {
                anySyncable = true;
                break;
            }
        }
        if (!anySyncable) {
            this.initialSyncedTo.remove(entityId);
            return;
        }

        Set<UUID> alreadySent = this.initialSyncedTo.computeIfAbsent(entityId, k -> ConcurrentHashMap.newKeySet());
        Set<ServerPlayer> tracking = new HashSet<>(PlayerLookup.tracking(entity));
        if (entity instanceof ServerPlayer self) tracking.add(self);
        Set<UUID> currentUuids = new HashSet<>();
        for (ServerPlayer player : tracking) currentUuids.add(player.getUUID());
        alreadySent.retainAll(currentUuids);

        for (ServerPlayer player : tracking) {
            if (alreadySent.add(player.getUUID())) this.sendAllSyncedTypes(player, entity);
        }
    }

    private void sendAllSyncedTypes(ServerPlayer player, Entity entity) {
        for (ElysiumAttachmentType<?> type : this.elysiumTypes.values()) {
            if (!type.isSynced()) continue;
            this.sendCurrentTo(player, entity, type);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> void sendCurrentTo(ServerPlayer player, Entity entity, ElysiumAttachmentType<T> type) {
        if (!entity.hasAttached(this.lookup(type))) return;
        T value = entity.getAttached(this.lookup(type));
        if (value == null) return;
        byte[] data = this.encodeSyncData(type, value);
        if (data == null) return;
        ElysiumAttachmentSyncDispatcher.sendToPlayer(player, entity, type.id(), data);
    }

    private void onPlayerJoin(ServerPlayer player) {
        this.sendAllSyncedTypes(player, player);
    }

    private void onPlayerDisconnect(ServerPlayer player) {
        UUID playerId = player.getUUID();
        for (Set<UUID> set : this.initialSyncedTo.values()) set.remove(playerId);
    }

    private void onEntityUnload(Entity entity) {
        this.initialSyncedTo.remove(entity.getUUID());
        this.lastSent.remove(entity.getUUID());
    }

    private void applyClientSync(int entityId, ResourceLocation attachmentId, @Nullable byte[] data) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Entity entity = mc.level.getEntity(entityId);
        if (entity == null) return;
        ElysiumAttachmentType<?> type = this.elysiumTypes.get(attachmentId);
        if (type == null || !type.isSynced()) return;
        this.decodeAndSet(entity, type, data);
    }

    @SuppressWarnings("unchecked")
    private <T> void decodeAndSet(Entity entity, ElysiumAttachmentType<T> type, @Nullable byte[] data) {
        if (data == null) {
            entity.setAttached(this.lookup(type), null);
            return;
        }
        ElysiumStreamCodec<? super FriendlyByteBuf, T> codec = (ElysiumStreamCodec<? super FriendlyByteBuf, T>) type.syncCodec();
        if (codec == null) return;
        try {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
            T value = codec.decode(buf);
            entity.setAttached(this.lookup(type), value);
        } catch (Exception ignored) {}
    }
}