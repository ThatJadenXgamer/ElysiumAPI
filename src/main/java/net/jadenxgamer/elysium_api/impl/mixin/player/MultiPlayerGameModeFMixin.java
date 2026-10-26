package net.jadenxgamer.elysium_api.impl.mixin.player;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.jadenxgamer.elysium_api.api.extensions.IElysiumBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeFMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @ModifyExpressionValue(
            method = "continueDestroyBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;getSoundType()Lnet/minecraft/world/level/block/SoundType;"
            )
    )
    private SoundType elysium$getSoundType(SoundType original, @Local(name = "blockState") BlockState state, @Local(argsOnly = true) BlockPos pos) {
        return ((IElysiumBlock) state.getBlock()).elysiumGetSoundType(state, this.minecraft.level, pos, null);
    }
}