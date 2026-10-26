package net.jadenxgamer.elysium_api.impl.mixin.block;

import net.jadenxgamer.elysium_api.api.extensions.IElysiumBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// asphodel::mixin -> loader=!neoforge -> env=common, file=elysium_api.mixins.json
@Mixin(PistonBaseBlock.class)
public abstract class PistonBaseBlockFLMixin {

    @Unique
    private Direction elysium$currentFacing;

    @Unique
    private boolean elysium$currentExtending;

    @Inject(
            method = "moveBlocks",
            at = @At("HEAD")
    )
    private void elysium$captureArgs(Level level, BlockPos pos, Direction facing, boolean extending, CallbackInfoReturnable<Boolean> cir) {
        this.elysium$currentFacing = facing;
        this.elysium$currentExtending = extending;
    }

    @Redirect(
            method = "moveBlocks",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z",
                    ordinal = 1
            ),
            require = 0
    )
    private boolean elysium$onDestroyedByPushReaction(Level level, BlockPos pos, BlockState state, int flags) {
        Direction pushDirection = this.elysium$currentExtending ? this.elysium$currentFacing : this.elysium$currentFacing.getOpposite();
        BlockState actualState = level.getBlockState(pos);
        FluidState fluid = level.getFluidState(pos);
        ((IElysiumBlock) actualState.getBlock()).elysiumOnDestroyedByPushReaction(actualState, level, pos, pushDirection, fluid);
        return true;
    }
}