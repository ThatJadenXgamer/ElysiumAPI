package net.jadenxgamer.elysium_api.impl.mixin.mechanics;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.jadenxgamer.elysium_api.api.extensions.IElysiumBlock;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PistonStructureResolver.class)
public abstract class PistonStructureResolverFMixin {

    @Shadow @Final private Direction pushDirection;

    @Inject(method = "isSticky", at = @At("HEAD"), cancellable = true, require = 0)
    private static void elysium$isSticky(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(((IElysiumBlock) state.getBlock()).elysiumIsStickyBlock(state));
    }

    @WrapOperation(
            method = "addBlockLine",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/piston/PistonStructureResolver;canStickToEachOther(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;)Z"
            ),
            require = 0
    )
    private boolean elysium$wrapBlockLineStick(BlockState state1, BlockState state2, Operation<Boolean> original) {
        Direction faceOnState1 = this.pushDirection.getOpposite();
        Direction faceOnState2 = this.pushDirection;
        return ((IElysiumBlock) state1.getBlock()).elysiumCanStickToFace(state1, state2, faceOnState1, faceOnState2)
                && ((IElysiumBlock) state2.getBlock()).elysiumCanStickToFace(state2, state1, faceOnState2, faceOnState1);
    }

    @WrapOperation(
            method = "addBranchingBlocks",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/piston/PistonStructureResolver;canStickToEachOther(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;)Z"
            ),
            require = 0
    )
    private boolean elysium$wrapBranchingStick(BlockState state1, BlockState state2, Operation<Boolean> original, @Local(name = "direction") Direction faceOnState2) {
        Direction faceOnState1 = faceOnState2.getOpposite();
        return ((IElysiumBlock) state1.getBlock()).elysiumCanStickToFace(state1, state2, faceOnState1, faceOnState2)
                && ((IElysiumBlock) state2.getBlock()).elysiumCanStickToFace(state2, state1, faceOnState2, faceOnState1);
    }
}