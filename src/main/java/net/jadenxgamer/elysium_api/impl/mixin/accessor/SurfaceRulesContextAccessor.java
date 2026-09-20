package net.jadenxgamer.elysium_api.impl.mixin.accessor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.SurfaceRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.function.Supplier;

@Mixin(SurfaceRules.Context.class)
public interface SurfaceRulesContextAccessor {
    @Accessor
    BlockPos.MutableBlockPos getPos();

    @Accessor
    Supplier<Holder<Biome>> getBiome();

    @Accessor
    int getBlockY();

    @Accessor
    int getStoneDepthBelow();

    @Accessor
    int getWaterHeight();

    @Accessor
    int getStoneDepthAbove();

    @Accessor
    long getLastUpdateY();

    @Accessor
    ChunkAccess getChunk();

    @Accessor
    RandomState getRandomState();

    @Accessor
    int getSurfaceDepth();

    @Accessor
    int getBlockZ();

    @Accessor
    int getBlockX();
}