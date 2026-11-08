package net.jadenxgamer.elysium_api.api.core.attachment.pipeline;

import net.jadenxgamer.elysium_api.api.network.ElysiumNetwork;
import net.jadenxgamer.elysium_api.api.network.ElysiumNetworkContext;
import net.jadenxgamer.elysium_api.mod.network.payload.client.ElysiumAttachmentSyncPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public final class ElysiumAttachmentSyncDispatcher {

    @Nullable private static ClientApplier clientApplier;

    private ElysiumAttachmentSyncDispatcher() {
    }

    public static void setClientApplier(@Nullable ClientApplier applier) {
        clientApplier = applier;
    }

    public static void sendToTracking(Entity entity, ResourceLocation attachmentId, byte @Nullable [] data) {
        ElysiumNetwork.sendToPlayersTracking(entity, new ElysiumAttachmentSyncPayload(entity.getId(), attachmentId, data));
    }

    public static void sendToTrackingAndSelf(Entity entity, ResourceLocation attachmentId, byte @Nullable [] data) {
        ElysiumNetwork.sendToPlayersTrackingAndSelf(entity, new ElysiumAttachmentSyncPayload(entity.getId(), attachmentId, data));
    }

    public static void sendToPlayer(ServerPlayer player, Entity entity, ResourceLocation attachmentId, byte @Nullable [] data) {
        ElysiumNetwork.sendToPlayer(player, new ElysiumAttachmentSyncPayload(entity.getId(), attachmentId, data));
    }

    @ApiStatus.Internal
    public static void onClientReceive(ElysiumAttachmentSyncPayload payload, ElysiumNetworkContext context) {
        ClientApplier applier = clientApplier;
        if (applier == null) return;
        applier.apply(payload.entityId(), payload.attachmentId(), payload.data());
    }

    @FunctionalInterface
    public interface ClientApplier {
        void apply(int entityId, ResourceLocation attachmentId, byte @Nullable [] data);
    }
}