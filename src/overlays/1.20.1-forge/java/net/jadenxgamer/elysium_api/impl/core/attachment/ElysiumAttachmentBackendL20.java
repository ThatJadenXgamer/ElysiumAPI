package net.jadenxgamer.elysium_api.impl.core.attachment;

import net.jadenxgamer.elysium_api.ElysiumAPI;
import net.jadenxgamer.elysium_api.api.core.attachment.ElysiumAttachmentRegistry;
import net.jadenxgamer.elysium_api.api.core.attachment.ElysiumAttachmentType;
import net.jadenxgamer.elysium_api.api.core.attachment.ElysiumAttachments;
import net.jadenxgamer.elysium_api.api.core.attachment.pipeline.ElysiumAttachmentBackend;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ApiStatus.Internal
public final class ElysiumAttachmentBackendL20 implements ElysiumAttachmentBackend {

    static final ResourceLocation CAPABILITY_KEY = ElysiumAPI.idPath("elysium_api", "attachment_holder");
    public static final Capability<IElysiumAttachmentHolder> CAPABILITY = CapabilityManager.get(new CapabilityToken<IElysiumAttachmentHolder>() {});

    private static final ElysiumAttachmentBackendL20 INSTANCE = new ElysiumAttachmentBackendL20();

    private final Map<ResourceLocation, ElysiumAttachmentType<?>> types = new HashMap<>();
    private final Map<UUID, PendingCopy> pendingCopies = new ConcurrentHashMap<>();

    private ElysiumAttachmentBackendL20() {
    }

    public static void installBackend(IEventBus modBus) {
        ElysiumAttachments.installBackend(INSTANCE);

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
        });

        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
            INSTANCE.pendingCopies.remove(event.getEntity().getUUID());
        });
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
    }

    private void captureAttachments(Player player, boolean keepEverything) {
        UUID uuid = player.getUUID();
        if (this.pendingCopies.containsKey(uuid)) return;
        IElysiumAttachmentHolder holder = this.resolveHolder(player);
        if (holder == null) return;
        Map<ResourceLocation, Object> stash = new HashMap<>();
        for (ElysiumAttachmentType<?> type : this.types.values()) {
            if (!type.isPersistent() && !type.isCopyOnDeath()) continue;
            Object value = holder.getData(type);
            if (value != null) stash.put(type.id(), value);
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
    }

    @SuppressWarnings("unchecked")
    private void applyStash(IElysiumAttachmentHolder holder, PendingCopy pending) {
        for (Map.Entry<ResourceLocation, Object> entry : pending.data().entrySet()) {
            ElysiumAttachmentType<?> type = this.types.get(entry.getKey());
            if (type == null) continue;
            if (pending.keepEverything()) {
                if (!type.isPersistent()) continue;
            } else {
                if (!type.isCopyOnDeath()) continue;
            }
            holder.setData((ElysiumAttachmentType<Object>) type, entry.getValue());
        }
    }

    @Override
    public void register(String modId, ElysiumAttachmentRegistry registry) {
        for (ElysiumAttachmentType<?> type : registry.types()) this.types.put(type.id(), type);
    }

    @Nullable
    public ElysiumAttachmentType<?> findType(ResourceLocation id) {
        return this.types.get(id);
    }

    public Map<ResourceLocation, ElysiumAttachmentType<?>> registeredTypes() {
        return this.types;
    }

    @Nullable
    private IElysiumAttachmentHolder resolveHolder(Object holder) {
        if (!(holder instanceof ICapabilityProvider provider)) return null;
        return provider.getCapability(CAPABILITY).resolve().orElse(null);
    }

    private IElysiumAttachmentHolder requireHolder(Object holder) {
        IElysiumAttachmentHolder cap = this.resolveHolder(holder);
        if (cap == null) {
            return NoOpHolder.INSTANCE;
        }
        return cap;
    }

    @Override
    public <T> T getData(Entity holder, ElysiumAttachmentType<T> type) {
        T value = this.requireHolder(holder).getData(type);
        return value != null ? value : type.defaultValue().get();
    }

    @Override
    public <T> T getData(BlockEntity holder, ElysiumAttachmentType<T> type) {
        T value = this.requireHolder(holder).getData(type);
        return value != null ? value : type.defaultValue().get();
    }

    @Override
    public <T> T getData(LevelChunk holder, ElysiumAttachmentType<T> type) {
        T value = this.requireHolder(holder).getData(type);
        return value != null ? value : type.defaultValue().get();
    }

    @Override
    public <T> void setData(Entity holder, ElysiumAttachmentType<T> type, @Nullable T value) {
        this.requireHolder(holder).setData(type, value);
    }

    @Override
    public <T> void setData(BlockEntity holder, ElysiumAttachmentType<T> type, @Nullable T value) {
        this.requireHolder(holder).setData(type, value);
        holder.setChanged();
    }

    @Override
    public <T> void setData(LevelChunk holder, ElysiumAttachmentType<T> type, @Nullable T value) {
        this.requireHolder(holder).setData(type, value);
        holder.setUnsaved(true);
    }

    @Override
    public <T> boolean hasData(Entity holder, ElysiumAttachmentType<T> type) {
        return this.requireHolder(holder).hasData(type);
    }

    @Override
    public <T> boolean hasData(BlockEntity holder, ElysiumAttachmentType<T> type) {
        return this.requireHolder(holder).hasData(type);
    }

    @Override
    public <T> boolean hasData(LevelChunk holder, ElysiumAttachmentType<T> type) {
        return this.requireHolder(holder).hasData(type);
    }

    @Override
    public <T> void removeData(Entity holder, ElysiumAttachmentType<T> type) {
        this.requireHolder(holder).setData(type, null);
    }

    @Override
    public <T> void removeData(BlockEntity holder, ElysiumAttachmentType<T> type) {
        this.requireHolder(holder).setData(type, null);
        holder.setChanged();
    }

    @Override
    public <T> void removeData(LevelChunk holder, ElysiumAttachmentType<T> type) {
        this.requireHolder(holder).setData(type, null);
        holder.setUnsaved(true);
    }

    private record PendingCopy(Map<ResourceLocation, Object> data, boolean keepEverything) {
    }

    private static final class NoOpHolder implements IElysiumAttachmentHolder {

        static final NoOpHolder INSTANCE = new NoOpHolder();

        private NoOpHolder() {
        }

        @Override
        public <T> @Nullable T getData(ElysiumAttachmentType<T> type) {
            return null;
        }

        @Override
        public <T> void setData(ElysiumAttachmentType<T> type, @Nullable T value) {
        }

        @Override
        public <T> boolean hasData(ElysiumAttachmentType<T> type) {
            return false;
        }
    }
}