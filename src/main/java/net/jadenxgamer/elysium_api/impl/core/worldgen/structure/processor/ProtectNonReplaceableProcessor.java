package net.jadenxgamer.elysium_api.impl.core.worldgen.structure.processor;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.jadenxgamer.elysium_api.impl.registry.ElysiumRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jetbrains.annotations.Nullable;

public class ProtectNonReplaceableProcessor extends StructureProcessor {

    private final HolderSet<Block> blacklist;

    public ProtectNonReplaceableProcessor(HolderSet<Block> blacklist) {
        this.blacklist = blacklist;
    }

    public static final Codec<ProtectNonReplaceableProcessor> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("blacklist").forGetter(p -> p.blacklist)
    ).apply(instance, ProtectNonReplaceableProcessor::new));

    @Override
    public @Nullable StructureTemplate.StructureBlockInfo processBlock(LevelReader level, BlockPos offset, BlockPos pos, StructureTemplate.StructureBlockInfo blockInfo, StructureTemplate.StructureBlockInfo relativeBlockInfo, StructurePlaceSettings settings) {
        BlockState existingState = level.getBlockState(relativeBlockInfo.pos());
        return level.getBlockState(relativeBlockInfo.pos()).canBeReplaced() || blacklistedBlock(existingState) ? relativeBlockInfo : null;
    }

    private boolean blacklistedBlock(BlockState state) {
        return blacklist.contains(state.getBlockHolder());
    }


    @Override
    protected StructureProcessorType<?> getType() {
        return ElysiumRegistries.PROTECT_NON_REPLACEABLE.get();
    }
}