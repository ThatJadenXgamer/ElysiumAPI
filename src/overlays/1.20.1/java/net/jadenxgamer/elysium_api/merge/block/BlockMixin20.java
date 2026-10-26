package net.jadenxgamer.elysium_api.merge.block;

import java.util.List;
import net.jadenxgamer.elysium_api.api.extensions.IElysiumBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public abstract class BlockMixin20 implements IElysiumBlock {

    @Inject(
            method = "playerWillDestroy",
            at = @At("HEAD"), cancellable = true
    )
    private void elysium$playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player, CallbackInfo ci) {
        this.elysiumOnPlayerWillDestroy(level, pos, state, player);
        ci.cancel();
    }

    @Inject(
            method = "getCloneItemStack",
            at = @At("HEAD"), cancellable = true
    )
    private void elysium$getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state, CallbackInfoReturnable<ItemStack> cir) {
        cir.setReturnValue(this.elysiumGetCloneItemStack(level, pos, state));
    }

    @Inject(
            method = "appendHoverText",
            at = @At("HEAD"), cancellable = true
    )
    private void elysium$appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag, CallbackInfo ci) {
        this.elysiumAppendHoverText(stack, level, tooltip, flag);
        ci.cancel();
    }
}