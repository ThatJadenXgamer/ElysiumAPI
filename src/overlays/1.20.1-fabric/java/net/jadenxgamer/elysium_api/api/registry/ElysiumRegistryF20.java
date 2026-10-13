package net.jadenxgamer.elysium_api.api.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ElysiumRegistryF20<T> {

    // asphodel::exclude
    protected final ResourceKey<Registry<T>> registryKey;
    // asphodel::exclude
    protected final String modId;
    // asphodel::exclude
    protected final Map<String, ElysiumRegHolder<?>> holders = new LinkedHashMap<>();

    public ElysiumRegistryF20(ResourceKey<Registry<T>> registryKey, String modId) {
        this.registryKey = registryKey;
        this.modId = modId;
    }

    public <E extends T> ElysiumRegHolder<E> register(String name, Supplier<E> supplier) {
        Registry<T> registry = resolveRegistry(this.registryKey);
        ResourceLocation id = new ResourceLocation(this.modId, name);
        E value = supplier.get();
        Registry.register(registry, id, value);
        ElysiumRegHolder<E> holder = new ElysiumRegHolder<>(() -> value, id);
        this.holders.put(name, holder);
        return holder;
    }

    public static void initialize() {
    }

    @SuppressWarnings("unchecked")
    private static <T> Registry<T> resolveRegistry(ResourceKey<Registry<T>> key) {
        Registry<Registry<?>> rootRegistry = (Registry<Registry<?>>) BuiltInRegistries.REGISTRY;
        return (Registry<T>) rootRegistry.get(key.location());
    }
}