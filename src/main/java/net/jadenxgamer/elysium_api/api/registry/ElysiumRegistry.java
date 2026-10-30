// asphodel::merge -> loader=forge, version=1.20.1 -> net.jadenxgamer.elysium_api.api.registry.ElysiumRegistryL20
// asphodel::merge -> loader=fabric, version=1.20.1 -> net.jadenxgamer.elysium_api.api.registry.ElysiumRegistryF20
// asphodel::merge -> loader=neoforge, version=1.21.1 -> net.jadenxgamer.elysium_api.api.registry.ElysiumRegistryN21
// asphodel::merge -> loader=fabric, version=1.21.1 -> net.jadenxgamer.elysium_api.api.registry.ElysiumRegistryF21
package net.jadenxgamer.elysium_api.api.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A mod loader agnostic deferred registry system.
 * <p>
 * A deferred registry is a list of entries that are declared early but only
 * created later, once the game has reached the right point in its startup
 * sequence. Instead of constructing your objects immediately, you hand over a
 * supplier and the registry calls it at the appropriate moment. This is the
 * same idea as the {@code DeferredRegister} system that Forge and NeoForge
 * provide, and ours is built on top of that concept so the same code works on
 * every loader and version we support.
 * <p>
 * To use it, you first obtain an instance of this class, wire it up to your
 * loader entrypoint by calling {@link #initialize()}, then declare entries
 * with {@link #register(String, Supplier)}. Each call returns an
 * {@link ElysiumRegHolder} you can store in a field for later access.
 */
public class ElysiumRegistry<T> {

    private static final Map<String, Object> EVENT_BUSES = new HashMap<>();

    /**
     * Registers a mod event bus for the given mod id.
     * <br>
     * You must call this exactly once from your Forge and NeoForge specific
     * entrypoint, before any registry work happens, because registry work is
     * driven by events fired on the mod bus for these two platforms.
     * <br>
     * On Fabric this call does nothing, so can entirely skip calling this on its entrypoint
     * <br>
     *
     * @param modId the mod id whose bus is being registered
     * @param bus the mod event bus instance
     */
    public static void setEventBus(String modId, Object bus) {
        EVENT_BUSES.put(modId, bus);
    }

    protected static Object getEventBus(String modId) {
        return EVENT_BUSES.get(modId);
    }

    protected final ResourceKey<Registry<T>> registryKey;
    protected final String modId;
    protected final Map<String, ElysiumRegHolder<?>> holders = new LinkedHashMap<>();

    /**
     * Creates a new registry handle for the given registry key.
     *
     * @param registryKey the resource key of the underlying registry
     * @param modId the mod id that entries registered here will use as their namespace
     */
    public ElysiumRegistry(ResourceKey<Registry<T>> registryKey, String modId) {
        this.registryKey = registryKey;
        this.modId = modId;
    }

    /**
     * Declares a new entry to be registered under this registry.
     * <br>
     * The supplier is not invoked right away. It will be called once the game
     * has reached the correct point in its lifecycle, at which point the
     * resulting object is pushed into the registry. Do not cache the object
     * yourself inside the supplier, since it is expected to produce a new
     * instance every time it is called.
     * <br>
     * The name you pass here is automatically combined with this registry's
     * mod id to form the entry's full resource location. If you need the
     * holder back later, the same instance is retrievable through
     * {@link #getHolder(String)}.
     *
     * @param name the path portion of the entry's resource location
     * @param supplier a factory for the entry, invoked exactly once
     * @param <E> the specific type of the entry
     * @return a holder that will point at the registered object once it exists
     */
    public <E extends T> ElysiumRegHolder<E> register(String name, Supplier<E> supplier) {
        throw new UnsupportedOperationException("ElysiumRegistry.register is not implemented on this platform");
    }

    /**
     * Wires this registry into your mod loader's registration pipeline.
     * <br>
     * This must be called from your mod's loader specific entrypoint, before
     * the game begins its registration pass. On Forge and NeoForge it attaches
     * to the mod event bus you registered through
     * {@link #setEventBus(String, Object)}. On Fabric, it sets up the
     * registration hooks directly.
     * <br>
     * You only need to call this if you intend to declare entries yourself
     * through {@link #register(String, Supplier)}. If you are exposing an
     * empty registry for downstream mods to populate, you can skip this
     * entirely.
     */
    public void initialize() {
        throw new UnsupportedOperationException("ElysiumRegistry.initialize is not implemented on this platform");
    }

    /**
     * Returns the resource key of the registry this instance is managing.
     *
     * @return the registry key
     */
    public ResourceKey<Registry<T>> getRegistryKey() {
        return registryKey;
    }

    /**
     * Returns the mod id that entries registered through this instance use as
     * their namespace.
     *
     * @return the mod id
     */
    public String getModId() {
        return modId;
    }

    /**
     * Returns the holder for an entry that was registered through this
     * instance, or null if no entry with that name exists.
     *
     * @param name the path portion of the entry's resource location
     * @return the holder, or null
     */
    public ElysiumRegHolder<?> getHolder(String name) {
        return holders.get(name);
    }

    /**
     * Returns the underlying registry once it has been created by the loader.
     * <br>
     * On Fabric the registry is available as soon as it has been built. On
     * Forge and NeoForge it only becomes available after the loader has had a
     * chance to create custom registries, which happens during mod
     * construction. If you call this before that point, or if the registry has
     * not been created at all, it returns null.
     * <br>
     * This is primarily useful when you want to expose a registry to
     * downstream mods so they can look up or iterate its entries directly.
     *
     * @return the registry, or null if it is not available yet
     */
    @SuppressWarnings("unchecked")
    public Registry<T> getRegistry() {
        return (Registry<T>) BuiltInRegistries.REGISTRY.get(this.registryKey.location());
    }
}