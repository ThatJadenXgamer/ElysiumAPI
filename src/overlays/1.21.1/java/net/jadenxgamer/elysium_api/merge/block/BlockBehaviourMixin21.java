package net.jadenxgamer.elysium_api.merge.block;

import net.jadenxgamer.elysium_api.api.extensions.IElysiumBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
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
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.class)
public abstract class BlockBehaviourMixin21 {

    @Shadow
    protected abstract void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random);

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
        cir.setReturnValue(((IElysiumBlock) this).elysiumUseWithoutItem(state, level, pos, player, hit));
    }

    @Inject(
            method = "isPathfindable",
            at = @At("HEAD"), cancellable = true
    )
    private void elysium$isPathfindable(BlockState state, PathComputationType type, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(((IElysiumBlock) this).elysiumIsPathfindable(state, type));
    }

    @Inject(
            method = "randomTick",
            at = @At("HEAD"), cancellable = true
    )
    private void elysium$randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        this.tick(state, level, pos, random); // 1.20.1 parity
        ci.cancel();
    }

    private static ItemInteractionResult toItemInteractionResult(InteractionResult result) {
        if (result == InteractionResult.SUCCESS) return ItemInteractionResult.SUCCESS;
        if (result == InteractionResult.CONSUME || result == InteractionResult.CONSUME_PARTIAL) return ItemInteractionResult.CONSUME;
        if (result == InteractionResult.FAIL) return ItemInteractionResult.FAIL;

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
}