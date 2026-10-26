package net.jadenxgamer.elysium_api.impl.mixin.level;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.jadenxgamer.elysium_api.api.extensions.IElysiumBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// asphodel::mixin -> loader=fabric -> env=common, file=elysium_api.fabric.mixins.json
@Mixin(LevelChunkSection.class)
public abstract class LevelChunkSelectionFMixin {

    @ModifyExpressionValue(
            method = "setBlockState(IIILnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;isAir()Z",
                    ordinal = 0
            ),
            require = 0
    )
    private static boolean elysium$isEmpty1(boolean original, @Local(ordinal = 1) BlockState oldState) {
        return ((IElysiumBlock) oldState.getBlock()).elysiumIsEmpty(oldState);
    }

    @ModifyExpressionValue(
            method = "setBlockState(IIILnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;isAir()Z",
                    ordinal = 1
            ),
            require = 0
    )
    private static boolean elysium$isEmpty2(boolean original, @Local(argsOnly = true) BlockState newState) {
        return ((IElysiumBlock) newState.getBlock()).elysiumIsEmpty(newState);
    }
}