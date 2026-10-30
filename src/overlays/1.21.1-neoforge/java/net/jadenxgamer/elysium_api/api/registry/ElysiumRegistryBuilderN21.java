package net.jadenxgamer.elysium_api.api.registry;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.NewRegistryEvent;

public class ElysiumRegistryBuilderN21<T> {

    protected final ResourceKey<Registry<T>> registryKey; // asphodel::exclude
    protected final String modId; // asphodel::exclude
    protected ResourceLocation defaultKey; // asphodel::exclude
    protected int maxId = -1; // asphodel::exclude
    protected boolean sync = false; // asphodel::exclude

    // asphodel::exclude
    public ElysiumRegistryBuilderN21(ResourceKey<Registry<T>> registryKey, String modId) {
        this.registryKey = registryKey;
        this.modId = modId;
    }

    protected void createCustomRegistry() {
        Object bus = ElysiumRegistry.getEventBus(this.modId);
        if (bus == null) throw new IllegalStateException("No event bus registered for mod id '" + this.modId + "'. Call ElysiumRegistry.setEventBus(\"" + this.modId + "\", bus) from the mod entry point before invoking build().");

        net.neoforged.neoforge.registries.RegistryBuilder<T> neoBuilder = new net.neoforged.neoforge.registries.RegistryBuilder<>(this.registryKey);
        if (this.defaultKey != null) neoBuilder.defaultKey(this.defaultKey);
        if (this.maxId >= 0) neoBuilder.maxId(this.maxId);
        neoBuilder.sync(this.sync);

        ((IEventBus) bus).addListener((NewRegistryEvent event) -> event.create(neoBuilder));
    }
}