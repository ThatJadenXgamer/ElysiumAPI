// asphodel::merge -> version=1.20.1 -> net.jadenxgamer.elysium_api.merge.block.BlockMixin20
// asphodel::merge -> version=1.21.1 -> net.jadenxgamer.elysium_api.merge.block.BlockMixin21
package net.jadenxgamer.elysium_api.impl.mixin.block;

import net.jadenxgamer.elysium_api.api.extensions.IElysiumBlock;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.BeaconBeamBlock;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Block.class)
public abstract class BlockMixin implements IElysiumBlock, BeaconBeamBlock {
    // refer to the asphodel merge FQNs for the other implementations

    @Override
    public DyeColor getColor() {
        // technically we're not supposed to pass in null here but we kind of HAVE to in order to avoid implementing-
        // the BeaconBeamBlock interface every goddamn time you want to modify the beacon beam just to make Fabric happy.
        return null;
    }
}