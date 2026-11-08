package net.jadenxgamer.elysium_api.impl.network;

import net.jadenxgamer.elysium_api.api.network.payload.ElysiumPayload;
import net.jadenxgamer.elysium_api.api.network.pipeline.ElysiumStreamCodec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class ElysiumStreamCodecBridge {

    private ElysiumStreamCodecBridge() {
    }

    public static <T> StreamCodec<? super FriendlyByteBuf, T> toVanilla(ElysiumStreamCodec<? super FriendlyByteBuf, T> codec) {
        return StreamCodec.of(codec::encode, codec::decode);
    }

    public static <T> ElysiumStreamCodec<? super FriendlyByteBuf, T> fromVanilla(StreamCodec<? super FriendlyByteBuf, T> codec) {
        return ElysiumStreamCodec.of(codec::encode, codec::decode);
    }

    public record ElysiumPayloadWrapper(ElysiumPayload payload, CustomPacketPayload.Type<ElysiumPayloadWrapper> type) implements CustomPacketPayload {}
}