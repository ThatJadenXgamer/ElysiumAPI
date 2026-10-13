package net.jadenxgamer.elysium_api.api.registry;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ElysiumRegistryN21<T> {

    // asphodel::exclude
    protected final ResourceKey<Registry<T>> registryKey;
    // asphodel::exclude
    protected final String modId;
    // asphodel::exclude
    protected final Map<String, ElysiumRegHolder<?>> holders = new LinkedHashMap<>();

    private final DeferredRegister<T> deferredRegister;

    public ElysiumRegistryN21(ResourceKey<Registry<T>> registryKey, String modId) {
        this.registryKey = registryKey;
        this.modId = modId;
        this.deferredRegister = DeferredRegister.create(registryKey, modId);
    }

    public <E extends T> ElysiumRegHolder<E> register(String name, Supplier<E> supplier) {
        var registered = this.deferredRegister.register(name, supplier);
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(this.modId, name);
        ElysiumRegHolder<E> holder = new ElysiumRegHolder<>(registered, id);
        this.holders.put(name, holder);
        return holder;
    }

    public void initialize() {
        Object bus = ElysiumRegistry.getEventBus(this.modId);
        if (bus == null) {
            throw new IllegalStateException("No event bus registered for mod id '" + this.modId + "'. Call ElysiumRegistry.setEventBus(\"" + this.modId + "\", bus)" +
                    " from the mod entry point before invoking initialize().");
        }
        this.deferredRegister.register((IEventBus) bus);
    }
}