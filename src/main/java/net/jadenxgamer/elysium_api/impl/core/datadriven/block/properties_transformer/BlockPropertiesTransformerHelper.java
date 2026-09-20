package net.jadenxgamer.elysium_api.impl.core.datadriven.block.properties_transformer;

import net.jadenxgamer.elysium_api.api.util.RegistryAccessHelper;
import net.jadenxgamer.elysium_api.impl.registry.ElysiumRegistries;
import net.jadenxgamer.elysium_api.impl.util.ConditionalBoolean;
import net.jadenxgamer.elysium_api.impl.util.ConditionalInt;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static net.jadenxgamer.elysium_api.ElysiumAPI.LOGGER;

public final class BlockPropertiesTransformerHelper {
    private static final ConcurrentHashMap<Block, Optional<BlockPropertiesTransformer>> CACHE = new ConcurrentHashMap<>();

    @Nullable
    public static BlockPropertiesTransformer getTransformer(Block block) {
        if (!RegistryAccessHelper.isServerAvailable()) return null;
        Optional<BlockPropertiesTransformer> cached = CACHE.get(block);
        if (cached != null) return cached.orElse(null);

        Optional<BlockPropertiesTransformer> computed = Optional.ofNullable(loadAndMergeTransformers(block));
        CACHE.put(block, computed);
        return computed.orElse(null);
    }

    private static BlockPropertiesTransformer loadAndMergeTransformers(Block block) {
        Optional<RegistryAccess> registryAccess = RegistryAccessHelper.getServer();
        if (registryAccess.isEmpty()) return null;

        Registry<BlockPropertiesTransformer> registry = registryAccess.get().registryOrThrow(ElysiumRegistries.Keys.BLOCK_PROPERTIES_TRANSFORMERS);
        Holder<Block> blockHolder = block.builtInRegistryHolder();

        List<BlockPropertiesTransformer> applicable = new ArrayList<>();
        for (Holder<BlockPropertiesTransformer> holder : registry.holders().toList()) {
            BlockPropertiesTransformer transformer = holder.value();
            if (transformer.blocks().contains(blockHolder)) applicable.add(transformer);
        }

        if (applicable.isEmpty()) return null;
        if (applicable.size() == 1) return applicable.getFirst();
        return mergeTransformers(applicable);
    }

    private static BlockPropertiesTransformer mergeTransformers(List<BlockPropertiesTransformer> transformers) {
        BlockPropertiesTransformer.BasicProperties mergedBasic = mergeBasic(transformers);
        BlockPropertiesTransformer.ConditionalProperties mergedConditional = mergeConditional(transformers);
        HolderSet<Block> blocks = transformers.getFirst().blocks();
        return new BlockPropertiesTransformer(blocks, mergedBasic, mergedConditional);
    }

    private static BlockPropertiesTransformer.BasicProperties mergeBasic(List<BlockPropertiesTransformer> transformers) {
        Optional<Float> destroySpeed = Optional.empty();
        Optional<Float> explosionResistance = Optional.empty();
        Optional<Boolean> requiresCorrectToolForDrops = Optional.empty();
        Optional<Boolean> randomTicking = Optional.empty();
        Optional<Float> friction = Optional.empty();
        Optional<Float> speedFactor = Optional.empty();
        Optional<Float> jumpFactor = Optional.empty();
        Optional<Boolean> canOcclude = Optional.empty();
        Optional<Boolean> isAir = Optional.empty();
        Optional<Boolean> ignitedByLava = Optional.empty();
        Optional<PushReactionEnum> pushReaction = Optional.empty();
        Optional<Boolean> spawnTerrainParticles = Optional.empty();
        Optional<Boolean> canBeReplaced = Optional.empty();

        for (BlockPropertiesTransformer entry : transformers) {
            if (destroySpeed.isEmpty() && entry.baseProperties().destroySpeed().isPresent()) destroySpeed = entry.baseProperties().destroySpeed();
            if (explosionResistance.isEmpty() && entry.baseProperties().explosionResistance().isPresent()) explosionResistance = entry.baseProperties().explosionResistance();
            if (requiresCorrectToolForDrops.isEmpty() && entry.baseProperties().requiresCorrectToolForDrops().isPresent()) requiresCorrectToolForDrops = entry.baseProperties().requiresCorrectToolForDrops();
            if (randomTicking.isEmpty() && entry.baseProperties().randomTicking().isPresent()) randomTicking = entry.baseProperties().randomTicking();
            if (friction.isEmpty() && entry.baseProperties().friction().isPresent()) friction = entry.baseProperties().friction();
            if (speedFactor.isEmpty() && entry.baseProperties().speedFactor().isPresent()) speedFactor = entry.baseProperties().speedFactor();
            if (jumpFactor.isEmpty() && entry.baseProperties().jumpFactor().isPresent()) jumpFactor = entry.baseProperties().jumpFactor();
            if (canOcclude.isEmpty() && entry.baseProperties().canOcclude().isPresent()) canOcclude = entry.baseProperties().canOcclude();
            if (isAir.isEmpty() && entry.baseProperties().isAir().isPresent()) isAir = entry.baseProperties().isAir();
            if (ignitedByLava.isEmpty() && entry.baseProperties().ignitedByLava().isPresent()) ignitedByLava = entry.baseProperties().ignitedByLava();
            if (pushReaction.isEmpty() && entry.baseProperties().pushReaction().isPresent()) pushReaction = entry.baseProperties().pushReaction();
            if (spawnTerrainParticles.isEmpty() && entry.baseProperties().spawnTerrainParticles().isPresent()) spawnTerrainParticles = entry.baseProperties().spawnTerrainParticles();
            if (canBeReplaced.isEmpty() && entry.baseProperties().canBeReplaced().isPresent()) canBeReplaced = entry.baseProperties().canBeReplaced();
        }

        return new BlockPropertiesTransformer.BasicProperties(
                destroySpeed, explosionResistance, requiresCorrectToolForDrops, randomTicking,
                friction, speedFactor, jumpFactor, canOcclude, isAir, ignitedByLava,
                pushReaction, spawnTerrainParticles, canBeReplaced
        );
    }

    private static BlockPropertiesTransformer.ConditionalProperties mergeConditional(List<BlockPropertiesTransformer> transformers) {
        Optional<ConditionalInt> lightEmission = Optional.empty();
        Optional<ConditionalBoolean> isValidSpawn = Optional.empty();
        Optional<ConditionalBoolean> isRedstoneConductor = Optional.empty();
        Optional<ConditionalBoolean> isSuffocating = Optional.empty();
        Optional<ConditionalBoolean> isViewBlocking = Optional.empty();
        Optional<ConditionalBoolean> hasPostProcess = Optional.empty();
        Optional<ConditionalBoolean> emissiveRendering = Optional.empty();

        for (BlockPropertiesTransformer entry : transformers) {
            if (lightEmission.isEmpty() && entry.conditionalProperties().lightEmission().isPresent()) lightEmission = entry.conditionalProperties().lightEmission();
            if (isValidSpawn.isEmpty() && entry.conditionalProperties().isValidSpawn().isPresent()) isValidSpawn = entry.conditionalProperties().isValidSpawn();
            if (isRedstoneConductor.isEmpty() && entry.conditionalProperties().isRedstoneConductor().isPresent()) isRedstoneConductor = entry.conditionalProperties().isRedstoneConductor();
            if (isSuffocating.isEmpty() && entry.conditionalProperties().isSuffocating().isPresent()) isSuffocating = entry.conditionalProperties().isSuffocating();
            if (isViewBlocking.isEmpty() && entry.conditionalProperties().isViewBlocking().isPresent()) isViewBlocking = entry.conditionalProperties().isViewBlocking();
            if (hasPostProcess.isEmpty() && entry.conditionalProperties().hasPostProcess().isPresent()) hasPostProcess = entry.conditionalProperties().hasPostProcess();
            if (emissiveRendering.isEmpty() && entry.conditionalProperties().emissiveRendering().isPresent()) emissiveRendering = entry.conditionalProperties().emissiveRendering();
        }

        return new BlockPropertiesTransformer.ConditionalProperties(
                lightEmission, isValidSpawn, isRedstoneConductor, isSuffocating,
                isViewBlocking, hasPostProcess, emissiveRendering
        );
    }

    public static void invalidateCache() {
        CACHE.clear();
        LOGGER.debug("BlockPropertiesTransformer cache cleared for new datapack entries");
    }
}