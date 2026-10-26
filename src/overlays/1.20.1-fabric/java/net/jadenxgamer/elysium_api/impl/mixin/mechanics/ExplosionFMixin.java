package net.jadenxgamer.elysium_api.impl.mixin.mechanics;

import com.llamalad7.mixinextras.sugar.Local;
import net.jadenxgamer.elysium_api.api.extensions.IElysiumBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Explosion.class)
public abstract class ExplosionFMixin {

    @Shadow @Final private Level level;

    @Redirect(
            method = "finalizeExplosion",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;dropFromExplosion(Lnet/minecraft/world/level/Explosion;)Z"),
            require = 0
    )
    private boolean elysium$canDropFromExplosion(Block block, Explosion explosion, @Local BlockState state, @Local(name = "blockPos") BlockPos pos) {
        return ((IElysiumBlock) block).elysiumCanDropFromExplosion(state, this.level, pos, explosion);
    }

    @Redirect(
            method = "finalizeExplosion",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/Block;wasExploded(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/Explosion;)V"
            ),
            require = 0
    )
    private void elysium$onBlockExploded(Block block, Level level, BlockPos pos, Explosion explosion, @Local BlockState blockState) {
        ((IElysiumBlock) block).elysiumOnBlockExploded(blockState, level, pos, explosion);
    }
}