package net.jadenxgamer.elysium_api.impl.core.misc.forge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.biome.*;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.BiomeSpecialEffectsBuilder;
import net.minecraftforge.common.world.ModifiableBiomeInfo;

import java.util.Optional;

public record EffectsBiomeModifier(HolderSet<Biome> biomes, Optional<Integer> skyColor, Optional<Integer> fogColor,
                                   Optional<Integer> waterColor, Optional<Integer> waterFogColor,
                                   Optional<Integer> grassColor, Optional<Integer> foliageColor, Optional<BiomeSpecialEffects.GrassColorModifier> grassColorModifier,
                                   Optional<AmbientParticleSettings> particle,
                                   Optional<Holder<SoundEvent>> ambientSound, Optional<AmbientAdditionsSettings> additionsSound, Optional<AmbientMoodSettings> moodSound, Optional<Music> music)
        implements BiomeModifier {
    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase == Phase.MODIFY && this.biomes.contains(biome)) {
            BiomeSpecialEffectsBuilder effectBuilder = builder.getSpecialEffects();
            skyColor.ifPresent(effectBuilder::skyColor);
            fogColor.ifPresent(effectBuilder::fogColor);
            waterColor.ifPresent(effectBuilder::waterColor);
            waterFogColor.ifPresent(effectBuilder::waterFogColor);
            grassColor.ifPresent(effectBuilder::grassColorOverride);
            foliageColor.ifPresent(effectBuilder::foliageColorOverride);
            grassColorModifier.ifPresent(effectBuilder::grassColorModifier);
            particle.ifPresent(effectBuilder::ambientParticle);

            ambientSound.ifPresent(effectBuilder::ambientLoopSound);
            additionsSound.ifPresent(effectBuilder::ambientAdditionsSound);
            moodSound.ifPresent(effectBuilder::ambientMoodSound);
            music.ifPresent(effectBuilder::backgroundMusic);
        }
    }

    @Override
    public Codec<? extends BiomeModifier> codec() {
        return CODEC;
    }

    public static final Codec<EffectsBiomeModifier> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Biome.LIST_CODEC.fieldOf("biomes").forGetter(EffectsBiomeModifier::biomes),
            Codec.INT.optionalFieldOf("sky_color").forGetter(EffectsBiomeModifier::skyColor),
            Codec.INT.optionalFieldOf("fog_color").forGetter(EffectsBiomeModifier::fogColor),
            Codec.INT.optionalFieldOf("water_color").forGetter(EffectsBiomeModifier::waterColor),
            Codec.INT.optionalFieldOf("water_fog_color").forGetter(EffectsBiomeModifier::waterFogColor),
            Codec.INT.optionalFieldOf("grass_color").forGetter(EffectsBiomeModifier::grassColor),
            Codec.INT.optionalFieldOf("foliage_color").forGetter(EffectsBiomeModifier::foliageColor),
            BiomeSpecialEffects.GrassColorModifier.CODEC.optionalFieldOf("grass_color_modifier").forGetter(EffectsBiomeModifier::grassColorModifier),
            AmbientParticleSettings.CODEC.optionalFieldOf("particle").forGetter(EffectsBiomeModifier::particle),
            SoundEvent.CODEC.optionalFieldOf("ambient_sound").forGetter(EffectsBiomeModifier::ambientSound),
            AmbientAdditionsSettings.CODEC.optionalFieldOf("additions_sound").forGetter(EffectsBiomeModifier::additionsSound),
            AmbientMoodSettings.CODEC.optionalFieldOf("mood_sound").forGetter(EffectsBiomeModifier::moodSound),
            Music.CODEC.optionalFieldOf("music").forGetter(EffectsBiomeModifier::music)
    ).apply(instance, EffectsBiomeModifier::new));
}