package net.jadenxgamer.elysium_api.impl.mixin.level;

import net.jadenxgamer.elysium_api.api.core.attachment.pipeline.ElysiumChunkTrackingHooks;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// asphodel::mixin -> loader=fabric -> env=common, file=elysium_api.fabric.mixins.json
@Mixin(ChunkMap.class)
public abstract class ChunkMapF21Mixin {

    @Inject(
            method = "markChunkPendingToSend(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/level/chunk/LevelChunk;)V",
            at = @At("TAIL")
    )
    private static void elysium$onChunkQueuedForSend(ServerPlayer player, LevelChunk chunk, CallbackInfo ci) {
        ElysiumChunkTrackingHooks.onChunkStartTracking(player, chunk.getPos());
    }
}