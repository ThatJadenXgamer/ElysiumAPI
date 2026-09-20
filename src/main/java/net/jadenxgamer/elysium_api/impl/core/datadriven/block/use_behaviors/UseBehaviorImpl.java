package net.jadenxgamer.elysium_api.impl.core.datadriven.block.use_behaviors;

import net.jadenxgamer.elysium_api.api.util.LookupRegistryHelper;
import net.jadenxgamer.elysium_api.api.util.RegistryAccessHelper;
import net.jadenxgamer.elysium_api.impl.mixin.block.BlockAccessor;
import net.jadenxgamer.elysium_api.impl.registry.ElysiumRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.List;
import java.util.Optional;

public class UseBehaviorImpl {

    public static void init(PlayerInteractEvent.RightClickBlock event) {
        if (!RegistryAccessHelper.isRegistryAccessAvailable()) return;

        Level level = event.getLevel();
        BlockState state = level.getBlockState(event.getPos());
        Player player = event.getEntity();
        ItemStack stack = player.getItemInHand(event.getHand());

        Optional<UseBehavior> useBehavior = RegistryAccessHelper.getServer()
                .flatMap(access -> access.registryOrThrow(ElysiumRegistries.Keys.USE_BEHAVIORS).stream()
                        .filter(s -> s.blocks().contains(state.getBlockHolder())
                                && s.itemCondition().contains(stack.getItemHolder()))
                        .findFirst());

        if (useBehavior.isEmpty()) return;

        if (level.isClientSide()) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }
        UseBehavior registry = useBehavior.get();
        BlockPos pos = getPosFromCodec(registry.behavior().pos(), registry.behavior().posOffset(), event);

        if (isPlaceRelated(registry) && !registry.behavior().canReplace() && !level.getBlockState(pos).canBeReplaced()) return; // Fails if the useBehavior is trying to place something is non-replaceable while the boolean to replace is false
        if (registry.blockstateCondition().isPresent() && !registry.blockstateCondition().get().matches(state)) return; // Fails if a blockstate_condition is present and the current block does not match said state
        if (!player.getAbilities().instabuild) handleItemAfterUse(registry.behavior().afterUseItem(), stack, event); // Handles after use behaviors of the use item, this does not fire in creative mode
        if (registry.behavior().sounds().isPresent()) {
            var sounds = registry.behavior().sounds().get();
            level.playSound(null, event.getPos(), sounds.soundEvent(), SoundSource.BLOCKS, sounds.volume(), sounds.pitch());
        }
        if (registry.behavior().particles().isPresent()) {
            var particles = registry.behavior().particles().get();
            trySpawnParticles((ServerLevel) level, pos, particles.particleType(), particles.count(), particles.speed(), particles.xOffset(), particles.yOffset(), particles.zOffset());
        }

        int chanceToFail = registry.chanceToFail();
        if (chanceToFail > 0 && level.random.nextInt(chanceToFail) != 0) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }

        switch (registry.behavior().type()) {
            case PLACE -> placeBlock(level, pos, state, registry.behavior().block().get(), registry.behavior(), event); // Places a Block
            case PLACE_ITSELF -> placeBlock(level, pos, state, state, registry.behavior(), event); // Places a Block of itself
            case DROP -> dropStack(level, pos, event.getFace(), registry.behavior().item().get(), registry.behavior().itemCount()); // Drops a Stack
            case DROP_ITSELF -> dropStack(level, pos, event.getFace(), BuiltInRegistries.BLOCK.getKey(state.getBlock()), registry.behavior().itemCount()); // Drops a Stack of itself
            case FEATURE -> placeFeature(level, pos, registry.behavior().feature().get()); // Places a PlacedFeature
            case INSERT_STACK -> insertStack(player, registry.behavior().item().get(), registry.behavior().itemCount()); // Inserts a Stack within your inventory
        }
        if (registry.behavior().breakParticles()) level.levelEvent(2001, pos, Block.getId(state)); // Spawns break particles in the modified position

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void placeBlock(Level level, BlockPos pos, BlockState originalState, BlockState newState, UseBehavior.Behavior behavior, PlayerInteractEvent.RightClickBlock event) {
        if (newState == null) return;
        BlockState finalState = newState;
        Optional<List<String>> copyProperties = behavior.copyProperties();
        if (copyProperties.isPresent()) {
            List<String> propertyNames = copyProperties.get();
            for (Property property : originalState.getProperties()) {
                if (propertyNames.contains(property.getName()) && newState.hasProperty(property)) {
                    finalState = finalState.setValue(property, originalState.getValue(property));
                }
            }
        }
        if (((BlockAccessor) finalState.getBlock()).elysium_api$canSurvive(finalState, level, pos)) {
            level.setBlock(pos, finalState, Block.UPDATE_ALL);
        }
    }


    private static void dropStack(Level level, BlockPos pos, Direction direction, ResourceLocation location, int count) {
        Item item = LookupRegistryHelper.getItem(location);
        Block.popResourceFromFace(level, pos, direction, new ItemStack(item, count));
    }

    private static void insertStack(Player player, ResourceLocation location, int count) {
        Item item = LookupRegistryHelper.getItem(location);
        player.getInventory().add(new ItemStack(item, count));
    }

    private static void placeFeature(Level level, BlockPos pos, ResourceLocation location) {
        if (level instanceof ServerLevel serverLevel) {
            ResourceKey<ConfiguredFeature<?, ?>> featureKey = ResourceKey.create(Registries.CONFIGURED_FEATURE, location);
            serverLevel.registryAccess().registry(Registries.CONFIGURED_FEATURE).flatMap(registry -> registry.getHolder(featureKey)).ifPresent(holder -> holder.value()
                    .place(serverLevel, serverLevel.getChunkSource().getGenerator(), serverLevel.random, pos));
        }
    }

    private static void handleItemAfterUse(AfterUseItemEnum afterUse, ItemStack stack, PlayerInteractEvent.RightClickBlock event) {
        switch (afterUse) {
            case CONSUME -> stack.shrink(1);
            case DAMAGE -> stack.hurtAndBreak(1, event.getEntity(), LivingEntity.getSlotForHand(event.getHand()));
        }
    }

    private static BlockPos getPosFromCodec(PosEnum pos, int offset, PlayerInteractEvent.RightClickBlock event) {
        BlockPos basePos = event.getPos();
        return switch (pos) {
            case ABOVE -> basePos.above(offset);
            case BELOW -> basePos.below(offset);
            case NORTH -> basePos.north(offset);
            case SOUTH -> basePos.south(offset);
            case EAST -> basePos.east(offset);
            case WEST -> basePos.west(offset);
            case RANDOM_HORIZONTAL -> { // Randomly chooses between the 4 cardinal directions
                Direction randomDir = Direction.Plane.HORIZONTAL.getRandomDirection(event.getLevel().random);
                yield basePos.relative(randomDir);
            }
            case RANDOM_VERTICAL -> { // Randomly chooses between above and below
                Direction randomDir = Direction.Plane.VERTICAL.getRandomDirection(event.getLevel().random);
                yield basePos.relative(randomDir);
            }
            default -> basePos;
        };
    }

    private static void trySpawnParticles(ServerLevel level, BlockPos pos, ResourceLocation location, int count, double speed, double xOffset, double yOffset, double zOffset) {
        ParticleType<?> particleType = LookupRegistryHelper.getParticleType(location);
        if (particleType instanceof SimpleParticleType simple) {
            level.sendParticles(simple, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, count, xOffset, yOffset, zOffset, speed);
        }
    }

    private static boolean isPlaceRelated(UseBehavior registry) {
        UseBehaviorTypeEnum type = registry.behavior().type();
        return type == UseBehaviorTypeEnum.PLACE || type == UseBehaviorTypeEnum.PLACE_ITSELF || type == UseBehaviorTypeEnum.FEATURE;
    }
}
