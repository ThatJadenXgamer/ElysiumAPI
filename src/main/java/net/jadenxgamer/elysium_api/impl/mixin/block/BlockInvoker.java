package net.jadenxgamer.elysium_api.impl.mixin.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Block.class)
public interface BlockInvoker {

    @Invoker("spawnDestroyParticles")
    void elysium$invokeSpawnDestroyParticles(Level level, Player player, BlockPos pos, BlockState state);
}