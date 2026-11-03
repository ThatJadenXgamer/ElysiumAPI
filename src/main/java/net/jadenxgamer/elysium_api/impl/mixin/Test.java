package net.jadenxgamer.elysium_api.impl.mixin;

import net.jadenxgamer.elysium_api.api.core.extensions.IElysiumBlock;
import net.jadenxgamer.elysium_api.mod.config.ElysiumConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SpongeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;

// asphodel::mixin -> loader=* -> env=common, file=elysium_api.mixins.json
@Mixin(SpongeBlock.class)
public class Test implements IElysiumBlock {

    @Override
    public InteractionResult elysiumUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        String side = level.isClientSide() ? "CLIENT" : "SERVER";
        String value = String.valueOf(ElysiumConfig.SPONGE_RIGHT_CLICK_TEST.get());
        player.sendSystemMessage(Component.literal("[" + side + "] sponge_right_click_test = " + value));
        return InteractionResult.SUCCESS;
    }
}