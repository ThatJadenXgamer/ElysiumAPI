package net.jadenxgamer.elysium_api.impl.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.jadenxgamer.elysium_api.api.extensions.IElysiumBlock;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(LevelRenderer.class)
public abstract class LevelRendererFMixin {

    @Shadow
    @Nullable
    private ClientLevel level;

    @ModifyExpressionValue(
            method = "getLightColor(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)I",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getLightEmission()I")
    )
    private static int elysium$getLightEmission(int original, @Local(argsOnly = true) BlockAndTintGetter level, @Local(argsOnly = true) BlockState state, @Local(argsOnly = true) BlockPos pos) {
        return ((IElysiumBlock) state.getBlock()).elysiumGetLightEmission(state, level, pos);
    }


    @ModifyExpressionValue(
            method = "levelEvent",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;getSoundType()Lnet/minecraft/world/level/block/SoundType;"
            )
    )
    private SoundType elysium$getSoundType(SoundType original, @Local(name = "blockState") BlockState state, @Local(argsOnly = true) BlockPos pos) {
        return ((IElysiumBlock) state.getBlock()).elysiumGetSoundType(state, this.level, pos, null);
    }
}