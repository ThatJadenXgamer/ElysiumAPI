package net.jadenxgamer.elysium_api.api.interfaces;

import net.jadenxgamer.elysium_api.api.interfaces.enums.ElysiumBubbleColumnDirection;
import net.jadenxgamer.elysium_api.impl.mixin.block.BlockInvoker;
import net.jadenxgamer.elysium_api.impl.mixin.block.FireBlockAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface IElysiumBlock {

    default BlockState elysiumOnPlayerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        ((BlockInvoker) this).elysium$invokeSpawnDestroyParticles(level, player, pos, state);
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
        Block self = ((Block) this);
        return self == Blocks.FIRE || self == Blocks.LAVA;
    }

    default boolean elysiumIsFertile(BlockState state, BlockGetter level, BlockPos pos) {
        if (state.getBlock() instanceof FarmBlock) {
            return state.getValue(FarmBlock.MOISTURE) > 0;
        } else return false;
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
        } else return ((IElysiumBlock) state.getBlock()).elysiumIsStickyBlock(state) || ((IElysiumBlock) other.getBlock()).elysiumIsStickyBlock(other);
    }

    default boolean elysiumIsFireSource(BlockState state, LevelReader level, BlockPos pos, Direction direction) {
        return state.is(level.dimensionType().infiniburn());
    }

    default int elysiumGetFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        if (state.hasProperty(BlockStateProperties.WATERLOGGED) && state.getValue(BlockStateProperties.WATERLOGGED)) return 0;
        return ((FireBlockAccessor) Blocks.FIRE).elysium$GetBurnOddsMap().getInt(state.getBlock());
    }

    default boolean elysiumIsFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return elysiumGetFlammability(state, level, pos, direction) > 0;
    }

    default int elysiumGetFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        if (state.hasProperty(BlockStateProperties.WATERLOGGED) && state.getValue(BlockStateProperties.WATERLOGGED)) return 0;
        return ((FireBlockAccessor) Blocks.FIRE).elysium$GetIgniteOddsMap().getInt(state.getBlock());
    }

    default void elysiumOnCaughtFire(BlockState state, Level level, BlockPos pos, @Nullable Direction direction, @Nullable LivingEntity igniter) {
    }

    default float elysiumGetExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
        return ((Block) this).getExplosionResistance();
    }

    default float elysiumGetFriction(BlockState state, LevelReader level, BlockPos pos, @Nullable Entity entity) {
        return ((Block) this).getFriction();
    }

    default boolean elysiumIsScaffolding(BlockState state, LevelReader level, BlockPos pos, LivingEntity entity) {
        return state.is(Blocks.SCAFFOLDING);
    }

    default boolean elysiumCanConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        if (state.is(Blocks.REDSTONE_WIRE)) {
            return true;
        } else if (state.is(Blocks.REPEATER)) {
            Direction facing = state.getValue(RepeaterBlock.FACING);
            return facing == direction || facing.getOpposite() == direction;
        } else if (state.is(Blocks.OBSERVER)) {
            return direction == state.getValue(ObserverBlock.FACING);
        } else {
            return state.isSignalSource() && direction != null;
        }
    }

    default @Nullable PushReaction elysiumGetPistonPushReaction(BlockState state) {
        return null;
    }

    default boolean elysiumIsEmpty(BlockState state) {
        return state.is(Blocks.AIR) || state.is(Blocks.CAVE_AIR) || state.is(Blocks.VOID_AIR);
    }

    default ElysiumBubbleColumnDirection elysiumGetBubbleColumnDirection(BlockState state) {
        if (state.is(Blocks.SOUL_SAND)) return ElysiumBubbleColumnDirection.UPWARD;
        else return state.is(Blocks.MAGMA_BLOCK) ? ElysiumBubbleColumnDirection.DOWNWARD : ElysiumBubbleColumnDirection.NONE;
    }
}