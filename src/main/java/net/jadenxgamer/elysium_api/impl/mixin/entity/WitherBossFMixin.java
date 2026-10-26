package net.jadenxgamer.elysium_api.impl.mixin.entity;

import com.llamalad7.mixinextras.sugar.Local;
import net.jadenxgamer.elysium_api.api.extensions.IElysiumBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(WitherBoss.class)
public abstract class WitherBossFMixin {

    @Redirect(
            method = "customServerAiStep",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/boss/wither/WitherBoss;canDestroy(Lnet/minecraft/world/level/block/state/BlockState;)Z"
            ),
            require = 0
    )
    private boolean elysium$canDestroy(BlockState state, @Local(name = "blockPos") BlockPos pos) {
        WitherBoss self = (WitherBoss) (Object) this;
        return ((IElysiumBlock) state.getBlock()).elysiumCanEntityDestroy(state, self.level(), pos, self);
    }
}