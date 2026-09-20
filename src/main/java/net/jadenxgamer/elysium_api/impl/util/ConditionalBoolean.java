package net.jadenxgamer.elysium_api.impl.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public record ConditionalBoolean(boolean defaultValue, List<BooleanRule> rules) {
    public static final Codec<ConditionalBoolean> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("default").forGetter(ConditionalBoolean::defaultValue),
            BooleanRule.CODEC.listOf().optionalFieldOf("rules", List.of()).forGetter(ConditionalBoolean::rules)
    ).apply(instance, ConditionalBoolean::new));

    public boolean getValue(BlockState state) {
        for (BooleanRule rule : rules) if (rule.condition().matches(state)) return rule.value();
        return defaultValue;
    }

    public record BooleanRule(BlockStatePropertiesCondition condition, boolean value) {
        public static final Codec<BooleanRule> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockStatePropertiesCondition.CODEC.fieldOf("when").forGetter(BooleanRule::condition),
                Codec.BOOL.fieldOf("value").forGetter(BooleanRule::value)
        ).apply(instance, BooleanRule::new));
    }
}