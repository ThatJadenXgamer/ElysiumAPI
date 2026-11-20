package net.jadenxgamer.elysium_api.impl.core.attachment;

import net.jadenxgamer.elysium_api.ElysiumAPI;
import net.jadenxgamer.elysium_api.api.core.attachment.ElysiumAttachmentRegistry;
import net.jadenxgamer.elysium_api.api.core.attachment.ElysiumAttachmentType;
import net.jadenxgamer.elysium_api.api.core.attachment.ElysiumAttachments;
import net.jadenxgamer.elysium_api.api.core.attachment.pipeline.ElysiumAttachmentBackend;
import net.jadenxgamer.elysium_api.api.network.pipeline.ElysiumStreamCodec;
import net.jadenxgamer.elysium_api.impl.network.ElysiumStreamCodecBridge;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class ElysiumAttachmentBackendN21 implements ElysiumAttachmentBackend {

    private static final ElysiumAttachmentBackendN21 INSTANCE = new ElysiumAttachmentBackendN21();

    private final Map<String, DeferredRegister<AttachmentType<?>>> deferredRegistersByModId = new HashMap<>();
    private final Map<ResourceLocation, AttachmentType<?>> nativeTypes = new HashMap<>();
    private IEventBus modBus;

    private ElysiumAttachmentBackendN21() {}

    public static void installBackend(IEventBus modBus) {
        INSTANCE.modBus = modBus;
        ElysiumAttachments.installBackend(INSTANCE);
        ElysiumAPI.LOGGER.info("Successfully installed Backend for Data Attachments Facade");
    }

    @Override
    public void register(String modId, ElysiumAttachmentRegistry registry) {
        if (this.deferredRegistersByModId.containsKey(modId)) throw new IllegalStateException("Attachments have already been registered for mod id '" + modId + "'; ElysiumAttachments.register must be called exactly once per mod.");
        DeferredRegister<AttachmentType<?>> deferred = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, modId);
        deferred.register(this.modBus);
        this.deferredRegistersByModId.put(modId, deferred);
        for (ElysiumAttachmentType<?> attachmentType : registry.types()) {
            this.registerType(deferred, attachmentType);
        }
    }

    private <T> void registerType(DeferredRegister<AttachmentType<?>> deferred, ElysiumAttachmentType<T> type) {
        deferred.register(type.id().getPath(), () -> {
            AttachmentType.Builder<T> builder = AttachmentType.builder(type.defaultValue());

            if (type.isPersistent() && type.codec() != null) {
                builder.serialize(type.codec());
                if (type.isCopyOnDeath()) builder.copyOnDeath();
            } else if (type.isCopyOnDeath()) {
                ElysiumAPI.LOGGER.warn("Attachment '{}' declares copyOnDeath but does not declare a codec; NeoForge requires serialization for copyOnDeath, so the flag is being ignored on this platform.", type.id());
            }

            ElysiumStreamCodec<? super FriendlyByteBuf, T> syncCodec = type.syncCodec();
            if (syncCodec != null) builder.sync(ElysiumStreamCodecBridge.toVanilla(syncCodec));

            AttachmentType<T> nativeType = builder.build();
            this.nativeTypes.put(type.id(), nativeType);
            return nativeType;
        });
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

    private <T> T readAttachment(IAttachmentHolder target, ElysiumAttachmentType<T> type) {
        return target.getExistingData(this.lookup(type)).orElseGet(type.defaultValue());
    }

    @Override
    public <T> void setData(Entity holder, ElysiumAttachmentType<T> type, @Nullable T value) {
        if (value == null) { this.removeData(holder, type); return; }
        holder.setData(this.lookup(type), value);
    }

    @Override
    public <T> void setData(BlockEntity holder, ElysiumAttachmentType<T> type, @Nullable T value) {
        if (value == null) { this.removeData(holder, type); return; }
        holder.setData(this.lookup(type), value);
    }

    @Override
    public <T> void setData(LevelChunk holder, ElysiumAttachmentType<T> type, @Nullable T value) {
        if (value == null) { this.removeData(holder, type); return; }
        holder.setData(this.lookup(type), value);
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

    private <T> boolean hasAttachment(IAttachmentHolder target, ElysiumAttachmentType<T> type) {
        return target.hasData(this.lookup(type));
    }

    @Override
    public <T> void removeData(Entity holder, ElysiumAttachmentType<T> type) {
        holder.removeData(this.lookup(type));
    }

    @Override
    public <T> void removeData(BlockEntity holder, ElysiumAttachmentType<T> type) {
        holder.removeData(this.lookup(type));
    }

    @Override
    public <T> void removeData(LevelChunk holder, ElysiumAttachmentType<T> type) {
        holder.removeData(this.lookup(type));
    }
}