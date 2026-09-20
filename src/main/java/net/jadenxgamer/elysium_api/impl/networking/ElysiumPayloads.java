package net.jadenxgamer.elysium_api.impl.networking;

import net.jadenxgamer.elysium_api.ElysiumAPI;
import net.jadenxgamer.elysium_api.impl.networking.to_client.ScreenFlashPayload;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ElysiumPayloads {
    private static final String VERSION = "0.1.1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ElysiumAPI.elysiumPath("main"),
            () -> VERSION,
            VERSION::equals,
            VERSION::equals
    );

    public static void register() {
        int id = 0;
        CHANNEL.messageBuilder(ScreenFlashPayload.class, id++)
                .encoder(ScreenFlashPayload::encode)
                .decoder(ScreenFlashPayload::new)
                .consumerMainThread(ScreenFlashPayload::handleDataOnClient)
                .add();
    }
}