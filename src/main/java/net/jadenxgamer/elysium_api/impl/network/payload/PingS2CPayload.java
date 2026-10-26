package net.jadenxgamer.elysium_api.impl.network.payload;

import net.jadenxgamer.elysium_api.ElysiumAPI;
import net.jadenxgamer.elysium_api.api.network.ElysiumNetworkContext;
import net.jadenxgamer.elysium_api.api.network.payload.ElysiumPayload;
import net.jadenxgamer.elysium_api.api.network.payload.ElysiumPayloadType;
import net.jadenxgamer.elysium_api.api.network.pipeline.ElysiumStreamCodec;

public record PingS2CPayload(String message) implements ElysiumPayload {

    public static final ElysiumPayloadType<PingS2CPayload> TYPE = ElysiumPayloadType.of(
            ElysiumAPI.idPath("elysium_api", "ping_s2c"),
            ElysiumStreamCodec.of(
                    (buf, payload) -> buf.writeUtf(payload.message()),
                    buf -> new PingS2CPayload(buf.readUtf())
            )
    );

    @Override
    public ElysiumPayloadType<? extends ElysiumPayload> type() {
        return TYPE;
    }

    public static void handleDataOnClient(PingS2CPayload payload, ElysiumNetworkContext context) {
        ElysiumAPI.LOGGER.info("[Elysium Network] Received S2C ping: {}", payload.message());
    }
}