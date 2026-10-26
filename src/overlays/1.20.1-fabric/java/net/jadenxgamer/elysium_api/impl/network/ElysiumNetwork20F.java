package net.jadenxgamer.elysium_api.impl.network;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.jadenxgamer.elysium_api.api.network.ElysiumNetwork;
import net.jadenxgamer.elysium_api.api.network.ElysiumNetworkContext;
import net.jadenxgamer.elysium_api.api.network.ElysiumNetworkDirection;
import net.jadenxgamer.elysium_api.api.network.ElysiumNetworkRegistry;
import net.jadenxgamer.elysium_api.api.network.payload.ElysiumPayload;
import net.jadenxgamer.elysium_api.api.network.payload.ElysiumPayloadType;
import net.jadenxgamer.elysium_api.api.network.pipeline.ElysiumNetworkBackend;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static net.jadenxgamer.elysium_api.ElysiumAPI.LOGGER;

public final class ElysiumNetwork20F implements ElysiumNetworkBackend {

    private static final ElysiumNetwork20F INSTANCE = new ElysiumNetwork20F();
    private static final String CHANNEL_SUFFIX = "payload";

    private final Map<String, ElysiumNetworkRegistry> registries = new ConcurrentHashMap<>();
    private final Map<ResourceLocation, String> payloadToModId = new ConcurrentHashMap<>();

    private volatile MinecraftServer currentServer;

    private ElysiumNetwork20F() {
    }

    public static void init() {
        ElysiumNetwork.installBackend(INSTANCE);
        ServerLifecycleEvents.SERVER_STARTING.register(server -> INSTANCE.currentServer = server);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> INSTANCE.currentServer = null);
    }

    @Override
    public void register(String modId, ElysiumNetworkRegistry registry) {
        ElysiumNetworkRegistry previous = this.registries.putIfAbsent(modId, registry);
        if (previous != null) throw new IllegalStateException("Networking registry for mod id '" + modId + "' has already been registered.");
        for (ElysiumNetworkRegistry.Registration<?> registration : registry.clientToServerRegistrations()) claimPayload(registration.type().id(), modId);
        for (ElysiumNetworkRegistry.Registration<?> registration : registry.serverToClientRegistrations()) claimPayload(registration.type().id(), modId);

        ResourceLocation channel = channelFor(modId);
        ServerPlayNetworking.registerGlobalReceiver(channel, (server, player, responseSender, buf, sender) ->
                this.handleServerReceive(modId, server, player, buf));
        ClientPlayNetworking.registerGlobalReceiver(channel, (client, handler, buf, responseSender) ->
                this.handleClientReceive(modId, client, buf));
    }

    private void claimPayload(ResourceLocation payloadId, String modId) {
        String existing = this.payloadToModId.putIfAbsent(payloadId, modId);
        if (existing != null && !existing.equals(modId)) {
            throw new IllegalStateException("Payload id '" + payloadId + "' has been claimed by both mod '" + existing + "' and mod '" + modId + "'.");
        }
    }

    private static ResourceLocation channelFor(String modId) {
        return new ResourceLocation(modId, CHANNEL_SUFFIX);
    }

    private void handleServerReceive(String modId, MinecraftServer server, ServerPlayer player, FriendlyByteBuf buf) {
        ElysiumNetworkRegistry registry = this.registries.get(modId);
        if (registry == null) return;

        ElysiumNetworkRegistry.Registration<?> matched = null;
        ElysiumPayload payload = null;
        try {
            ResourceLocation typeId = buf.readResourceLocation();
            for (ElysiumNetworkRegistry.Registration<?> registration : registry.clientToServerRegistrations()) {
                if (registration.type().id().equals(typeId)) {
                    matched = registration;
                    payload = decodeUnchecked(registration.type(), buf);
                    break;
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to decode incoming C2S payload on channel '{}'", channelFor(modId), e);
            return;
        }
        if (matched == null) {
            LOGGER.warn("Received unknown C2S payload on channel '{}'", channelFor(modId));
            return;
        }
        ElysiumNetworkRegistry.Registration<?> finalMatched = matched;
        ElysiumPayload finalPayload = payload;
        server.execute(() -> {
            ElysiumNetworkContext context = new ServerContext(player, modId);
            dispatch(finalMatched, finalPayload, context);
        });
    }

    private void handleClientReceive(String modId, Minecraft client, FriendlyByteBuf buf) {
        ElysiumNetworkRegistry registry = this.registries.get(modId);
        if (registry == null) return;

        ElysiumNetworkRegistry.Registration<?> matched = null;
        ElysiumPayload payload = null;
        try {
            ResourceLocation typeId = buf.readResourceLocation();
            for (ElysiumNetworkRegistry.Registration<?> registration : registry.serverToClientRegistrations()) {
                if (registration.type().id().equals(typeId)) {
                    matched = registration;
                    payload = decodeUnchecked(registration.type(), buf);
                    break;
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to decode incoming S2C payload on channel '{}'", channelFor(modId), e);
            return;
        }
        if (matched == null) {
            LOGGER.warn("Received unknown S2C payload on channel '{}'", channelFor(modId));
            return;
        }
        ElysiumNetworkRegistry.Registration<?> finalMatched = matched;
        ElysiumPayload finalPayload = payload;
        client.execute(() -> {
            ElysiumNetworkContext context = new ClientContext(modId);
            dispatch(finalMatched, finalPayload, context);
        });
    }

    private static <T extends ElysiumPayload> T decodeUnchecked(ElysiumPayloadType<T> type, FriendlyByteBuf buf) {
        return type.codec().decode(buf);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void dispatch(ElysiumNetworkRegistry.Registration<?> registration, ElysiumPayload payload, ElysiumNetworkContext context) {
        try { ((ElysiumNetworkRegistry.Handler) registration.handler()).receive(payload, context); }
        catch (Throwable t) { LOGGER.error("Payload handler for '{}' threw an exception", registration.type().id(), t); }
    }

    @Override
    public void sendToServer(ElysiumPayload payload) {
        String modId = modIdForSend(payload, true);
        ClientPlayNetworking.send(channelFor(modId), encodeEnvelope(payload));
    }

    @Override
    public void sendToPlayer(Player player, ElysiumPayload payload) {
        if (!(player instanceof ServerPlayer serverPlayer)) throw new IllegalArgumentException("sendToPlayer requires a ServerPlayer.");
        String modId = modIdForSend(payload, false);
        ServerPlayNetworking.send(serverPlayer, channelFor(modId), encodeEnvelope(payload));
    }

    @Override
    public void sendToAllPlayers(ElysiumPayload payload) {
        MinecraftServer server = this.currentServer;
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) sendToPlayer(player, payload);
    }

    @Override
    public void sendToAllPlayersInLevel(ServerLevel level, ElysiumPayload payload) {
        modIdForSend(payload, false);
        for (ServerPlayer player : PlayerLookup.world(level)) sendToPlayer(player, payload);
    }

    @Override
    public void sendToPlayersNear(ServerLevel level, Vec3 center, double radius, ElysiumPayload payload) {
        modIdForSend(payload, false);
        for (ServerPlayer player : PlayerLookup.around(level, center, radius)) sendToPlayer(player, payload);
    }

    @Override
    public void sendToPlayersTrackingChunk(ServerLevel level, ChunkPos chunkPos, ElysiumPayload payload) {
        modIdForSend(payload, false);
        for (ServerPlayer player : PlayerLookup.tracking(level, chunkPos)) sendToPlayer(player, payload);
    }

    @Override
    public void sendToPlayersTrackingBlockEntity(BlockEntity blockEntity, ElysiumPayload payload) {
        modIdForSend(payload, false);
        for (ServerPlayer player : PlayerLookup.tracking(blockEntity)) sendToPlayer(player, payload);
    }

    @Override
    public void sendToPlayersTracking(Entity entity, ElysiumPayload payload) {
        for (ServerPlayer player : PlayerLookup.tracking(entity)) sendToPlayer(player, payload);
    }

    @Override
    public void sendToPlayersTrackingAndSelf(Entity entity, ElysiumPayload payload) {
        Set<ServerPlayer> targets = new HashSet<>(PlayerLookup.tracking(entity));
        if (entity instanceof ServerPlayer serverPlayer) targets.add(serverPlayer);
        for (ServerPlayer player : targets) sendToPlayer(player, payload);
    }

    @Override
    public void sendToAllPlayersExceptSelf(ServerPlayer excluded, ElysiumPayload payload) {
        modIdForSend(payload, false);
        for (ServerPlayer player : excluded.getServer().getPlayerList().getPlayers()) if (player != excluded) sendToPlayer(player, payload);
    }

    private String modIdForSend(ElysiumPayload payload, boolean toServer) {
        String modId = this.payloadToModId.get(payload.type().id());
        if (modId == null) throw new IllegalStateException("Payload type '" + payload.type().id() + "' has not been registered.");
        ElysiumNetworkRegistry registry = this.registries.get(modId);
        if (registry == null) throw new IllegalStateException("Networking registry for mod id '" + modId + "' is not available.");

        Collection<ElysiumNetworkRegistry.Registration<?>> registrations = toServer ? registry.clientToServerRegistrations() : registry.serverToClientRegistrations();

        for (ElysiumNetworkRegistry.Registration<?> registration : registrations) if (registration.type().id().equals(payload.type().id())) return modId;
        throw new IllegalArgumentException("Payload type '" + payload.type().id() + "' is not registered for " + (toServer ? "client-to-server" : "server-to-client") + " sending.");
    }

    private static FriendlyByteBuf encodeEnvelope(ElysiumPayload payload) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeResourceLocation(payload.type().id());
        encodePayloadUnchecked(payload, buf);
        return buf;
    }

    @SuppressWarnings("unchecked")
    private static <T extends ElysiumPayload> void encodePayloadUnchecked(T payload, FriendlyByteBuf buf) {
        ElysiumPayloadType<T> type = (ElysiumPayloadType<T>) payload.type();
        type.codec().encode(buf, payload);
    }

    private static final class ServerContext implements ElysiumNetworkContext {

        private final ServerPlayer player;
        private final String modId;

        private ServerContext(ServerPlayer player, String modId) {
            this.player = player;
            this.modId = modId;
        }

        @Override
        public Player player() {
            return this.player;
        }

        @Override
        public MinecraftServer server() {
            return this.player.getServer();
        }

        @Override
        public ElysiumNetworkDirection direction() {
            return ElysiumNetworkDirection.PLAY_TO_SERVER;
        }

        @Override
        public void reply(ElysiumPayload payload) {
            ServerPlayNetworking.send(this.player, channelFor(this.modId), encodeEnvelope(payload));
        }

        @Override
        public void enqueueWork(Runnable task) {
            this.player.getServer().execute(task);
        }
    }

    private record ClientContext(String modId) implements ElysiumNetworkContext {

        @Override
        public Player player() {
            return Minecraft.getInstance().player;
        }

        @Override
        public MinecraftServer server() {
            return Minecraft.getInstance().getSingleplayerServer();
        }

        @Override
        public ElysiumNetworkDirection direction() {
            return ElysiumNetworkDirection.PLAY_TO_CLIENT;
        }

        @Override
        public void reply(ElysiumPayload payload) {
            ClientPlayNetworking.send(channelFor(this.modId), encodeEnvelope(payload));
        }

        @Override
        public void enqueueWork(Runnable task) {
            Minecraft.getInstance().execute(task);
        }
    }
}