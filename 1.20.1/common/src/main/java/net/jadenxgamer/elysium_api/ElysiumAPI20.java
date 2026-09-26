package net.jadenxgamer.elysium_api;

import net.minecraft.resources.ResourceLocation;

public class ElysiumAPI20 {

    public static void setup20() {

    }
    public static ResourceLocation elysiumPath(String path) {
        return new ResourceLocation(ElysiumAPI.MOD_ID, path);
    }

    public static ResourceLocation idPath(String namespace, String path) {
        return new ResourceLocation(namespace, path);
    }
}