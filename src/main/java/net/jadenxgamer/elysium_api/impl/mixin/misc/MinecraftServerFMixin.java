package net.jadenxgamer.elysium_api.impl.mixin.misc;

import net.jadenxgamer.elysium_api.api.util.RegistryAccessHelper;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// asphodel::mixin -> loader=fabric -> env=common, file=elysium_api.fabric.mixins.json
@Mixin(MinecraftServer.class)
public abstract class MinecraftServerFMixin {

    @Inject(method = "<init>", at = @At("RETURN"))
    private void elysium$captureServer(CallbackInfo ci) {
        RegistryAccessHelper.elysium$setCurrentServer((MinecraftServer) (Object) this);
    }

    @Inject(method = "stopServer", at = @At("HEAD"))
    private void elysium$clearServer(CallbackInfo ci) {
        RegistryAccessHelper.elysium$setCurrentServer(null);
    }
}