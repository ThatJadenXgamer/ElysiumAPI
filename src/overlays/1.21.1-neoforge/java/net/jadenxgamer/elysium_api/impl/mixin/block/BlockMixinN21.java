package net.jadenxgamer.elysium_api.impl.mixin.block;

import net.jadenxgamer.elysium_api.api.interfaces.IElysiumBlock;
import net.jadenxgamer.elysium_api.api.interfaces.enums.ElysiumBubbleColumnDirection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.enums.BubbleColumnDirection;
import net.neoforged.neoforge.common.extensions.IBlockExtension;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Block.class)
public abstract class BlockMixinN21 implements IElysiumBlock, IBlockExtension {

    @Override
    public boolean isStickyBlock(BlockState state) {
        return this.elysiumIsStickyBlock(state);
    }

    @Override
    public boolean canStickTo(BlockState state, BlockState other) {
        return this.elysiumCanStickTo(state, other);
    }

    @Override
    public BubbleColumnDirection getBubbleColumnDirection(BlockState state) {
        ElysiumBubbleColumnDirection direction = this.elysiumGetBubbleColumnDirection(state);
        return switch (direction) {
            case UPWARD -> BubbleColumnDirection.UPWARD;
            case DOWNWARD -> BubbleColumnDirection.DOWNWARD;
            case NONE -> BubbleColumnDirection.NONE;
        };
    }

    @Override
    public boolean isBurning(BlockState state, BlockGetter level, BlockPos pos) {
        return this.elysiumIsBurning(state, level, pos);
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return this.elysiumGetFlammability(state, level, pos, direction);
    }

    @Override
    public boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return this.elysiumIsFlammable(state, level, pos, direction);
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return this.elysiumGetFireSpreadSpeed(state, level, pos, direction);
    }

    @Override
    public void onCaughtFire(BlockState state, Level level, BlockPos pos, @Nullable Direction direction, @Nullable LivingEntity igniter) {
        this.elysiumOnCaughtFire(state, level, pos, direction, igniter);
    }

    @Override
    public boolean isFireSource(BlockState state, LevelReader level, BlockPos pos, Direction direction) {
        return this.elysiumIsFireSource(state, level, pos, direction);
    }

    @Override
    public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
        return this.elysiumGetExplosionResistance(state, level, pos, explosion);
    }

    @Override
    public float getFriction(BlockState state, LevelReader level, BlockPos pos, @Nullable Entity entity) {
        return this.elysiumGetFriction(state, level, pos, entity);
    }

    @Override
    public boolean isScaffolding(BlockState state, LevelReader level, BlockPos pos, LivingEntity entity) {
        return this.elysiumIsScaffolding(state, level, pos, entity);
    }

    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        return this.elysiumCanConnectRedstone(state, level, pos, direction);
    }
}