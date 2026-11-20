package net.jadenxgamer.elysium_api.impl.mixin.level;

import net.jadenxgamer.elysium_api.api.core.attachment.pipeline.ElysiumEntityTrackingHooks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// asphodel::mixin -> loader=fabric -> env=common, file=elysium_api.fabric.mixins.json
@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
public abstract class TrackedEntityFMixin {

    @Shadow @Final private Entity entity;

    @Inject(method = "updatePlayer", at = @At("HEAD"))
    private void elysium$onStartTracking(ServerPlayer player, CallbackInfo ci) {
        if (player != this.entity) ElysiumEntityTrackingHooks.onPlayerStartTracking(player, this.entity);
    }

    @Inject(method = "removePlayer", at = @At("HEAD"))
    private void elysium$onStopTracking(ServerPlayer player, CallbackInfo ci) {
        if (player != this.entity) ElysiumEntityTrackingHooks.onPlayerStopTracking(player, this.entity);
    }
}