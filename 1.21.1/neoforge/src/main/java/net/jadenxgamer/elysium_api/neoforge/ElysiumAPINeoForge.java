package net.jadenxgamer.elysium_api.neoforge;

import net.jadenxgamer.elysium_api.ElysiumAPI;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(ElysiumAPI.MOD_ID)
public class ElysiumAPINeoForge {

    public ElysiumAPINeoForge(IEventBus eventBus) {
        ElysiumAPI.sharedSetup();
        ElysiumAPI.setup21();
    }
}