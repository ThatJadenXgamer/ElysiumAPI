package net.jadenxgamer.elysium_api.api.network.payload;

/**
 * The base interface for all custom network payloads.
 * <p>
 * A payload represents a single packet or message sent across the network.
 * To create a payload, implement this interface (records are highly recommended),
 * declare a static {@code ElysiumPayloadType} for it, and return that type in
 * {@link #type()}.
 * <p>
 * This interface is intentionally minimal. It exists solely to group payloads
 * together and provide the type identifier necessary for dispatching.
 */
public interface ElysiumPayload {

    /**
     * The unique type identifier for this payload.
     * <br>
     * This must return a constant instance; it is used to route incoming
     * payloads to the correct handler.
     *
     * @return the payload type
     */
    ElysiumPayloadType<? extends ElysiumPayload> type();
}