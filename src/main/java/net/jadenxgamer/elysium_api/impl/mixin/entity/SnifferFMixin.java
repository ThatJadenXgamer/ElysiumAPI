package net.jadenxgamer.elysium_api.impl.mixin.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.jadenxgamer.elysium_api.api.extensions.IElysiumBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// asphodel::mixin -> loader=fabric -> env=common, file=elysium_api.fabric.mixins.json
@Mixin(Sniffer.class)
public abstract class SnifferFMixin {

    @ModifyExpressionValue(
            method = "emitDiggingParticles",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;getSoundType()Lnet/minecraft/world/level/block/SoundType;"
            ),
            require = 0
    )
    private SoundType elysium$getSoundType(SoundType original, @Local(name = "blockPos") BlockPos pos, @Local(name = "blockState") BlockState state) {
        Entity self = (Entity) (Object) this;
        return ((IElysiumBlock) state.getBlock()).elysiumGetSoundType(state, self.level(), pos, self);
    }
}