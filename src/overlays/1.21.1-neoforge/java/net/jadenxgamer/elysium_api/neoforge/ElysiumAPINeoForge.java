package net.jadenxgamer.elysium_api.neoforge;

import net.jadenxgamer.elysium_api.ElysiumAPI;
import net.jadenxgamer.elysium_api.api.config.ElysiumConfigManager;
import net.jadenxgamer.elysium_api.api.registry.ElysiumRegistry;
import net.jadenxgamer.elysium_api.impl.core.attachment.ElysiumAttachmentBackendN21;
import net.jadenxgamer.elysium_api.impl.network.ElysiumNetwork21N;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

@Mod(ElysiumAPI.MOD_ID)
public class ElysiumAPINeoForge {

    public ElysiumAPINeoForge(IEventBus eventBus) {
        ElysiumRegistry.setEventBus(ElysiumAPI.MOD_ID, eventBus);

        ElysiumNetwork21N.installBackend(eventBus);
        ElysiumAttachmentBackendN21.installBackend(eventBus);

        ElysiumAPI.sharedSetup();
        ElysiumAPI.setup21();

        NeoForge.EVENT_BUS.addListener((ServerStartedEvent event) -> ElysiumConfigManager.onServerStarting(event.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent event) -> ElysiumConfigManager.onServerStopping(event.getServer()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer sp) ElysiumConfigManager.syncToPlayer(sp);
        });
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> ElysiumConfigManager.onClientDisconnected());
    }
}