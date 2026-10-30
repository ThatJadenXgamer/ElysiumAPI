package net.jadenxgamer.elysium_api.api.registry;

import com.mojang.serialization.Lifecycle;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.fabricmc.fabric.api.event.registry.RegistryAttribute;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

public class ElysiumRegistryBuilderF20<T> {

    protected final ResourceKey<Registry<T>> registryKey; // asphodel::exclude
    protected final String modId; // asphodel::exclude
    protected ResourceLocation defaultKey; // asphodel::exclude
    protected int maxId = -1; // asphodel::exclude
    protected boolean sync = false; // asphodel::exclude

    // asphodel::exclude
    public ElysiumRegistryBuilderF20(ResourceKey<Registry<T>> registryKey, String modId) {
        this.registryKey = registryKey;
        this.modId = modId;
    }

    protected void createCustomRegistry() {
        FabricRegistryBuilder<T, ? extends WritableRegistry<T>> builder;
        if (this.defaultKey != null && this.maxId >= 0) {
            ElysiumDefaultedMappedRegistryF20<T> reg = new ElysiumDefaultedMappedRegistryF20<>(this.defaultKey.toString(), this.registryKey, Lifecycle.stable(), false);
            reg.elysium$setMaxId(this.maxId);
            builder = FabricRegistryBuilder.from(reg);
        } else if (this.maxId >= 0) {
            ElysiumMappedRegistryF20<T> reg = new ElysiumMappedRegistryF20<>(this.registryKey, Lifecycle.stable(), false);
            reg.elysium$setMaxId(this.maxId);
            builder = FabricRegistryBuilder.from(reg);
        } else if (this.defaultKey != null) {
            builder = FabricRegistryBuilder.createDefaulted(this.registryKey, this.defaultKey);
        } else {
            builder = FabricRegistryBuilder.createSimple(this.registryKey);
        }

        if (this.sync) builder.attribute(RegistryAttribute.SYNCED);

        builder.buildAndRegister();
    }
}