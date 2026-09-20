package net.jadenxgamer.elysium_api.impl.core.surface_rules;

import net.jadenxgamer.elysium_api.impl.mixin.biome.NoiseGeneratorSettingsAccessor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.neoforged.fml.ModList;

import java.util.ArrayList;
import java.util.List;

public class ElysiumSurfaceRulesManager {
    public static final List<SurfaceRules.RuleSource> OVERWORLD_SURFACE_RULES = new ArrayList<>();
    public static final List<SurfaceRules.RuleSource> NETHER_SURFACE_RULES = new ArrayList<>();
    public static final List<SurfaceRules.RuleSource> END_SURFACE_RULES = new ArrayList<>();

    public static SurfaceRules.RuleSource getForMergingRules(List<SurfaceRules.RuleSource> dimensionRules, SurfaceRules.RuleSource originalRules) {
        if (dimensionRules.isEmpty()) return null;

        List<SurfaceRules.RuleSource> combinedRules = new ArrayList<>(dimensionRules);
        combinedRules.add(originalRules);
        return SurfaceRules.sequence(combinedRules.toArray(SurfaceRules.RuleSource[]::new));
    }

    public static void handleSurfaceRules(ResourceKey<LevelStem> dimension, NoiseBasedChunkGenerator noiseGenerator) {
        if (ModList.get().isLoaded("terrablender")) return;

        SurfaceRules.RuleSource newRules = null;
        SurfaceRules.RuleSource originalRules = noiseGenerator.settings.value().surfaceRule();

        if (dimension.equals(LevelStem.OVERWORLD)) newRules = ElysiumSurfaceRulesManager.getForMergingRules(ElysiumSurfaceRulesManager.OVERWORLD_SURFACE_RULES, originalRules);
        else if (dimension.equals(LevelStem.NETHER)) newRules = ElysiumSurfaceRulesManager.getForMergingRules(ElysiumSurfaceRulesManager.NETHER_SURFACE_RULES, originalRules);
        else if (dimension.equals(LevelStem.END)) newRules = ElysiumSurfaceRulesManager.getForMergingRules(ElysiumSurfaceRulesManager.END_SURFACE_RULES, originalRules);

        if (newRules != null) ((NoiseGeneratorSettingsAccessor) (Object) noiseGenerator.settings.value()).elysium_api$setSurfaceRule(newRules);
    }
}