package net.jadenxgamer.elysium_api.impl.core.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.jadenxgamer.elysium_api.ElysiumAPI;
import net.jadenxgamer.elysium_api.impl.registry.ElysiumBlocks;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class StructureStamp extends Feature<StructureStamp.Config> {

    public StructureStamp(Codec<Config> pCodec) {
        super(pCodec);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> context) {
        BlockPos origin = context.origin();
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        Config config = context.config();

        if (config.canPlaceOn().isPresent() && !level.getBlockState(origin.below()).is(config.canPlaceOn().get())) return false;
        return config.templates().getRandom(random)
                .map(selected -> placeTemplate(selected.data(), origin, level, random, config))
                .orElseGet(() -> {
                    ElysiumAPI.LOGGER.warn("StructureStamp failed to place at '{}' due to a lack of templates. This could happen if the templates weighted list is empty.", origin);
                    return false;
                });
    }

    private boolean placeTemplate(ResourceLocation templateLocation, BlockPos origin, WorldGenLevel level, RandomSource random, Config config) {
        StructureTemplate template = level.getLevel().getServer().getStructureManager().getOrCreate(templateLocation);
        Rotation rotation = config.rotation().isEmpty() ? Rotation.getRandom(random) : config.rotation().get();

        BlockPos pivot = BlockPos.ZERO;
        BlockPos placementPos;

        switch (config.originType()) {
            case CENTERED -> {
                Vec3i size = template.getSize();
                pivot = new BlockPos(size.getX() / 2, 0, size.getZ() / 2);
                placementPos = origin.subtract(pivot);
            }
            case ANCHORED -> {
                BlockPos anchorLocal = findAnchor(template);
                if (anchorLocal == null) {
                    placementPos = origin;
                    ElysiumAPI.LOGGER.warn("No Structure Stamp Anchor was found within '{}', fallback to CORNER origin_type.", templateLocation);
                } else {
                    pivot = anchorLocal;
                    placementPos = origin.subtract(anchorLocal);
                }
            }
            default -> placementPos = origin; // CORNER
        }

        int offset = config.originOffset().sample(random);
        placementPos = placementPos.offset(0, offset, 0);

        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setRotation(rotation)
                .setRotationPivot(pivot)
                .setRandom(random)
                .setLiquidSettings(config.liquidSettings);

        for (StructureProcessor processor : config.processors().value().list()) settings.addProcessor(processor);
        if (config.originType() == OriginType.ANCHORED) settings.addProcessor(new AnchorRemovalProcessor());

        template.placeInWorld(level, placementPos, placementPos, settings, random, 3);
        return true;
    }

    @Nullable
    private static BlockPos findAnchor(StructureTemplate template) {
        for (StructureTemplate.Palette palette : template.palettes) {
            for (StructureTemplate.StructureBlockInfo info : palette.blocks()) {
                if (info.state().is(ElysiumBlocks.STRUCTURE_STAMP_ANCHOR.get())) return info.pos();
            }
        }
        return null;
    }

    public record Config(
            WeightedRandomList<WeightedEntry.Wrapper<ResourceLocation>> templates, Optional<HolderSet<Block>> canPlaceOn, Holder<StructureProcessorList> processors,
            Optional<Rotation> rotation, LiquidSettings liquidSettings, OriginType originType, IntProvider originOffset
    ) implements FeatureConfiguration {

        public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                WeightedRandomList.codec(WeightedEntry.Wrapper.codec(ResourceLocation.CODEC)).fieldOf("templates").forGetter(Config::templates),
                RegistryCodecs.homogeneousList(Registries.BLOCK).optionalFieldOf("can_place_on").forGetter(Config::canPlaceOn),
                StructureProcessorType.LIST_CODEC.fieldOf("processors").forGetter(Config::processors),
                Rotation.CODEC.optionalFieldOf("rotation").forGetter(Config::rotation),
                LiquidSettings.CODEC.fieldOf("liquid_settings").orElse(LiquidSettings.APPLY_WATERLOGGING).forGetter(Config::liquidSettings),
                OriginType.CODEC.optionalFieldOf("origin_type", OriginType.CORNER).forGetter(Config::originType),
                IntProvider.CODEC.optionalFieldOf("origin_offset", ConstantInt.of(0)).forGetter(Config::originOffset)
        ).apply(instance, Config::new));
    }

    public enum OriginType implements StringRepresentable {
        CORNER("corner"),
        CENTERED("centered"),
        ANCHORED("anchored");

        private final String name;
        public static final StringRepresentableCodec<OriginType> CODEC = StringRepresentable.fromEnum(OriginType::values);

        OriginType(String name) {
            this.name = name;
        }

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static class AnchorRemovalProcessor extends StructureProcessor {
        @Override
        public @Nullable StructureTemplate.StructureBlockInfo processBlock(LevelReader level, BlockPos offset, BlockPos pos, StructureTemplate.StructureBlockInfo blockInfo, StructureTemplate.StructureBlockInfo relativeBlockInfo, StructurePlaceSettings settings) {
            if (relativeBlockInfo.state().is(ElysiumBlocks.STRUCTURE_STAMP_ANCHOR.get())) return null;
            return super.processBlock(level, offset, pos, blockInfo, relativeBlockInfo, settings);
        }

        @Override
        protected StructureProcessorType<?> getType() {
            return StructureProcessorType.BLOCK_IGNORE;
        }
    }
}