package net.jadenxgamer.elysium_api.mod.network.payload.client;

import net.jadenxgamer.elysium_api.ElysiumAPI;
import net.jadenxgamer.elysium_api.api.config.ElysiumConfigManager;
import net.jadenxgamer.elysium_api.api.network.ElysiumNetworkContext;
import net.jadenxgamer.elysium_api.api.network.payload.ElysiumPayload;
import net.jadenxgamer.elysium_api.api.network.payload.ElysiumPayloadType;
import net.jadenxgamer.elysium_api.api.network.pipeline.ElysiumStreamCodec;
import net.minecraft.network.FriendlyByteBuf;

import java.util.LinkedHashMap;
import java.util.Map;

public record ConfigSyncPayload(String modId, Map<String, String> values) implements ElysiumPayload {

    private static final ElysiumStreamCodec<FriendlyByteBuf, String> STRING_CODEC = ElysiumStreamCodec.of(
            FriendlyByteBuf::writeUtf,
            FriendlyByteBuf::readUtf
    );

    private static final ElysiumStreamCodec<FriendlyByteBuf, Map<String, String>> MAP_CODEC = ElysiumStreamCodec.of(
            (buf, map) -> {
                buf.writeVarInt(map.size());
                for (Map.Entry<String, String> e : map.entrySet()) {
                    buf.writeUtf(e.getKey());
                    buf.writeUtf(e.getValue());
                }
            },
            buf -> {
                int size = buf.readVarInt();
                Map<String, String> map = new LinkedHashMap<>(size);
                for (int i = 0; i < size; i++) {
                    map.put(buf.readUtf(), buf.readUtf());
                }
                return map;
            }
    );

    public static final ElysiumPayloadType<ConfigSyncPayload> TYPE = ElysiumPayloadType.of(
            ElysiumAPI.elysiumPath("config_sync_s2c"),
            ElysiumStreamCodec.composite(
                    STRING_CODEC, ConfigSyncPayload::modId,
                    MAP_CODEC, ConfigSyncPayload::values,
                    ConfigSyncPayload::new
            )
    );

    @Override
    public ElysiumPayloadType<? extends ElysiumPayload> type() {
        return TYPE;
    }

    public static void handleDataOnClient(ConfigSyncPayload payload, ElysiumNetworkContext context) {
        ElysiumConfigManager.applyServerSync(payload.modId(), payload.values(), context.server() == null);
    }
}