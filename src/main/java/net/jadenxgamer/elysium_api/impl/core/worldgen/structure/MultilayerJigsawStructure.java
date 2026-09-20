package net.jadenxgamer.elysium_api.impl.core.worldgen.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.jadenxgamer.elysium_api.api.tags.ElysiumTags;
import net.jadenxgamer.elysium_api.impl.registry.ElysiumRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Multilayered Jigsaw Structures unlike their cringe normal {@link JigsawStructure} are more tailored made for
 * multilayered dimensions such as The Nether or The End.
 * <p>
 * Rather than just pasting in the structure at whatever Y coordinate it choose from its range it first iterates through
 * the current Y it choose all the way down till the sea level of the dimension and tries to find a valid floor to paste it on
 * <p>
 * This prevents weird floating template or islands forming because of it since it is guaranteed to always spawn on some kind of pre-generated floor
 */
public class MultilayerJigsawStructure extends Structure {
    private final Holder<StructureTemplatePool> startPool;
    private final Optional<ResourceLocation> startJigsawName;
    private final int size;
    private final HeightProvider startHeight;
    private final Optional<Heightmap.Types> projectStartToHeightmap;
    private final int maxDistanceFromCenter;

    public static final Codec<MultilayerJigsawStructure> CODEC = RecordCodecBuilder.<MultilayerJigsawStructure>create(instance ->
            instance.group(MultilayerJigsawStructure.settingsCodec(instance),
                    StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(structure -> structure.startPool),
                    ResourceLocation.CODEC.optionalFieldOf("start_jigsaw_name").forGetter(structure -> structure.startJigsawName),
                    Codec.intRange(0, 30).fieldOf("size").forGetter(structure -> structure.size),
                    HeightProvider.CODEC.fieldOf("start_height").forGetter(structure -> structure.startHeight),
                    Heightmap.Types.CODEC.optionalFieldOf("project_start_to_heightmap").forGetter(structure -> structure.projectStartToHeightmap),
                    Codec.intRange(1, 128).fieldOf("max_distance_from_center").forGetter(structure -> structure.maxDistanceFromCenter)
            ).apply(instance, MultilayerJigsawStructure::new));

    public MultilayerJigsawStructure(StructureSettings config, Holder<StructureTemplatePool> startPool, Optional<ResourceLocation> startJigsawName, int size, HeightProvider startHeight, Optional<Heightmap.Types> projectStartToHeightmap, int maxDistanceFromCenter) {
        super(config);
        this.startPool = startPool;
        this.startJigsawName = startJigsawName;
        this.size = size;
        this.startHeight = startHeight;
        this.projectStartToHeightmap = projectStartToHeightmap;
        this.maxDistanceFromCenter = maxDistanceFromCenter;
    }

    @Override
    public GenerationStep.@NotNull Decoration step() {
        return GenerationStep.Decoration.SURFACE_STRUCTURES;
    }

    @Override
    protected @NotNull Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        WorldgenRandom random = context.random();
        int x = context.chunkPos().getMinBlockX() + random.nextInt(16);
        int z = context.chunkPos().getMinBlockZ() + random.nextInt(16);
        WorldGenerationContext heightContext = new WorldGenerationContext(context.chunkGenerator(), context.heightAccessor());
        int y = this.startHeight.sample(random, heightContext);
        NoiseColumn noiseColumn = context.chunkGenerator().getBaseColumn(x, z, context.heightAccessor(), context.randomState());
        int seaLevel = context.chunkGenerator().getSeaLevel();

        BlockPos placementPos = findPlacementPosition(noiseColumn, x, y, z, seaLevel);
        if (placementPos == null) return Optional.empty();

        return JigsawPlacement.addPieces(context, this.startPool, this.startJigsawName, this.size, placementPos,
                false, this.projectStartToHeightmap, this.maxDistanceFromCenter);
    }

    private BlockPos findPlacementPosition(NoiseColumn noiseColumn, int x, int startY, int z, int seaLevel) {
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos(x, startY, z);

        for (int currentY = startY; currentY > seaLevel; currentY--) {
            BlockState currentBlock = noiseColumn.getBlock(currentY);
            BlockState belowBlock = noiseColumn.getBlock(currentY - 1);

            if (currentBlock.isAir() && isSuitableSurface(belowBlock, mutablePos.setY(currentY - 1))) {
                return new BlockPos(x, currentY - 1, z);
            }
        }

        return null;
    }

    private boolean isSuitableSurface(BlockState state, BlockPos pos) {
        return state.is(ElysiumTags.Blocks.MULTILAYER_JIGSAW_NON_SOLID_VALID) || state.isFaceSturdy(EmptyBlockGetter.INSTANCE, pos, Direction.UP);
    }

    @Override
    public @NotNull StructureType<?> type() {
        return ElysiumRegistries.MULTILAYERED_JIGSAW.get();
    }
}