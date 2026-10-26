package net.jadenxgamer.elysium_api.impl.network;

import net.jadenxgamer.elysium_api.api.network.payload.ElysiumPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ElysiumPayloadWrapper(ElysiumPayload payload, Type<ElysiumPayloadWrapper> type) implements CustomPacketPayload {}