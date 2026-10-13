package net.jadenxgamer.elysium_api.api.interfaces;

import net.jadenxgamer.elysium_api.api.interfaces.enums.ElysiumBubbleColumnDirection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface IElysiumBlock {

    default BlockState elysiumOnPlayerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        level.levelEvent(player, 2001, pos, Block.getId(state));
        if (state.is(BlockTags.GUARDED_BY_PIGLINS)) PiglinAi.angerNearbyPiglins(player, false);
        level.gameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Context.of(player, state));
        return state;
    }

    default ItemStack elysiumGetCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return new ItemStack((ItemLike) this);
    }

    default void elysiumAppendHoverText(ItemStack stack, List<Component> tooltip, TooltipFlag flag) {
    }

    default InteractionResult elysiumUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return InteractionResult.PASS;
    }

    default InteractionResult elysiumUseItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        return InteractionResult.PASS;
    }

    default boolean elysiumIsPathfindable(BlockState state, PathComputationType type) {
        return switch (type) {
            case LAND, AIR -> !state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
            case WATER -> state.getFluidState().is(FluidTags.WATER);
        };
    }

    default boolean elysiumIsBurning(BlockState state, BlockGetter level, BlockPos pos) {
        Block self = (Block)(Object) this;
        return self == Blocks.FIRE || self == Blocks.LAVA;
    }

    default boolean elysiumIsFertile(BlockState state, BlockGetter level, BlockPos pos) {
        if (state.getBlock() instanceof FarmBlock) {
            return (Integer)state.getValue(FarmBlock.MOISTURE) > 0;
        } else {
            return false;
        }
    }

    default boolean elysiumIsConduitFrame(BlockState state, LevelReader level, BlockPos pos, BlockPos conduit) {
        return state.getBlock() == Blocks.PRISMARINE || state.getBlock() == Blocks.PRISMARINE_BRICKS || state.getBlock() == Blocks.SEA_LANTERN || state.getBlock() == Blocks.DARK_PRISMARINE;
    }

    default boolean elysiumIsPortalFrame(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(Blocks.OBSIDIAN);
    }

    default boolean elysiumIsSlimeBlock(BlockState state) {
        return state.getBlock() == Blocks.SLIME_BLOCK;
    }

    default boolean elysiumIsStickyBlock(BlockState state) {
        return state.getBlock() == Blocks.SLIME_BLOCK || state.getBlock() == Blocks.HONEY_BLOCK;
    }

    default boolean elysiumCanStickTo(BlockState state, BlockState other) {
        if (state.getBlock() == Blocks.HONEY_BLOCK && other.getBlock() == Blocks.SLIME_BLOCK) {
            return false;
        } else if (state.getBlock() == Blocks.SLIME_BLOCK && other.getBlock() == Blocks.HONEY_BLOCK) {
            return false;
        } else {
            return ((IElysiumBlock) state.getBlock()).elysiumIsStickyBlock(state) || ((IElysiumBlock) other.getBlock()).elysiumIsStickyBlock(other);
        }
    }

    default boolean elysiumIsFireSource(BlockState state, LevelReader level, BlockPos pos, Direction direction) {
        return state.is(level.dimensionType().infiniburn());
    }

    default @Nullable PushReaction elysiumGetPistonPushReaction(BlockState state) {
        return null;
    }

    default boolean elysiumIsEmpty(BlockState state) {
        return state.is(Blocks.AIR) || state.is(Blocks.CAVE_AIR) || state.is(Blocks.VOID_AIR);
    }

    default ElysiumBubbleColumnDirection elysiumGetBubbleColumnDirection(BlockState state) {
        if (state.is(Blocks.SOUL_SAND)) {
            return ElysiumBubbleColumnDirection.UPWARD;
        } else {
            return state.is(Blocks.MAGMA_BLOCK) ? ElysiumBubbleColumnDirection.DOWNWARD : ElysiumBubbleColumnDirection.NONE;
        }
    }
}