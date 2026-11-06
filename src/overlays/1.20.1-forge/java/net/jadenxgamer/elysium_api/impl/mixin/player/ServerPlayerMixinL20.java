package net.jadenxgamer.elysium_api.impl.mixin.player;

import net.jadenxgamer.elysium_api.impl.core.attachment.ElysiumAttachmentBackendL20;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// asphodel::mixin -> loader=forge, version=1.20.1 -> file=elysium_api.forge.mixins.json, env=common
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixinL20 {

    @Inject(method = "restoreFrom", at = @At("HEAD"))
    private void elysium$captureAttachments(ServerPlayer oldPlayer, boolean keepEverything, CallbackInfo ci) {
        ElysiumAttachmentBackendL20.captureBeforeRestore(oldPlayer, keepEverything);
    }

    @Inject(method = "restoreFrom", at = @At("TAIL"))
    private void elysium$applyAttachments(ServerPlayer oldPlayer, boolean keepEverything, CallbackInfo ci) {
        ElysiumAttachmentBackendL20.applyAfterRestore((ServerPlayer) (Object) this);
    }
}