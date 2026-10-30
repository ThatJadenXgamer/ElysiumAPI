package net.jadenxgamer.elysium_api.api.registry;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.NewRegistryEvent;
import net.minecraftforge.registries.RegistryBuilder;

public class ElysiumRegistryBuilderL20<T> {

    protected final ResourceKey<Registry<T>> registryKey; // asphodel::exclude
    protected final String modId; // asphodel::exclude
    protected ResourceLocation defaultKey; // asphodel::exclude
    protected int maxId = -1; // asphodel::exclude
    protected boolean sync = false; // asphodel::exclude

    // asphodel::exclude
    public ElysiumRegistryBuilderL20(ResourceKey<Registry<T>> registryKey, String modId) {
        this.registryKey = registryKey;
        this.modId = modId;
    }

    protected void createCustomRegistry() {
        Object bus = ElysiumRegistry.getEventBus(this.modId);
        if (bus == null) throw new IllegalStateException("No event bus registered for mod id '" + this.modId + "'. Call ElysiumRegistry.setEventBus(\"" + this.modId + "\", bus) from the mod entry point before invoking build().");

        RegistryBuilder<T> forgeBuilder = new RegistryBuilder<>();
        forgeBuilder.setName(this.registryKey.location());
        if (this.defaultKey != null) forgeBuilder.setDefaultKey(this.defaultKey);
        if (this.maxId >= 0) forgeBuilder.setMaxID(this.maxId);
        if (!this.sync) forgeBuilder.disableSync();

        ((IEventBus) bus).addListener((NewRegistryEvent event) -> event.create(forgeBuilder));
    }
}