package net.jadenxgamer.elysium_api.fabric;

import net.fabricmc.api.ModInitializer;
import net.jadenxgamer.elysium_api.ElysiumAPI;
import net.jadenxgamer.elysium_api.impl.network.ElysiumNetwork21F;

public class ElysiumAPIFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        ElysiumNetwork21F.init();
        ElysiumAPI.sharedSetup();
        ElysiumAPI.setup21();
    }
}