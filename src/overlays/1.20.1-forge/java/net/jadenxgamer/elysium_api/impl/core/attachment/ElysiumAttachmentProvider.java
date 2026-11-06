package net.jadenxgamer.elysium_api.impl.core.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.jadenxgamer.elysium_api.api.core.attachment.ElysiumAttachmentType;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

@ApiStatus.Internal
public final class ElysiumAttachmentProvider implements ICapabilitySerializable<CompoundTag>, IElysiumAttachmentHolder {

    private final ElysiumAttachmentBackendL20 backend;
    private final Map<ResourceLocation, Object> attachments = new HashMap<>();

    public ElysiumAttachmentProvider(ElysiumAttachmentBackendL20 backend) {
        this.backend = backend;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ElysiumAttachmentBackendL20.CAPABILITY) return LazyOptional.of(() -> this).cast();
        return LazyOptional.empty();
    }

    @Override
    @SuppressWarnings("unchecked")
    public @Nullable <T> T getData(ElysiumAttachmentType<T> type) {
        return (T) this.attachments.get(type.id());
    }

    @Override
    public <T> void setData(ElysiumAttachmentType<T> type, @Nullable T value) {
        if (value == null) this.attachments.remove(type.id());
        else this.attachments.put(type.id(), value);
    }

    @Override
    public <T> boolean hasData(ElysiumAttachmentType<T> type) {
        return this.attachments.containsKey(type.id());
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        for (Map.Entry<ResourceLocation, Object> entry : this.attachments.entrySet()) {
            ElysiumAttachmentType<?> rawType = this.backend.findType(entry.getKey());
            if (rawType == null || !rawType.isPersistent()) continue;
            Codec<Object> codec = (Codec<Object>) rawType.codec();
            if (codec == null) continue;
            DataResult<Tag> result = codec.encodeStart(NbtOps.INSTANCE, entry.getValue());
            result.result().ifPresent(t -> tag.put(entry.getKey().toString(), t));
        }
        return tag;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void deserializeNBT(CompoundTag tag) {
        for (Map.Entry<ResourceLocation, ElysiumAttachmentType<?>> entry : this.backend.registeredTypes().entrySet()) {
            ElysiumAttachmentType<?> rawType = entry.getValue();
            if (!rawType.isPersistent()) continue;
            String key = entry.getKey().toString();
            if (!tag.contains(key)) continue;
            Codec<Object> codec = (Codec<Object>) rawType.codec();
            if (codec == null) continue;
            Tag raw = tag.get(key);
            DataResult<Object> result = codec.parse(NbtOps.INSTANCE, raw);
            result.result().ifPresent(value -> this.attachments.put(entry.getKey(), value));
        }
    }
}