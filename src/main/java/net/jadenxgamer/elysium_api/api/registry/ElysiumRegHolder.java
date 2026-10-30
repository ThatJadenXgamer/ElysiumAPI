package net.jadenxgamer.elysium_api.api.registry;

import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

/**
 * A reference to an entry registered through {@link ElysiumRegistry}.
 * <p>
 * Instances are returned from {@link ElysiumRegistry#register(String, Supplier)}.
 * The value is not available until registration has completed, so call
 * {@link #get()} once the game is up and running.
 * <p>
 * This class implements {@link Supplier}, which means a holder can be passed
 * anywhere a supplier of the same type is expected.
 *
 * @param <T> the type of object this holder points at
 */
public class ElysiumRegHolder<T> implements Supplier<T> {

    private final Supplier<T> supplier;
    private final ResourceLocation key;

    public ElysiumRegHolder(Supplier<T> supplier, ResourceLocation key) {
        this.supplier = supplier;
        this.key = key;
    }

    /**
     * Returns the object this holder points at.
     * <br>
     * This should only be called after registration has completed.
     *
     * @return the registered object
     */
    @Override
    public T get() {
        return supplier.get();
    }

    /**
     * Returns the resource location this holder was registered under.
     *
     * @return the resource location
     */
    public ResourceLocation getKey() {
        return key;
    }
}