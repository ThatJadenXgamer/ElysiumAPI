package net.jadenxgamer.elysium_api.impl.core.datadriven.item.properties_transformer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;

import java.util.Optional;

public record ItemPropertiesTransformer(HolderSet<Item> items, Properties properties) {

    public static final Codec<ItemPropertiesTransformer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            RegistryCodecs.homogeneousList(Registries.ITEM).fieldOf("items").forGetter(ItemPropertiesTransformer::items),
            Properties.CODEC.fieldOf("properties").forGetter(ItemPropertiesTransformer::properties)
    ).apply(instance, ItemPropertiesTransformer::new));

    public record Properties(
            Optional<Integer> maxStackSize,
            Optional<Integer> maxDamage,
            Optional<RarityEnum> rarity,
            Optional<Boolean> fireResistant,
            Optional<FoodProperties> food,
            Optional<Holder<Item>> craftRemainder,
            Optional<Boolean> canRepair
    ) {
        public static final Codec<Properties> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.optionalFieldOf("max_stack_size").forGetter(Properties::maxStackSize),
                Codec.INT.optionalFieldOf("max_damage").forGetter(Properties::maxDamage),
                RarityEnum.CODEC.optionalFieldOf("rarity").forGetter(Properties::rarity),
                Codec.BOOL.optionalFieldOf("fire_resistant").forGetter(Properties::fireResistant),
                FoodProperties.DIRECT_CODEC.optionalFieldOf("food").forGetter(Properties::food),
                BuiltInRegistries.ITEM.holderByNameCodec().optionalFieldOf("craft_remainder").forGetter(Properties::craftRemainder),
                Codec.BOOL.optionalFieldOf("can_repair").forGetter(Properties::canRepair)
        ).apply(instance, Properties::new));
    }
}