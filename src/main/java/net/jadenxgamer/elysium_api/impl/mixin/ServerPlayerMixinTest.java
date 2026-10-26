package net.jadenxgamer.elysium_api.impl.mixin;

import net.jadenxgamer.elysium_api.impl.network.ElysiumNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// asphodel::mixin -> loader=* -> env=common, file=elysium_api.mixins.json
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixinTest {

    @Unique
    private boolean elysium$hasPinged = false;

    @Inject(method = "tick", at = @At("HEAD"))
    private void elysium$testS2CPing(CallbackInfo ci) {
        if (!this.elysium$hasPinged) {
            this.elysium$hasPinged = true;
            ElysiumNetworking.pingTo((Player) (Object) this, "Hello from Server (S2C)");
        }
    }
}