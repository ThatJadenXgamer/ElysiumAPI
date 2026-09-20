package net.jadenxgamer.elysium_api.api.util;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;

public final class ModDetectionHelper {

    private ModDetectionHelper() {}

    public static boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    public static boolean isModLoadedEarly(String modId) {
        return FMLLoader.getLoadingModList().getModFileById(modId) != null;
    }
}