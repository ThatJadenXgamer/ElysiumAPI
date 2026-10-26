package net.jadenxgamer.elysium_api.api.extensions;

import net.jadenxgamer.elysium_api.api.extensions.enums.ElysiumBubbleColumnDirection;
import net.jadenxgamer.elysium_api.api.unification.ElysiumDyeColors;
import net.jadenxgamer.elysium_api.api.util.ElysiumExplosionHelper;
import net.jadenxgamer.elysium_api.api.unification.ElysiumPathType;
import net.jadenxgamer.elysium_api.impl.mixin.block.BlockAccessor;
import net.jadenxgamer.elysium_api.impl.mixin.block.FireBlockAccessor;
import net.jadenxgamer.elysium_api.api.unification.IElysiumExplosion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * A cross-loader and cross-version extension interface for {@code Block} that
 * unifies the previously fragmented block behavior APIs into a single
 * consistent surface.
 * <p>
 * Both Forge and NeoForge ship their own extension interfaces with overlapping
 * but incompatible methods, while Fabric exposes no equivalent hooks at all and
 * vanilla itself inlines many block interactions directly at their callsites.
 * This interface collapses all three cases into one set of {@code elysium*}
 * methods that are consulted regardless of which loader or Minecraft version is
 * running underneath.
 * <p>
 * In practice this means mod developers writing a {@code Block} subclass can override
 * any one of these methods and have that override take effect on Fabric, Forge, and
 * NeoForge, on both 1.20.1 and 1.21.1. Our unified methods are wired up through
 * three complementary pipelines:
 * <ul>
 *     <li>On NeoForge and Forge, the loader's own extension interface methods
 *     delegate here, so mods written against either loader's established
 *     conventions continue to work without any changes. Additionally, some methods on
 *     Forge-side got modernized to receive parity from Neo's implementations and standards.</li>
 *     <li>On Fabric, vanilla callsites that would otherwise never consult these
 *     behaviors are patched to route through the same methods, granting
 *     Fabric parity with the extension-only features that were previously
 *     exclusive to Forge-family loaders.</li>
 * </ul>
 * On every loader, vanilla behavior is preserved by default, so
 * overriding an {@code elysium*} method is purely additive.</li>
 * Furthermore it may be safer to do so on Asphodel in some cases where the vanilla
 * implementation of some methods may differ slightly across versions.
 */
public interface IElysiumBlock {

    private Block self() {
        return (Block) this;
    }

    /**
     * Called when a player breaks this block, before the block is removed.
     * The default implementation spawns destroy particles, angers nearby
     * piglins when applicable, and fires the block destruction game event.
     * <br>
     * <li>Original Analog: {@code Block.playerWillDestroy}
     * <li>Purpose: Cross-version abstraction
     *
     * @param level the level
     * @param pos the position in the level
     * @param state the current state
     * @param player the player breaking the block
     * @return the state to pass forward; returning the input unchanged is safe
     */
    default BlockState elysiumOnPlayerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        ((BlockAccessor) this).elysium$invokeSpawnDestroyParticles(level, player, pos, state);
        if (state.is(BlockTags.GUARDED_BY_PIGLINS)) PiglinAi.angerNearbyPiglins(player, false);
        level.gameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Context.of(player, state));
        return state;
    }

    /**
     * Returns the item stack obtained by middle-clicking (pick block interaction)
     * on this block.
     * <br>
     * <li>Original Analog: {@code Block.getCloneItemStack}
     * <li>Purpose: Cross-version abstraction
     *
     * @param level the level
     * @param pos the position in the level
     * @param state the current state
     * @return the item stack to display for pick block; defaults to the block's item form
     */
    default ItemStack elysiumGetCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return new ItemStack((ItemLike) this);
    }

    /**
     * Appends lines to the item tooltip of this block's item form.
     * <br>
     * <li>Original Analog: {@code Block.appendHoverText}
     * <li>Purpose: Cross-version abstraction
     *
     * @param stack the item stack being rendered
     * @param level the level the tooltip is being rendered in, may be null
     * @param tooltip the list of tooltip lines to append to
     * @param flag tooltip flag carrying the advanced flag and context
     */
    default void elysiumAppendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
    }

    /**
     * Handles right-click interaction when the used item does not intercept
     * the interaction itself.
     * <br>
     * <li>Original Analog: {@code BlockBehaviour.useWithoutItem} (1.21.1)
     * <li>Purpose: Cross-version abstraction
     *
     * @param state the current state
     * @param level the level
     * @param pos the position in the level
     * @param player the interacting player
     * @param hitResult the hit result of the interaction
     * @return the interaction result; {@link InteractionResult#PASS} by default
     */
    default InteractionResult elysiumUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return InteractionResult.PASS;
    }

    /**
     * Handles right-click interaction with a specific item stack held by
     * the player.
     * <br>
     * <li>Original Analog: {@code BlockBehaviour.useItemOn} (1.21.1)
     * <li>Purpose: Cross-version abstraction
     *
     * @param stack the item stack held by the player
     * @param state the current state
     * @param level the level
     * @param pos the position in the level
     * @param player the interacting player
     * @param hand the hand holding the item
     * @param hitResult the hit result of the interaction
     * @return the interaction result; {@link InteractionResult#PASS} by default
     */
    default InteractionResult elysiumUseItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        return InteractionResult.PASS;
    }

    /**
     * Whether this block is traversable by mob pathfinding for the given
     * movement type.
     * <br>
     * <li>Original Analog: {@code BlockBehaviour.isPathfindable}
     * <li>Purpose: Cross-version abstraction
     *
     * @param state the current state
     * @param type the path computation type being queried
     * @return true if this block can be traversed for the given type
     */
    default boolean elysiumIsPathfindable(BlockState state, PathComputationType type) {
        return switch (type) {
            case LAND, AIR -> !state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
            case WATER -> state.getFluidState().is(FluidTags.WATER);
        };
    }

    /**
     * Called when this block is caught in an explosion, before the block is
     * removed. Full control over the drop pipeline is provided through the
     * {@code dropConsumer}, and block destruction is deferred to
     * {@link #elysiumOnBlockExploded} so the two stages can be customized
     * independently. The default implementation matches 1.21.1's
     * onExplosionHit exactly.
     * <br>
     * On 1.21.1 this is wired through BlockBehaviour's native extension call;
     * on 1.20.1 vanilla's explosion finalization loop is redirected to invoke
     * this method so both versions share one unified surface.
     * <br>
     * <li>Original Analog: {@code BlockState.onExplosionHit} (1.21.1)
     * <li>Purpose: Cross-version backport.
     *
     * @param state the current state
     * @param level the level
     * @param pos the position in the level
     * @param explosion the explosion hitting this block
     * @param dropConsumer the consumer that receives this block's drops
     */
    default void elysiumOnExplosionHit(BlockState state, Level level, BlockPos pos, Explosion explosion, BiConsumer<ItemStack, BlockPos> dropConsumer) {
        if (!state.isAir() && ElysiumExplosionHelper.elysiumGetBlockInteraction(explosion) != IElysiumExplosion.ElysiumBlockInteraction.TRIGGER_BLOCK) {
            Block block = state.getBlock();
            boolean flag = explosion.getIndirectSourceEntity() instanceof Player;
            if (((IElysiumBlock) block).elysiumCanDropFromExplosion(state, level, pos, explosion) && level instanceof ServerLevel serverlevel) {
                BlockEntity blockEntity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
                LootParams.Builder builder = new LootParams.Builder(serverlevel)
                        .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                        .withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
                        .withOptionalParameter(LootContextParams.BLOCK_ENTITY, blockEntity)
                        .withOptionalParameter(LootContextParams.THIS_ENTITY, explosion.getDirectSourceEntity());
                if (ElysiumExplosionHelper.elysiumGetBlockInteraction(explosion) == IElysiumExplosion.ElysiumBlockInteraction.DESTROY_WITH_DECAY) {
                    builder.withParameter(LootContextParams.EXPLOSION_RADIUS, ElysiumExplosionHelper.elysiumGetRadius(explosion));
                }
                state.spawnAfterBreak(serverlevel, pos, ItemStack.EMPTY, flag);
                state.getDrops(builder).forEach(stack -> dropConsumer.accept(stack, pos));
            }
            ((IElysiumBlock) block).elysiumOnBlockExploded(state, level, pos, explosion);
        }
    }

    /**
     * Whether contact with this block damages entities as if it were fire.
     * <br>
     * <li>Original Analog: {@code BlockState.isBurning}
     * <li>Purpose: Forge and NeoForge extension parity.
     *
     * @param state the current state
     * @param level the level
     * @param pos the position in the level
     * @return true if entities touching this block take fire damage
     */
    default boolean elysiumIsBurning(BlockState state, BlockGetter level, BlockPos pos) {
        Block self = self();
        return self == Blocks.FIRE || self == Blocks.LAVA;
    }

    /**
     * Whether this block counts as fertile soil, granting growth rate
     * bonuses to crops planted on top of it.
     * <br>
     * <li>Original Analog: {@code BlockState.isFertile}
     * <li>Purpose: Forge and NeoForge extension parity.
     *
     * @param state the current state
     * @param level the level
     * @param pos the position in the level
     * @return true if this block fertilizes crops on top
     */
    default boolean elysiumIsFertile(BlockState state, BlockGetter level, BlockPos pos) {
        if (state.getBlock() instanceof FarmBlock) {
            return state.getValue(FarmBlock.MOISTURE) > 0;
        } else return false;
    }

    /**
     * Whether this block can act as a frame for an active conduit.
     * <br>
     * <li>Original Analog: {@code BlockState.isConduitFrame}
     * <li>Purpose: Forge and NeoForge extension parity.
     *
     * @param state the current state
     * @param level the level reader
     * @param pos the position in the level
     * @param conduit the position of the conduit being evaluated
     * @return true if this block counts as a conduit frame
     */
    default boolean elysiumIsConduitFrame(BlockState state, LevelReader level, BlockPos pos, BlockPos conduit) {
        return state.getBlock() == Blocks.PRISMARINE || state.getBlock() == Blocks.PRISMARINE_BRICKS || state.getBlock() == Blocks.SEA_LANTERN || state.getBlock() == Blocks.DARK_PRISMARINE;
    }

    /**
     * Whether this block can act as a nether portal frame.
     * <br>
     * <li>Original Analog: {@code BlockState.isPortalFrame}
     * <li>Purpose: Forge and NeoForge extension parity.
     *
     * @param state the current state
     * @param level the level
     * @param pos the position in the level
     * @return true if this block counts as a nether portal frame
     */
    default boolean elysiumIsPortalFrame(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(Blocks.OBSIDIAN);
    }

    /**
     * Whether this block bouncy launches entities when it is attached to the end of a piston and collides with an entity.
     * <br>
     * NOTE: this method's name was altered on our end to better reflect its actual behavior and purpose;
     * as the Forge & NeoForge docs were misrepresenting what this method was actually hooked up to.
     * <br>
     * <li>Original Analog: {@code BlockState.isSlimeBlock}
     * <li>Purpose: Elysium expanded Forge and NeoForge extension parity.
     *
     * @param state the current state
     * @return true if this block bouncy launches entities when pushed
     */
    default boolean elysiumIsBouncyLaunch(BlockState state) {
        return state.getBlock() == Blocks.SLIME_BLOCK;
    }

    /**
     * Whether this block is sticky for piston push and pull behavior.
     * <br>
     * <li>Original Analog: {@code BlockState.isStickyBlock}
     * <li>Purpose: Forge and NeoForge extension parity.
     *
     * @param state the current state
     * @return true if this block sticks to adjacent blocks during piston movement
     */
    default boolean elysiumIsStickyBlock(BlockState state) {
        return state.getBlock() == Blocks.SLIME_BLOCK || state.getBlock() == Blocks.HONEY_BLOCK;
    }

    /**
     * Determines whether {@code state} may stick to {@code other} when the two blocks are
     * adjacent. This is the base entry point that {@link #elysiumCanStickToFace} falls back to
     * when no per-face override is present, so third-party mods that override this method continue to have
     * their implementation honored across all three loaders.
     * <br>
     * <li>Original Analog: {@code BlockState.canStickTo} 
     * <li>Purpose: Forge and NeoForge extension parity.
     *
     * @param state the current state
     * @param other the other block state being evaluated
     * @return true if this block may stick to the other
     */
    default boolean elysiumCanStickTo(BlockState state, BlockState other) {
        if (state.getBlock() == Blocks.HONEY_BLOCK && other.getBlock() == Blocks.SLIME_BLOCK) {
            return false;
        } else if (state.getBlock() == Blocks.SLIME_BLOCK && other.getBlock() == Blocks.HONEY_BLOCK) {
            return false;
        } else return ((IElysiumBlock) state.getBlock()).elysiumIsStickyBlock(state) || ((IElysiumBlock) other.getBlock()).elysiumIsStickyBlock(other);
    }

    /**
     * Face-aware version of the piston stickiness check. Determines whether
     * {@code selfState} may stick to {@code otherState} when the two blocks are
     * adjacent, taking the touching faces into account. This method replaces the
     * usual Forge and NeoForge {@code canStickTo} with a per-face variant that
     * resolves consistently across all three loaders.
     * <br>
     * NOTE: The default implementation delegates to {@link #elysiumCanStickTo}, which
     * means any existing override of the non-face variant continues to work for
     * piston resolution without modification. Mods that require face-sensitive
     * stickiness can override this method directly and skip the fallback.
     * <br>
     * <li>Original Analog: {@code BlockState.canStickTo} 
     * <li>Purpose: Elysium provided extension.
     *
     * @param selfState the state of the block being queried
     * @param otherState the state of the adjacent block
     * @param selfFace the face on selfState that is touching the other block
     * @param otherFace the face on otherState that is touching selfState
     * @return true if selfState may stick to otherState from the given faces
     */
    default boolean elysiumCanStickToFace(BlockState selfState, BlockState otherState, Direction selfFace, Direction otherFace) {
        return ((IElysiumBlock) selfState.getBlock()).elysiumCanStickTo(selfState, otherState);
    }

    /**
     * Whether this block sustains fire indefinitely, e.g. as a permanent
     * fire source on the dimension's infiniburn tag.
     * <br>
     * <li>Original Analog: {@code BlockState.isFireSource}
     * <li>Purpose: Forge and NeoForge extension parity.
     *
     * @param state the current state
     * @param level the level reader
     * @param pos the position in the level
     * @param direction the direction fire is approaching from
     * @return true if this block is an infinite fire source
     */
    default boolean elysiumIsFireSource(BlockState state, LevelReader level, BlockPos pos, Direction direction) {
        return state.is(level.dimensionType().infiniburn());
    }

    /**
     * Chance for fire to consume this block, on a scale of 0 to 300.
     * Higher values burn faster.
     * <br>
     * <li>Original Analog: {@code BlockState.getFlammability}
     * <li>Purpose: Forge and NeoForge extension parity.
     *
     * @param state the current state
     * @param level the level
     * @param pos the position in the level
     * @param direction the face being queried
     * @return the burn odds of this block, or 0 if waterlogged
     */
    default int elysiumGetFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        if (state.hasProperty(BlockStateProperties.WATERLOGGED) && state.getValue(BlockStateProperties.WATERLOGGED)) return 0;
        return ((FireBlockAccessor) Blocks.FIRE).elysium$GetBurnOddsMap().getInt(state.getBlock());
    }

    /**
     * Whether the given face of this block can be set on fire.
     * <br>
     * <li>Original Analog: {@code BlockState.isFlammable}
     * <li>Purpose: Forge and NeoForge extension parity.
     *
     * @param state the current state
     * @param level the level
     * @param pos the position in the level
     * @param direction the face being queried
     * @return true if this block can catch fire from the given face
     */
    default boolean elysiumIsFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return elysiumGetFlammability(state, level, pos, direction) > 0;
    }

    /**
     * Encouragement for fire to spread to this block, on a scale of 0 to 300.
     * Higher values spread faster.
     * <br>
     * <li>Original Analog: {@code BlockState.getFireSpreadSpeed}
     * <li>Purpose: Forge and NeoForge extension parity.
     *
     * @param state the current state
     * @param level the level
     * @param pos the position in the level
     * @param direction the face being queried
     * @return the ignite odds of this block, or 0 if waterlogged
     */
    default int elysiumGetFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        if (state.hasProperty(BlockStateProperties.WATERLOGGED) && state.getValue(BlockStateProperties.WATERLOGGED)) return 0;
        return ((FireBlockAccessor) Blocks.FIRE).elysium$GetIgniteOddsMap().getInt(state.getBlock());
    }

    /**
     * Called when this block catches fire.
     * <br>
     * <li>Original Analog: {@code BlockState.onCaughtFire}
     * <li>Purpose: Forge and NeoForge extension parity.
     *
     * @param state the current state
     * @param level the level
     * @param pos the position in the level
     * @param direction the face that caught fire, may be null
     * @param igniter the entity that caused the fire, may be null
     */
    default void elysiumOnCaughtFire(BlockState state, Level level, BlockPos pos, @Nullable Direction direction, @Nullable LivingEntity igniter) {
    }

    /**
     * Whether this block drops its loot when destroyed by an explosion.
     * <br>
     * <li>Original Analog: {@code Block.dropFromExplosion}
     * <li>Purpose: Elysium expanded Forge and NeoForge extension parity.
     *
     * @param state the current state
     * @param level the level
     * @param pos the position in the level
     * @param explosion the explosion destroying this block
     * @return true if this block drops loot from the explosion
     */
    default boolean elysiumCanDropFromExplosion(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
        return self().dropFromExplosion(explosion);
    }

    /**
     * Called when this block is destroyed by an explosion. The default
     * removes the block and fires the wasExploded callback.
     * <br>
     * <li>Original Analog: {@code BlockState.onBlockExploded}
     * <li>Purpose: Forge and NeoForge extension parity.
     *
     * @param state the current state
     * @param level the level
     * @param pos the position in the level
     * @param explosion the explosion destroying this block
     */
    default void elysiumOnBlockExploded(BlockState state, Level level, BlockPos pos, Explosion explosion) {
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        self().wasExploded(level, pos, explosion);
    }

    /**
     * Experience awarded when this block is broken, before enchantment
     * modifiers are applied.
     * <br>
     * <li>Original Analog: {@code BlockState.getExpDrop}
     * <li>Purpose: Forge and NeoForge extension parity.
     *
     * @param state the current state
     * @param level the level accessor
     * @param pos the position in the level
     * @param blockEntity the block entity being broken, may be null
     * @param breaker the entity breaking the block, may be null
     * @param tool the tool used to break the block
     * @return the base experience to drop
     */
    default int elysiumGetExpDrop(BlockState state, LevelAccessor level, BlockPos pos, @Nullable BlockEntity blockEntity, @Nullable Entity breaker, ItemStack tool) {
        return 0;
    }

    /**
     * Whether the given entity may destroy this block through its natural
     * destruction behavior, such as the Ender Dragon or the Wither.
     * <br>
     * <li>Original Analog: {@code BlockState.canEntityDestroy}
     * <li>Purpose: Forge and NeoForge extension parity.
     *
     * @param state the current state
     * @param level the level
     * @param pos the position in the level
     * @param entity the entity attempting destruction
     * @return true if the entity may destroy this block
     */
    default boolean elysiumCanEntityDestroy(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
        if (entity instanceof EnderDragon) return !this.self().defaultBlockState().is(BlockTags.DRAGON_IMMUNE);
        else if ((entity instanceof WitherBoss) || (entity instanceof WitherSkull)) return state.isAir() || WitherBoss.canDestroy(state);

        return true;
    }

    /**
     * Resistance to explosions at the given position, overriding the
     * block's base resistance value when position-dependent.
     * <br>
     * <li>Original Analog: {@code BlockState.getExplosionResistance}
     * <li>Purpose: Forge and NeoForge extension parity.
     *
     * @param state the current state
     * @param level the level
     * @param pos the position in the level
     * @param explosion the explosion being evaluated
     * @return the explosion resistance at this position
     */
    default float elysiumGetExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
        return self().getExplosionResistance();
    }

    /**
     * Slipperiness at the given position. Lower values cause more sliding;
     * note that entities may reduce slipperiness by a certain factor of their own;
     * for {@link LivingEntity}, this is {@code .91}.
     * {@link net.minecraft.world.entity.item.ItemEntity} uses {@code .98}, and
     * {@link net.minecraft.world.entity.projectile.FishingHook} uses {@code .92}.
     * <br>
     * <li>Original Analog: {@code BlockState.getFriction}
     * <li>Purpose: Forge and NeoForge extension parity.
     *
     * @param state state of the block
     * @param level the level
     * @param pos the position in the level
     * @param entity the entity in question, may be null
     * @return the factor by which the entity's motion should be multiplied
     */
    default float elysiumGetFriction(BlockState state, LevelReader level, BlockPos pos, @Nullable Entity entity) {
        return self().getFriction();
    }

    /**
     * Whether this block's light emission depends on context such as
     * position or neighboring blocks. Returning {@code true} signals the
     * lighting engine to evaluate {@link #elysiumGetLightEmission} at each
     * candidate position rather than trusting the cached per-state value.
     * <br>
     * <li>Original Analog: {@code BlockState.hasDynamicLightEmission}
     * <li>Purpose: NeoForge extension parity.
     *
     * @param state the current state
     * @return true if this block emits context-dependent light
     */
    default boolean elysiumHasDynamicLightEmission(BlockState state) {
        return false;
    }

    /**
     * Light level emitted by this block at the given position, in the range
     * 0 to 15. Only queried at arbitrary positions when
     * {@link #elysiumHasDynamicLightEmission} returns {@code true};
     * otherwise only the cached value at {@code BlockPos.ZERO} is used.
     * <br>
     * <li>Original Analog: {@code BlockState.getLightEmission}
     * <li>Purpose: Forge and NeoForge extension parity.
     *
     * @param state the current state
     * @param level the level
     * @param pos the position in the level
     * @return the light level emitted, between 0 and 15
     */
    default int elysiumGetLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getLightEmission();
    }

    /**
     * Color used to render this block on a filled map. {@code defaultColor}
     * carries the vanilla value derived from the block's map color property.
     * <br>
     * <li>Original Analog: {@code BlockState.getMapColor}
     * <li>Purpose: Forge and NeoForge extension parity.
     *
     * @param state the current state
     * @param level the level
     * @param pos the position in the level
     * @param defaultColor the vanilla map color for this state
     * @return the map color to use at this position
     */
    default MapColor elysiumGetMapColor(BlockState state, BlockGetter level, BlockPos pos, MapColor defaultColor) {
        return defaultColor;
    }

    /**
     * Sound type (break, step, place, hit, fall) used for this block at the
     * given position, optionally scoped to the interacting entity.
     * <br>
     * <li>Original Analog: {@code BlockState.getSoundType}
     * <li>Purpose: Forge and NeoForge extension parity.
     *
     * @param state the current state
     * @param level the level reader
     * @param pos the position in the level
     * @param entity the entity interacting with the block, may be null
     * @return the sound type to use at this position
     */
    default SoundType elysiumGetSoundType(BlockState state, LevelReader level, BlockPos pos, @Nullable Entity entity) {
        return state.getSoundType();
    }

    /**
     * Returns the state after applying the given rotation. Called by
     * structure placement and by anything else that needs a rotated copy.
     * <br>
     * <li>Original Analog: {@code BlockState.rotate}
     * <li>Purpose: Forge and NeoForge extension parity.
     *
     * @param state the current state
     * @param level the level accessor
     * @param pos the position in the level
     * @param rotation the rotation to apply
     * @return the rotated state
     */
    default BlockState elysiumRotate(BlockState state, LevelAccessor level, BlockPos pos, Rotation rotation) {
        return state.rotate(rotation);
    }

    /**
     * Whether this block should receive indirect power from neighboring
     * strong sources such as redstone blocks, as if it were a redstone
     * conductor.
     * <br>
     * <li>Original Analog: {@code BlockState.shouldCheckWeakPower}
     * <li>Purpose: Forge and NeoForge extension parity.
     *
     * @param state the current state
     * @param level the signal getter
     * @param pos the position in the level
     * @param side the side being queried
     * @return true if this block should receive weak redstone power
     */
    default boolean elysiumShouldCheckWeakPower(BlockState state, SignalGetter level, BlockPos pos, Direction side) {
        return state.isRedstoneConductor(level, pos);
    }

    /**
     * Whether this block should be notified of changes that occur through a
     * solid conductor, enabling comparator-like two-block-away propagation.
     * <br>
     * <li>Original Analog: {@code BlockState.getWeakChanges}
     * <li>Purpose: Forge and NeoForge extension parity.
     *
     * @param state the current state
     * @param level the level reader
     * @param pos the position in the level
     * @return true if this block listens for weak redstone changes
     */
    default boolean elysiumGetWeakChanges(BlockState state, LevelReader level, BlockPos pos) {
        return false;
    }

    /**
     * Called when a neighboring block's state changes through a redstone
     * conductor. Fires on horizontal neighbors of a block that has an
     * analog output signal.
     * <br>
     * <li>Original Analog: {@code BlockState.onNeighborChange}
     * <li>Purpose: Forge and NeoForge extension parity.
     *
     * @param state the current state
     * @param level the level reader
     * @param pos the position in the level
     * @param neighbor the position of the changed neighbor
     */
    default void elysiumOnNeighborChange(BlockState state, LevelReader level, BlockPos pos, BlockPos neighbor) {
    }

    /**
     * Whether this block acts as scaffolding for movement, allowing
     * entities to climb down while sneaking.
     * <br>
     * <li>Original Analog: {@code BlockState.isScaffolding}
     * <li>Purpose: Elysium expanded Forge and NeoForge extension parity.
     *
     * @param state the current state
     * @param level the level reader
     * @param pos the position in the level
     * @param entity the entity being evaluated
     * @return true if the entity can use this block as scaffolding
     */
    default boolean elysiumIsScaffolding(BlockState state, LevelReader level, BlockPos pos, LivingEntity entity) {
        return state.is(Blocks.SCAFFOLDING);
    }

    /**
     * Whether redstone dust visually connects to this block from the given
     * direction. A {@code null} direction indicates the query is unattached.
     * <br>
     * <li>Original Analog: {@code BlockState.canConnectRedstone}
     * <li>Purpose: Forge and NeoForge extension parity.
     *
     * @param state the current state
     * @param level the level
     * @param pos the position in the level
     * @param direction the direction redstone would connect from, may be null
     * @return true if redstone should connect from the given direction
     */
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

    /**
     * How pistons interact with this block. Returns {@code null} to fall
     * back to the {@link BlockState#getPistonPushReaction} value configured
     * through the block's properties.
     * <ul>
     * <li>NORMAL: is pushable and pullable by sticky pistons</li>
     * <li>DESTROY: is being destroyed on pushing and pulling</li>
     * <li>BLOCK: is not being able to be moved</li>
     * <li>IGNORE: only usable by entities</li>
     * <li>PUSH_ONLY: can only be pushed, blocks on trying to be pulled</li>
     * <br>
     * <li>Original Analog: {@code BlockState.getPistonPushReaction}
     * <li>Purpose: Forge and NeoForge extension parity.
     *
     * @param state the current state
     * @return the push reaction for this block, or null to use the default
     */
    default @Nullable PushReaction elysiumGetPistonPushReaction(BlockState state) {
        return null;
    }

    /**
     * Whether this block should be treated as empty space, distinguishing
     * air-like blocks from others for code that needs finer granularity
     * than a single air check.
     * <br>
     * <li>Original Analog: {@code BlockState.isEmpty}
     * <li>Purpose: Forge and NeoForge extension parity.
     *
     * @param state the current state
     * @return true if this block should be treated as empty space
     */
    default boolean elysiumIsEmpty(BlockState state) {
        return state.is(Blocks.AIR) || state.is(Blocks.CAVE_AIR) || state.is(Blocks.VOID_AIR);
    }

    /**
     * The direction a bubble column forms when this block is submerged underwater.
     * <br>
     * NOTE: The block itself will still need to call
     * {@link net.minecraft.world.level.block.BubbleColumnBlock#updateColumn}
     * in its tick method and schedule a block tick in the block's onPlace. Also,
     * schedule a fluid tick in updateShape if the update direction is up.
     * Both are needed in order to get bubble columns to function properly. See
     * {@link net.minecraft.world.level.block.SoulSandBlock} and
     * {@link net.minecraft.world.level.block.MagmaBlock} for example.
     * <br>
     * <li>Original Analog: {@code BlockState.getBubbleColumnDirection}
     * <li>Purpose: Forge and NeoForge extension parity.
     *
     * @param state the current state
     * @return the bubble column direction, {@link ElysiumBubbleColumnDirection#NONE} for no column
     */
    default ElysiumBubbleColumnDirection elysiumGetBubbleColumnDirection(BlockState state) {
        if (state.is(Blocks.SOUL_SAND)) return ElysiumBubbleColumnDirection.UPWARD;
        else return state.is(Blocks.MAGMA_BLOCK) ? ElysiumBubbleColumnDirection.DOWNWARD : ElysiumBubbleColumnDirection.NONE;
    }

    /**
     * Overrides the pathfinding weight classification for this block at a
     * given position, controlling how mobs evaluate it as terrain.
     * <br>
     * Returning {@code null} (the default) defers to vanilla's automatic
     * classification. Returning a value bypasses it entirely.
     * <br>
     * <li>Original Analog: {@code BlockState.getBlockPathType}
     * <li>Purpose: Forge and NeoForge extension parity, with a version-agnostic path type.
     *
     * @param state the current state
     * @param level the level
     * @param pos the position in the level
     * @param mob the mob performing pathfinding, may be null
     * @return the path type to use, or null to fall back to vanilla classification
     */
    default @Nullable ElysiumPathType elysiumGetBlockPathType(BlockState state, BlockGetter level, BlockPos pos, @Nullable Mob mob) {
        return null;
    }

    /**
     * Overrides the pathfinding classification that this block applies to
     * its neighbors, controlling how mobs treat nearby hazards when
     * traversing terrain. Returned values are used as the adjacent block's
     * path type immediately, bypassing vanilla's per-block danger
     * comparisons.
     * <br>
     * Returning {@code null} (the default) defers to vanilla's automatic
     * classification.
     * <br>
     * <li>Original Analog: {@code BlockState.getAdjacentBlockPathType}
     * <li>Purpose: Forge and NeoForge extension parity, with a version-agnostic path type.
     *
     * @param state the current state
     * @param level the level
     * @param pos the position in the level
     * @param mob the mob performing pathfinding, may be null
     * @param originalType the path type of the block being walked on
     * @return the path type to apply to this neighbor, or null to fall back to vanilla
     */
    default @Nullable ElysiumPathType elysiumGetAdjacentBlockPathType(BlockState state, BlockGetter level, BlockPos pos, @Nullable Mob mob, @Nullable ElysiumPathType originalType) {
        return null;
    }

    /**
     * Determines whether the player may harvest this block, i.e. whether it
     * drops loot when broken with the currently held tool. The base check
     * matches vanilla: harvestable unless the block requires a correct tool
     * and the held item is not one.
     * <br>
     * <li>Original Analog: {@code BlockState.canHarvestBlock}
     * <li>Purpose: Forge and NeoForge extension parity.
     *
     * @param state the current state
     * @param level the level
     * @param pos the position in the level
     * @param player the player breaking the block
     * @return true if the block should drop loot for this player
     */
    default boolean elysiumCanHarvestBlock(BlockState state, BlockGetter level, BlockPos pos, Player player) {
        return !state.requiresCorrectToolForDrops() || player.getMainHandItem().isCorrectToolForDrops(state);
    }

    /**
     * Called when a player destroys this block, before the block is actually
     * removed. Returning {@code false} prevents the block from being removed,
     * which lets overrides veto destruction or substitute their own removal
     * logic. The default matches NeoForge's behavior: on the client, replace the block with its fluid's legacy form;
     * on the server, remove the block and let the fluid settle in naturally.
     * client.
     * <br>
     * <li>Original Analog: {@code BlockState.onDestroyedByPlayer}
     * <li>Purpose: Forge and NeoForge extension parity.
     *
     * @param state the current state
     * @param level the level
     * @param pos the position in the level
     * @param player the player destroying the block
     * @param willHarvest true if this block will drop loot in the same pass
     * @param fluid the fluid state at this position
     * @return true if the block was removed, false to keep it in place
     */
    default boolean elysiumOnDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        return level.isClientSide() ? level.setBlock(pos, fluid.createLegacyBlock(), 11) : level.removeBlock(pos, false);
    }

    /**
     * Whether this block's collision should extend above its own block
     * space for the given entity, Use this to replicate fence and wall behavior.
     * <br>
     * <li>Original Analog: {@code BlockState.collisionExtendsVertically}
     * <li>Purpose: Forge and NeoForge extension parity.
     *
     * @param state the current state
     * @param level the level
     * @param pos the position in the level
     * @param collidingEntity the entity being supported
     * @return true if the collision extends upward past the block's own space
     */
    default boolean elysiumCollisionExtendsVertically(BlockState state, BlockGetter level, BlockPos pos, Entity collidingEntity) {
        return state.is(BlockTags.FENCES) || state.is(BlockTags.WALLS) || state.getBlock() instanceof FenceGateBlock;
    }

    /**
     * Color multiplier applied to a beacon beam passing through this block.
     * Returns {@code null} to leave the beam unaffected, or a packed RGB
     * integer to tint it.
     * <br>
     * <li>Original Analog: {@code BlockState.getBeaconColorMultiplier}
     * <li>Purpose: Elysium expanded Forge and NeoForge extension parity.
     *
     * @param state the current state
     * @param level the level
     * @param pos the position in the level
     * @param beaconPos the position of the beacon emitting the beam
     * @return the packed RGB color, or null to leave the beam unmodified
     */
    default @Nullable Integer elysiumGetBeaconColorMultiplier(BlockState state, LevelReader level, BlockPos pos, BlockPos beaconPos) {
        DyeColor color = ((BeaconBeamBlock) state.getBlock()).getColor();
        if (color == null) return null;
        return ElysiumDyeColors.getTextureDiffuseColor(color);
    }

    /**
     * Whether this block hides the neighboring block's face pointing towards
     * it, when queried from that neighbor's rendering path. Return
     * {@code true} to make the neighbor's face cull even though this block
     * would not normally occlude it through vanilla checks.
     * <br>
     * This method is queried on the <em>neighbor</em> (the block doing the
     * hiding); the {@code state} parameter is that neighbor's own state, and
     * {@code neighborState} is the state of the block being hidden.
     * <br>
     * Mods that only want to add culling for their own blocks should override
     * {@code Block.skipRendering} instead, which runs in the same code path
     * and does not require this method to be consulted.
     * <br>
     * <li>Original Analog: {@code BlockState.hidesNeighborFace}
     * <li>Purpose: Forge and NeoForge extension parity.
     *
     * @param level the level being rendered
     * @param pos the neighbor's position in the level
     * @param state the neighbor's own state (this block)
     * @param neighborState the state of the neighboring block being hidden
     * @param direction the direction from the neighbor towards the hidden block
     * @return true if the neighboring face should be culled
     */
    default boolean elysiumHidesNeighborFace(BlockGetter level, BlockPos pos, BlockState state, BlockState neighborState, Direction direction) {
        return false;
    }

    /**
     * Whether this block allows adjacent blocks to hide its own faces through
     * {@link #elysiumHidesNeighborFace}. Returning {@code true} enables the
     * check; returning {@code false} disables external face hiding for this
     * block entirely, so it always renders its own faces regardless of what
     * neighbors say.
     * <br>
     * Vanilla's default returns {@code true} on the server and, on NeoForge,
     * client-side block-in-solid-layer detection on the client. This
     * implementation returns {@code true} unconditionally; client-only
     * overrides that need to detect solid-layer membership should override
     * and consult whatever detection mechanism is available on the target
     * loader.
     * <br>
     * <li>Original Analog: {@code BlockState.supportsExternalFaceHiding}
     * <li>Purpose: Forge and NeoForge extension parity.
     *
     * @param state the current state
     * @return true if neighboring blocks may hide this block's faces
     */
    default boolean elysiumSupportsExternalFaceHiding(BlockState state) {
        return true;
    }

    /**
     * Called to determine whether this block should use the fluid overlay texture or flowing texture when it is placed under the fluid.
     * <br>
     * <li>Original Analog: {@code shouldDisplayFluidOverlay}
     * <li>Purpose: Forge and NeoForge extension parity.
     *
     * @param state the current state
     * @param level the level being rendered
     * @param pos the position in the level
     * @param fluidState the fluid state at this position
     * @return true if the fluid overlay texture should be used for this block
     */
    default boolean elysiumShouldDisplayFluidOverlay(BlockState state, BlockAndTintGetter level, BlockPos pos, FluidState fluidState) {
        return state.getBlock() instanceof HalfTransparentBlock || state.getBlock() instanceof LeavesBlock;
    }

    /**
     * Allows a block to override the standard vanilla landing particles.
     * Consulted from LivingEntity.checkFallDamage on the server-side
     * before the vanilla ServerLevel.sendParticles call.
     * <br>
     * <li>Original Analog: {@code BlockState.addLandingEffects}
     * <li>Purpose: Forge and NeoForge extension parity.
     *
     * @param state the current state
     * @param level the current server level
     * @param pos the position in the level
     * @param entity the entity that landed on the block
     * @param numberOfParticles the number of particles vanilla would have spawned
     * @return true to prevent vanilla landing particles from spawning
     */
    default boolean elysiumAddLandingEffects(BlockState state, ServerLevel level, BlockPos pos, LivingEntity entity, int numberOfParticles) {
        return false;
    }

    /**
     * Allows a block to override the standard vanilla running particles.
     * Called from {@code Entity.spawnSprintParticle} on both the client and
     * server, so implementors should check the side if their behavior is
     * side-specific. Vanilla only spawns running particles on the client.
     * <br>
     * <li>Original Analog: {@code BlockState.addRunningEffects}
     * <li>Purpose: Forge and NeoForge extension parity.
     *
     * @param state the current state
     * @param level the level
     * @param pos the position in the level
     * @param entity the entity running on the block
     * @return true to prevent vanilla running particles from spawning
     */
    default boolean elysiumAddRunningEffects(BlockState state, Level level, BlockPos pos, Entity entity) {
        return false;
    }

    /**
     * Called after the block state at the given position has been changed and
     * all neighbor updates have settled. Fires on both the client and server.
     * Modifying the level is disallowed in this method; use it to recompute
     * cached data based on the new state or on how neighbors reacted.
     * <br>
     * <li>Original Analog: {@code BlockState.onBlockStateChange}
     * <li>Purpose: Forge and NeoForge extension parity.
     *
     * @param level the level the state was modified in
     * @param pos the position in the level
     * @param oldState the previous state, may be from a different block
     * @param newState the new state
     */
    default void elysiumOnBlockStateChange(LevelReader level, BlockPos pos, BlockState oldState, BlockState newState) {
    }

    /**
     * Called when a block is removed by a piston with {@link PushReaction#DESTROY}.
     * The block is still intact at the time of this call.
     * <br>
     * NOTE: only invoked if {@code BlockState.getPistonPushReaction} returns
     * {@link PushReaction#DESTROY} for this block. Fires on both the client and
     * server sides.
     * <br>
     * <li>Original Analog: {@code BlockState.onDestroyedByPushReaction}
     * <li>Purpose: NeoForge extension parity.
     *
     * @param state the current state
     * @param level the level
     * @param pos the position in the level
     * @param pushDirection the direction the block is being pushed
     * @param fluid the current fluid state at this position
     */
    default void elysiumOnDestroyedByPushReaction(BlockState state, Level level, BlockPos pos, Direction pushDirection, FluidState fluid) {
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), level.isClientSide ? 11 : 3);
        level.gameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Context.of(state));
    }
}