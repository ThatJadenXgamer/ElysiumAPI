package net.jadenxgamer.elysium_api.impl.network.payload;

import net.jadenxgamer.elysium_api.ElysiumAPI;
import net.jadenxgamer.elysium_api.api.network.ElysiumNetworkContext;
import net.jadenxgamer.elysium_api.api.network.payload.ElysiumPayload;
import net.jadenxgamer.elysium_api.api.network.payload.ElysiumPayloadType;
import net.jadenxgamer.elysium_api.api.network.pipeline.ElysiumStreamCodec;

public record PingC2SPayload(String message) implements ElysiumPayload {

    public static final ElysiumPayloadType<PingC2SPayload> TYPE = ElysiumPayloadType.of(
            ElysiumAPI.idPath("elysium_api", "ping_c2s"),
            ElysiumStreamCodec.of(
                    (buf, payload) -> buf.writeUtf(payload.message()),
                    buf -> new PingC2SPayload(buf.readUtf())
            )
    );

    @Override
    public ElysiumPayloadType<? extends ElysiumPayload> type() {
        return TYPE;
    }

    public static void handleDataOnServer(PingC2SPayload payload, ElysiumNetworkContext context) {
        ElysiumAPI.LOGGER.info("[Elysium Network] Received C2S ping from {}: {}", context.player().getScoreboardName(), payload.message());
        context.reply(new PingS2CPayload("pong: " + payload.message()));
    }
}