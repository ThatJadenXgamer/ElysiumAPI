package net.jadenxgamer.elysium_api.impl.mixin.level;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.jadenxgamer.elysium_api.api.extensions.IElysiumBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Level.class)
public abstract class LevelFMixin {

    @ModifyExpressionValue(
            method = "updateNeighbourForOutputSignal",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;isRedstoneConductor(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Z"
            )
    )
    private boolean elysium$getWeakChanges(boolean original, @Local BlockState blockState, @Local(argsOnly = true) BlockPos pos) {
        if (original) return true;
        return ((IElysiumBlock) blockState.getBlock()).elysiumGetWeakChanges(blockState, (LevelReader) this, pos);
    }

    @Redirect(
            method = "updateNeighbourForOutputSignal",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;hasChunkAt(Lnet/minecraft/core/BlockPos;)Z"
            )
    )
    private boolean elysium$onNeighborChange(Level level, BlockPos pos, @Local(argsOnly = true) BlockPos originPos) {
        boolean result = level.hasChunkAt(pos);
        if (result) {
            BlockState neighborState = level.getBlockState(pos);
            ((IElysiumBlock) neighborState.getBlock()).elysiumOnNeighborChange(neighborState, level, pos, originPos);
        }
        return result;
    }

    @Inject(method = "onBlockStateChange", at = @At("HEAD"))
    private void elysium$onBlockStateChange(BlockPos pos, BlockState oldState, BlockState newState, CallbackInfo ci) {
        ((IElysiumBlock) newState.getBlock()).elysiumOnBlockStateChange((LevelReader) this, pos, oldState, newState);
    }
}