package net.jadenxgamer.elysium_api.impl.mixin.compat;

import net.jadenxgamer.elysium_api.ElysiumAPI;
import net.jadenxgamer.elysium_api.impl.core.biome.MosaicBiomeSource;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import terrablender.api.RegionType;
import terrablender.api.SurfaceRuleManager;
import terrablender.worldgen.IExtendedNoiseGeneratorSettings;

import static terrablender.util.LevelUtils.getRegionTypeForDimension;

@Pseudo
@Mixin(targets = "terrablender.util.LevelUtils")
public abstract class LevelUtilsMixin {

    @Inject(
            method = "initializeBiomes",
            at = @At(value = "HEAD")
    )
    private static void elysium_api$initializeBiomesFix(RegistryAccess registryAccess, Holder<DimensionType> dimensionType, ResourceKey<LevelStem> levelResourceKey, ChunkGenerator chunkGenerator, long seed, CallbackInfo ci) {
        /*
         * Dear Terrablender developers, why in the flying fuck are your SurfaceRules tied to the MultiNoiseBiomeSource??? 🥀 🥀 🥀
         * Please look into having surface rules globally apply to a dimension even if they aren't MultiNoise, your API isn't exclusively being used for biome distribution,
         * Many other mods leverage off your systems for their SurfaceRules, since your API is so invasive that it quite literally does not like modifications to surface rules otherwise.
         */
        if (chunkGenerator instanceof NoiseBasedChunkGenerator noiseBasedChunkGenerator) {
            NoiseGeneratorSettings generatorSettings = noiseBasedChunkGenerator.generatorSettings().value();
            if (chunkGenerator.getBiomeSource() instanceof MosaicBiomeSource) {
                RegionType regionType = getRegionTypeForDimension(dimensionType);
                if (regionType != null) {
                    SurfaceRuleManager.RuleCategory ruleCategory = null;
                    switch (regionType) {
                        case OVERWORLD -> ruleCategory = SurfaceRuleManager.RuleCategory.OVERWORLD;
                        case NETHER -> ruleCategory = SurfaceRuleManager.RuleCategory.NETHER;
                    }
                    if (ruleCategory == null) return;
                    ((IExtendedNoiseGeneratorSettings) (Object) generatorSettings).setRuleCategory(ruleCategory);
                    ElysiumAPI.LOGGER.info("MosaicBiomeSource successfully patched surface rules from Terrablender for dimension: '{}'", dimensionType.getRegisteredName());
                }
            }
        }
    }
}
