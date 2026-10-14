package net.jadenxgamer.elysium_api.impl.mixin.block;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FireBlock.class)
public interface FireBlockAccessor {

    @Accessor("burnOdds")
    Object2IntMap<Block> elysium$GetBurnOddsMap();

    @Accessor("igniteOdds")
    Object2IntMap<Block> elysium$GetIgniteOddsMap();
}