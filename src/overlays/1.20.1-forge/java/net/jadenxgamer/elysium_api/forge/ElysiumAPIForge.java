package net.jadenxgamer.elysium_api.forge;

import net.jadenxgamer.elysium_api.ElysiumAPI;
import net.jadenxgamer.elysium_api.api.registry.ElysiumRegistry;
import net.jadenxgamer.elysium_api.impl.network.ElysiumNetwork20L;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(ElysiumAPI.MOD_ID)
public class ElysiumAPIForge {

    public ElysiumAPIForge(FMLJavaModLoadingContext context) {
        ElysiumNetwork20L.init();
        ElysiumRegistry.setEventBus(ElysiumAPI.MOD_ID, context.getModEventBus());
        ElysiumAPI.sharedSetup();
        ElysiumAPI.setup20();
    }
}