package net.jadenxgamer.elysium_api.impl.mixin.player;

import net.jadenxgamer.elysium_api.api.core.extensions.IElysiumItem;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// asphodel::mixin -> loader=* -> env=common, file=elysium_api.mixins.json
@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {

    @Inject(
            method = "drop",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void elysium$drop(boolean fullStack, CallbackInfoReturnable<Boolean> cir) {
        /*
         * For whatever reason, the maintainers of Forge and NeoForge never caught that the onDroppedByPlayer method only gets
         * queried on the ServerPlayer side while the client still attempts to drop and remove the item. causing a desync,
         * I noticed this while adding a parity to this method on Fabric's side, this mixin should fix that issue on all loaders though.
         */
        var player = (LocalPlayer) (Object) this;
        var inventory = player.getInventory();
        var selected = inventory.getSelected();
        if (selected.isEmpty() || !((IElysiumItem) selected.getItem()).elysiumOnDroppedByPlayer(selected, player)) {
            ServerboundPlayerActionPacket.Action action = fullStack
                    ? ServerboundPlayerActionPacket.Action.DROP_ALL_ITEMS
                    : ServerboundPlayerActionPacket.Action.DROP_ITEM;
            player.connection.send(new ServerboundPlayerActionPacket(action, BlockPos.ZERO, Direction.DOWN));
            cir.setReturnValue(false);
        }
    }
}