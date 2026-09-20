package net.jadenxgamer.elysium_api.impl.core.datadriven.block.properties_transformer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.jadenxgamer.elysium_api.impl.util.ConditionalBoolean;
import net.jadenxgamer.elysium_api.impl.util.ConditionalInt;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;

import java.util.Optional;

public record BlockPropertiesTransformer(
        HolderSet<Block> blocks,
        BasicProperties baseProperties,
        ConditionalProperties conditionalProperties
) {
    public static final Codec<BlockPropertiesTransformer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("blocks").forGetter(BlockPropertiesTransformer::blocks),
            BasicProperties.CODEC.fieldOf("base_properties").forGetter(BlockPropertiesTransformer::baseProperties),
            ConditionalProperties.CODEC.fieldOf("conditional_properties").forGetter(BlockPropertiesTransformer::conditionalProperties)
    ).apply(instance, BlockPropertiesTransformer::new));

    public record BasicProperties(
            Optional<Float> destroySpeed,
            Optional<Float> explosionResistance,
            Optional<Boolean> requiresCorrectToolForDrops,
            Optional<Boolean> randomTicking,
            Optional<Float> friction,
            Optional<Float> speedFactor,
            Optional<Float> jumpFactor,
            Optional<Boolean> canOcclude,
            Optional<Boolean> isAir,
            Optional<Boolean> ignitedByLava,
            Optional<PushReactionEnum> pushReaction,
            Optional<Boolean> spawnTerrainParticles,
            Optional<Boolean> canBeReplaced
    ) {
        public static final Codec<BasicProperties> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.optionalFieldOf("destroy_speed").forGetter(BasicProperties::destroySpeed),
                Codec.FLOAT.optionalFieldOf("explosion_resistance").forGetter(BasicProperties::explosionResistance),
                Codec.BOOL.optionalFieldOf("requires_correct_tool_for_drops").forGetter(BasicProperties::requiresCorrectToolForDrops),
                Codec.BOOL.optionalFieldOf("random_ticking").forGetter(BasicProperties::randomTicking),
                Codec.FLOAT.optionalFieldOf("friction").forGetter(BasicProperties::friction),
                Codec.FLOAT.optionalFieldOf("speed_factor").forGetter(BasicProperties::speedFactor),
                Codec.FLOAT.optionalFieldOf("jump_factor").forGetter(BasicProperties::jumpFactor),
                Codec.BOOL.optionalFieldOf("can_occlude").forGetter(BasicProperties::canOcclude),
                Codec.BOOL.optionalFieldOf("is_air").forGetter(BasicProperties::isAir),
                Codec.BOOL.optionalFieldOf("ignited_by_lava").forGetter(BasicProperties::ignitedByLava),
                PushReactionEnum.CODEC.optionalFieldOf("push_reaction").forGetter(BasicProperties::pushReaction),
                Codec.BOOL.optionalFieldOf("spawn_terrain_particles").forGetter(BasicProperties::spawnTerrainParticles),
                Codec.BOOL.optionalFieldOf("can_be_replaced").forGetter(BasicProperties::canBeReplaced)
        ).apply(instance, BasicProperties::new));
    }

    public record ConditionalProperties(
            Optional<ConditionalInt> lightEmission,
            Optional<ConditionalBoolean> isValidSpawn,
            Optional<ConditionalBoolean> isRedstoneConductor,
            Optional<ConditionalBoolean> isSuffocating,
            Optional<ConditionalBoolean> isViewBlocking,
            Optional<ConditionalBoolean> hasPostProcess,
            Optional<ConditionalBoolean> emissiveRendering
    ) {
        public static final Codec<ConditionalProperties> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ConditionalInt.CODEC.optionalFieldOf("light_emission").forGetter(ConditionalProperties::lightEmission),
                ConditionalBoolean.CODEC.optionalFieldOf("is_valid_spawn").forGetter(ConditionalProperties::isValidSpawn),
                ConditionalBoolean.CODEC.optionalFieldOf("is_redstone_conductor").forGetter(ConditionalProperties::isRedstoneConductor),
                ConditionalBoolean.CODEC.optionalFieldOf("is_suffocating").forGetter(ConditionalProperties::isSuffocating),
                ConditionalBoolean.CODEC.optionalFieldOf("is_view_blocking").forGetter(ConditionalProperties::isViewBlocking),
                ConditionalBoolean.CODEC.optionalFieldOf("has_post_process").forGetter(ConditionalProperties::hasPostProcess),
                ConditionalBoolean.CODEC.optionalFieldOf("emissive_rendering").forGetter(ConditionalProperties::emissiveRendering)
        ).apply(instance, ConditionalProperties::new));
    }
}