// asphodel::merge -> version=1.20.1 -> net.jadenxgamer.elysium_api.merge.block.BlockMixin20
// asphodel::merge -> version=1.21.1 -> net.jadenxgamer.elysium_api.merge.block.BlockMixin21
package net.jadenxgamer.elysium_api.impl.mixin.block;

import net.jadenxgamer.elysium_api.api.interfaces.IElysiumBlock;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Block.class)
public abstract class BlockMixin implements IElysiumBlock {}
// refer to the merges for actual implementations