package net.jadenxgamer.elysium_api.impl.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.jadenxgamer.elysium_api.api.extensions.IElysiumBlock;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// asphodel::mixin -> loader=fabric -> env=client, file=elysium_api.fabric.mixins.json
@Mixin(LiquidBlockRenderer.class)
public abstract class LiquidBlockRendererFMixin {

    @ModifyExpressionValue(
            method = "tesselate",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/BlockAndTintGetter;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;",
                    ordinal = 7
            )
    )
    private BlockState elysium$overrideOverlayState(BlockState original, @Local(argsOnly = true) BlockAndTintGetter level,
                                                    @Local(name = "blockPos") BlockPos blockPos, @Local(argsOnly = true) FluidState fluidState) {
        boolean vanilla = original.getBlock() instanceof HalfTransparentBlock || original.getBlock() instanceof LeavesBlock;
        boolean elysium = ((IElysiumBlock) original.getBlock()).elysiumShouldDisplayFluidOverlay(original, level, blockPos, fluidState);
        if (elysium == vanilla) return original;
        return elysium ? Blocks.ICE.defaultBlockState() : Blocks.AIR.defaultBlockState();
    }
}