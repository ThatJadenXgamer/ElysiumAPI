package net.jadenxgamer.elysium_api.impl.mixin.mechanics;

import net.jadenxgamer.elysium_api.api.extensions.IElysiumBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(ExplosionDamageCalculator.class)
public abstract class ExplosionDamageCalculatorFMixin {

    @Inject(
            method = "getBlockExplosionResistance",
            at = @At("HEAD"), cancellable = true
    )
    private void elysium$getBlockExplosionResistance(Explosion explosion, BlockGetter level, BlockPos pos, BlockState state, FluidState fluid, CallbackInfoReturnable<Optional<Float>> cir) {
        if (state.isAir()) {
            cir.setReturnValue(Optional.empty());
            return;
        }
        float resistance = ((IElysiumBlock) state.getBlock()).elysiumGetExplosionResistance(state, level, pos, explosion);
        cir.setReturnValue(Optional.of(Math.max(resistance, fluid.getExplosionResistance())));
    }
}