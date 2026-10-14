package net.jadenxgamer.elysium_api.impl.mixin.block;

import net.jadenxgamer.elysium_api.api.interfaces.IElysiumBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseMixin {

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
}