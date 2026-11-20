package net.jadenxgamer.elysium_api.mod.network.payload.client;

import net.jadenxgamer.elysium_api.api.network.payload.ElysiumPayload;
import net.jadenxgamer.elysium_api.api.network.payload.ElysiumPayloadType;
import net.jadenxgamer.elysium_api.api.network.pipeline.ElysiumStreamCodec;
import net.jadenxgamer.elysium_api.api.unification.ElysiumResourceLocation;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public record ElysiumEntityAttachmentSyncPayload(int entityId, ResourceLocation attachmentId, byte @Nullable [] data) implements ElysiumPayload {

    public static final ElysiumPayloadType<ElysiumEntityAttachmentSyncPayload> TYPE = ElysiumPayloadType.of(
            ElysiumResourceLocation.fromNamespaceAndPath("elysium_api", "attachment_sync"),
            ElysiumStreamCodec.of(
                    (buf, payload) -> {
                        buf.writeVarInt(payload.entityId());
                        buf.writeResourceLocation(payload.attachmentId());
                        boolean present = payload.data() != null;
                        buf.writeBoolean(present);
                        if (present) buf.writeByteArray(payload.data());
                    },
                    buf -> {
                        int entityId = buf.readVarInt();
                        ResourceLocation attachmentId = buf.readResourceLocation();
                        boolean present = buf.readBoolean();
                        byte[] data = present ? buf.readByteArray() : null;
                        return new ElysiumEntityAttachmentSyncPayload(entityId, attachmentId, data);
                    }
            )
    );

    @Override
    public ElysiumPayloadType<? extends ElysiumPayload> type() {
        return TYPE;
    }
}