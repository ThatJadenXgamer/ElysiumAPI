package net.jadenxgamer.elysium_api.impl.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public record ConditionalInt(int defaultValue, List<IntRule> rules) {
    public static final Codec<ConditionalInt> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("default").forGetter(ConditionalInt::defaultValue),
            IntRule.CODEC.listOf().optionalFieldOf("rules", List.of()).forGetter(ConditionalInt::rules)
    ).apply(instance, ConditionalInt::new));

    public int getValue(BlockState state) {
        for (IntRule rule : rules) if (rule.condition().matches(state)) return rule.value();
        return defaultValue;
    }

    public record IntRule(BlockStatePropertiesCondition condition, int value) {
        public static final Codec<IntRule> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockStatePropertiesCondition.CODEC.fieldOf("when").forGetter(IntRule::condition),
                Codec.INT.fieldOf("value").forGetter(IntRule::value)
        ).apply(instance, IntRule::new));
    }
}