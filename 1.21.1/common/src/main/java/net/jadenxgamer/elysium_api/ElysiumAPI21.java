package net.jadenxgamer.elysium_api;

import net.minecraft.resources.ResourceLocation;

public class ElysiumAPI21 {

    public static void setup21() {

    }

    public static ResourceLocation elysiumPath(String path) {
        return ResourceLocation.fromNamespaceAndPath(ElysiumAPI.MOD_ID, path);
    }

    public static ResourceLocation idPath(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }
}