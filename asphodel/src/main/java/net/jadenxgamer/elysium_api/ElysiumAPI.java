// asphodel::merge -> loader=*, version=1.20.1 -> net.jadenxgamer.elysium_api.ElysiumAPI20
// asphodel::merge -> loader=*, version=1.21.1 -> net.jadenxgamer.elysium_api.ElysiumAPI21
package net.jadenxgamer.elysium_api;

import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElysiumAPI {
    public static final String MOD_ID = "elysium_api";
    public static final Logger LOGGER = LoggerFactory.getLogger("Elysium-API");

    public static void sharedSetup() {
        ModBlocks.init();
    }

    public static void setup20() {}
    public static void setup21() {}

    public static ResourceLocation elysiumPath(String path) {
        return new ResourceLocation(ElysiumAPI.MOD_ID, path);
    }

    public static ResourceLocation idPath(String namespace, String path) {
        return new ResourceLocation(namespace, path);
    }
}