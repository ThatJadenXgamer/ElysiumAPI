package net.jadenxgamer.elysium_api.impl.core.datadriven.item.properties_transformer;

import net.jadenxgamer.elysium_api.ElysiumAPI;
import net.jadenxgamer.elysium_api.api.util.RegistryAccessHelper;
import net.jadenxgamer.elysium_api.impl.registry.ElysiumRegistries;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class ItemPropertiesTransformerHelper {
    private static final ConcurrentHashMap<Item, Optional<ItemPropertiesTransformer>> CACHE = new ConcurrentHashMap<>();

    @Nullable
    public static ItemPropertiesTransformer getTransformer(Item item) {
        if (!RegistryAccessHelper.isServerAvailable()) return null;
        Optional<ItemPropertiesTransformer> cached = CACHE.get(item);
        if (cached != null) return cached.orElse(null);

        Optional<ItemPropertiesTransformer> computed = Optional.ofNullable(loadAndMergeTransformers(item));
        CACHE.put(item, computed);
        return computed.orElse(null);
    }

    private static ItemPropertiesTransformer loadAndMergeTransformers(Item item) {
        Optional<RegistryAccess> registryAccess = RegistryAccessHelper.getServer();
        if (registryAccess.isEmpty()) return null;

        Registry<ItemPropertiesTransformer> registry = registryAccess.get().registryOrThrow(ElysiumRegistries.Keys.ITEM_PROPERTIES_TRANSFORMERS);
        Holder<Item> itemHolder = item.builtInRegistryHolder();

        List<ItemPropertiesTransformer> applicable = new ArrayList<>();
        for (Holder<ItemPropertiesTransformer> holder : registry.holders().toList()) {
            ItemPropertiesTransformer transformer = holder.value();
            if (transformer.items().contains(itemHolder)) applicable.add(transformer);
        }

        if (applicable.isEmpty()) return null;
        if (applicable.size() == 1) return applicable.getFirst();
        return mergeTransformers(applicable);
    }

    private static ItemPropertiesTransformer mergeTransformers(List<ItemPropertiesTransformer> transformers) {
        ItemPropertiesTransformer.Properties mergedProperties = mergeProperties(transformers);
        HolderSet<Item> items = transformers.getFirst().items();
        return new ItemPropertiesTransformer(items, mergedProperties);
    }

    private static ItemPropertiesTransformer.Properties mergeProperties(List<ItemPropertiesTransformer> transformers) {
        Optional<Integer> maxStackSize = Optional.empty();
        Optional<Integer> maxDamage = Optional.empty();
        Optional<RarityEnum> rarity = Optional.empty();
        Optional<Boolean> fireResistant = Optional.empty();
        Optional<FoodProperties> food = Optional.empty();
        Optional<Holder<Item>> craftRemainder = Optional.empty();
        Optional<Boolean> canRepair = Optional.empty();

        for (ItemPropertiesTransformer transformer : transformers) {
            ItemPropertiesTransformer.Properties props = transformer.properties();
            if (maxStackSize.isEmpty() && props.maxStackSize().isPresent()) maxStackSize = props.maxStackSize();
            if (maxDamage.isEmpty() && props.maxDamage().isPresent()) maxDamage = props.maxDamage();
            if (rarity.isEmpty() && props.rarity().isPresent()) rarity = props.rarity();
            if (fireResistant.isEmpty() && props.fireResistant().isPresent()) fireResistant = props.fireResistant();
            if (food.isEmpty() && props.food().isPresent()) food = props.food();
            if (craftRemainder.isEmpty() && props.craftRemainder().isPresent()) craftRemainder = props.craftRemainder();
            if (canRepair.isEmpty() && props.canRepair().isPresent()) canRepair = props.canRepair();
        }

        return new ItemPropertiesTransformer.Properties(maxStackSize, maxDamage, rarity, fireResistant, food, craftRemainder, canRepair);
    }

    public static void invalidateCache() {
        CACHE.clear();
        ElysiumAPI.LOGGER.debug("ItemPropertiesTransformer cache cleared for new datapack entries");
    }
}