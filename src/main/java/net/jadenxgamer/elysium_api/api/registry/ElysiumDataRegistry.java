// asphodel::merge -> loader=forge, version=1.20.1 -> net.jadenxgamer.elysium_api.api.registry.ElysiumDataRegistryL20
// asphodel::merge -> loader=fabric, version=1.20.1 -> net.jadenxgamer.elysium_api.api.registry.ElysiumDataRegistryF20
// asphodel::merge -> loader=neoforge, version=1.21.1 -> net.jadenxgamer.elysium_api.api.registry.ElysiumDataRegistryN21
// asphodel::merge -> loader=fabric, version=1.21.1 -> net.jadenxgamer.elysium_api.api.registry.ElysiumDataRegistryF21
package net.jadenxgamer.elysium_api.api.registry;

import com.mojang.serialization.Codec;
import net.jadenxgamer.elysium_api.ElysiumAPI;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A mod loader agnostic way to add your own data-driven registries.
 * <p>
 * A data-driven registry, sometimes called a datapack registry, is a registry
 * whose entries are loaded from JSON files inside datapacks rather than being
 * created in code. This is how the base game handles things like biomes,
 * dimensions, damage types and enchantments, and it is a great fit for any
 * features that server operators, modpack creators or frankly anyone looking to tweak or
 * add additional content onto your mod's infrastructure in an accessible way.
 * <p>
 * The way it works is exactly the same idea as the datapack registry support
 * that Forge and NeoForge provide. You hand over a {@code Codec} that knows how
 * to read and write your entries, and the game takes care of scanning datapacks
 * for matching JSON files and turning them into registry entries. The builder
 * hides the differences between each loader and version so the same setup code
 * works everywhere.
 * <p>
 * Typical use looks something like this:
 * <pre>{@code
 * public static final ResourceKey<Registry<MyThing>> MY_THING_KEY =
 *     ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(MOD_ID, "my_thing"));
 *
 * public static void datapackInit() {
 *     ElysiumDataRegistry.create(MY_THING_KEY, MyThing.CODEC);
 * }
 * }</pre>
 * Call {@code create} from your mod's loader specific entrypoint. On Forge and
 * NeoForge this requires the mod event bus to be registered through
 * {@link ElysiumRegistry#setEventBus(String, Object)} before that point, the
 * same requirement that custom registries have.
 * <p>
 * Entries will be loaded from {@code data/<pack_namespace>/<mod_id>/<path>/} for a
 * registry whose key is {@code <mod_id>:<path>}. If you add a network codec
 * through the three argument overload, the loaded entries will also be synced
 * to connected clients. If you do not, the registry still loads on both sides
 * from their local datapacks, but the server cannot send changes to clients.
 * Network sync of data-driven registries is available on every loader and
 * version we support, including Fabric 1.20.1 where the API backports the
 * capability that Forge and NeoForge expose through their loader events.
 *
 * @param <T> the type of object stored in the registry
 */
public class ElysiumDataRegistry<T> {

    private static final Set<ResourceLocation> REGISTERED_KEYS = ConcurrentHashMap.newKeySet();

    protected final ResourceKey<Registry<T>> registryKey;
    protected final String modId;
    protected final Codec<T> codec;
    @Nullable protected final Codec<T> networkCodec;

    protected ElysiumDataRegistry(ResourceKey<Registry<T>> registryKey, Codec<T> codec, @Nullable Codec<T> networkCodec) {
        this.registryKey = registryKey;
        this.modId = registryKey.location().getNamespace();
        this.codec = codec;
        this.networkCodec = networkCodec;
    }

    /**
     * Creates a new data-driven registry that is not synced to connected clients.
     * <p>
     * This is equivalent to calling
     * {@link #create(ResourceKey, Codec, Codec)} with a null network codec.
     * The registry will still be available on the client for single player and
     * for datapacks that ship alongside the mod, since both sides read from
     * their own local datapacks. What it will not do is push server side
     * changes to clients that connect over the network. Use this overload when
     * the entries are only ever accessed on the server, or when you are happy
     * with the mod's own bundled datapack being the source on both sides.
     *
     * @param key the resource key of the registry to create
     * @param codec the codec used to read entries from datapacks
     * @param <T> the type of object stored in the registry
     * @return the registry handle
     * @see #create(ResourceKey, Codec, Codec)
     */
    public static <T> ElysiumDataRegistry<T> create(ResourceKey<Registry<T>> key, Codec<T> codec) {
        return create(key, codec, null);
    }

    /**
     * Creates a new data-driven registry that is synced to connected clients.
     * <p>
     * If the network codec is null, the registry is loaded from datapacks on
     * both sides but not synced over the network. If the network codec is
     * provided, the server will also send loaded entries to clients that
     * connect, using that codec, and any client without the mod will fail to join.
     * <p>
     * The behaviour matches NeoForge's {@code dataPackRegistry} with the same
     * arguments on every loader and version we support.
     *
     * @param key the resource key of the registry to create
     * @param codec the codec used to read entries from datapacks
     * @param networkCodec the codec used to sync entries to clients, or null
     * @param <T> the type of object stored in the registry
     * @return the registry handle
     */
    public static <T> ElysiumDataRegistry<T> create(ResourceKey<Registry<T>> key, Codec<T> codec, @Nullable Codec<T> networkCodec) {
        ElysiumDataRegistry<T> registry = new ElysiumDataRegistry<>(key, codec, networkCodec);
        REGISTERED_KEYS.add(key.location());
        registry.registerToLoader();
        return registry;
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
     * Returns the mod id that owns this registry, derived from the namespace of its key.
     *
     * @return the mod id
     */
    public String getModId() {
        return modId;
    }

    /**
     * Returns the codec used to read entries from datapacks.
     *
     * @return the codec
     */
    public Codec<T> getCodec() {
        return codec;
    }

    /**
     * Returns the codec used to sync entries to clients, or null if this
     * registry is not synced.
     *
     * @return the network codec, or null
     */
    @Nullable
    public Codec<T> getNetworkCodec() {
        return networkCodec;
    }

    /**
     * Returns whether this registry is synced to connected clients.
     *
     * @return true if a network codec was provided
     */
    public boolean isSynced() {
        return networkCodec != null;
    }

    @ApiStatus.Internal
    public static boolean shouldNamespacePrefix(ResourceLocation location) {
        return REGISTERED_KEYS.contains(location);
    }

    @ApiStatus.Internal
    public static Codec<?> getNetworkCodecFor(ResourceKey<? extends Registry<?>> key) {
        throw new UnsupportedOperationException("ElysiumDataRegistry.getNetworkCodecFor is not implemented on this platform");
    }

    @ApiStatus.Internal
    public static boolean isNetworkable(ResourceKey<? extends Registry<?>> key) {
        throw new UnsupportedOperationException("ElysiumDataRegistry.isNetworkable is not implemented on this platform");
    }

    protected void registerToLoader() {
        throw new UnsupportedOperationException("ElysiumDataRegistry.registerToLoader is not implemented on this platform");
    }
}