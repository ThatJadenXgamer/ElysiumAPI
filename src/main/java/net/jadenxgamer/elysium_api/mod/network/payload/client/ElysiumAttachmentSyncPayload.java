package net.jadenxgamer.elysium_api.mod.network.payload.client;

import net.jadenxgamer.elysium_api.ElysiumAPI;
import net.jadenxgamer.elysium_api.api.network.payload.ElysiumPayload;
import net.jadenxgamer.elysium_api.api.network.payload.ElysiumPayloadType;
import net.jadenxgamer.elysium_api.api.network.pipeline.ElysiumStreamCodec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public record ElysiumAttachmentSyncPayload(int entityId, ResourceLocation attachmentId, byte @Nullable [] data) implements ElysiumPayload {

    public static final ElysiumPayloadType<ElysiumAttachmentSyncPayload> TYPE = ElysiumPayloadType.of(
            ElysiumAPI.elysiumPath("attachment_sync"),
            ElysiumStreamCodec.of(
                    (FriendlyByteBuf buf, ElysiumAttachmentSyncPayload payload) -> {
                        buf.writeVarInt(payload.entityId());
                        buf.writeResourceLocation(payload.attachmentId());
                        boolean present = payload.data() != null;
                        buf.writeBoolean(present);
                        if (present) buf.writeByteArray(payload.data());
                    },
                    (FriendlyByteBuf buf) -> {
                        int entityId = buf.readVarInt();
                        ResourceLocation attachmentId = buf.readResourceLocation();
                        boolean present = buf.readBoolean();
                        byte[] data = present ? buf.readByteArray() : null;
                        return new ElysiumAttachmentSyncPayload(entityId, attachmentId, data);
                    }
            )
    );

    @Override
    public ElysiumPayloadType<? extends ElysiumPayload> type() {
        return TYPE;
    }
}