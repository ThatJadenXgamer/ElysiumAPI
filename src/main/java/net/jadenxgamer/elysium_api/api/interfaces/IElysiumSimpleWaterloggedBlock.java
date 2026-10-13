package net.jadenxgamer.elysium_api.api.interfaces;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

public interface IElysiumSimpleWaterloggedBlock extends SimpleWaterloggedBlock {
    // asphodel::patch -> version=1.21.1, id=p21

    default boolean elysiumCanPlaceLiquid(@Nullable Player player, BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
        return fluid == Fluids.WATER;
    }

    default boolean elysiumPlaceLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluidState) {
        if (!(Boolean)state.getValue(BlockStateProperties.WATERLOGGED) && fluidState.getType() == Fluids.WATER) {
            if (!level.isClientSide()) {
                level.setBlock(pos, state.setValue(BlockStateProperties.WATERLOGGED, Boolean.valueOf(true)), 3);
                level.scheduleTick(pos, fluidState.getType(), fluidState.getType().getTickDelay(level));
            }
            return true;
        }
        return false;
    }

    default ItemStack elysiumPickupBlock(@Nullable Player player, LevelAccessor level, BlockPos pos, BlockState state) {
        if (state.getValue(BlockStateProperties.WATERLOGGED)) {
            level.setBlock(pos, state.setValue(BlockStateProperties.WATERLOGGED, Boolean.valueOf(false)), 3);
            if (!state.canSurvive(level, pos)) {
                level.destroyBlock(pos, true);
            }
            return new ItemStack(Items.WATER_BUCKET);
        }
        return ItemStack.EMPTY;
    }

    default Optional<SoundEvent> elysiumGetPickupSound() {
        return Fluids.WATER.getPickupSound();
    }

    default boolean canPlaceLiquid(BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) { // remove*p21
        return this.elysiumCanPlaceLiquid(null, level, pos, state, fluid); // remove*p21
    } // remove*p21

    // mass*p21 play
    // default boolean canPlaceLiquid(@Nullable Player player, BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
    //     return this.elysiumCanPlaceLiquid(player, level, pos, state, fluid);
    // }
    // mass*p21 pause

    default boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluidState) {
        return this.elysiumPlaceLiquid(level, pos, state, fluidState);
    }

    default ItemStack pickupBlock(LevelAccessor level, BlockPos pos, BlockState state) { // remove*p21
        return this.elysiumPickupBlock(null, level, pos, state); // remove*p21
    } // remove*p21

    // mass*p21 play
    // default ItemStack pickupBlock(@Nullable Player player, LevelAccessor level, BlockPos pos, BlockState state) {
    //     return this.elysiumPickupBlock(player, level, pos, state);
    // }
    // mass*p21 pause

    default Optional<SoundEvent> getPickupSound() {
        return this.elysiumGetPickupSound();
    }
}