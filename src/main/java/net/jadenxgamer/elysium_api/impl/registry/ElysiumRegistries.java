package net.jadenxgamer.elysium_api.impl.registry;

import com.mojang.serialization.Codec;
import net.jadenxgamer.elysium_api.ElysiumAPI;
import net.jadenxgamer.elysium_api.api.surface_rules.condition.BiomeTagConditionSource;
import net.jadenxgamer.elysium_api.api.surface_rules.condition.BlockMatchConditionSource;
import net.jadenxgamer.elysium_api.impl.core.biome.MosaicBiomeSource;
import net.jadenxgamer.elysium_api.impl.core.datadriven.block.BlockSoundTransformer;
import net.jadenxgamer.elysium_api.impl.core.datadriven.block.properties_transformer.BlockPropertiesTransformer;
import net.jadenxgamer.elysium_api.impl.core.datadriven.block.use_behaviors.UseBehavior;
import net.jadenxgamer.elysium_api.impl.core.datadriven.brewing_recipe.ElysiumBrewingRecipe;
import net.jadenxgamer.elysium_api.impl.core.datadriven.item.properties_transformer.ItemPropertiesTransformer;
import net.jadenxgamer.elysium_api.impl.core.datadriven.mosaic.MosaicBiomeEntry;
import net.jadenxgamer.elysium_api.impl.core.misc.forge.EffectsBiomeModifier;
import net.jadenxgamer.elysium_api.impl.core.worldgen.feature.StructureStamp;
import net.jadenxgamer.elysium_api.impl.core.worldgen.structure.GiantJigsawStructure;
import net.jadenxgamer.elysium_api.impl.core.worldgen.structure.MultilayerJigsawStructure;
import net.jadenxgamer.elysium_api.impl.core.worldgen.structure.processor.ProtectNonReplaceableProcessor;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DataPackRegistryEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.NewRegistryEvent;

import java.util.function.Supplier;

public class ElysiumRegistries {

    public static final DeferredRegister<Codec<? extends BiomeSource>> BIOME_SOURCES = DeferredRegister.create(BuiltInRegistries.BIOME_SOURCE.key(), ElysiumAPI.MOD_ID);
    private static final DeferredRegister<Codec<? extends BiomeModifier>> BIOME_MODIFIERS = DeferredRegister.create(ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, ElysiumAPI.MOD_ID);
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(BuiltInRegistries.FEATURE.key(), ElysiumAPI.MOD_ID);
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPE = DeferredRegister.create(BuiltInRegistries.STRUCTURE_TYPE.key(), ElysiumAPI.MOD_ID);
    public static final DeferredRegister<StructureProcessorType<?>> STRUCTURE_PROCESSOR = DeferredRegister.create(BuiltInRegistries.STRUCTURE_PROCESSOR.key(), ElysiumAPI.MOD_ID);
    public static final DeferredRegister<Codec<? extends SurfaceRules.ConditionSource>> MATERIAL_CONDITIONS = DeferredRegister.create(BuiltInRegistries.MATERIAL_CONDITION.key(), ElysiumAPI.MOD_ID);

    public static final Supplier<Codec<MosaicBiomeSource>> MOSAIC = BIOME_SOURCES.register("mosaic", () -> MosaicBiomeSource.CODEC);
    public static final Supplier<Codec<EffectsBiomeModifier>> EFFECTS_MODIFIER = BIOME_MODIFIERS.register("effects_modifier", () -> EffectsBiomeModifier.CODEC);
    public static final Supplier<Feature<StructureStamp.Config>> STRUCTURE_STAMP = FEATURES.register("structure_stamp", () -> new StructureStamp(StructureStamp.Config.CODEC));
    public static final Supplier<StructureType<MultilayerJigsawStructure>> MULTILAYERED_JIGSAW = STRUCTURE_TYPE.register("multilayered_jigsaw", () -> () -> MultilayerJigsawStructure.CODEC);
    public static final Supplier<StructureType<GiantJigsawStructure>> GIANT_JIGSAW = STRUCTURE_TYPE.register("giant_jigsaw", () -> () -> GiantJigsawStructure.CODEC);
    public static final Supplier<StructureProcessorType<ProtectNonReplaceableProcessor>> PROTECT_NON_REPLACEABLE = STRUCTURE_PROCESSOR.register("protect_non_replaceable", () -> () -> ProtectNonReplaceableProcessor.CODEC);
    public static final Supplier<Codec<? extends SurfaceRules.ConditionSource>> BIOME_TAG = MATERIAL_CONDITIONS.register("biome_tag", () -> (Codec<? extends SurfaceRules.ConditionSource>) BiomeTagConditionSource.CODEC.codec());
    public static final Supplier<Codec<? extends SurfaceRules.ConditionSource>> BLOCK_MATCH = MATERIAL_CONDITIONS.register("block_match", () -> (Codec<? extends SurfaceRules.ConditionSource>) BlockMatchConditionSource.CODEC.codec());

    private static <T> ResourceKey<Registry<T>> key(String name) {
        return ResourceKey.createRegistryKey(ElysiumAPI.elysiumPath(name));
    }

    public static void init(IEventBus eventBus) {
        BIOME_SOURCES.register(eventBus);
        BIOME_MODIFIERS.register(eventBus);
        FEATURES.register(eventBus);
        STRUCTURE_TYPE.register(eventBus);
        STRUCTURE_PROCESSOR.register(eventBus);
        MATERIAL_CONDITIONS.register(eventBus);
    }

    public static void registryInit(NewRegistryEvent event) {
    }

    public static void datapackInit(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(Keys.USE_BEHAVIORS, UseBehavior.CODEC);
        event.dataPackRegistry(Keys.BLOCK_SOUND_TRANSFORMERS, BlockSoundTransformer.CODEC, BlockSoundTransformer.CODEC);
        event.dataPackRegistry(Keys.BREWING_RECIPES, ElysiumBrewingRecipe.CODEC, ElysiumBrewingRecipe.CODEC);
        event.dataPackRegistry(Keys.MOSAIC_BIOME_ENTRY, MosaicBiomeEntry.CODEC);
        event.dataPackRegistry(Keys.BLOCK_PROPERTIES_TRANSFORMERS, BlockPropertiesTransformer.CODEC, BlockPropertiesTransformer.CODEC);
        event.dataPackRegistry(Keys.ITEM_PROPERTIES_TRANSFORMERS, ItemPropertiesTransformer.CODEC, ItemPropertiesTransformer.CODEC);
    }

    public static final class Keys {
        public static final ResourceKey<Registry<UseBehavior>> USE_BEHAVIORS = key("block/use_behaviors");
        public static final ResourceKey<Registry<BlockSoundTransformer>> BLOCK_SOUND_TRANSFORMERS = key("block/sound_transformers");
        public static final ResourceKey<Registry<ElysiumBrewingRecipe>> BREWING_RECIPES = key("brewing_recipes");
        public static final ResourceKey<Registry<MosaicBiomeEntry>> MOSAIC_BIOME_ENTRY = key("mosaic_biome_entry");
        public static final ResourceKey<Registry<BlockPropertiesTransformer>> BLOCK_PROPERTIES_TRANSFORMERS = key("block/properties_transformer");
        public static final ResourceKey<Registry<ItemPropertiesTransformer>> ITEM_PROPERTIES_TRANSFORMERS = key("item/properties_transformer");
    }
}