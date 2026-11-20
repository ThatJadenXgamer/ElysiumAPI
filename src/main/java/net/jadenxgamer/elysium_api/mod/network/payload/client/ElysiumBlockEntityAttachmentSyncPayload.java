package net.jadenxgamer.elysium_api.mod.network.payload.client;

import net.jadenxgamer.elysium_api.api.network.payload.ElysiumPayload;
import net.jadenxgamer.elysium_api.api.network.payload.ElysiumPayloadType;
import net.jadenxgamer.elysium_api.api.network.pipeline.ElysiumStreamCodec;
import net.jadenxgamer.elysium_api.api.unification.ElysiumResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public record ElysiumBlockEntityAttachmentSyncPayload(ResourceLocation dimension, BlockPos pos, ResourceLocation attachmentId, byte @Nullable [] data) implements ElysiumPayload {

    public static final ElysiumPayloadType<ElysiumBlockEntityAttachmentSyncPayload> TYPE = ElysiumPayloadType.of(
            ElysiumResourceLocation.fromNamespaceAndPath("elysium_api", "block_entity_attachment_sync"),
            ElysiumStreamCodec.of(
                    (buf, payload) -> {
                        buf.writeResourceLocation(payload.dimension());
                        buf.writeBlockPos(payload.pos());
                        buf.writeResourceLocation(payload.attachmentId());
                        boolean present = payload.data() != null;
                        buf.writeBoolean(present);
                        if (present) buf.writeByteArray(payload.data());
                    },
                    buf -> {
                        ResourceLocation dimension = buf.readResourceLocation();
                        BlockPos pos = buf.readBlockPos();
                        ResourceLocation attachmentId = buf.readResourceLocation();
                        boolean present = buf.readBoolean();
                        byte[] data = present ? buf.readByteArray() : null;
                        return new ElysiumBlockEntityAttachmentSyncPayload(dimension, pos, attachmentId, data);
                    }
            )
    );

    @Override
    public ElysiumPayloadType<? extends ElysiumPayload> type() {
        return TYPE;
    }
}