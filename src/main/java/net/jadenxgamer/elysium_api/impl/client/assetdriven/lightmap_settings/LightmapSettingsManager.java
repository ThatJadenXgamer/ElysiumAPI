package net.jadenxgamer.elysium_api.impl.client.assetdriven.lightmap_settings;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.jadenxgamer.elysium_api.ElysiumAPI;
import net.jadenxgamer.elysium_api.api.util.ClientTimeHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.*;

public class LightmapSettingsManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new Gson();

    private static final Vector3f currentSkyColor = new Vector3f(1.0f, 1.0f, 1.0f);
    private static final Vector3f currentBlockColor = new Vector3f(1.0f, 1.0f, 1.0f);
    private static float currentAmbientBrightness = 0.0f;

    private static final List<LightmapSettings> LIGHTMAP_SETTINGS = new ArrayList<>();
    private static final Set<ResourceLocation> ENABLED_EVENT_FLAGS = new HashSet<>();

    public static final ResourceLocation GUI_LIGHTMAP = ElysiumAPI.elysiumPath("textures/misc/gui.png");
    private boolean usingGuiLightmap = false;

    public LightmapSettingsManager() {
        super(GSON, "elysium_api/lightmap_settings");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> elements, @NotNull ResourceManager manager, @NotNull ProfilerFiller profiler) {
        LIGHTMAP_SETTINGS.clear();
        for (JsonElement element : elements.values()) {
            try {
                JsonObject json = element.getAsJsonObject();
                LightmapSettings settings = LightmapSettings.parseSetting(json);
                LIGHTMAP_SETTINGS.add(settings);
            } catch (Exception e) {
                ElysiumAPI.LOGGER.warn("Couldn't load LightmapSettings: {}", e.getMessage());
            }
        }
        LIGHTMAP_SETTINGS.sort(Comparator.comparingInt(LightmapSettings::priority).reversed());
    }

    @ApiStatus.Internal
    public Triple<Vector3f, Vector3f, Float> getSettings(Player player) {
        if (player == null) return Triple.of(new Vector3f(1.0f, 1.0f, 1.0f), new Vector3f(1.0f, 1.0f, 1.0f), 0.0f);

        Level level = player.level();
        BlockPos pos = BlockPos.containing(player.getX(), player.getEyeY(), player.getZ());
        ResourceLocation biome = level.registryAccess().registryOrThrow(Registries.BIOME).getKey(level.getBiome(pos).value());
        ResourceLocation dimension = level.dimension().location();

        LightmapSettings matched = null;
        for (LightmapSettings settings : LIGHTMAP_SETTINGS) {
            if (matches(settings, biome, dimension)) {
                matched = settings;
                break;
            }
        }

        Vector3f targetSky = new Vector3f(1.0f, 1.0f, 1.0f);
        Vector3f targetBlock = new Vector3f(1.0f, 1.0f, 1.0f);
        float targetBrightness = 0.0f;
        float fadeMultiplier = 1.0f;

        if (matched != null) {
            targetSky = matched.skyLightColor();
            targetBlock = matched.blockLightColor();
            targetBrightness = matched.ambientBrightness();
            fadeMultiplier = matched.fadeMultiplier();
        }

        float delta = ClientTimeHelper.getGameTimeDeltaTicks() * 0.03f * fadeMultiplier;
        currentSkyColor.lerp(targetSky, delta);
        currentBlockColor.lerp(targetBlock, delta);
        currentAmbientBrightness = Mth.lerp(delta, currentAmbientBrightness, targetBrightness);

        return Triple.of(currentSkyColor, currentBlockColor, currentAmbientBrightness);
    }

    private boolean matches(LightmapSettings settings, ResourceLocation biome, ResourceLocation dimension) {
        if (!settings.eventFlags().isEmpty()) {
            for (ResourceLocation flag : settings.eventFlags()) if (!ENABLED_EVENT_FLAGS.contains(flag)) return false;
        }

        return switch (settings.type()) {
            case GLOBAL -> true;
            case BIOME -> settings.biomes().contains(biome);
            case DIMENSION -> settings.dimensions().contains(dimension);
            case NOT_BIOME -> !settings.biomes().contains(biome);
            case NOT_DIMENSION -> !settings.dimensions().contains(dimension);
        };
    }

    public static void enableEventFlag(ResourceLocation flag) {
        ENABLED_EVENT_FLAGS.add(flag);
    }

    public static void disableEventFlag(ResourceLocation flag) {
        ENABLED_EVENT_FLAGS.remove(flag);
    }

    @ApiStatus.Internal
    public void setupForGUI(boolean gui) {
        usingGuiLightmap = gui;
    }

    @ApiStatus.Internal
    public boolean isGui() {
        return usingGuiLightmap;
    }
}