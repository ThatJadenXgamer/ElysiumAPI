package net.jadenxgamer.elysium_api.impl.client.conditional_resource;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraftforge.common.crafting.CraftingHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static net.jadenxgamer.elysium_api.ElysiumAPI.LOGGER;

public final class ConditionalAssetsHelper {
    private static final String CONDITIONS_KEY = "forge:conditions";
    private static final String CONDITION_SUFFIX = "_condition.json";
    private static final Map<String, Map<ResourceLocation, Boolean>> CONDITION_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Set<ResourceLocation>> CONDITION_FILE_PRESENCE = new ConcurrentHashMap<>();

    private ConditionalAssetsHelper() {}

    public static boolean isAssetHidden(PackResources pack, PackType type, ResourceLocation location) {
        String packId = pack.packId();
        Set<ResourceLocation> present = CONDITION_FILE_PRESENCE.computeIfAbsent(packId, k -> ConcurrentHashMap.newKeySet());

        if (!present.contains(location)) {
            ResourceLocation conditionLoc = ResourceLocation.fromNamespaceAndPath(location.getNamespace(), location.getPath() + CONDITION_SUFFIX);
            try (InputStream stream = openResource(pack, type, conditionLoc)) {
                if (stream == null) return false;
            } catch (IOException e) {
                LOGGER.error("Failed to check condition file {} from pack {}", conditionLoc, pack.packId(), e);
                return false;
            }
            present.add(location);
        }

        return CONDITION_CACHE.computeIfAbsent(packId, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(location, loc -> evaluateConditionForAsset(pack, type, loc));
    }

    private static boolean evaluateConditionForAsset(PackResources pack, PackType type, ResourceLocation location) {
        ResourceLocation conditionLoc = ResourceLocation.fromNamespaceAndPath(location.getNamespace(), location.getPath() + CONDITION_SUFFIX);
        InputStream conditionStream;
        try {
            conditionStream = openResource(pack, type, conditionLoc);
        } catch (IOException e) {
            LOGGER.error("Failed to open condition file {} from pack {}", conditionLoc, pack.packId(), e);
            return false;
        }
        if (conditionStream == null) return false;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conditionStream, StandardCharsets.UTF_8))) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject()) {
                LOGGER.warn("Condition file {} is not a JSON object, ignoring", conditionLoc);
                return false;
            }
            JsonObject obj = root.getAsJsonObject();
            if (!obj.has(CONDITIONS_KEY)) return false;

            JsonElement conditionsElem = obj.get(CONDITIONS_KEY);
            if (!conditionsElem.isJsonArray()) {
                LOGGER.warn("Condition file {} has '{}' field but it is not an array", conditionLoc, CONDITIONS_KEY);
                return false;
            }
            if (conditionsElem.getAsJsonArray().isEmpty()) return false;

            boolean allTrue = CraftingHelper.processConditions(obj, CONDITIONS_KEY, ClientIConditionContext.INSTANCE);
            if (!allTrue && LOGGER.isDebugEnabled())
                LOGGER.debug("Asset {} hidden due to conditions in {}", location, conditionLoc);
            return !allTrue;

        } catch (IOException e) {
            LOGGER.error("Failed to read condition file {} from pack {}", conditionLoc, pack.packId(), e);
        } catch (Exception e) {
            LOGGER.error("Unexpected error evaluating conditions for {} in pack {}", location, pack.packId(), e);
        }
        return false;
    }

    private static InputStream openResource(PackResources pack, PackType type, ResourceLocation location) throws IOException {
        IoSupplier<InputStream> supplier = pack.getResource(type, location);
        return supplier == null ? null : supplier.get();
    }

    public static void invalidateCache() {
        CONDITION_CACHE.clear();
        CONDITION_FILE_PRESENCE.clear();
        LOGGER.debug("Conditional-Loaded Assets cache cleared");
    }
}