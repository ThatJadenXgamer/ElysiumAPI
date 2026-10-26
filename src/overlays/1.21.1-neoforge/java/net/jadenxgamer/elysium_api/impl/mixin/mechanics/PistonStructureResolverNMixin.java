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

@Mixin(PistonStructureResolver.class)
public abstract class PistonStructureResolverNMixin {

    @Shadow @Final private Direction pushDirection;

    @WrapOperation(
            method = "addBlockLine",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;canStickTo(Lnet/minecraft/world/level/block/state/BlockState;)Z",
                    ordinal = 0
            ),
            require = 0
    )
    private boolean elysium$wrapBlockLineStick1(BlockState oldState, BlockState blockstate, Operation<Boolean> original) {
        Direction face = this.pushDirection.getOpposite();
        return ((IElysiumBlock) oldState.getBlock()).elysiumCanStickToFace(oldState, blockstate, face, face.getOpposite());
    }

    @WrapOperation(
            method = "addBlockLine",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;canStickTo(Lnet/minecraft/world/level/block/state/BlockState;)Z",
                    ordinal = 1
            ),
            require = 0
    )
    private boolean elysium$wrapBlockLineStick2(BlockState blockstate, BlockState oldState, Operation<Boolean> original) {
        Direction face = this.pushDirection;
        return ((IElysiumBlock) blockstate.getBlock()).elysiumCanStickToFace(blockstate, oldState, face, face.getOpposite());
    }

    @WrapOperation(
            method = "addBranchingBlocks",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;canStickTo(Lnet/minecraft/world/level/block/state/BlockState;)Z",
                    ordinal = 0
            ),
            require = 0
    )
    private boolean elysium$wrapBranchingStick1(BlockState blockstate1, BlockState blockstate, Operation<Boolean> original, @Local Direction direction) {
        Direction face = direction.getOpposite();
        return ((IElysiumBlock) blockstate1.getBlock()).elysiumCanStickToFace(blockstate1, blockstate, face, face.getOpposite());
    }

    @WrapOperation(
            method = "addBranchingBlocks",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;canStickTo(Lnet/minecraft/world/level/block/state/BlockState;)Z",
                    ordinal = 1
            ),
            require = 0
    )
    private boolean elysium$wrapBranchingStick2(BlockState blockstate, BlockState blockstate1, Operation<Boolean> original, @Local Direction direction) {
        return ((IElysiumBlock) blockstate.getBlock()).elysiumCanStickToFace(blockstate, blockstate1, direction, direction.getOpposite());
    }
}