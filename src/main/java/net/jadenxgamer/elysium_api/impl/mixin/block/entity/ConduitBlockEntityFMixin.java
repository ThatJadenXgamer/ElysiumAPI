package net.jadenxgamer.elysium_api.impl.mixin.block.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.jadenxgamer.elysium_api.api.extensions.IElysiumBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ConduitBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(ConduitBlockEntity.class)
public abstract class ConduitBlockEntityFMixin {

    @ModifyExpressionValue(
            method = "updateShape",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"
            ),
            require = 0
    )
    private static boolean elysium$isConduitFrame(boolean original, @Local BlockState blockState, @Local(name = "blockPos2") BlockPos framePos,
                                                  @Local(argsOnly = true) Level level, @Local(argsOnly = true) BlockPos conduitPos) {
        return ((IElysiumBlock) blockState.getBlock()).elysiumIsConduitFrame(blockState, level, framePos, conduitPos);
    }

    @Redirect(
            method = "updateShape",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;add(Ljava/lang/Object;)Z"
            ),
            require = 0
    )
    private static boolean elysium$addUnique(List<BlockPos> positions, Object pos) {
        BlockPos framePos = (BlockPos) pos;
        if (positions.contains(framePos)) return true;
        return positions.add(framePos);
    }
}