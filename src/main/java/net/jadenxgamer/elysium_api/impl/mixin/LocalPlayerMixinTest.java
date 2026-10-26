package net.jadenxgamer.elysium_api.impl.mixin;

import net.jadenxgamer.elysium_api.impl.network.ElysiumNetworking;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// asphodel::mixin -> loader=* -> env=common, file=elysium_api.mixins.json
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixinTest {

    @Unique
    private boolean elysium$hasPinged = false;

    @Inject(method = "tick", at = @At("HEAD"))
    private void elysium$testC2SPing(CallbackInfo ci) {
        if (!this.elysium$hasPinged) {
            this.elysium$hasPinged = true;
            ElysiumNetworking.ping("Hello from Client (C2S)");
        }
    }
}