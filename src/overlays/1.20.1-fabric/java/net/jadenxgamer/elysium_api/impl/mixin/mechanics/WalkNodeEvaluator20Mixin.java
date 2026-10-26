package net.jadenxgamer.elysium_api.impl.mixin.mechanics;

import net.jadenxgamer.elysium_api.api.extensions.IElysiumBlock;
import net.jadenxgamer.elysium_api.api.unification.ElysiumPathType;
import net.jadenxgamer.elysium_api.api.unification.ElysiumPathTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WalkNodeEvaluator.class)
public abstract class WalkNodeEvaluator20Mixin {

    @Inject(method = "getBlockPathTypeRaw", at = @At("RETURN"), cancellable = true)
    private static void elysium$getBlockPathType(BlockGetter level, BlockPos pos, CallbackInfoReturnable<BlockPathTypes> cir) {
        BlockState state = level.getBlockState(pos);
        ElysiumPathType custom = ((IElysiumBlock) state.getBlock()).elysiumGetBlockPathType(state, level, pos, null);
        if (custom != null) cir.setReturnValue(ElysiumPathTypes.toVanilla(custom));
    }

    @Inject(method = "checkNeighbourBlocks", at = @At("HEAD"), cancellable = true)
    private static void elysium$checkNeighbourBlocks(BlockGetter level, BlockPos.MutableBlockPos centerPos, BlockPathTypes nodeType, CallbackInfoReturnable<BlockPathTypes> cir) {
        int ci = centerPos.getX();
        int cj = centerPos.getY();
        int ck = centerPos.getZ();
        ElysiumPathType original = ElysiumPathTypes.fromVanilla(nodeType);
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                for (int k = -1; k <= 1; k++) {
                    if (i != 0 || k != 0) {
                        BlockPos neighborPos = new BlockPos(ci + i, cj + j, ck + k);
                        BlockState neighborState = level.getBlockState(neighborPos);
                        ElysiumPathType custom = ((IElysiumBlock) neighborState.getBlock()).elysiumGetAdjacentBlockPathType(neighborState, level, neighborPos, null, original);
                        if (custom != null) {
                            cir.setReturnValue(ElysiumPathTypes.toVanilla(custom));
                            return;
                        }
                    }
                }
            }
        }
    }
}