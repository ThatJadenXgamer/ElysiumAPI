package net.jadenxgamer.elysium_api.impl.mixin.block;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.jadenxgamer.elysium_api.api.extensions.IElysiumBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CropBlock.class)
public abstract class CropBlockFMixin {

    @ModifyExpressionValue(
            method = "getGrowthSpeed",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z",
                    ordinal = 0
            ),
            require = 0
    )
    private static boolean elysium$isValidSoil(boolean original, @Local BlockState blockState, @Local(argsOnly = true) BlockGetter level,
                                               @Local(argsOnly = true) BlockPos pos, @Local(name = "i") int i, @Local(name = "j") int j) {
        if (original) return true;
        BlockPos soilPos = pos.below().offset(i, 0, j);
        return blockState.getBlock() instanceof FarmBlock || ((IElysiumBlock) blockState.getBlock()).elysiumIsFertile(blockState, level, soilPos);
    }

    @ModifyExpressionValue(
            method = "getGrowthSpeed",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;getValue(Lnet/minecraft/world/level/block/state/properties/Property;)Ljava/lang/Comparable;"
            ),
            require = 0
    )
    private static Comparable<?> elysium$getMoisture(Comparable<?> original, @Local BlockState blockState, @Local(argsOnly = true) BlockGetter level,
                                                     @Local(argsOnly = true) BlockPos pos, @Local(name = "i") int i, @Local(name = "j") int j) {
        BlockPos soilPos = pos.below().offset(i, 0, j);
        boolean fertile = ((IElysiumBlock) blockState.getBlock()).elysiumIsFertile(blockState, level, soilPos);
        return fertile ? 1 : 0;
    }
}