package net.jadenxgamer.elysium_api.impl.core.datadriven.block.use_behaviors;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.jadenxgamer.elysium_api.impl.util.BlockStatePropertiesCondition;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Optional;

public record UseBehavior(HolderSet<Block> blocks, Optional<BlockStatePropertiesCondition> blockstateCondition, HolderSet<Item> itemCondition, int chanceToFail, Behavior behavior) {
    public static final Codec<UseBehavior> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("blocks").forGetter(UseBehavior::blocks),
            BlockStatePropertiesCondition.CODEC.optionalFieldOf("blockstate_condition").forGetter(UseBehavior::blockstateCondition),
            RegistryCodecs.homogeneousList(Registries.ITEM).fieldOf("item_condition").forGetter(UseBehavior::itemCondition),
            Codec.INT.optionalFieldOf("chance_to_fail", 0).forGetter(UseBehavior::chanceToFail),
            Behavior.CODEC.fieldOf("behaviors").forGetter(UseBehavior::behavior)
    ).apply(instance, UseBehavior::new));

    public record Behavior(UseBehaviorTypeEnum type, Optional<BlockState> block,
                           Optional<ResourceLocation> item, int itemCount, Optional<ResourceLocation> feature,
                           PosEnum pos, int posOffset, AfterUseItemEnum afterUseItem, boolean canReplace, boolean breakParticles,
                           Optional<Sounds> sounds, Optional<Particles> particles,
                           Optional<List<String>> copyProperties) {
        public static final Codec<Behavior> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                UseBehaviorTypeEnum.CODEC.optionalFieldOf("type", UseBehaviorTypeEnum.PLACE).forGetter(Behavior::type),
                BlockState.CODEC.optionalFieldOf("blockstate").forGetter(Behavior::block),
                ResourceLocation.CODEC.optionalFieldOf("item").forGetter(Behavior::item),
                Codec.INT.optionalFieldOf("item_count", 1).forGetter(Behavior::itemCount),
                ResourceLocation.CODEC.optionalFieldOf("feature").forGetter(Behavior::feature),
                PosEnum.CODEC.optionalFieldOf("pos", PosEnum.NOOP).forGetter(Behavior::pos),
                Codec.INT.optionalFieldOf("pos_offset", 1).forGetter(Behavior::posOffset),
                AfterUseItemEnum.CODEC.optionalFieldOf("after_use_item", AfterUseItemEnum.NOTHING).forGetter(Behavior::afterUseItem),
                Codec.BOOL.optionalFieldOf("can_replace_solids", false).forGetter(Behavior::canReplace),
                Codec.BOOL.optionalFieldOf("break_particles", false).forGetter(Behavior::breakParticles),
                Sounds.CODEC.optionalFieldOf("sounds").forGetter(Behavior::sounds),
                Particles.CODEC.optionalFieldOf("particles").forGetter(Behavior::particles),
                Codec.list(Codec.STRING).optionalFieldOf("copy_properties").forGetter(Behavior::copyProperties)
        ).apply(instance, Behavior::new));
    }

    public record Sounds(SoundEvent soundEvent, float volume, float pitch) {

        public static final Codec<Sounds> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BuiltInRegistries.SOUND_EVENT.byNameCodec().fieldOf("sound_event").forGetter(Sounds::soundEvent),
                Codec.FLOAT.optionalFieldOf("volume", 1.0f).forGetter(Sounds::volume),
                Codec.FLOAT.optionalFieldOf("pitch", 1.0f).forGetter(Sounds::pitch)
        ).apply(instance, Sounds::new));

        public float getVolume() {
            return volume;
        }
    }

    public record Particles(ResourceLocation particleType, int count, double speed, double xOffset, double yOffset, double zOffset) {
        public static final Codec<Particles> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("particle_type").forGetter(Particles::particleType),
                Codec.INT.optionalFieldOf("count", 1).forGetter(Particles::count),
                Codec.DOUBLE.optionalFieldOf("speed", 0.0).forGetter(Particles::speed),
                Codec.DOUBLE.optionalFieldOf("x_offset", 0.0).forGetter(Particles::xOffset),
                Codec.DOUBLE.optionalFieldOf("y_offset", 0.0).forGetter(Particles::yOffset),
                Codec.DOUBLE.optionalFieldOf("z_offset", 0.0).forGetter(Particles::zOffset)
        ).apply(instance, Particles::new));
    }
}