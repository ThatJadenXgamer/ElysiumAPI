package net.jadenxgamer.elysium_api.impl.networking.to_client;

import net.jadenxgamer.elysium_api.ElysiumAPI;
import net.jadenxgamer.elysium_api.api.client.screen_flash.ScreenFlash;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ScreenFlashPayload(int fadeIn, int hold, int fadeOut, int color,
                                 boolean firstPersonOnly, boolean force) {

    public ScreenFlashPayload(FriendlyByteBuf buf) {
        this(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                buf.readBoolean(), buf.readBoolean());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(fadeIn);
        buf.writeVarInt(hold);
        buf.writeVarInt(fadeOut);
        buf.writeVarInt(color);
        buf.writeBoolean(firstPersonOnly);
        buf.writeBoolean(force);
    }

    public void handleDataOnClient(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ElysiumAPI.LOGGER.debug("Received ScreenFlashPayload: {}", this);
            ScreenFlash.triggerScreenFlash(fadeIn, hold, fadeOut, color, firstPersonOnly, force);
        });
        ctx.setPacketHandled(true);
    }
}