package net.jadenxgamer.elysium_api.impl.mixin.player;

import net.jadenxgamer.elysium_api.api.core.extensions.IElysiumItem;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// asphodel::mixin -> loader=fabric -> env=common, file=elysium_api.fabric.mixins.json
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerFMixin {

    @Inject(
            method = "drop(Z)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayer;getInventory()Lnet/minecraft/world/entity/player/Inventory;",
                    shift = At.Shift.AFTER
            ),
            cancellable = true
    )
    private void elysium$drop(boolean dropStack, CallbackInfoReturnable<Boolean> cir) {
        var player = (ServerPlayer) (Object) this;
        var inventory = player.getInventory();
        var selected = inventory.getSelected();
        if (selected.isEmpty() || !((IElysiumItem) selected.getItem()).elysiumOnDroppedByPlayer(selected, player)) cir.setReturnValue(false);
    }
}