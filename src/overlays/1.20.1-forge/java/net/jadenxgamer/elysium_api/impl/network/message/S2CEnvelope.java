package net.jadenxgamer.elysium_api.impl.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record S2CEnvelope(ResourceLocation typeId, byte[] body) {

    public static void encode(S2CEnvelope msg, FriendlyByteBuf buf) {
        buf.writeResourceLocation(msg.typeId());
        buf.writeByteArray(msg.body());
    }

    public static S2CEnvelope decode(FriendlyByteBuf buf) {
        return new S2CEnvelope(buf.readResourceLocation(), buf.readByteArray());
    }
}