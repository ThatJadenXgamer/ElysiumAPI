package net.jadenxgamer.elysium_api.impl.mixin.block;

import net.jadenxgamer.elysium_api.api.extensions.IElysiumBlock;
import net.jadenxgamer.elysium_api.api.unification.ElysiumPathType;
import net.jadenxgamer.elysium_api.api.unification.ElysiumPathTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraftforge.common.extensions.IForgeBlock;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Block.class)
public abstract class BlockMixinL20 implements IElysiumBlock, IForgeBlock {

    @Override
    public boolean isStickyBlock(BlockState state) {
        return this.elysiumIsStickyBlock(state);
    }

    @Override
    public boolean canStickTo(BlockState state, BlockState other) {
        return this.elysiumCanStickTo(state, other);
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
    public boolean canDropFromExplosion(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
        return this.elysiumCanDropFromExplosion(state, level, pos, explosion);
    }

    @Override
    public void onBlockExploded(BlockState state, Level level, BlockPos pos, Explosion explosion) {
        this.elysiumOnBlockExploded(state, level, pos, explosion);
    }

    @Override
    public int getExpDrop(BlockState state, LevelReader level, RandomSource randomSource, BlockPos pos, int fortuneLevel, int silkTouchLevel) {
        LevelAccessor accessor = level instanceof LevelAccessor la ? la : null;
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return this.elysiumGetExpDrop(state, accessor, pos, blockEntity, null, ItemStack.EMPTY);
    }

    @Override
    public boolean canEntityDestroy(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
        return this.elysiumCanEntityDestroy(state, level, pos, entity);
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
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        return this.elysiumGetLightEmission(state, level, pos);
    }

    @Override
    public MapColor getMapColor(BlockState state, BlockGetter level, BlockPos pos, MapColor defaultColor) {
        return this.elysiumGetMapColor(state, level, pos, defaultColor);
    }

    @Override
    public SoundType getSoundType(BlockState state, LevelReader level, BlockPos pos, @Nullable Entity entity) {
        return this.elysiumGetSoundType(state, level, pos, entity);
    }

    @Override
    public BlockState rotate(BlockState state, LevelAccessor level, BlockPos pos, Rotation direction) {
        return this.elysiumRotate(state, level, pos, direction);
    }

    @Override
    public boolean shouldCheckWeakPower(BlockState state, SignalGetter level, BlockPos pos, Direction side) {
        return this.elysiumShouldCheckWeakPower(state, level, pos, side);
    }

    @Override
    public boolean getWeakChanges(BlockState state, LevelReader level, BlockPos pos) {
        return this.elysiumGetWeakChanges(state, level, pos);
    }

    @Override
    public void onNeighborChange(BlockState state, LevelReader level, BlockPos pos, BlockPos neighbor) {
        this.elysiumOnNeighborChange(state, level, pos, neighbor);
    }

    @Override
    public boolean isScaffolding(BlockState state, LevelReader level, BlockPos pos, LivingEntity entity) {
        return this.elysiumIsScaffolding(state, level, pos, entity);
    }

    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        return this.elysiumCanConnectRedstone(state, level, pos, direction);
    }

    @Override
    public boolean canHarvestBlock(BlockState state, BlockGetter level, BlockPos pos, Player player) {
        return this.elysiumCanHarvestBlock(state, level, pos, player);
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        this.elysiumOnPlayerWillDestroy(level, pos, state, player);
        return this.elysiumOnDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
    }

    @Override
    public boolean collisionExtendsVertically(BlockState state, BlockGetter level, BlockPos pos, Entity collidingEntity) {
        return this.elysiumCollisionExtendsVertically(state, level, pos, collidingEntity);
    }

    @Override
    public @Nullable BlockPathTypes getBlockPathType(BlockState state, BlockGetter level, BlockPos pos, @Nullable Mob mob) {
        return ElysiumPathTypes.toVanilla(this.elysiumGetBlockPathType(state, level, pos, mob));
    }

    @Override
    public @Nullable BlockPathTypes getAdjacentBlockPathType(BlockState state, BlockGetter level, BlockPos pos, @Nullable Mob mob, BlockPathTypes originalType) {
        ElysiumPathType elysiumOriginal = ElysiumPathTypes.fromVanilla(originalType);
        return ElysiumPathTypes.toVanilla(this.elysiumGetAdjacentBlockPathType(state, level, pos, mob, elysiumOriginal));
    }

    @Override
    public @Nullable float[] getBeaconColorMultiplier(BlockState state, LevelReader level, BlockPos pos, BlockPos beaconPos) {
        Integer packed = this.elysiumGetBeaconColorMultiplier(state, level, pos, beaconPos);
        return packed != null ? new float[]{((packed >> 16) & 0xFF) / 255.0F, ((packed >> 8) & 0xFF) / 255.0F, (packed & 0xFF) / 255.0F} : null;
    }

    @Override
    public boolean hidesNeighborFace(BlockGetter level, BlockPos pos, BlockState state, BlockState neighborState, Direction dir) {
        return this.elysiumHidesNeighborFace(level, pos, state, neighborState, dir);
    }

    @Override
    public boolean supportsExternalFaceHiding(BlockState state) {
        return this.elysiumSupportsExternalFaceHiding(state);
    }

    @Override
    public boolean shouldDisplayFluidOverlay(BlockState state, BlockAndTintGetter level, BlockPos pos, FluidState fluidState) {
        return this.elysiumShouldDisplayFluidOverlay(state, level, pos, fluidState);
    }

    @Override
    public boolean addLandingEffects(BlockState state1, ServerLevel level, BlockPos pos, BlockState state2, LivingEntity entity, int numberOfParticles) {
        return this.elysiumAddLandingEffects(state1, level, pos, entity, numberOfParticles);
    }

    @Override
    public boolean addRunningEffects(BlockState state, Level level, BlockPos pos, Entity entity) {
        return this.elysiumAddRunningEffects(state, level, pos, entity);
    }

    @Override
    public void onBlockStateChange(LevelReader level, BlockPos pos, BlockState oldState, BlockState newState) {
        this.elysiumOnBlockStateChange(level, pos, oldState, newState);
    }
}