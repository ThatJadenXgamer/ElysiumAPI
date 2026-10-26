package net.jadenxgamer.elysium_api.impl.mixin.block.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.jadenxgamer.elysium_api.api.extensions.IElysiumBlock;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PistonMovingBlockEntity.class)
public class PistonMovingBlockEntityFMixin {

    @ModifyExpressionValue(
            method = "moveCollidedEntities",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"
            ),
            require = 0
    )
    private static boolean elysium$isSlimeBlock(boolean original, @Local(argsOnly = true) PistonMovingBlockEntity piston) {
        return ((IElysiumBlock) piston.getMovedState().getBlock()).elysiumIsBouncyLaunch(piston.getMovedState());
    }
}