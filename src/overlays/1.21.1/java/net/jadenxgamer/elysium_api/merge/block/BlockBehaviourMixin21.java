package net.jadenxgamer.elysium_api.merge.block;

import net.jadenxgamer.elysium_api.api.interfaces.IElysiumBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
public abstract class BlockBehaviourMixin21 {

    @Inject(
            method = "useItemOn",
            at = @At("HEAD"), cancellable = true
    )
    private void elysium$useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<ItemInteractionResult> cir) {
        InteractionResult result = ((IElysiumBlock) this).elysiumUseItemOn(stack, state, level, pos, player, hand, hit);
        cir.setReturnValue(toItemInteractionResult(result));
    }

    @Inject(
            method = "useWithoutItem",
            at = @At("HEAD"), cancellable = true
    )
    private void elysium$useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        cir.setReturnValue(((IElysiumBlock)  this).elysiumUseWithoutItem(state, level, pos, player, hit));
    }

    @Inject(
            method = "isPathfindable",
            at = @At("HEAD"), cancellable = true
    )
    private void elysium$isPathfindable(BlockState state, PathComputationType type, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(((IElysiumBlock) this).elysiumIsPathfindable(state, type));
    }

    private static ItemInteractionResult toItemInteractionResult(InteractionResult result) {
        if (result == InteractionResult.SUCCESS) {
            return ItemInteractionResult.SUCCESS;
        }
        if (result == InteractionResult.CONSUME || result == InteractionResult.CONSUME_PARTIAL) {
            return ItemInteractionResult.CONSUME;
        }
        if (result == InteractionResult.FAIL) {
            return ItemInteractionResult.FAIL;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
}