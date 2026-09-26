package net.jadenxgamer.elysium_api.impl.core.datadriven.block.use_behaviors;

import net.jadenxgamer.elysium_api.api.charon.CharonContext;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
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
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;
import java.util.Optional;

public class UseBehaviorImpl {

    public static void init(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand, BlockHitResult hitResult, CharonContext ctx) {
        if (!RegistryAccessHelper.isRegistryAccessAvailable()) return;

        BlockPos eventPos = hitResult.getBlockPos();
        BlockState state = level.getBlockState(eventPos);

        Optional<UseBehavior> useBehavior = RegistryAccessHelper.getServer()
                .flatMap(access -> access.registryOrThrow(ElysiumRegistries.Keys.USE_BEHAVIORS).stream()
                        .filter(s -> s.blocks().contains(state.getBlockHolder())
                                && s.itemCondition().contains(stack.getItemHolder()))
                        .findFirst());

        if (useBehavior.isEmpty()) return;

        if (level.isClientSide()) {
            ctx.setReturn(InteractionResult.SUCCESS);
            return;
        }
        UseBehavior registry = useBehavior.get();
        BlockPos pos = getPosFromCodec(registry.behavior().pos(), registry.behavior().posOffset(), eventPos, level);

        if (isPlaceRelated(registry) && !registry.behavior().canReplace() && !level.getBlockState(pos).canBeReplaced()) return;
        if (registry.blockstateCondition().isPresent() && !registry.blockstateCondition().get().matches(state)) return;
        if (!player.getAbilities().instabuild) handleItemAfterUse(registry.behavior().afterUseItem(), stack, player, hand);
        if (registry.behavior().sounds().isPresent()) {
            var sounds = registry.behavior().sounds().get();
            level.playSound(null, eventPos, sounds.soundEvent(), SoundSource.BLOCKS, sounds.volume(), sounds.pitch());
        }
        if (registry.behavior().particles().isPresent()) {
            var particles = registry.behavior().particles().get();
            trySpawnParticles((ServerLevel) level, pos, particles.particleType(), particles.count(), particles.speed(), particles.xOffset(), particles.yOffset(), particles.zOffset());
        }

        int chanceToFail = registry.chanceToFail();
        if (chanceToFail > 0 && level.random.nextInt(chanceToFail) != 0) {
            ctx.setReturn(InteractionResult.SUCCESS);
            return;
        }

        switch (registry.behavior().type()) {
            case PLACE -> placeBlock(level, pos, state, registry.behavior().block().get(), registry.behavior());
            case PLACE_ITSELF -> placeBlock(level, pos, state, state, registry.behavior());
            case DROP -> dropStack(level, pos, hitResult.getDirection(), registry.behavior().item().get(), registry.behavior().itemCount());
            case DROP_ITSELF -> dropStack(level, pos, hitResult.getDirection(), BuiltInRegistries.BLOCK.getKey(state.getBlock()), registry.behavior().itemCount());
            case FEATURE -> placeFeature(level, pos, registry.behavior().feature().get());
            case INSERT_STACK -> insertStack(player, registry.behavior().item().get(), registry.behavior().itemCount());
        }
        if (registry.behavior().breakParticles()) level.levelEvent(2001, pos, Block.getId(state));

        ctx.setReturn(InteractionResult.SUCCESS);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void placeBlock(Level level, BlockPos pos, BlockState originalState, BlockState newState, UseBehavior.Behavior behavior) {
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

    private static void handleItemAfterUse(AfterUseItemEnum afterUse, ItemStack stack, Player player, InteractionHand hand) {
        switch (afterUse) {
            case CONSUME -> stack.shrink(1);
            case DAMAGE -> stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        }
    }

    private static BlockPos getPosFromCodec(PosEnum pos, int offset, BlockPos basePos, Level level) {
        return switch (pos) {
            case ABOVE -> basePos.above(offset);
            case BELOW -> basePos.below(offset);
            case NORTH -> basePos.north(offset);
            case SOUTH -> basePos.south(offset);
            case EAST -> basePos.east(offset);
            case WEST -> basePos.west(offset);
            case RANDOM_HORIZONTAL -> {
                Direction randomDir = Direction.Plane.HORIZONTAL.getRandomDirection(level.random);
                yield basePos.relative(randomDir);
            }
            case RANDOM_VERTICAL -> {
                Direction randomDir = Direction.Plane.VERTICAL.getRandomDirection(level.random);
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