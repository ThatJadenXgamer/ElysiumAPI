package net.jadenxgamer.elysium_api.api.network;

/**
 * Represents the direction a payload is traveling across the network.
 * <p>
 * This is a version-agnostic and loader-agnostic mirror of the various
 * network direction enums provided by vanilla and the mod loaders.
 */
public enum ElysiumNetworkDirection {

    /**
     * The payload is traveling from the client to the server.
     */
    PLAY_TO_SERVER,

    /**
     * The payload is traveling from the server to the client.
     */
    PLAY_TO_CLIENT
}