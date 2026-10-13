package net.jadenxgamer.elysium_api.merge.block;

import net.jadenxgamer.elysium_api.api.interfaces.IElysiumBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.class)
public abstract class BlockBehaviourMixin20 {

    @Inject(
            method = "use",
            at = @At("HEAD"), cancellable = true
    )
    private void elysium$use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        IElysiumBlock self = (IElysiumBlock) this;
        ItemStack stack = player.getItemInHand(hand);
        InteractionResult itemResult = self.elysiumUseItemOn(stack, state, level, pos, player, hand, hit);
        if (itemResult != InteractionResult.PASS) {
            cir.setReturnValue(itemResult);
            return;
        }
        cir.setReturnValue(self.elysiumUseWithoutItem(state, level, pos, player, hit));
    }

    @Inject(
            method = "isPathfindable",
            at = @At("HEAD"), cancellable = true
    )
    private void elysium$isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(((IElysiumBlock) this).elysiumIsPathfindable(state, type));
    }
}