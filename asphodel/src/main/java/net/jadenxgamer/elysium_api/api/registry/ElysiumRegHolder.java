package net.jadenxgamer.elysium_api.api.registry;

import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

public class ElysiumRegHolder<T> implements Supplier<T> {

    private final Supplier<T> supplier;
    private final ResourceLocation key;

    public ElysiumRegHolder(Supplier<T> supplier, ResourceLocation key) {
        this.supplier = supplier;
        this.key = key;
    }

    @Override
    public T get() {
        return supplier.get();
    }

    public ResourceLocation getKey() {
        return key;
    }
}