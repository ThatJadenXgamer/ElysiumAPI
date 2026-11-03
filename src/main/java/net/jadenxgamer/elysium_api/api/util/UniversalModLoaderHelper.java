package net.jadenxgamer.elysium_api.api.util;

import java.nio.file.Path;

public final class UniversalModLoaderHelper {

    // asphodel::patch -> loader=fabric, id=fabric
    // asphodel::patch -> loader=forge, id=forge
    // asphodel::patch -> loader=neoforge, id=neoforge

    private UniversalModLoaderHelper() {}

    /**
     * A common-side way to check if a given mod of the provided namespace is currently loaded that works across Fabric, Forge and NeoForge.
     * @param modId the namespace of a mod you're looking for.
     * @return passes if the namespace was found in the list of loaded mods on their respective loaders.
     */
    public static boolean isModLoaded(String modId) {
        // return net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded(modId); *fabric
        // return net.minecraftforge.fml.ModList.get().isLoaded(modId); *forge
        // return net.neoforged.fml.ModList.get().isLoaded(modId); *neoforge

        return false; // remove*fabric || forge || neoforge
    }

    /**
     * A common-side way to check if the game is currently running in a development environment that works across Fabric, Forge and NeoForge.
     * @return passes if the game was launched via a development pipeline rather than a production launcher.
     */
    public static boolean isDevelopmentEnvironment() {
        // return net.fabricmc.loader.api.FabricLoader.getInstance().isDevelopmentEnvironment(); *fabric
        // return !net.minecraftforge.fml.loading.FMLEnvironment.production; *forge
        // return !net.neoforged.fml.loading.FMLEnvironment.production; *neoforge

        return false; // remove*fabric || forge || neoforge
    }

    /**
     * A common-side way to check if the game is currently running in a production environment that works across Fabric, Forge and NeoForge.
     * @return passes if the game was launched via a production launcher rather than a development pipeline.
     */
    public static boolean isProductionEnvironment() {
        return !isDevelopmentEnvironment();
    }

    /**
     * A common-side way to obtain the game's config directory <code>(""&lt;instanceDir&gt;"/config/")</code> that works across Fabric, Forge and NeoForge.
     * @return the absolute path to the config directory, which the loader guarantees exists on disk.
     */
    public static Path getConfigDirectory() {
        // return net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir(); *fabric
        // return net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get(); *forge
        // return net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get(); *neoforge

        throw new UnsupportedOperationException("Elysium config directory requested without a mod loader present"); // remove*fabric || forge || neoforge
    }
}