package net.jadenxgamer.elysium_api.fabric;

import net.jadenxgamer.elysium_api.ElysiumAPI;
import net.jadenxgamer.elysium_api.impl.network.ElysiumNetwork20F;
import net.fabricmc.api.ModInitializer;

public class ElysiumAPIFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        ElysiumNetwork20F.init();
        ElysiumAPI.sharedSetup();
        ElysiumAPI.setup20();
    }
}