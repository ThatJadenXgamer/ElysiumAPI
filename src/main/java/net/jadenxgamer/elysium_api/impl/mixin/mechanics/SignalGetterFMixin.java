// asphodel::eject -> loader=forge
package net.jadenxgamer.elysium_api.impl.mixin.mechanics;

import net.jadenxgamer.elysium_api.api.extensions.IElysiumBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SignalGetter.class)
public interface SignalGetterFMixin extends BlockGetter {

    @Shadow
    int getDirectSignalTo(BlockPos p_277959_);

    @Inject(
            method = "getSignal",
            at = @At("HEAD"), cancellable = true, require = 0
    )
    private void elysium$shouldCheckWeakPower(BlockPos pos, Direction direction, CallbackInfoReturnable<Integer> cir) {
        BlockState state = this.getBlockState(pos);
        int raw = state.getSignal(this, pos, direction);
        boolean check = ((IElysiumBlock) state.getBlock()).elysiumShouldCheckWeakPower(state, (SignalGetter) this, pos, direction);
        cir.setReturnValue(check ? Math.max(raw, this.getDirectSignalTo(pos)) : raw);
    }
}