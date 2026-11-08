package net.jadenxgamer.elysium_api.api.client.render;

import net.jadenxgamer.elysium_api.ElysiumAPI;
import net.jadenxgamer.elysium_api.api.unification.ElysiumResourceLocation;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ElysiumRenderTypes {
    private static final Map<ResourceLocation, RenderType> BY_NAME = new ConcurrentHashMap<>();

    private ElysiumRenderTypes() {}

    public static void capture(String name, RenderType renderType) {
        ResourceLocation id = tryParseId(name);
        if (id != null && renderType != null) BY_NAME.putIfAbsent(id, renderType);
    }

    @Nullable
    public static RenderType get(String id) {
        ResourceLocation location = tryParseId(id);
        return location != null ? BY_NAME.get(location) : null;
    }

    @Nullable
    public static RenderType get(ResourceLocation id) {
        return BY_NAME.get(id);
    }

    @Nullable
    public static ResourceLocation tryParseId(String id) {
        try { return ElysiumResourceLocation.parse(id); } catch (Exception e) { return null; }
    }
}