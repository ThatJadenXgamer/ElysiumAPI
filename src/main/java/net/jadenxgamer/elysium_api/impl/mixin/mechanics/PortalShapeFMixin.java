package net.jadenxgamer.elysium_api.impl.mixin.mechanics;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.jadenxgamer.elysium_api.api.extensions.IElysiumBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.PortalShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PortalShape.class)
public abstract class PortalShapeFMixin {

    @ModifyExpressionValue(
            method = "method_30487",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"
            ),
            require = 0
    )
    private static boolean elysium$isPortalFrame(boolean original, @Local(argsOnly = true) BlockState blockState,
                                                 @Local(argsOnly = true) BlockGetter blockGetter, @Local(argsOnly = true) BlockPos blockPos) {
        return ((IElysiumBlock) blockState.getBlock()).elysiumIsPortalFrame(blockState, blockGetter, blockPos);
    }
}