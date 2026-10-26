package net.jadenxgamer.elysium_api.impl.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record C2SEnvelope(ResourceLocation typeId, byte[] body) {

    public static void encode(C2SEnvelope msg, FriendlyByteBuf buf) {
        buf.writeResourceLocation(msg.typeId());
        buf.writeByteArray(msg.body());
    }

    public static C2SEnvelope decode(FriendlyByteBuf buf) {
        return new C2SEnvelope(buf.readResourceLocation(), buf.readByteArray());
    }
}