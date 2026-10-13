package net.jadenxgamer.elysium_api.impl.mixin.block;

import net.jadenxgamer.elysium_api.api.interfaces.IElysiumBlock;
import net.jadenxgamer.elysium_api.api.interfaces.enums.ElysiumBubbleColumnDirection;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.enums.BubbleColumnDirection;
import net.neoforged.neoforge.common.extensions.IBlockExtension;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Block.class)
public abstract class BlockMixinN21 implements IElysiumBlock, IBlockExtension {

    @Override
    public boolean isStickyBlock(BlockState state) {
        return this.elysiumIsStickyBlock(state);
    }

    @Override
    public boolean canStickTo(BlockState state, BlockState other) {
        return this.elysiumCanStickTo(state, other);
    }

    @Override
    public BubbleColumnDirection getBubbleColumnDirection(BlockState state) {
        ElysiumBubbleColumnDirection direction = this.elysiumGetBubbleColumnDirection(state);
        return switch (direction) {
            case UPWARD -> BubbleColumnDirection.UPWARD;
            case DOWNWARD -> BubbleColumnDirection.DOWNWARD;
            case NONE -> BubbleColumnDirection.NONE;
        };
    }
}