package net.jadenxgamer.elysium_api.impl.mixin.mechanics;

import net.jadenxgamer.elysium_api.api.extensions.IElysiumBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PistonStructureResolver.class)
public abstract class PistonStructureResolverLMixin {

    @Shadow @Final private Direction pushDirection;

    @Redirect(
            method = "addBlockLine",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;canStickTo(Lnet/minecraft/world/level/block/state/BlockState;)Z",
                    ordinal = 0
            )
    )
    private boolean elysium$wrapBlockLineStick1(BlockState oldState, BlockState blockstate) {
        Direction face = this.pushDirection.getOpposite();
        return ((IElysiumBlock) oldState.getBlock()).elysiumCanStickToFace(oldState, blockstate, face, face.getOpposite());
    }

    @Redirect(
            method = "addBlockLine",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;canStickTo(Lnet/minecraft/world/level/block/state/BlockState;)Z",
                    ordinal = 1
            )
    )
    private boolean elysium$wrapBlockLineStick2(BlockState blockstate, BlockState oldState) {
        Direction face = this.pushDirection;
        return ((IElysiumBlock) blockstate.getBlock()).elysiumCanStickToFace(blockstate, oldState, face, face.getOpposite());
    }

    @Redirect(
            method = "addBranchingBlocks",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/piston/PistonStructureResolver;addBlockLine(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)Z"
            )
    )
    private boolean elysium$wrapBranchingAddBlockLine(PistonStructureResolver instance, BlockPos blockpos, Direction direction) {
        Level level = ((PistonStructureResolverLAccess) instance).elysium$getLevel();
        BlockPos fromPos = blockpos.relative(direction.getOpposite());
        BlockState blockstate1 = level.getBlockState(blockpos);
        BlockState blockstate = level.getBlockState(fromPos);

        boolean selfSticks = ((IElysiumBlock) blockstate1.getBlock()).elysiumCanStickToFace(blockstate1, blockstate, direction.getOpposite(), direction);
        boolean otherSticks = ((IElysiumBlock) blockstate.getBlock()).elysiumCanStickToFace(blockstate, blockstate1, direction, direction.getOpposite());

        if (!selfSticks || !otherSticks) return true;

        return ((PistonStructureResolverLAccess) instance).elysium$invokeAddBlockLine(blockpos, direction);
    }
}