package net.jadenxgamer.elysium_api.impl.mixin.block;

import com.llamalad7.mixinextras.sugar.Local;
import net.jadenxgamer.elysium_api.api.extensions.IElysiumBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BeaconBlockEntity.class)
public abstract class BeaconBlockEntity20Mixin {

    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;getBlock()Lnet/minecraft/world/level/block/Block;"
            ),
            require = 0
    )
    private static Block elysium$getBlockOrNull(BlockState state, @Local(name = "blockPos") BlockPos beamPos,
                                                @Local(argsOnly = true) BlockPos beaconPos, @Local(argsOnly = true) Level level) {
        Block block = state.getBlock();
        Integer color = ((IElysiumBlock) block).elysiumGetBeaconColorMultiplier(state, level, beamPos, beaconPos);
        return color != null ? block : null;
    }

    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/DyeColor;getTextureDiffuseColors()[F"
            )
    )
    private static float[] elysium$getColors(DyeColor color, @Local(name = "blockState") BlockState state, @Local(name = "blockPos") BlockPos beamPos,
                                             @Local(argsOnly = true) Level level, @Local(argsOnly = true) BlockPos beaconPos) {
        Integer packed = ((IElysiumBlock) state.getBlock()).elysiumGetBeaconColorMultiplier(state, level, beamPos, beaconPos);
        if (packed == null) throw new IllegalStateException("Beacon tick reached color lookup for a block with no beacon color. This should obviously not happen.");
        return new float[]{
                ((packed >> 16) & 0xFF) / 255.0F,
                ((packed >> 8) & 0xFF) / 255.0F,
                (packed & 0xFF) / 255.0F
        };
    }
}