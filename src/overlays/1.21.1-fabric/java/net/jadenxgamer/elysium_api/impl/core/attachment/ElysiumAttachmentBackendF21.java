package net.jadenxgamer.elysium_api.impl.core.attachment;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.jadenxgamer.elysium_api.api.core.attachment.ElysiumAttachmentRegistry;
import net.jadenxgamer.elysium_api.api.core.attachment.ElysiumAttachmentType;
import net.jadenxgamer.elysium_api.api.core.attachment.pipeline.ElysiumAttachmentBackend;
import net.jadenxgamer.elysium_api.api.core.attachment.ElysiumAttachments;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class ElysiumAttachmentBackendF21 implements ElysiumAttachmentBackend {

    private static final ElysiumAttachmentBackendF21 INSTANCE = new ElysiumAttachmentBackendF21();

    private final Map<ResourceLocation, AttachmentType<?>> fabricTypes = new HashMap<>();

    private ElysiumAttachmentBackendF21() {}

    public static void installBackend() {
        ElysiumAttachments.installBackend(INSTANCE);
    }

    @Override
    public void register(String modId, ElysiumAttachmentRegistry registry) {
        for (ElysiumAttachmentType<?> type : registry.types()) {
            this.registerType(type);
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
        if (value == null) this.removeData(holder, type);
        else holder.setAttached(this.lookup(type), value);
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
        holder.setAttached(this.lookup(type), null);
    }

    @Override
    public <T> void removeData(BlockEntity holder, ElysiumAttachmentType<T> type) {
        holder.setAttached(this.lookup(type), null);
    }

    @Override
    public <T> void removeData(LevelChunk holder, ElysiumAttachmentType<T> type) {
        holder.setAttached(this.lookup(type), null);
    }
}