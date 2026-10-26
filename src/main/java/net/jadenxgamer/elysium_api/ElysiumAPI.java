// asphodel::merge -> loader=*, version=1.20.1 -> net.jadenxgamer.elysium_api.ElysiumAPI20
// asphodel::merge -> loader=*, version=1.21.1 -> net.jadenxgamer.elysium_api.ElysiumAPI21
package net.jadenxgamer.elysium_api;

import net.jadenxgamer.elysium_api.impl.network.ElysiumNetworking;
import net.jadenxgamer.elysium_api.impl.registry.ElysiumBlocks;
import net.jadenxgamer.elysium_api.impl.registry.ElysiumItems;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElysiumAPI {
    // asphodel::patch -> loader=*, version=1.20.1, id=p20
    // asphodel::patch -> loader=*, version=1.21.1, id=p21

    public static final String MOD_ID = "elysium_api";
    public static final Logger LOGGER = LoggerFactory.getLogger("Elysium-API");

    public static void sharedSetup() {
        ElysiumBlocks.init();
        ElysiumItems.init();
        ElysiumNetworking.init();
    }

    public static void setup20() {}
    public static void setup21() {}

    public static ResourceLocation elysiumPath(String path) {
        // return ResourceLocation.fromNamespaceAndPath(ElysiumAPI.MOD_ID, path); *p21
        return new ResourceLocation(ElysiumAPI.MOD_ID, path); // remove*p21
    }

    public static ResourceLocation idPath(String namespace, String path) {
        // return ResourceLocation.fromNamespaceAndPath(namespace, path); *p21
        return new ResourceLocation(namespace, path); // remove*p21
    }
}