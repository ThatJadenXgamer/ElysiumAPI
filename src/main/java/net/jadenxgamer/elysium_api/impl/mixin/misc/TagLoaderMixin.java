package net.jadenxgamer.elysium_api.impl.mixin.misc;

import com.google.gson.*;
import com.mojang.serialization.*;
import net.jadenxgamer.elysium_api.ElysiumAPI;
import net.jadenxgamer.elysium_api.impl.util.TagParseResult;
import net.minecraft.core.Holder;
import net.minecraft.resources.*;
import net.minecraft.server.packs.resources.*;
import net.minecraft.tags.*;
import net.minecraft.tags.TagLoader.EntryWithSource;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(TagLoader.class)
public abstract class TagLoaderMixin {
    // asphodel::patch -> loader=*, version=1.21.1, id=patch21

    @Unique private static final AtomicBoolean elysium_api$announced = new AtomicBoolean();
    @Unique private final Map<ResourceLocation, Set<ResourceLocation>> elysium_api$removals = new HashMap<>();
    @Shadow @Final private String directory;

    @Inject(
            method = "load",
            at = @At("RETURN")
    )
    private void elysium_api$load(ResourceManager resourceManager, CallbackInfoReturnable<Map<ResourceLocation, List<EntryWithSource>>> callback) {
        var result = callback.getReturnValue();
        if (result == null) return;
        elysium_api$removals.clear();

        var canonicalReplaced = new HashSet<ResourceLocation>();
        var canonical = FileToIdConverter.json(this.directory);
        canonical.listMatchingResourceStacks(resourceManager).forEach((fileId, stack) -> {
            var tagId = canonical.fileToId(fileId);
            var parsed = elysium_api$parse(stack, tagId);
            if (parsed.replaced) canonicalReplaced.add(tagId);
            result.put(tagId, parsed.entries);
        });

        var alternate = elysium_api$alt(this.directory);
        if (alternate == null) return;
        var converter = FileToIdConverter.json(alternate);
        converter.listMatchingResourceStacks(resourceManager).forEach((fileId, stack) -> {
            var tagId = converter.fileToId(fileId);
            var parsed = elysium_api$parse(stack, tagId);
            if (parsed.entries.isEmpty() && !parsed.replaced) return;
            if (canonicalReplaced.contains(tagId) && !parsed.replaced) return;
            var list = result.computeIfAbsent(tagId, key -> new ArrayList<>());
            if (parsed.replaced) list.clear();
            list.addAll(parsed.entries);
            if (!parsed.entries.isEmpty() && elysium_api$announced.compareAndSet(false, true)) {
                ElysiumAPI.LOGGER.info("Tag forwards compatibility active, tags are now also parsed from future 1.21.1 singular-case directories"); // remove*patch21
                // ElysiumAPI.LOGGER.info("Tag backwards compatibility active, tags are now also parsed from legacy 1.20.1 plural-case directories"); *patch21
            }
        });
    }

    @Inject(
            method = "build(Ljava/util/Map;)Ljava/util/Map;",
            at = @At("RETURN")
    )
    private void elysium_api$build(Map<ResourceLocation, List<EntryWithSource>> builders, CallbackInfoReturnable<Map<ResourceLocation, Collection<?>>> callback) {
        if (elysium_api$removals.isEmpty()) return;
        var result = callback.getReturnValue();
        if (result == null) return;
        elysium_api$removals.forEach((tagId, removalIds) -> {
            var contents = (Collection<?>) result.get(tagId);
            if (contents == null || contents.isEmpty()) return;
            var filtered = new ArrayList<>(contents.size());
            for (var entry : contents) if (!(entry instanceof Holder<?> holder && holder.unwrapKey().map(key -> removalIds.contains(key.location())).orElse(false))) filtered.add(entry);
            if (filtered.size() != contents.size()) result.put(tagId, filtered);
        });
    }

    @Unique
    private TagParseResult elysium_api$parse(List<Resource> stack, ResourceLocation tagId) {
        var result = new TagParseResult();
        for (var resource : stack) {
            try (var reader = resource.openAsReader()) {
                var json = JsonParser.parseReader(reader);

                var tagFile = TagFile.CODEC.parse(JsonOps.INSTANCE, elysium_api$sanitize(json)).getOrThrow(false, ElysiumAPI.LOGGER::error); // remove*patch21
                // var tagFile = TagFile.CODEC.parse(JsonOps.INSTANCE, elysium_api$sanitize(json)).getOrThrow(); *patch21

                if (tagFile.replace()) { result.entries.clear(); result.replaced = true; }
                for (var entry : tagFile.entries()) result.entries.add(new EntryWithSource(entry, resource.sourcePackId()));
                elysium_api$extract(json, tagId);
            } catch (Exception exception) {
                ElysiumAPI.LOGGER.error("Couldn't read tag list {} from {}", tagId, resource.sourcePackId(), exception);
            }
        }
        return result;
    }

    @Unique
    private void elysium_api$extract(JsonElement json, ResourceLocation tagId) {
        if (!json.isJsonObject() || !(json.getAsJsonObject().get("elysium_remove") instanceof JsonArray array) || array.isEmpty()) return;
        var set = elysium_api$removals.computeIfAbsent(tagId, key -> new HashSet<>());
        for (var element : array) {
            if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) continue;
            var location = ResourceLocation.tryParse(element.getAsString());
            if (location != null) set.add(location);
        }
    }

    @Unique
    private static JsonElement elysium_api$sanitize(JsonElement json) {
        if (!json.isJsonObject() || !(json.getAsJsonObject().get("values") instanceof JsonArray values)) return json;
        var cleaned = new JsonArray(values.size());
        for (var element : values) {
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                var id = element.getAsString();
                if (ResourceLocation.tryParse(id.startsWith("#") ? id.substring(1) : id) == null) continue;
                var replacement = new JsonObject();
                replacement.addProperty("id", id);
                replacement.addProperty("required", false);
                cleaned.add(replacement);
            } else if (element.isJsonObject()) {
                var object = element.getAsJsonObject();
                if (!(object.get("id") instanceof JsonPrimitive primitive) || !primitive.isString()) continue;
                var id = primitive.getAsString();
                if (ResourceLocation.tryParse(id.startsWith("#") ? id.substring(1) : id) == null) continue;
                if (!object.has("required")) object.addProperty("required", false);
                cleaned.add(object);
            }
        }
        json.getAsJsonObject().add("values", cleaned);
        return json;
    }

    @Unique
    private static String elysium_api$alt(String directory) {
        if (directory == null || !directory.startsWith("tags/") || directory.endsWith("/")) return null;
        return directory.substring(directory.lastIndexOf('/') + 1).endsWith("s") ? directory.substring(0, directory.length() - 1) : directory + "s";
    }
}