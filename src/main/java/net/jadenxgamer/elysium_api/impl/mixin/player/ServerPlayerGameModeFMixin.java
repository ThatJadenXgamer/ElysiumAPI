package net.jadenxgamer.elysium_api.impl.mixin.player;

import com.llamalad7.mixinextras.sugar.Local;
import net.jadenxgamer.elysium_api.api.extensions.IElysiumBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// asphodel::mixin -> loader=fabric -> env=common, file=elysium_api.fabric.mixins.json
@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeFMixin {

    @Shadow @Final protected ServerPlayer player;

    @Redirect(
            method = "destroyBlock",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;hasCorrectToolForDrops(Lnet/minecraft/world/level/block/state/BlockState;)Z"),
            require = 0
    )
    private boolean elysium$canHarvest(ServerPlayer player, BlockState state, @Local() BlockState blockState, @Local(argsOnly = true) BlockPos pos) {
        return ((IElysiumBlock) state.getBlock()).elysiumCanHarvestBlock(state, player.serverLevel(), pos, player);
    }

    @Redirect(
            method = "destroyBlock",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z"),
            require = 0
    )
    private boolean elysium$onDestroyedByPlayer(ServerLevel level, BlockPos pos, boolean moving, @Local(name = "blockState") BlockState state) {
        boolean willHarvest = ((IElysiumBlock) state.getBlock()).elysiumCanHarvestBlock(state, level, pos, this.player);
        FluidState fluid = level.getFluidState(pos);
        return ((IElysiumBlock) state.getBlock()).elysiumOnDestroyedByPlayer(state, level, pos, this.player, willHarvest, fluid);
    }
}