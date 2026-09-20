package net.jadenxgamer.elysium_api.impl.mixin.block;

import net.jadenxgamer.elysium_api.api.extensions.IElysiumBlockExtension;
import net.jadenxgamer.elysium_api.impl.core.datadriven.block.properties_transformer.BlockPropertiesTransformer;
import net.jadenxgamer.elysium_api.impl.core.datadriven.block.properties_transformer.BlockPropertiesTransformerHelper;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public abstract class BlockMixin implements IElysiumBlockExtension {

    @Unique
    private final Block elysium$self = ((Block) (Object) this);

    @Inject(
            method = "getFriction",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void elysium_api$overrideFriction(CallbackInfoReturnable<Float> cir) {
        BlockPropertiesTransformer transformer = BlockPropertiesTransformerHelper.getTransformer(elysium$self);
        if (transformer != null && transformer.baseProperties().friction().isPresent()) {
            cir.setReturnValue(transformer.baseProperties().friction().get());
        }
    }

    @Inject(
            method = "getSpeedFactor",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void elysium_api$overrideSpeedFactor(CallbackInfoReturnable<Float> cir) {
        BlockPropertiesTransformer transformer = BlockPropertiesTransformerHelper.getTransformer(elysium$self);
        if (transformer != null && transformer.baseProperties().speedFactor().isPresent()) {
            cir.setReturnValue(transformer.baseProperties().speedFactor().get());
        }
    }

    @Inject(
            method = "getJumpFactor",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void elysium_api$overrideJumpFactor(CallbackInfoReturnable<Float> cir) {
        BlockPropertiesTransformer transformer = BlockPropertiesTransformerHelper.getTransformer(elysium$self);
        if (transformer != null && transformer.baseProperties().jumpFactor().isPresent()) {
            cir.setReturnValue(transformer.baseProperties().jumpFactor().get());
        }
    }

    @Inject(
            method = "getExplosionResistance",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void elysium_api$overrideExplosionResistance(CallbackInfoReturnable<Float> cir) {
        BlockPropertiesTransformer transformer = BlockPropertiesTransformerHelper.getTransformer(elysium$self);
        if (transformer != null && transformer.baseProperties().explosionResistance().isPresent()) {
            cir.setReturnValue(transformer.baseProperties().explosionResistance().get());
        }
    }
}