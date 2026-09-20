package net.jadenxgamer.elysium_api.impl.client.assetdriven.fog_settings;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.jadenxgamer.elysium_api.impl.client.assetdriven.FogLightSettingsType;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public record FogSettings(FogLightSettingsType type, Set<ResourceLocation> biomes, Set<ResourceLocation> dimensions, Set<ResourceLocation> eventFlags,
                          int priority, float fadeMultiplier, float fogStartMultiplier, float fogEndMultiplier) {

    public static FogSettings parseSetting(JsonObject json) {
        FogLightSettingsType type = FogLightSettingsType.GLOBAL;
        if (json.has("type")) type = FogLightSettingsType.byName(json.get("type").getAsString(), FogLightSettingsType.GLOBAL);

        Set<ResourceLocation> biomes = parseIdentifierSet(json, "biomes");
        Set<ResourceLocation> dimensions = parseIdentifierSet(json, "dimensions");
        Set<ResourceLocation> eventFlags = parseIdentifierSet(json, "event_flags");
        int priority = json.has("priority") ? json.get("priority").getAsInt() : 0;
        float fadeMultiplier = json.has("fade_multiplier") ? json.get("fade_multiplier").getAsFloat() : 1.0f;

        float fogStartMultiplier = json.has("fog_start_multiplier") ? json.get("fog_start_multiplier").getAsFloat() : 1.0f;
        float fogEndMultiplier = json.has("fog_end_multiplier") ? json.get("fog_end_multiplier").getAsFloat() : 1.0f;

        return new FogSettings(type, biomes, dimensions, eventFlags, priority, fadeMultiplier, fogStartMultiplier, fogEndMultiplier);
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
}