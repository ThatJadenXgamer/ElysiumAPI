package net.jadenxgamer.elysium_api.impl.mixin.block;

import net.jadenxgamer.elysium_api.api.extensions.IElysiumBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseFMixin {

    @Shadow
    protected abstract BlockState asState();

    @Inject(method = "getPistonPushReaction", at = @At("RETURN"), cancellable = true)
    private void elysium$getPistonPushReaction(CallbackInfoReturnable<PushReaction> cir) {
        BlockState state = this.asState();
        PushReaction elysium = ((IElysiumBlock) state.getBlock()).elysiumGetPistonPushReaction(state);
        if (elysium != null) {
            cir.setReturnValue(elysium);
        }
    }

    @Inject(
            method = "getMapColor",
            at = @At("RETURN"), cancellable = true
    )
    private void elysium$getMapColor(BlockGetter level, BlockPos pos, CallbackInfoReturnable<MapColor> cir) {
        BlockState state = this.asState();
        MapColor result = ((IElysiumBlock) state.getBlock()).elysiumGetMapColor(state, level, pos, cir.getReturnValue());
        cir.setReturnValue(result);
    }
}