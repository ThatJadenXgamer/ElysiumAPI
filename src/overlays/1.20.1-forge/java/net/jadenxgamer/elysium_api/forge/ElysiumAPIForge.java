package net.jadenxgamer.elysium_api.forge;

import net.jadenxgamer.elysium_api.ElysiumAPI;
import net.jadenxgamer.elysium_api.api.config.ElysiumConfigManager;
import net.jadenxgamer.elysium_api.api.registry.ElysiumRegistry;
import net.jadenxgamer.elysium_api.impl.network.ElysiumNetwork20L;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(ElysiumAPI.MOD_ID)
public class ElysiumAPIForge {

    public ElysiumAPIForge(FMLJavaModLoadingContext context) {
        ElysiumNetwork20L.init();
        ElysiumRegistry.setEventBus(ElysiumAPI.MOD_ID, context.getModEventBus());
        ElysiumAPI.sharedSetup();
        ElysiumAPI.setup20();

        MinecraftForge.EVENT_BUS.addListener((ServerStartedEvent event) -> ElysiumConfigManager.onServerStarting(event.getServer()));
        MinecraftForge.EVENT_BUS.addListener((ServerStoppingEvent event) -> ElysiumConfigManager.onServerStopping(event.getServer()));
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer sp) ElysiumConfigManager.syncToPlayer(sp);
        });
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> ElysiumConfigManager.onClientDisconnected());
    }
}