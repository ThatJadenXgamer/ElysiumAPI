package net.jadenxgamer.elysium_api.impl.mixin.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.jadenxgamer.elysium_api.api.extensions.IElysiumBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// asphodel::mixin -> loader=fabric -> env=common, file=elysium_api.fabric.mixins.json
@Mixin(AbstractHorse.class)
public abstract class AbstractHorseFMixin {

    @ModifyExpressionValue(
            method = "playStepSound",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;getSoundType()Lnet/minecraft/world/level/block/SoundType;",
                    ordinal = 0
            ),
            require = 0
    )
    private SoundType elysium$getSoundType1(SoundType original, @Local(argsOnly = true) BlockPos pos, @Local(argsOnly = true) BlockState state) {
        Entity self = (Entity) (Object) this;
        return ((IElysiumBlock) state.getBlock()).elysiumGetSoundType(state, self.level(), pos, self);
    }

    @ModifyExpressionValue(
            method = "playStepSound",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;getSoundType()Lnet/minecraft/world/level/block/SoundType;",
                    ordinal = 1
            ),
            require = 0
    )
    private SoundType elysium$getSoundType2(SoundType original, @Local(argsOnly = true) BlockPos pos, @Local(argsOnly = true) BlockState state) {
        Entity self = (Entity) (Object) this;
        return ((IElysiumBlock) state.getBlock()).elysiumGetSoundType(state, self.level(), pos, self);
    }
}