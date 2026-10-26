package net.jadenxgamer.elysium_api.impl.mixin.mechanics;

import net.jadenxgamer.elysium_api.api.extensions.IElysiumBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NodeEvaluator.class)
public abstract class NodeEvaluatorMixin {

    @Inject(
            method = "isBurningBlock(Lnet/minecraft/world/level/block/state/BlockState;)Z",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private static void elysium$isBurningBlock(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (((IElysiumBlock) state.getBlock()).elysiumIsBurning(state, EmptyBlockGetter.INSTANCE, BlockPos.ZERO)) cir.setReturnValue(true);
    }
}