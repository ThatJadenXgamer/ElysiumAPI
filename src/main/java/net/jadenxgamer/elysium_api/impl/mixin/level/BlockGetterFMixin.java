// asphodel::eject -> loader=forge
package net.jadenxgamer.elysium_api.impl.mixin.level;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.jadenxgamer.elysium_api.api.extensions.IElysiumBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BlockGetter.class)
public interface BlockGetterFMixin {

    @Shadow
    BlockState getBlockState(BlockPos p_45571_);

    @ModifyExpressionValue(
            method = "getLightEmission",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getLightEmission()I"), require = 0
    )
    private int elysium$getLightEmission(int original, BlockPos pos) {
        return ((IElysiumBlock) this.getBlockState(pos).getBlock()).elysiumGetLightEmission(this.getBlockState(pos), (BlockGetter) this, pos);
    }
}