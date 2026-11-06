package net.jadenxgamer.elysium_api.fabric;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.jadenxgamer.elysium_api.ElysiumAPI;
import net.jadenxgamer.elysium_api.api.config.ElysiumConfigManager;
import net.jadenxgamer.elysium_api.impl.core.attachment.ElysiumAttachmentBackendF20;
import net.jadenxgamer.elysium_api.impl.network.ElysiumNetwork20F;
import net.fabricmc.api.ModInitializer;

public class ElysiumAPIFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        ElysiumNetwork20F.installBackend();
        ElysiumAttachmentBackendF20.installBackend();

        ElysiumAPI.sharedSetup();
        ElysiumAPI.setup20();

        ServerLifecycleEvents.SERVER_STARTED.register(ElysiumConfigManager::onServerStarting);
        ServerLifecycleEvents.SERVER_STOPPING.register(ElysiumConfigManager::onServerStopping);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ElysiumConfigManager.onClientDisconnected());

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                ElysiumConfigManager.syncToPlayer(handler.getPlayer())
        );
    }
}