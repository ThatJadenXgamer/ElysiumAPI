package net.jadenxgamer.elysium_api.impl.networking;

import net.jadenxgamer.elysium_api.ElysiumAPI;
import net.jadenxgamer.elysium_api.impl.networking.to_client.ScreenFlashPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ElysiumPayloads {
    private static final String VERSION = "0.1.1";

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(ElysiumAPI.MOD_ID).versioned(VERSION);

        registrar.playToClient(ScreenFlashPayload.TYPE, ScreenFlashPayload.CODEC, ScreenFlashPayload::handleDataOnClient);
    }
}