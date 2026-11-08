package net.jadenxgamer.elysium_api.mod.network;

import net.jadenxgamer.elysium_api.ElysiumAPI;
import net.jadenxgamer.elysium_api.api.core.attachment.pipeline.ElysiumAttachmentSyncDispatcher;
import net.jadenxgamer.elysium_api.api.network.ElysiumNetwork;
import net.jadenxgamer.elysium_api.api.network.ElysiumNetworkRegistry;
import net.jadenxgamer.elysium_api.mod.network.payload.client.ConfigSyncPayload;
import net.jadenxgamer.elysium_api.mod.network.payload.client.ElysiumAttachmentSyncPayload;
import net.jadenxgamer.elysium_api.mod.network.payload.client.PingS2CPayload;
import net.jadenxgamer.elysium_api.mod.network.payload.server.PingC2SPayload;

public class ElysiumNetworking {

    public static void init() {
        ElysiumNetwork.register(ElysiumAPI.MOD_ID, ElysiumNetworking::registerPayloads);
    }

    public static void registerPayloads(ElysiumNetworkRegistry registry) {
        // TO CLIENT
        registry.registerS2C(PingS2CPayload.TYPE, PingS2CPayload::handleDataOnClient);
        registry.registerS2C(ConfigSyncPayload.TYPE, ConfigSyncPayload::handleDataOnClient);
        registry.registerS2C(ElysiumAttachmentSyncPayload.TYPE, ElysiumAttachmentSyncDispatcher::onClientReceive);

        // TO SERVER
        registry.registerC2S(PingC2SPayload.TYPE, PingC2SPayload::handleDataOnServer);

        // BIDIRECTIONAL
    }
}