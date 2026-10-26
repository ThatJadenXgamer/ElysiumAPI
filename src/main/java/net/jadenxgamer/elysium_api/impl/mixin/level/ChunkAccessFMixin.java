// asphodel::eject -> loader=forge
package net.jadenxgamer.elysium_api.impl.mixin.level;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.jadenxgamer.elysium_api.api.extensions.IElysiumBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// asphodel::mixin -> loader=fabric -> env=common, file=elysium_api.fabric.mixins.json
@Mixin(ChunkAccess.class)
public abstract class ChunkAccessFMixin implements BlockGetter {

    @ModifyExpressionValue(
            method = "method_51523",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getLightEmission()I")
    )
    private static int elysium$getLightEmission(int original, @Local(argsOnly = true) BlockState state) {
        IElysiumBlock block = (IElysiumBlock) state.getBlock();
        if (block.elysiumHasDynamicLightEmission(state)) return 1;
        return block.elysiumGetLightEmission(state, EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
    }
}