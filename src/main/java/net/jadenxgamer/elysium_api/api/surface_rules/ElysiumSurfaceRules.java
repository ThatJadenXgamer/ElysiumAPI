package net.jadenxgamer.elysium_api.api.surface_rules;

import net.jadenxgamer.elysium_api.api.surface_rules.condition.BiomeTagConditionSource;
import net.jadenxgamer.elysium_api.api.surface_rules.condition.BlockMatchConditionSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;

public class ElysiumSurfaceRules {

    public static BiomeTagConditionSource isBiomeTag(TagKey<Biome> biomeTags) {
        return new BiomeTagConditionSource(biomeTags);
    }

    public static BlockMatchConditionSource doesBlockMatch(Block block) {
        return new BlockMatchConditionSource(block);
    }
}
