package net.jadenxgamer.elysium_api.impl.mixin.entity;

import net.jadenxgamer.elysium_api.api.extensions.IElysiumBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// asphodel::mixin -> loader=fabric -> env=common, file=elysium_api.fabric.mixins.json
@Mixin(Entity.class)
public abstract class EntityCollisionFMixin {

    @Inject(method = "getOnPos(F)Lnet/minecraft/core/BlockPos;", at = @At("HEAD"), cancellable = true, require = 0)
    private void elysium$getOnPos(float yOffset, CallbackInfoReturnable<BlockPos> cir) {
        Entity self = (Entity) (Object) this;
        if (self.mainSupportingBlockPos.isEmpty()) return;

        BlockPos blockPos = self.mainSupportingBlockPos.get();
        if (!(yOffset > 1.0E-5F)) {
            cir.setReturnValue(blockPos);
            return;
        }
        BlockState blockState = self.level().getBlockState(blockPos);
        boolean extendsVertically = ((IElysiumBlock) blockState.getBlock()).elysiumCollisionExtendsVertically(blockState, self.level(), blockPos, self);

        if (yOffset <= 0.5F && extendsVertically) cir.setReturnValue(blockPos);
        else cir.setReturnValue(blockPos.atY(Mth.floor(self.getY() - (double) yOffset)));
    }
}