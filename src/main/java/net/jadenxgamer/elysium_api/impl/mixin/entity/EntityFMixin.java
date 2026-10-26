package net.jadenxgamer.elysium_api.impl.mixin.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.jadenxgamer.elysium_api.api.extensions.IElysiumBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityFMixin {

    @Shadow
    @Deprecated
    public abstract BlockPos getOnPosLegacy();

    @Shadow
    private Level level;

    @ModifyExpressionValue(
            method = "playStepSound",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;getSoundType()Lnet/minecraft/world/level/block/SoundType;"
            )
    )
    private SoundType elysium$getSoundTypeStep(SoundType original, @Local(argsOnly = true) BlockPos pos, @Local(argsOnly = true) BlockState state) {
        Entity self = (Entity) (Object) this;
        return ((IElysiumBlock) state.getBlock()).elysiumGetSoundType(state, self.level(), pos, self);
    }

    @ModifyExpressionValue(
            method = "playMuffledStepSound",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;getSoundType()Lnet/minecraft/world/level/block/SoundType;"
            ),
            require = 0
    )
    private SoundType elysium$getSoundTypeMuffled(SoundType original, @Local(argsOnly = true) BlockState state) {
        Entity self = (Entity) (Object) this;
        return ((IElysiumBlock) state.getBlock()).elysiumGetSoundType(state, self.level(), self.getOnPos(), self);
    }

    @ModifyExpressionValue(
            method = "playCombinationStepSounds",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;getSoundType()Lnet/minecraft/world/level/block/SoundType;"
            ),
            require = 0
    )
    private SoundType elysium$getSoundTypeCombination(SoundType original, @Local(name = "primaryState") BlockState primaryState) {
        Entity self = (Entity) (Object) this;
        return ((IElysiumBlock) primaryState.getBlock()).elysiumGetSoundType(primaryState, self.level(), self.getOnPos(), self);
    }

    @Redirect(
            method = "playCombinationStepSounds",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;playMuffledStepSound(Lnet/minecraft/world/level/block/state/BlockState;)V"
            ),
            require = 0
    )
    private void elysium$redirectMuffledInCombination(Entity instance, BlockState secondaryState) {
        Entity self = (Entity) (Object) this;
        BlockPos secondaryPos = self.getOnPos().below();
        SoundType soundType = ((IElysiumBlock) secondaryState.getBlock()).elysiumGetSoundType(secondaryState, self.level(), secondaryPos, self);
        self.playSound(soundType.getStepSound(), soundType.getVolume() * 0.05F, soundType.getPitch() * 0.8F);
    }

    @Inject(
            method = "spawnSprintParticle",
            at = @At("HEAD"),
            cancellable = true)

    private void elysium$addRunningEffects(CallbackInfo ci) {
        BlockPos pos = this.getOnPosLegacy();
        BlockState state = this.level.getBlockState(pos);
        if (((IElysiumBlock) state.getBlock()).elysiumAddRunningEffects(state, this.level, pos, (Entity) (Object) this)) ci.cancel();
    }
}