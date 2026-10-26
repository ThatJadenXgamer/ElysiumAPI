package net.jadenxgamer.elysium_api.api.network;

import net.jadenxgamer.elysium_api.api.network.payload.ElysiumPayload;
import net.jadenxgamer.elysium_api.api.network.payload.ElysiumPayloadType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A registry for declaring C2S and S2C payload handlers.
 * <p>
 * An instance of this class is provided to the configurator lambda in
 * {@link ElysiumNetwork#register(String, java.util.function.Consumer)}.
 * You should use its methods to map your payload types to their corresponding
 * handler functions.
 */
public final class ElysiumNetworkRegistry {

    private final Map<ResourceLocation, Registration<?>> clientToServer = new LinkedHashMap<>();
    private final Map<ResourceLocation, Registration<?>> serverToClient = new LinkedHashMap<>();

    /**
     * Registers a handler for a Client-to-Server payload.
     * <br>
     * The payload will be received by the server whenever a client sends it.
     *
     * @param type the payload type
     * @param handler the handler function to invoke when the payload arrives
     * @param <T> the payload class
     */
    public <T extends ElysiumPayload> void registerC2S(ElysiumPayloadType<T> type, Handler<T> handler) {
        Registration<?> previous = this.clientToServer.putIfAbsent(type.id(), new Registration<>(type, handler));
        if (previous != null) {
            throw new IllegalStateException("Duplicate C2S payload registration for id '" + type.id() + "'.");
        }
    }

    /**
     * Registers a handler for a Server-to-Client payload.
     * <br>
     * The payload will be received by the client whenever the server sends it
     * to them.
     *
     * @param type the payload type
     * @param handler the handler function to invoke when the payload arrives
     * @param <T> the payload class
     */
    public <T extends ElysiumPayload> void registerS2C(ElysiumPayloadType<T> type, Handler<T> handler) {
        Registration<?> previous = this.serverToClient.putIfAbsent(type.id(), new Registration<>(type, handler));
        if (previous != null) {
            throw new IllegalStateException("Duplicate S2C payload registration for id '" + type.id() + "'.");
        }
    }

    /**
     * Registers the same payload type for both directions with the same handler.
     * <br>
     * This is a convenience shortcut that combines {@link #registerC2S} and
     * {@link #registerS2C} with an identical handler. Use this when a payload
     * should be receivable on both sides and both sides should react the same way.
     * <br>
     * This mirrors NeoForge's {@code playBidirectional(type, codec, handler)}.
     *
     * @param type the payload type to register bidirectionally
     * @param handler the handler invoked on whichever side receives the payload
     * @param <T> the payload class
     */
    public <T extends ElysiumPayload> void registerBidirectional(ElysiumPayloadType<T> type, Handler<T> handler) {
        registerC2S(type, handler);
        registerS2C(type, handler);
    }

    /**
     * Registers the same payload type for both directions with two distinct handlers.
     * <br>
     * This is a convenience shortcut that combines {@link #registerC2S} and
     * {@link #registerS2C} with direction-specific handlers. Use this when a single
     * payload class is used in both directions but the two sides need to react
     * differently.
     * <br>
     * The {@code clientHandler} fires when the client receives the payload from
     * the server (S2C direction). The {@code serverHandler} fires when the server
     * receives the payload from a client (C2S direction).
     * <br>
     * This mirrors NeoForge's {@code playBidirectional(type, codec, clientHandler, serverHandler)}.
     *
     * @param type the payload type to register bidirectionally
     * @param clientHandler the handler that runs on the client (S2C reception)
     * @param serverHandler the handler that runs on the server (C2S reception)
     * @param <T> the payload class
     */
    public <T extends ElysiumPayload> void registerBidirectional(ElysiumPayloadType<T> type, Handler<T> clientHandler, Handler<T> serverHandler) {
        registerC2S(type, serverHandler);
        registerS2C(type, clientHandler);
    }

    @ApiStatus.Internal
    public Collection<Registration<?>> clientToServerRegistrations() {
        return this.clientToServer.values();
    }

    @ApiStatus.Internal
    public Collection<Registration<?>> serverToClientRegistrations() {
        return this.serverToClient.values();
    }

    /**
     * A functional interface representing the handler for a payload.
     *
     * @param <T> the payload class
     */
    @FunctionalInterface
    public interface Handler<T extends ElysiumPayload> {
        void receive(T payload, ElysiumNetworkContext context);
    }

    public record Registration<T extends ElysiumPayload>(ElysiumPayloadType<T> type, Handler<T> handler) {}
}