package net.jadenxgamer.elysium_api.impl.network;

import net.jadenxgamer.elysium_api.ElysiumAPI;
import net.jadenxgamer.elysium_api.api.network.ElysiumNetwork;
import net.jadenxgamer.elysium_api.api.network.ElysiumNetworkRegistry;
import net.jadenxgamer.elysium_api.impl.network.payload.PingC2SPayload;
import net.jadenxgamer.elysium_api.impl.network.payload.PingS2CPayload;
import net.minecraft.world.entity.player.Player;

public class ElysiumNetworking {

    public static void init() {
        ElysiumNetwork.register(ElysiumAPI.MOD_ID, ElysiumNetworking::registerPayloads);
    }

    public static void registerPayloads(ElysiumNetworkRegistry registry) {
        registry.registerS2C(PingS2CPayload.TYPE, PingS2CPayload::handleDataOnClient);
        registry.registerC2S(PingC2SPayload.TYPE, PingC2SPayload::handleDataOnServer);
    }

    public static void ping(String message) {
        ElysiumNetwork.sendToServer(new PingC2SPayload(message));
    }

    public static void pingTo(Player player, String message) {
        ElysiumNetwork.sendToPlayer(player, new PingS2CPayload(message));
    }
}