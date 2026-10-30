package net.jadenxgamer.elysium_api.api.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;

import java.util.Optional;
import org.jetbrains.annotations.ApiStatus;

/**
 * Cross loader and cross version helper for obtaining a {@link RegistryAccess}.
 * <p>
 * No setup call is required from the mod entry point on any platform. The
 * class is ready to use as soon as the mod is loaded.
 */
public final class RegistryAccessHelper {

    // asphodel::patch -> loader=fabric, id=fabric
    // asphodel::patch -> loader=forge, id=forge
    // asphodel::patch -> loader=neoforge, id=neoforge

    @ApiStatus.Internal private static volatile MinecraftServer elysium$currentServer;
    @ApiStatus.Internal public static void elysium$setCurrentServer(MinecraftServer server) { elysium$currentServer = server; }

    private RegistryAccessHelper() {}

    /**
     * Returns the server's {@link RegistryAccess} if a server is currently running.
     *
     * @return optional containing the server registry access, or empty if no server is available
     */
    public static Optional<RegistryAccess> getServer() {
        MinecraftServer server = null; // remove*fabric && forge && neoforge
        // MinecraftServer server = elysium$currentServer; *fabric
        // MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer(); *forge
        // MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer(); *neoforge
        return server == null ? Optional.empty() : Optional.of(server.registryAccess());
    }

    /**
     * Returns the client's {@link RegistryAccess} if the client is connected to a singleplayer or multiplayer world.
     * <p>
     * On a dedicated server this method always returns an empty optional.
     *
     * @return optional containing the client registry access, or empty if not on client or not connected
     */
    public static Optional<RegistryAccess> getClient() {
        boolean dedicatedServer = false; // remove*fabric && forge && neoforge
        // boolean dedicatedServer = net.fabricmc.loader.api.FabricLoader.getInstance().getEnvironmentType() == net.fabricmc.api.EnvType.SERVER; *fabric
        // boolean dedicatedServer = net.minecraftforge.fml.loading.FMLEnvironment.dist == net.minecraftforge.api.distmarker.Dist.DEDICATED_SERVER; *forge
        // boolean dedicatedServer = net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.DEDICATED_SERVER; *neoforge
        if (dedicatedServer) return Optional.empty();
        var connection = Minecraft.getInstance().getConnection();
        return connection == null ? Optional.empty() : Optional.of(connection.registryAccess());
    }

    /**
     * Returns the {@link RegistryAccess} from the given level, if the level is non-null.
     *
     * @param level the level from which to obtain the registry access
     * @return optional containing the level's registry access, or empty if {@code level == null}
     */
    public static Optional<RegistryAccess> fromLevel(Level level) {
        return level == null ? Optional.empty() : Optional.of(level.registryAccess());
    }

    /**
     * Returns the best currently available {@link RegistryAccess}: the client
     * one when present, otherwise the server one.
     *
     * @return optional containing the best registry access, or empty if none is available
     */
    public static Optional<RegistryAccess> getCurrent() {
        return getClient().or(RegistryAccessHelper::getServer);
    }

    /**
     * Checks whether any {@link RegistryAccess} is currently available.
     *
     * @return {@code true} if a registry access can be obtained, {@code false} otherwise
     */
    public static boolean isRegistryAccessAvailable() {
        return isClientAvailable() || isServerAvailable();
    }

    /**
     * Checks whether a server-side {@link RegistryAccess} is available.
     *
     * @return {@code true} if a server is running
     */
    public static boolean isServerAvailable() {
        return getServer().isPresent();
    }

    /**
     * Checks whether a client-side {@link RegistryAccess} is available.
     * <p>
     * Always returns {@code false} on a dedicated server.
     *
     * @return {@code true} if on client and connected to a world
     */
    public static boolean isClientAvailable() {
        return getClient().isPresent();
    }
}