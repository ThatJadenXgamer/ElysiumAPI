package net.jadenxgamer.elysium_api.impl.mixin.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.function.Supplier;

@Mixin(Block.class)
public interface BlockAccessor {

    @Invoker("spawnDestroyParticles")
    void elysium$invokeSpawnDestroyParticles(Level level, Player player, BlockPos pos, BlockState state);

    @Invoker("popExperience")
    void elysium$invokePopExperience(ServerLevel level, BlockPos pos, int amount);
}