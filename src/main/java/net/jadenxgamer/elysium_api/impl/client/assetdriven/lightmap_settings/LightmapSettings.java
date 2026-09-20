package net.jadenxgamer.elysium_api.impl.client.assetdriven.lightmap_settings;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.jadenxgamer.elysium_api.impl.client.assetdriven.FogLightSettingsType;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public record LightmapSettings(FogLightSettingsType type, Set<ResourceLocation> biomes, Set<ResourceLocation> dimensions, Set<ResourceLocation> eventFlags,
                               int priority, float fadeMultiplier, Vector3f skyLightColor, Vector3f blockLightColor, float ambientBrightness) {

    public static LightmapSettings parseSetting(JsonObject json) {
        FogLightSettingsType type = FogLightSettingsType.GLOBAL;
        if (json.has("type")) type = FogLightSettingsType.byName(json.get("type").getAsString(), FogLightSettingsType.GLOBAL);

        Set<ResourceLocation> biomes = parseIdentifierSet(json, "biomes");
        Set<ResourceLocation> dimensions = parseIdentifierSet(json, "dimensions");
        Set<ResourceLocation> eventFlags = parseIdentifierSet(json, "event_flags");
        int priority = json.has("priority") ? json.get("priority").getAsInt() : 0;
        float fadeMultiplier = json.has("fade_multiplier") ? json.get("fade_multiplier").getAsFloat() : 1.0f;

        Vector3f skyColor = new Vector3f(1.0f, 1.0f, 1.0f);
        Vector3f blockColor = new Vector3f(1.0f, 1.0f, 1.0f);
        float ambientBrightness = 0.0f;

        if (json.has("sky_light_color")) skyColor = parseHex(json.get("sky_light_color").getAsString());
        if (json.has("block_light_color")) blockColor = parseHex(json.get("block_light_color").getAsString());
        if (json.has("ambient_brightness")) ambientBrightness = json.get("ambient_brightness").getAsFloat();

        return new LightmapSettings(type, biomes, dimensions, eventFlags, priority, fadeMultiplier, skyColor, blockColor, ambientBrightness);
    }

    private static Set<ResourceLocation> parseIdentifierSet(JsonObject json, String key) {
        if (!json.has(key)) return Collections.emptySet();
        Set<ResourceLocation> set = new HashSet<>();
        JsonArray array = json.getAsJsonArray(key);
        for (var element : array) {
            ResourceLocation loc = ResourceLocation.tryParse(element.getAsString());
            if (loc != null) set.add(loc);
        }
        return set;
    }

    private static Vector3f parseHex(String hexString) {
        if (hexString.startsWith("#")) hexString = hexString.substring(1);
        int color = Integer.parseInt(hexString, 16);
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        return new Vector3f(r, g, b);
    }
}