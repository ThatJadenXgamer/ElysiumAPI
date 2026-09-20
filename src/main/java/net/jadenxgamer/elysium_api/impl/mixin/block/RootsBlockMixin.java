package net.jadenxgamer.elysium_api.impl.mixin.block;

import net.jadenxgamer.elysium_api.api.tags.ElysiumTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.RootsBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RootsBlock.class)
public abstract class RootsBlockMixin {

    @Inject(
            method = "mayPlaceOn",
            at = @At(value = "TAIL"),
            cancellable = true
    )
    private void elysium_api$changeCanPlantOnTop(BlockState state, BlockGetter level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (state.is(ElysiumTags.Blocks.ROOTS_PLANTABLE_ON)) cir.setReturnValue(true);
    }
}
