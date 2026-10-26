package net.jadenxgamer.elysium_api.neoforge;

import net.jadenxgamer.elysium_api.ElysiumAPI;
import net.jadenxgamer.elysium_api.api.registry.ElysiumRegistry;
import net.jadenxgamer.elysium_api.impl.network.ElysiumNetwork21N;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(ElysiumAPI.MOD_ID)
public class ElysiumAPINeoForge {

    public ElysiumAPINeoForge(IEventBus eventBus) {
        ElysiumRegistry.setEventBus(ElysiumAPI.MOD_ID, eventBus);
        ElysiumNetwork21N.init(eventBus);
        ElysiumAPI.sharedSetup();
        ElysiumAPI.setup21();
    }
}