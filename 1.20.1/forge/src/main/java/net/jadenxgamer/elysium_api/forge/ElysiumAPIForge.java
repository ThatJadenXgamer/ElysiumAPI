package net.jadenxgamer.elysium_api.forge;

import net.jadenxgamer.elysium_api.ElysiumAPI;
import net.minecraftforge.fml.common.Mod;

@Mod(ElysiumAPI.MOD_ID)
public class ElysiumAPIForge {

    public ElysiumAPIForge() {
        ElysiumAPI.sharedSetup();
        ElysiumAPI.setup20();
    }
}