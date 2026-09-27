package net.jadenxgamer.elysium_api.forge;

import net.jadenxgamer.elysium_api.ElysiumAPI;
import net.jadenxgamer.elysium_api.api.registry.ElysiumRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(ElysiumAPI.MOD_ID)
public class ElysiumAPIForge {

    public ElysiumAPIForge() {
        ElysiumRegistry.setEventBus(ElysiumAPI.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
        ElysiumAPI.sharedSetup();
        ElysiumAPI.setup20();
    }
}