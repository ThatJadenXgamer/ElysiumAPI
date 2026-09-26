package net.jadenxgamer.elysium_api.fabric;

import net.jadenxgamer.elysium_api.ElysiumAPI;
import net.fabricmc.api.ModInitializer;

public class ElysiumAPIFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        ElysiumAPI.sharedSetup();
        ElysiumAPI.setup21();
    }
}