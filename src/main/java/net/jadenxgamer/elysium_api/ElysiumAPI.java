// asphodel::merge -> loader=*, version=1.20.1 -> net.jadenxgamer.elysium_api.ElysiumAPI20
// asphodel::merge -> loader=*, version=1.21.1 -> net.jadenxgamer.elysium_api.ElysiumAPI21
package net.jadenxgamer.elysium_api;

import net.jadenxgamer.elysium_api.api.config.ElysiumConfigManager;
import net.jadenxgamer.elysium_api.api.unification.ElysiumResourceLocation;
import net.jadenxgamer.elysium_api.impl.client.resourcedriven.fog_settings.FogSettingsManager;
import net.jadenxgamer.elysium_api.impl.client.resourcedriven.lightmap_settings.LightmapSettingsManager;
import net.jadenxgamer.elysium_api.mod.config.ElysiumConfig;
import net.jadenxgamer.elysium_api.mod.network.ElysiumNetworking;
import net.jadenxgamer.elysium_api.mod.registry.ElysiumBlocks;
import net.jadenxgamer.elysium_api.mod.registry.ElysiumData;
import net.jadenxgamer.elysium_api.mod.registry.ElysiumItems;
import net.jadenxgamer.elysium_api.mod.registry.ElysiumRegistries;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElysiumAPI {
    // asphodel::patch -> loader=*, version=1.20.1, id=p20
    // asphodel::patch -> loader=*, version=1.21.1, id=p21

    public static final String MOD_ID = "elysium_api";
    public static final Logger LOGGER = LoggerFactory.getLogger("Elysium API");
    public static final FogSettingsManager FOG_SETTINGS = new FogSettingsManager();
    public static final LightmapSettingsManager LIGHTMAP_SETTINGS = new LightmapSettingsManager();

    public static void sharedSetup() {
        ElysiumConfigManager.register(MOD_ID, ElysiumConfig.class);

        ElysiumData.init();
        ElysiumRegistries.init();
        ElysiumBlocks.init();
        ElysiumItems.init();
        ElysiumNetworking.init();
    }

    public static void setup20() {}
    public static void setup21() {}

    public static ResourceLocation elysiumPath(String path) {
        return ElysiumResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static ResourceLocation vanillaPath(String path) {
        return ElysiumResourceLocation.withDefaultNamespace(path);
    }

    public static ResourceLocation idPath(String namespace, String path) {
        return ElysiumResourceLocation.fromNamespaceAndPath(namespace, path);
    }
}