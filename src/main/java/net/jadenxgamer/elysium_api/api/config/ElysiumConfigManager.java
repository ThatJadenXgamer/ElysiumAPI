package net.jadenxgamer.elysium_api.api.config;

import net.jadenxgamer.elysium_api.impl.config.ConfigRegistry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.ApiStatus;

import java.util.Map;

/**
 * The central manager for all config operations in Elysium API.
 * <p>
 * This class provides utility for creating configs, reading entries, writing to disk or reloading saved configs.
 * <p>
 * To declare config values, create a class of {@code public static final} fields, each of
 * type {@link ElysiumConfigEntry}, and pass that class to {@link #register(String, Class)}
 * during your mod's common initialization phase. Once registered, use
 * {@link ElysiumConfigEntry#get()} at call sites, and the various lifecycle methods here to
 * reload, save, or sync entries.
 * <p>
 * The public {@code reload} and {@code save} methods form the primary user-facing surface.
 * The {@code on*}, {@code sync*}, and {@code applyServerSync} methods are invoked by the
 * loader entry points that Elysium API wires up on each platform, and are not intended for
 * direct use by downstream mods.
 */
public final class ElysiumConfigManager {

    private ElysiumConfigManager() {}

    /**
     * Registers a config class and loads its values from disk.
     * <p>
     * The class is scanned reflectively for public static fields of type
     * {@link ElysiumConfigEntry}. Entries are validated by name, category and uniqueness, then
     * grouped by their declared {@link ConfigLoadType}, and each group is loaded from its
     * corresponding file under {@code <gameDir>/config/<modId>/<loadType>.css}. If the file
     * does not exist yet, it is created with the default values for that group.
     * <p>
     * Registration is one-shot per mod id; registering the same mod twice will throw with a
     * clear error. Call this method exactly ONCE from your common initialization phase.
     *
     * @param modId the mod id that owns these entries, used as the directory name on disk
     * @param configClass the class whose static {@link ElysiumConfigEntry} fields should be registered
     */
    public static void register(String modId, Class<?> configClass) {
        ConfigRegistry.register(modId, configClass);
    }

    /**
     * Reloads every config file for the given mod from disk.
     * <p>
     * This re-reads all load types (COMMON, CLIENT, SERVER, and STARTUP) in a single pass.
     * The active file for each spec is used, which means a per-world server override is
     * reloaded from the override file while it is active, and the global file is reloaded
     * otherwise.
     *
     * @param modId the mod id whose entries should be reloaded
     */
    public static void reload(String modId) {
        ConfigRegistry.reload(modId);
    }

    /**
     * Reloads a single load type for the given mod from disk.
     * <p>
     * Useful when a caller only cares about one config section.
     *
     * @param modId the mod id whose entries should be reloaded
     * @param type the specific {@link ConfigLoadType} to reload
     */
    public static void reload(String modId, ConfigLoadType type) {
        ConfigRegistry.reload(modId, type);
    }

    /**
     * Writes every config file for the given mod back to disk using the current in-memory values.
     * <p>
     * Save targets are resolved per-spec via the active file. If a mod's SERVER config is
     * currently synced from a remote server, the save call for that spec is refused with a
     * warning rather than clobbering the local file, since the in-memory values did not
     * originate from disk.
     *
     * @param modId the mod id whose entries should be saved
     */
    public static void save(String modId) {
        ConfigRegistry.save(modId);
    }

    /**
     * Writes a single load type for the given mod back to disk using the current in-memory values.
     *
     * @param modId the mod id whose entries should be saved
     * @param type the specific {@link ConfigLoadType} to save
     */
    public static void save(String modId, ConfigLoadType type) {
        ConfigRegistry.save(modId, type);
    }

    @ApiStatus.Internal
    public static void onServerStarting(MinecraftServer server) {
        ConfigRegistry.onServerStarting(server);
    }

    @ApiStatus.Internal
    public static void onServerStopping(MinecraftServer server) {
        ConfigRegistry.onServerStopping(server);
    }

    @ApiStatus.Internal
    public static void onClientDisconnected() {
        ConfigRegistry.onClientDisconnected();
    }

    /**
     * Pushes the current SERVER config values of every registered mod to a single player.
     * <p>
     * Intended to be called from a player-join event on the loader side. For each mod that
     * has registered SERVER entries, one {@code ConfigSyncPayload} is sent containing the
     * mod id and a map of {@code "category.name"} to string-encoded values. Entries the
     * receiving client does not have are silently ignored on the client side.
     *
     * @param player the player who just joined and should receive the sync
     */
    public static void syncToPlayer(ServerPlayer player) {
        ConfigRegistry.syncToPlayer(player);
    }

    /**
     * Re-broadcasts the current SERVER config values of a single mod to every connected player.
     * <p>
     * Call this after programmatically changing a SERVER entry so that the change propagates
     * to everyone who is already connected. Server-authoritative changes made at runtime
     * should always be followed by this call, otherwise the new value will only be seen by
     * players who join afterward.
     *
     * @param modId the mod id whose current SERVER values should be re-sent
     */
    public static void broadcastSync(String modId) {
        ConfigRegistry.broadcastSync(modId);
    }

    @ApiStatus.Internal
    public static void applyServerSync(String modId, Map<String, String> values, boolean pureClient) {
        ConfigRegistry.applyServerSync(modId, values, pureClient);
    }
}