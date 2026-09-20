package net.jadenxgamer.elysium_api;

import net.jadenxgamer.elysium_api.impl.client.assetdriven.fog_settings.FogSettingsManager;
import net.jadenxgamer.elysium_api.impl.client.assetdriven.lightmap_settings.LightmapSettingsManager;
import net.jadenxgamer.elysium_api.impl.config.ElysiumConfigImpl;
import net.jadenxgamer.elysium_api.impl.registry.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(ElysiumAPI.MOD_ID)
public final class ElysiumAPI {
    public static final String MOD_ID = "elysium_api";
    public static final Logger LOGGER = LoggerFactory.getLogger("ElysiumAPI");
    public static final FogSettingsManager FOG_SETTINGS = new FogSettingsManager();
    public static final LightmapSettingsManager LIGHTMAP_SETTINGS = new LightmapSettingsManager();

    public ElysiumAPI(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        context.registerConfig(ModConfig.Type.COMMON, ElysiumConfigImpl.COMMON);

        ElysiumRegistries.init(modEventBus);
        ElysiumAttributes.init(modEventBus);
        ElysiumBlocks.init(modEventBus);
        ElysiumItems.init(modEventBus);
    }

    public static ResourceLocation elysiumPath(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static ResourceLocation idPath(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }
}