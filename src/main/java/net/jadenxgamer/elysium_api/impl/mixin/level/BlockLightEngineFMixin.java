package net.jadenxgamer.elysium_api.impl.mixin.level;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.jadenxgamer.elysium_api.api.extensions.IElysiumBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.BlockLightEngine;
import net.minecraft.world.level.lighting.BlockLightSectionStorage;
import net.minecraft.world.level.lighting.LightEngine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

// asphodel::mixin -> loader=fabric -> env=common, file=elysium_api.fabric.mixins.json
@Mixin(BlockLightEngine.class)
public abstract class BlockLightEngineFMixin extends LightEngine {

    @Shadow
    @Final
    private BlockPos.MutableBlockPos mutablePos;

    protected BlockLightEngineFMixin(LightChunkGetter chunkSource, BlockLightSectionStorage storage) {
        super(chunkSource, storage);
    }

    @ModifyExpressionValue(
            method = "getEmission",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getLightEmission()I"), require = 0
    )
    private int elysium$getLightEmission(int original, @Local(argsOnly = true) BlockState state) {
        return ((IElysiumBlock) state.getBlock()).elysiumGetLightEmission(state, this.chunkSource.getLevel(), this.mutablePos);
    }

    @ModifyExpressionValue(
            method = "method_51532", // thank you https://wiki.fabricmc.net/drafts:mixin_injectors!
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getLightEmission()I"), require = 0
    )
    private int elysium$lambdaLightEmissionInPropagate(int original, @Local(argsOnly = true) BlockPos blockPos, @Local(argsOnly = true) BlockState state) {
        return ((IElysiumBlock) state.getBlock()).elysiumGetLightEmission(state, this.chunkSource.getLevel(), blockPos);
    }
}