package net.jadenxgamer.elysium_api.api.registry;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import org.jetbrains.annotations.Nullable;

public class ElysiumDataRegistryN21<T> {

    protected final ResourceKey<Registry<T>> registryKey; // asphodel::exclude
    protected final String modId; // asphodel::exclude
    protected final Codec<T> codec; // asphodel::exclude
    @Nullable protected final Codec<T> networkCodec; // asphodel::exclude

    // asphodel::exclude
    protected ElysiumDataRegistryN21(ResourceKey<Registry<T>> registryKey, Codec<T> codec, @Nullable Codec<T> networkCodec) {
        this.registryKey = registryKey;
        this.modId = registryKey.location().getNamespace();
        this.codec = codec;
        this.networkCodec = networkCodec;
    }

    protected void registerToLoader() {
        Object bus = ElysiumRegistry.getEventBus(this.modId);
        if (bus == null) {
            throw new IllegalStateException("No event bus registered for mod id '" + this.modId + "'. Call ElysiumRegistry.setEventBus(\"" + this.modId + "\", bus) from the mod entry point before invoking ElysiumDataRegistry.create().");
        }
        ((IEventBus) bus).addListener((DataPackRegistryEvent.NewRegistry event) -> event.dataPackRegistry(this.registryKey, this.codec, this.networkCodec));
    }
}