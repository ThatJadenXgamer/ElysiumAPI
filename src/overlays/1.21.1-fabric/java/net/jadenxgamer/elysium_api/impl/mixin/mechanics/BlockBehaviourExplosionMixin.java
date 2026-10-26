package net.jadenxgamer.elysium_api.impl.mixin.mechanics;

import com.llamalad7.mixinextras.sugar.Local;
import net.jadenxgamer.elysium_api.api.extensions.IElysiumBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BlockBehaviour.class)
public abstract class BlockBehaviourExplosionMixin {

    @Redirect(
            method = "onExplosionHit",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/Block;dropFromExplosion(Lnet/minecraft/world/level/Explosion;)Z"
            ),
            require = 0
    )
    private boolean elysium$canDropFromExplosion(Block block, Explosion explosion, @Local(argsOnly = true) BlockState state, @Local(argsOnly = true) Level level, @Local(argsOnly = true) BlockPos pos) {
        return ((IElysiumBlock) block).elysiumCanDropFromExplosion(state, level, pos, explosion);
    }

    @Redirect(
            method = "onExplosionHit",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/Block;wasExploded(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/Explosion;)V"
            ),
            require = 0
    )
    private void elysium$onBlockExploded(Block block, Level level, BlockPos pos, Explosion explosion, @Local(argsOnly = true) BlockState state) {
        ((IElysiumBlock) block).elysiumOnBlockExploded(state, level, pos, explosion);
    }
}