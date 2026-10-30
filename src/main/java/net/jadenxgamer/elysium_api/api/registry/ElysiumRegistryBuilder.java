// asphodel::merge -> loader=forge, version=1.20.1 -> net.jadenxgamer.elysium_api.api.registry.ElysiumRegistryBuilderL20
// asphodel::merge -> loader=fabric, version=1.20.1 -> net.jadenxgamer.elysium_api.api.registry.ElysiumRegistryBuilderF20
// asphodel::merge -> loader=neoforge, version=1.21.1 -> net.jadenxgamer.elysium_api.api.registry.ElysiumRegistryBuilderN21
// asphodel::merge -> loader=fabric, version=1.21.1 -> net.jadenxgamer.elysium_api.api.registry.ElysiumRegistryBuilderF21
package net.jadenxgamer.elysium_api.api.registry;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

/**
 * A mod loader agnostic way to add your own custom registries.
 * <p>
 * Custom registries are useful whenever you want to register a type of object
 * that the base game does not already have a registry for. Vanilla, Forge,
 * NeoForge and Fabric each handle this a little differently, and the builder
 * takes care of those differences so the same setup code works everywhere.
 * Its surface mirrors the {@code RegistryBuilder} that Forge and NeoForge
 * provide, with a couple of small helpers so the same options are available on
 * every platform we support.
 * <p>
 * Typical use looks like this:
 * <pre>{@code
 * public static final ElysiumRegistry<MyThing> MY_THINGS =
 *      new ElysiumRegistryBuilder<MyThing>(MY_THING_KEY, MOD_ID)
 *          .maxId(255)
 *          .build();
 * }</pre>
 * The {@link #build()} call creates the registry and returns the
 * {@link ElysiumRegistry} you will use to declare entries. Call it from your
 * mod's loader specific entrypoint, before any of your other registry setup
 * runs. If you intend to register entries yourself, remember to also call
 * {@link ElysiumRegistry#initialize()} from the same place.
 */
public class ElysiumRegistryBuilder<T> {

    protected final ResourceKey<Registry<T>> registryKey;
    protected final String modId;
    protected ResourceLocation defaultKey;
    protected int maxId = -1;
    protected boolean sync = false;

    /**
     * Creates a new builder for a custom registry.
     *
     * @param registryKey the resource key of the registry to create
     * @param modId the mod ID that will own this registry
     */
    public ElysiumRegistryBuilder(ResourceKey<Registry<T>> registryKey, String modId) {
        this.registryKey = registryKey;
        this.modId = modId;
    }

    /**
     * Sets the default entry for this registry.
     * <br>
     * A registry that has a default entry will hand that entry back from a
     * lookup method when the requested entry is not present, rather than
     * returning null. This is the same concept as vanilla's defaulted
     * registries.
     *
     * @param key the resource location of the entry to use as the default
     * @return this builder, for chaining
     * @see #defaultKey(ResourceKey)
     */
    public ElysiumRegistryBuilder<T> defaultKey(ResourceLocation key) {
        this.defaultKey = key;
        return this;
    }

    /**
     * Sets the default entry for this registry using a resource key.
     * <br>
     * Identical to {@link #defaultKey(ResourceLocation)} but accepts the
     * typed key form for convenience.
     *
     * @param key the resource key of the entry to use as the default
     * @return this builder
     */
    public ElysiumRegistryBuilder<T> defaultKey(ResourceKey<T> key) {
        this.defaultKey = key.location();
        return this;
    }

    /**
     * Sets the highest numerical ID an entry in this registry is allowed to use.
     * <br>
     * Numerical IDs are used to reference entries compactly over the network,
     * so capping them is a good way to keep your registry within a size that
     * fits comfortably in a packet. Attempting to register an entry past this
     * limit will throw. The cap is enforced on every platform we support. If
     * you never call this method, no limit is applied.
     * <br>
     * The value must be at least zero. A max ID of 100 permits IDs from 0
     * through 100 inclusive, which means up to 101 entries.
     *
     * @param maxId the highest permitted numerical ID
     * @return this builder
     * @throws IllegalArgumentException if {@code maxId} is negative
     */
    public ElysiumRegistryBuilder<T> maxId(int maxId) {
        if (maxId < 0) throw new IllegalArgumentException("maxId must be greater than or equal to zero");
        this.maxId = maxId;
        return this;
    }

    /**
     * Sets whether this registry should be synchronized with connected clients.
     * <br>
     * The default is false. Turn this on if clients need to know about the
     * entries in this registry, which is common for anything referenced over
     * the network.
     *
     * @param sync whether the registry should be synced to clients
     * @return this builder
     */
    public ElysiumRegistryBuilder<T> sync(boolean sync) {
        this.sync = sync;
        return this;
    }

    /**
     * Creates the registry and returns a handle for it.
     * <br>
     * The returned {@link ElysiumRegistry} can then be used to declare
     * entries, or handed out to downstream mods so they can populate it
     * themselves. On Forge and NeoForge this hooks into the loader's custom
     * registry creation event, which means your mod event bus must already be
     * registered through {@link ElysiumRegistry#setEventBus(String, Object)}.
     * On Fabric the registry is created immediately.
     *
     * @return the registry handle
     * @throws IllegalStateException if the mod event bus has not been registered yet on Forge or NeoForge
     */
    public ElysiumRegistry<T> build() {
        this.createCustomRegistry();
        return new ElysiumRegistry<>(this.registryKey, this.modId);
    }

    protected void createCustomRegistry() {
        throw new UnsupportedOperationException("ElysiumRegistryBuilder.createCustomRegistry is not implemented on this platform");
    }
}