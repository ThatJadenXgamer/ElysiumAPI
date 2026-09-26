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

    }

    public static void setup20() {} // stub
    public static void setup21() {} // stub
    public static ResourceLocation elysiumPath(String path) { return null; } // stub
    public static ResourceLocation idPath(String namespace, String path) { return null; } // stub
}