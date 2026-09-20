package net.jadenxgamer.elysium_api.impl.mixin.block;

import net.jadenxgamer.elysium_api.impl.core.datadriven.block.properties_transformer.BlockPropertiesTransformer;
import net.jadenxgamer.elysium_api.impl.core.datadriven.block.properties_transformer.BlockPropertiesTransformerHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseMixin {

    @Shadow public abstract Block getBlock();

    @Shadow
    protected abstract BlockState asState();

    @Inject(
            method = "getDestroySpeed",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void elysium_api$overrideDestroySpeed(CallbackInfoReturnable<Float> cir) {
        BlockPropertiesTransformer transformer = BlockPropertiesTransformerHelper.getTransformer(getBlock());
        if (transformer != null && transformer.baseProperties().destroySpeed().isPresent()) {
            cir.setReturnValue(transformer.baseProperties().destroySpeed().get());
        }
    }

    @Inject(
            method = "canBeReplaced()Z",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void elysium_api$overrideCanBeReplaced(CallbackInfoReturnable<Boolean> cir) {
        BlockPropertiesTransformer transformer = BlockPropertiesTransformerHelper.getTransformer(getBlock());
        if (transformer != null && transformer.baseProperties().canBeReplaced().isPresent()) {
            cir.setReturnValue(transformer.baseProperties().canBeReplaced().get());
        }
    }

    @Inject(
            method = "getLightEmission",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void elysium_api$overrideLightEmission(CallbackInfoReturnable<Integer> cir) {
        BlockPropertiesTransformer transformer = BlockPropertiesTransformerHelper.getTransformer(getBlock());
        if (transformer != null && transformer.conditionalProperties().lightEmission().isPresent()) {
            cir.setReturnValue(transformer.conditionalProperties().lightEmission().get().getValue(asState()));
        }
    }

    @Inject(
            method = "canOcclude",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void elysium_api$overrideCanOcclude(CallbackInfoReturnable<Boolean> cir) {
        BlockPropertiesTransformer transformer = BlockPropertiesTransformerHelper.getTransformer(getBlock());
        if (transformer != null && transformer.baseProperties().canOcclude().isPresent()) {
            cir.setReturnValue(transformer.baseProperties().canOcclude().get());
        }
    }

    @Inject(
            method = "isAir",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void elysium_api$overrideIsAir(CallbackInfoReturnable<Boolean> cir) {
        BlockPropertiesTransformer transformer = BlockPropertiesTransformerHelper.getTransformer(getBlock());
        if (transformer != null && transformer.baseProperties().isAir().isPresent()) {
            cir.setReturnValue(transformer.baseProperties().isAir().get());
        }
    }

    @Inject(
            method = "ignitedByLava",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void elysium_api$overrideIgnitedByLava(CallbackInfoReturnable<Boolean> cir) {
        BlockPropertiesTransformer transformer = BlockPropertiesTransformerHelper.getTransformer(getBlock());
        if (transformer != null && transformer.baseProperties().ignitedByLava().isPresent()) {
            cir.setReturnValue(transformer.baseProperties().ignitedByLava().get());
        }
    }

    @Inject(
            method = "getPistonPushReaction",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void elysium_api$overridePushReaction(CallbackInfoReturnable<PushReaction> cir) {
        BlockPropertiesTransformer transformer = BlockPropertiesTransformerHelper.getTransformer(getBlock());
        if (transformer != null && transformer.baseProperties().pushReaction().isPresent()) {
            cir.setReturnValue(transformer.baseProperties().pushReaction().get().toPushReaction());
        }
    }

    @Inject(
            method = "shouldSpawnTerrainParticles",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void elysium_api$overrideSpawnTerrainParticles(CallbackInfoReturnable<Boolean> cir) {
        BlockPropertiesTransformer transformer = BlockPropertiesTransformerHelper.getTransformer(getBlock());
        if (transformer != null && transformer.baseProperties().spawnTerrainParticles().isPresent()) {
            cir.setReturnValue(transformer.baseProperties().spawnTerrainParticles().get());
        }
    }

    @Inject(
            method = "requiresCorrectToolForDrops",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void elysium_api$overrideRequiresCorrectTool(CallbackInfoReturnable<Boolean> cir) {
        BlockPropertiesTransformer transformer = BlockPropertiesTransformerHelper.getTransformer(getBlock());
        if (transformer != null && transformer.baseProperties().requiresCorrectToolForDrops().isPresent()) {
            cir.setReturnValue(transformer.baseProperties().requiresCorrectToolForDrops().get());
        }
    }

    @Inject(
            method = "isRandomlyTicking",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void elysium_api$overrideRandomTicking(CallbackInfoReturnable<Boolean> cir) {
        BlockPropertiesTransformer transformer = BlockPropertiesTransformerHelper.getTransformer(getBlock());
        if (transformer != null && transformer.baseProperties().randomTicking().isPresent()) {
            cir.setReturnValue(transformer.baseProperties().randomTicking().get());
        }
    }

    @Inject(
            method = "isValidSpawn",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void elysium_api$overrideIsValidSpawn(BlockGetter level, BlockPos pos, EntityType<?> entityType, CallbackInfoReturnable<Boolean> cir) {
        BlockPropertiesTransformer transformer = BlockPropertiesTransformerHelper.getTransformer(getBlock());
        if (transformer != null && transformer.conditionalProperties().isValidSpawn().isPresent()) {
            cir.setReturnValue(transformer.conditionalProperties().isValidSpawn().get().getValue(asState()));
        }
    }

    @Inject(
            method = "isRedstoneConductor",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void elysium_api$overrideIsRedstoneConductor(BlockGetter level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        BlockPropertiesTransformer transformer = BlockPropertiesTransformerHelper.getTransformer(getBlock());
        if (transformer != null && transformer.conditionalProperties().isRedstoneConductor().isPresent()) {
            cir.setReturnValue(transformer.conditionalProperties().isRedstoneConductor().get().getValue(asState()));
        }
    }

    @Inject(
            method = "isSuffocating",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void elysium_api$overrideIsSuffocating(BlockGetter level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        BlockPropertiesTransformer transformer = BlockPropertiesTransformerHelper.getTransformer(getBlock());
        if (transformer != null && transformer.conditionalProperties().isSuffocating().isPresent()) {
            cir.setReturnValue(transformer.conditionalProperties().isSuffocating().get().getValue(asState()));
        }
    }

    @Inject(
            method = "isViewBlocking",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void elysium_api$overrideIsViewBlocking(BlockGetter level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        BlockPropertiesTransformer transformer = BlockPropertiesTransformerHelper.getTransformer(getBlock());
        if (transformer != null && transformer.conditionalProperties().isViewBlocking().isPresent()) {
            cir.setReturnValue(transformer.conditionalProperties().isViewBlocking().get().getValue(asState()));
        }
    }

    @Inject(
            method = "hasPostProcess",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void elysium_api$overrideHasPostProcess(BlockGetter level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        BlockPropertiesTransformer transformer = BlockPropertiesTransformerHelper.getTransformer(getBlock());
        if (transformer != null && transformer.conditionalProperties().hasPostProcess().isPresent()) {
            cir.setReturnValue(transformer.conditionalProperties().hasPostProcess().get().getValue(asState()));
        }
    }

    @Inject(
            method = "emissiveRendering",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void elysium_api$overrideEmissiveRendering(BlockGetter level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        BlockPropertiesTransformer transformer = BlockPropertiesTransformerHelper.getTransformer(getBlock());
        if (transformer != null && transformer.conditionalProperties().emissiveRendering().isPresent()) {
            cir.setReturnValue(transformer.conditionalProperties().emissiveRendering().get().getValue(asState()));
        }
    }
}