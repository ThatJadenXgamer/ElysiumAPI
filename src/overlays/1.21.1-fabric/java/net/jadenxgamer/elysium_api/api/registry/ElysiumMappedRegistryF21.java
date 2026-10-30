package net.jadenxgamer.elysium_api.api.registry;

import com.mojang.serialization.Lifecycle;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

public class ElysiumMappedRegistryF21<T> extends MappedRegistry<T> {

    private int elysium$maxId = -1;
    private int elysium$nextId = 0;

    public ElysiumMappedRegistryF21(ResourceKey<? extends Registry<T>> key, Lifecycle lifecycle, boolean intrusive) {
        super(key, lifecycle, intrusive);
    }

    public ElysiumMappedRegistryF21<T> elysium$setMaxId(int maxId) {
        this.elysium$maxId = maxId;
        return this;
    }

    @Override
    public Holder.Reference<T> register(ResourceKey<T> key, T value, RegistrationInfo info) {
        if (this.elysium$maxId >= 0 && this.elysium$nextId > this.elysium$maxId)
            throw new IllegalStateException("Registry " + this.key().location() + " exceeded maxId " + this.elysium$maxId + " when registering " + key.location());
        Holder.Reference<T> ref = super.register(key, value, info);
        this.elysium$nextId++;
        return ref;
    }
}