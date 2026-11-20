package net.jadenxgamer.elysium_api.mod.network.payload.client;

import net.jadenxgamer.elysium_api.api.network.payload.ElysiumPayload;
import net.jadenxgamer.elysium_api.api.network.payload.ElysiumPayloadType;
import net.jadenxgamer.elysium_api.api.network.pipeline.ElysiumStreamCodec;
import net.jadenxgamer.elysium_api.api.unification.ElysiumResourceLocation;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.Nullable;

public record ElysiumChunkAttachmentSyncPayload(ResourceLocation dimension, ChunkPos pos, ResourceLocation attachmentId, byte @Nullable [] data) implements ElysiumPayload {

    public static final ElysiumPayloadType<ElysiumChunkAttachmentSyncPayload> TYPE = ElysiumPayloadType.of(
            ElysiumResourceLocation.fromNamespaceAndPath("elysium_api", "chunk_attachment_sync"),
            ElysiumStreamCodec.of(
                    (buf, payload) -> {
                        buf.writeResourceLocation(payload.dimension());
                        buf.writeInt(payload.pos().x);
                        buf.writeInt(payload.pos().z);
                        buf.writeResourceLocation(payload.attachmentId());
                        boolean present = payload.data() != null;
                        buf.writeBoolean(present);
                        if (present) buf.writeByteArray(payload.data());
                    },
                    buf -> {
                        ResourceLocation dimension = buf.readResourceLocation();
                        ChunkPos pos = new ChunkPos(buf.readInt(), buf.readInt());
                        ResourceLocation attachmentId = buf.readResourceLocation();
                        boolean present = buf.readBoolean();
                        byte[] data = present ? buf.readByteArray() : null;
                        return new ElysiumChunkAttachmentSyncPayload(dimension, pos, attachmentId, data);
                    }
            )
    );

    @Override
    public ElysiumPayloadType<? extends ElysiumPayload> type() {
        return TYPE;
    }
}