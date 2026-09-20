package net.jadenxgamer.elysium_api.impl.core.datadriven.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;

public record BlockSoundTransformer(HolderSet<Block> blocks, SoundEvent breakSound, SoundEvent stepSound,
                                    SoundEvent placeSound, SoundEvent hitSound, SoundEvent fallSound,
                                    float volume, float pitch) {
    public static final Codec<BlockSoundTransformer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("blocks").forGetter(BlockSoundTransformer::blocks),
            BuiltInRegistries.SOUND_EVENT.byNameCodec().fieldOf("break_sound").forGetter(BlockSoundTransformer::breakSound),
            BuiltInRegistries.SOUND_EVENT.byNameCodec().fieldOf("step_sound").forGetter(BlockSoundTransformer::stepSound),
            BuiltInRegistries.SOUND_EVENT.byNameCodec().fieldOf("place_sound").forGetter(BlockSoundTransformer::placeSound),
            BuiltInRegistries.SOUND_EVENT.byNameCodec().fieldOf("hit_sound").forGetter(BlockSoundTransformer::hitSound),
            BuiltInRegistries.SOUND_EVENT.byNameCodec().fieldOf("fall_sound").forGetter(BlockSoundTransformer::fallSound),
            Codec.FLOAT.fieldOf("volume").orElse(1.0f).forGetter(BlockSoundTransformer::volume),
            Codec.FLOAT.fieldOf("pitch").orElse(1.0f).forGetter(BlockSoundTransformer::pitch)
    ).apply(instance, BlockSoundTransformer::new));

    public SoundType toSoundType() {
        return new SoundType(volume, pitch, breakSound, stepSound, placeSound, hitSound, fallSound);
    }
}