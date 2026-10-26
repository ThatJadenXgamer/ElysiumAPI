package net.jadenxgamer.elysium_api.impl.mixin.level;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.jadenxgamer.elysium_api.api.extensions.IElysiumBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ChunkAccess.class)
public abstract class ChunkAccessLMixin {

    @ModifyExpressionValue(
            method = "lambda$findBlockLightSources$2(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;getLightEmission(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)I"
            ),
            require = 0,
            remap = false
    )
    private int elysium$dynamicLightEmission(int original, @Local(argsOnly = true) BlockState state) {
        if (((IElysiumBlock) state.getBlock()).elysiumHasDynamicLightEmission(state)) return 1;
        return original;
    }
}