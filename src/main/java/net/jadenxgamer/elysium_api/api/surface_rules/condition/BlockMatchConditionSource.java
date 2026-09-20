package net.jadenxgamer.elysium_api.api.surface_rules.condition;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.jadenxgamer.elysium_api.impl.mixin.accessor.SurfaceRulesContextAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.SurfaceRules.Context;

public record BlockMatchConditionSource(Block targetBlock) implements SurfaceRules.ConditionSource {
    public static final KeyDispatchDataCodec<BlockMatchConditionSource> CODEC =
            KeyDispatchDataCodec.of(RecordCodecBuilder.mapCodec(instance -> instance.group(
                    BuiltInRegistries.BLOCK.byNameCodec().fieldOf("block").forGetter(BlockMatchConditionSource::targetBlock)
            ).apply(instance, BlockMatchConditionSource::new)));

    @Override
    public KeyDispatchDataCodec<? extends SurfaceRules.ConditionSource> codec() {
        return CODEC;
    }

    @Override
    public SurfaceRules.Condition apply(Context ctx) {
        SurfaceRulesContextAccessor accessor = (SurfaceRulesContextAccessor) (Object) ctx;
        BlockPos.MutableBlockPos pos = accessor.getPos();
        return () -> {
            if (pos != null) {
                pos.set(accessor.getBlockX(), accessor.getBlockY(), accessor.getBlockZ());
                return accessor.getChunk().getBlockState(pos).is(targetBlock);
            }
            return false;
        };
    }

    @Override
    public String toString() {
        return "BlockMatchConditionSource[targetBlock=" + this.targetBlock + "]";
    }
}