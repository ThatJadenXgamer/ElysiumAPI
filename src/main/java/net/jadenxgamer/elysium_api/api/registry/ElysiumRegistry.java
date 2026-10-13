// asphodel::merge -> loader=forge, version=1.20.1 -> net.jadenxgamer.elysium_api.api.registry.ElysiumRegistryL20
// asphodel::merge -> loader=fabric, version=1.20.1 -> net.jadenxgamer.elysium_api.api.registry.ElysiumRegistryF20
// asphodel::merge -> loader=neoforge, version=1.21.1 -> net.jadenxgamer.elysium_api.api.registry.ElysiumRegistryN21
// asphodel::merge -> loader=fabric, version=1.21.1 -> net.jadenxgamer.elysium_api.api.registry.ElysiumRegistryF21
package net.jadenxgamer.elysium_api.api.registry;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ElysiumRegistry<T> {

    private static final Map<String, Object> EVENT_BUSES = new HashMap<>();

    public static void setEventBus(String modId, Object bus) {
        EVENT_BUSES.put(modId, bus);
    }

    protected static Object getEventBus(String modId) {
        return EVENT_BUSES.get(modId);
    }

    protected final ResourceKey<Registry<T>> registryKey;
    protected final String modId;
    protected final Map<String, ElysiumRegHolder<?>> holders = new LinkedHashMap<>();

    public ElysiumRegistry(ResourceKey<Registry<T>> registryKey, String modId) {
        this.registryKey = registryKey;
        this.modId = modId;
    }

    public <E extends T> ElysiumRegHolder<E> register(String name, Supplier<E> supplier) {
        throw new UnsupportedOperationException("ElysiumRegistry.register is not implemented on this platform");
    }

    public void initialize() {
        throw new UnsupportedOperationException("ElysiumRegistry.initialize is not implemented on this platform");
    }

    public ResourceKey<Registry<T>> getRegistryKey() {
        return registryKey;
    }

    public String getModId() {
        return modId;
    }

    public ElysiumRegHolder<?> getHolder(String name) {
        return holders.get(name);
    }
}