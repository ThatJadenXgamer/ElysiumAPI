package net.jadenxgamer.elysium_api.api.surface_rules.condition;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.jadenxgamer.elysium_api.impl.mixin.accessor.SurfaceRulesContextAccessor;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.SurfaceRules;

public record BiomeTagConditionSource(TagKey<Biome> biomeTag) implements SurfaceRules.ConditionSource {
    public static final KeyDispatchDataCodec<BiomeTagConditionSource> CODEC =
            KeyDispatchDataCodec.of(RecordCodecBuilder.<BiomeTagConditionSource>mapCodec(instance -> instance.group(
                    TagKey.hashedCodec(Registries.BIOME).fieldOf("biome_tag").forGetter(BiomeTagConditionSource::biomeTag)
            ).apply(instance, BiomeTagConditionSource::new)).codec());

    @Override
    public KeyDispatchDataCodec<? extends SurfaceRules.ConditionSource> codec() {
        return CODEC;
    }

    @Override
    public SurfaceRules.Condition apply(final SurfaceRules.Context p_context) {
        class BiomeCondition extends SurfaceRules.LazyYCondition {
            BiomeCondition() {
                super(p_context);
            }

            @Override
            protected boolean compute() {
                Holder<Biome> biome = ((SurfaceRulesContextAccessor) (Object) context).getBiome().get();
                return biome.is(biomeTag);
            }
        }

        return new BiomeCondition();
    }

    @Override
    public String toString() {
        return "BiomeTagConditionSource[biomeTag=" + this.biomeTag + "]";
    }
}