package net.jadenxgamer.elysium_api.impl.mixin.item;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.jadenxgamer.elysium_api.api.extensions.IElysiumBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BlockItem.class)
public abstract class BlockItemFMixin {

    @ModifyExpressionValue(
            method = "place",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/BlockItem;getPlaceSound(Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/sounds/SoundEvent;"
            )
    )
    private SoundEvent elysium$getPlaceSound(SoundEvent original, @Local(name = "blockState2") BlockState state,
                                             @Local Level level, @Local BlockPos blockPos, @Local(argsOnly = true) BlockPlaceContext context) {
        return ((IElysiumBlock) state.getBlock()).elysiumGetSoundType(state, level, blockPos, context.getPlayer()).getPlaceSound();
    }

    @ModifyExpressionValue(
            method = "place",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;getSoundType()Lnet/minecraft/world/level/block/SoundType;"
            )
    )
    private SoundType elysium$getSoundTypeForVolume(SoundType original, @Local(name = "blockState2") BlockState state,
                                                    @Local Level level, @Local BlockPos blockPos, @Local(argsOnly = true) BlockPlaceContext context) {
        return ((IElysiumBlock) state.getBlock()).elysiumGetSoundType(state, level, blockPos, context.getPlayer());
    }
}