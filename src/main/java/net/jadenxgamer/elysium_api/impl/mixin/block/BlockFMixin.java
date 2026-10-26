package net.jadenxgamer.elysium_api.impl.mixin.block;

import net.jadenxgamer.elysium_api.api.extensions.IElysiumBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeaconBeamBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public abstract class BlockFMixin {

    @Inject(
            method = "dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At("HEAD")
    )
    private static void elysium$getExpDrop(BlockState state, Level level, BlockPos pos, @Nullable BlockEntity blockEntity, @Nullable Entity entity, ItemStack tool, CallbackInfo ci) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        int exp = ((IElysiumBlock) state.getBlock()).elysiumGetExpDrop(state, level, pos, blockEntity, entity, tool);
        if (exp > 0) ((BlockAccessor) state.getBlock()).elysium$invokePopExperience(serverLevel, pos, exp);
    }

    @Inject(
            method = "shouldRenderFace",
            at = @At("HEAD"), cancellable = true
    )
    private static void elysium$checkExternalFaceHiding(BlockState state, BlockGetter level, BlockPos offset, Direction face, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        BlockState neighborState = level.getBlockState(pos);
        boolean hides = ((IElysiumBlock) neighborState.getBlock()).elysiumHidesNeighborFace(level, pos, neighborState, state, face.getOpposite());
        if (hides && ((IElysiumBlock) state.getBlock()).elysiumSupportsExternalFaceHiding(state)) cir.setReturnValue(false);
    }
}