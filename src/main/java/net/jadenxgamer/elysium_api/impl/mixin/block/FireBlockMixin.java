package net.jadenxgamer.elysium_api.impl.mixin.block;

import com.llamalad7.mixinextras.sugar.Local;
import net.jadenxgamer.elysium_api.api.interfaces.IElysiumBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FireBlock.class)
public abstract class FireBlockMixin {

    @Inject(
            method = "canBurn",
            at = @At("HEAD"), cancellable = true
    )
    private void elysium$canBurn(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(((IElysiumBlock) state.getBlock()).elysiumGetFireSpreadSpeed(state, null, null, Direction.UP) > 0);
    }

    @Inject(
            method = "checkBurnOut",
            at = @At("HEAD"), cancellable = true
    )
    private void elysium$checkBurnOut(Level level, BlockPos pos, int chance, RandomSource random, int age, CallbackInfo ci) {
        BlockState state = level.getBlockState(pos);
        int flammability = ((IElysiumBlock) state.getBlock()).elysiumGetFlammability(state, level, pos, Direction.UP);
        if (random.nextInt(chance) < flammability) {
            ((IElysiumBlock) state.getBlock()).elysiumOnCaughtFire(state, level, pos, Direction.UP, null);
            if (random.nextInt(age + 10) < 5 && !level.isRainingAt(pos)) {
                int newAge = Math.min(age + random.nextInt(5) / 4, 15);
                BlockState newState = net.minecraft.world.level.block.BaseFireBlock.getState(level, pos);
                if (newState.is(Blocks.FIRE)) newState = newState.setValue(FireBlock.AGE, newAge);

                level.setBlock(pos, newState, 3);
            } else level.removeBlock(pos, false);
            if (state.getBlock() instanceof TntBlock) TntBlock.explode(level, pos);
        }
        ci.cancel();
    }

    @Inject(
            method = "getIgniteOdds(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)I",
            at = @At("HEAD"), cancellable = true
    )
    private void elysium$getIgniteOdds(LevelReader level, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        if (!level.isEmptyBlock(pos)) {
            cir.setReturnValue(0);
            return;
        }
        int i = 0;
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            BlockState state = level.getBlockState(neighborPos);
            int spread = ((IElysiumBlock) state.getBlock()).elysiumGetFireSpreadSpeed(state, level, neighborPos, direction.getOpposite());
            i = Math.max(spread, i);
        }
        cir.setReturnValue(i);
    }

    @Redirect(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/tags/TagKey;)Z")
    )
    private boolean elysium$isFireSource(BlockState state, TagKey<Block> tag, @Local(argsOnly = true) ServerLevel level, @Local(argsOnly = true) BlockPos pos) {
        return ((IElysiumBlock) state.getBlock()).elysiumIsFireSource(state, level, pos.below(), Direction.UP);
    }
}