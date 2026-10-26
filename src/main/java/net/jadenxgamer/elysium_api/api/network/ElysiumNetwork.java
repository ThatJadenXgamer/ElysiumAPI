package net.jadenxgamer.elysium_api.api.network;

import net.jadenxgamer.elysium_api.api.network.payload.ElysiumPayload;
import net.jadenxgamer.elysium_api.api.network.pipeline.ElysiumNetworkBackend;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Consumer;

/**
 * The central facade for all networking operations in Elysium API.
 * <p>
 * This class provides a unified, cross-loader, and cross-version entry point
 * for sending payloads. Whether you are running on 1.20.1 or 1.21.1, and
 * regardless of whether the underlying mod loader is Fabric, Forge, or NeoForge,
 * the methods here behave identically. It handles the translation between legacy
 * packet channels and modern payload registries behind the scenes.
 * <p>
 * To register your payloads, you should call {@link #register(String, Consumer)}
 * from your mod's common initialization phase. Once registered, use the various
 * {@code sendTo*} methods to dispatch your data to the client or server.
 */
public final class ElysiumNetwork {

    private static ElysiumNetworkBackend backend;

    private ElysiumNetwork() {
    }

    /**
     * Registers a set of payloads for a specific mod ID.
     *
     * @param modId the mod ID registering these payloads
     * @param configurator the lambda that populates the registry
     */
    public static void register(String modId, Consumer<ElysiumNetworkRegistry> configurator) {
        ElysiumNetworkRegistry registry = new ElysiumNetworkRegistry();
        configurator.accept(registry);
        requireBackend().register(modId, registry);
    }

    /**
     * Sends a payload from the client to the server.
     *
     * @param payload the payload to send
     */
    public static void sendToServer(ElysiumPayload payload) {
        requireBackend().sendToServer(payload);
    }

    /**
     * Sends a payload from the server to a specific player.
     *
     * @param player the target player
     * @param payload the payload to send
     */
    public static void sendToPlayer(Player player, ElysiumPayload payload) {
        requireBackend().sendToPlayer(player, payload);
    }

    /**
     * Sends a payload from the server to all currently connected players.
     *
     * @param payload the payload to send
     */
    public static void sendToAllPlayers(ElysiumPayload payload) {
        requireBackend().sendToAllPlayers(payload);
    }

    /**
     * Sends a payload from the server to all players currently in a specific level.
     *
     * @param level the target level
     * @param payload the payload to send
     */
    public static void sendToAllPlayersInLevel(ServerLevel level, ElysiumPayload payload) {
        requireBackend().sendToAllPlayersInLevel(level, payload);
    }

    /**
     * Sends a payload to all players within a specific radius of a center point in a level.
     *
     * @param level the target level
     * @param center the center position
     * @param radius the maximum distance in blocks
     * @param payload the payload to send
     */
    public static void sendToPlayersNear(ServerLevel level, Vec3 center, double radius, ElysiumPayload payload) {
        requireBackend().sendToPlayersNear(level, center, radius, payload);
    }

    /**
     * Sends a payload to all players who are currently tracking a specific chunk.
     *
     * @param level the target level
     * @param chunkPos the target chunk
     * @param payload the payload to send
     */
    public static void sendToPlayersTrackingChunk(ServerLevel level, ChunkPos chunkPos, ElysiumPayload payload) {
        requireBackend().sendToPlayersTrackingChunk(level, chunkPos, payload);
    }

    /**
     * Sends a payload to all players who are currently tracking the chunk containing a block entity.
     *
     * @param blockEntity the target block entity
     * @param payload the payload to send
     */
    public static void sendToPlayersTrackingBlockEntity(BlockEntity blockEntity, ElysiumPayload payload) {
        requireBackend().sendToPlayersTrackingBlockEntity(blockEntity, payload);
    }

    /**
     * Sends a payload to all players who are currently tracking the given entity.
     *
     * @param entity the entity being tracked
     * @param payload the payload to send
     */
    public static void sendToPlayersTracking(Entity entity, ElysiumPayload payload) {
        requireBackend().sendToPlayersTracking(entity, payload);
    }

    /**
     * Sends a payload to all players tracking the given entity, including the entity itself
     * if it happens to be a player.
     *
     * @param entity the entity being tracked
     * @param payload the payload to send
     */
    public static void sendToPlayersTrackingAndSelf(Entity entity, ElysiumPayload payload) {
        requireBackend().sendToPlayersTrackingAndSelf(entity, payload);
    }

    /**
     * Sends a payload to all connected players except for one specific player.
     *
     * @param excluded the player to skip
     * @param payload the payload to send
     */
    public static void sendToAllPlayersExceptSelf(ServerPlayer excluded, ElysiumPayload payload) {
        requireBackend().sendToAllPlayersExceptSelf(excluded, payload);
    }

    @ApiStatus.Internal
    public static void installBackend(ElysiumNetworkBackend backend) {
        if (ElysiumNetwork.backend != null) throw new IllegalStateException("Elysium networking backend has already been installed; refusing to replace.");
        ElysiumNetwork.backend = backend;
    }

    private static ElysiumNetworkBackend requireBackend() {
        if (backend == null) throw new IllegalStateException("Elysium networking backend has not been installed. A loader adapter should have called ElysiumNetwork.installBackend during mod initialization, before any other mod invokes ElysiumNetwork.register or any ElysiumNetwork.send* method.");
        return backend;
    }
}