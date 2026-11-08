package net.jadenxgamer.elysium_api.api.unification;

import net.minecraft.resources.ResourceLocation;

public class ElysiumResourceLocation {

    // asphodel::patch -> loader=*, version=1.21.1, id=p21

    public static ResourceLocation fromNamespaceAndPath(String namespace, String path) {
        // return ResourceLocation.fromNamespaceAndPath(namespace, path); *p21
        return new ResourceLocation(namespace, path); // remove*p21
    }

    public static ResourceLocation parse(String id) {
        // return ResourceLocation.parse(id); *p21
        return new ResourceLocation(id); // remove*p21
    }

    public static ResourceLocation withDefaultNamespace(String path) {
        // return ResourceLocation.withDefaultNamespace(path); *p21
        return new ResourceLocation("minecraft", path); // remove*p21
    }
}