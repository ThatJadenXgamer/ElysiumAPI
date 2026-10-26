package net.jadenxgamer.elysium_api.api.network;

import net.jadenxgamer.elysium_api.api.network.payload.ElysiumPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * The context surrounding the receipt of a payload.
 * <p>
 * When a registered handler is invoked, it receives an instance of this interface.
 * It provides access to the player who sent the payload, the server instance (if
 * applicable), and utility methods for replying or offloading work to the main
 * game thread.
 * <p>
 * Contexts are direction-agnostic: a C2S handler will have a context pointing to
 * the sender, while an S2C handler will have a context pointing to the local player.
 */
public interface ElysiumNetworkContext {

    /**
     * The player associated with this payload.
     * <br>
     * <li>If handled on the server (C2S), this is the {@code ServerPlayer} who sent the payload.
     * <li>If handled on the client (S2C), this is the local player instance.
     * <br>
     * This is guaranteed to be non-null when the handler executes.
     *
     * @return the player
     */
    Player player();

    /**
     * The current MinecraftServer instance, or null if handled on the client side
     * in a multiplayer environment.
     * <br>
     * On a dedicated server or the integrated server, this returns the active server
     * instance. On the client, this is only non-null if you are playing in single-player.
     *
     * @return the server instance, or null
     */
    @Nullable
    MinecraftServer server();

    /**
     * The direction this payload was traveling.
     *
     * @return the network direction
     */
    ElysiumNetworkDirection direction();

    /**
     * Convenience check to see if this handler is running on the client side.
     *
     * @return true if the direction is {@link ElysiumNetworkDirection#PLAY_TO_CLIENT}
     */
    default boolean isClientSide() {
        return direction() == ElysiumNetworkDirection.PLAY_TO_CLIENT;
    }

    /**
     * Replies to the sender with a new payload.
     * <br>
     * This is a convenience shortcut. If called from a C2S handler, it sends the payload
     * back to the sender (S2C). If called from an S2C handler, it sends the payload back
     * to the server (C2S).
     * <br>
     * The payload provided here must be registered for the appropriate direction.
     *
     * @param payload the payload to send back
     */
    void reply(ElysiumPayload payload);

    /**
     * Schedules a task to run on the main thread of the current logical side.
     * <br>
     * While payload handlers are typically invoked on the main thread automatically, this
     * method is useful if you need to offload heavy work to a background thread and then
     * sync the results back to the game safely.
     *
     * @param task the task to run on the main thread
     */
    void enqueueWork(Runnable task);
}