package net.jadenxgamer.elysium_api.api.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.Optional;
import java.util.function.Supplier;

public final class RegistryAccessHelper {

    private RegistryAccessHelper() {}

    /**
     * Returns the server's {@link RegistryAccess} if a server is currently running.
     *
     * @return optional containing the server registry access, or empty if no server is available
     */
    public static Optional<RegistryAccess> getServer() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server == null ? Optional.empty() : Optional.of(server.registryAccess());
    }

    /**
     * Returns the client's {@link RegistryAccess} if the client is connected to a singleplayer or multiplayer world
     * <p>
     * On a dedicated servers this method always returns an empty optional.
     *
     * @return optional containing the client registry access, or empty if not on client or not connected
     */
    public static Optional<RegistryAccess> getClient() {
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return Optional.empty();
        }
        var connection = Minecraft.getInstance().getConnection();
        return connection == null ? Optional.empty() : Optional.of(connection.registryAccess());
    }

    /**
     * Returns the {@link RegistryAccess} from the given level, if the level is non‑null.
     *
     * @param level the level from which to obtain the registry access
     * @return optional containing the level's registry access, or empty if {@code level == null}
     */
    public static Optional<RegistryAccess> fromLevel(Level level) {
        return level == null ? Optional.empty() : Optional.of(level.registryAccess());
    }

    /**
     * Returns the best currently available {@link RegistryAccess}:
     * <ul>
     *     <li>On client: tries client first, then server (for integrated server)</li>
     *     <li>On dedicated server: tries server only</li>
     * </ul>
     *
     * @return optional containing the best registry access, or empty if none is available
     */
    public static Optional<RegistryAccess> getCurrent() {
        if (FMLEnvironment.dist == Dist.CLIENT) return getClient().or(RegistryAccessHelper::getServer);
        else return getServer();
    }

    //  Availability checks //

    /**
     * Checks whether any {@link RegistryAccess} is currently available.
     * <p>
     * On client: returns {@code true} if the client is connected to a world.
     * On dedicated server: returns {@code true} if a server is running.
     *
     * @return {@code true} if a registry access can be obtained, {@code false} otherwise
     */
    public static boolean isRegistryAccessAvailable() {
        if (FMLEnvironment.dist == Dist.CLIENT) return Minecraft.getInstance().getConnection() != null;
        else  return ServerLifecycleHooks.getCurrentServer() != null;
    }

    /**
     * Checks whether a server-side {@link RegistryAccess} is available.<p>
     *
     * @return {@code true} if a server is running
     */
    public static boolean isServerAvailable() {
        return ServerLifecycleHooks.getCurrentServer() != null;
    }

    /**
     * Checks whether a client-side {@link RegistryAccess} is available.<p>
     * Always returns {@code false} on a dedicated server.
     *
     * @return {@code true} if on client and connected to a world
     */
    public static boolean isClientAvailable() {
        return FMLEnvironment.dist == Dist.CLIENT && Minecraft.getInstance().getConnection() != null;
    }
}