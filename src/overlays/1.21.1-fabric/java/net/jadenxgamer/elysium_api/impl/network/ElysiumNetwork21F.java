package net.jadenxgamer.elysium_api.impl.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.jadenxgamer.elysium_api.api.network.ElysiumNetwork;
import net.jadenxgamer.elysium_api.api.network.ElysiumNetworkContext;
import net.jadenxgamer.elysium_api.api.network.ElysiumNetworkDirection;
import net.jadenxgamer.elysium_api.api.network.ElysiumNetworkRegistry;
import net.jadenxgamer.elysium_api.api.network.payload.ElysiumPayload;
import net.jadenxgamer.elysium_api.api.network.payload.ElysiumPayloadType;
import net.jadenxgamer.elysium_api.api.network.pipeline.ElysiumNetworkBackend;
import net.jadenxgamer.elysium_api.api.network.pipeline.ElysiumStreamCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
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

public final class ElysiumNetwork21F implements ElysiumNetworkBackend {

    private static final ElysiumNetwork21F INSTANCE = new ElysiumNetwork21F();

    private final Map<String, ElysiumNetworkRegistry> registries = new ConcurrentHashMap<>();
    private final Map<ResourceLocation, String> payloadToModId = new ConcurrentHashMap<>();
    private final Map<ResourceLocation, CustomPacketPayload.Type<ElysiumPayloadWrapper>> c2sTypes = new ConcurrentHashMap<>();
    private final Map<ResourceLocation, CustomPacketPayload.Type<ElysiumPayloadWrapper>> s2cTypes = new ConcurrentHashMap<>();

    private volatile MinecraftServer currentServer;

    private ElysiumNetwork21F() {
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

        for (ElysiumNetworkRegistry.Registration<?> registration : registry.clientToServerRegistrations()) {
            claimPayload(registration.type().id(), modId);
            CustomPacketPayload.Type<ElysiumPayloadWrapper> type = new CustomPacketPayload.Type<>(registration.type().id());
            this.c2sTypes.put(registration.type().id(), type);
            StreamCodec<? super FriendlyByteBuf, ElysiumPayloadWrapper> codec = wrapCodec(registration);
            PayloadTypeRegistry.playC2S().register(type, codec);
            ServerPlayNetworking.registerGlobalReceiver(type, (wrapper, context) ->
                    handleReceive(modId, wrapper.payload(), new ServerContext(context.player())));
        }

        for (ElysiumNetworkRegistry.Registration<?> registration : registry.serverToClientRegistrations()) {
            claimPayload(registration.type().id(), modId);
            CustomPacketPayload.Type<ElysiumPayloadWrapper> type = new CustomPacketPayload.Type<>(registration.type().id());
            this.s2cTypes.put(registration.type().id(), type);
            StreamCodec<? super FriendlyByteBuf, ElysiumPayloadWrapper> codec = wrapCodec(registration);
            PayloadTypeRegistry.playS2C().register(type, codec);
            ClientPlayNetworking.registerGlobalReceiver(type, (wrapper, context) ->
                    handleReceive(modId, wrapper.payload(), new ClientContext()));
        }
    }

    private void claimPayload(ResourceLocation payloadId, String modId) {
        String existing = this.payloadToModId.putIfAbsent(payloadId, modId);
        if (existing != null && !existing.equals(modId)) {
            throw new IllegalStateException("Payload id '" + payloadId + "' has been claimed by both mod '" + existing + "' and mod '" + modId + "'.");
        }
    }

    private static StreamCodec<? super FriendlyByteBuf, ElysiumPayloadWrapper> wrapCodec(ElysiumNetworkRegistry.Registration<?> registration) {
        ElysiumStreamCodec<? super FriendlyByteBuf, ElysiumPayloadWrapper> codec = ElysiumStreamCodec.of(
                (buf, wrapper) -> encodePayloadUnchecked(registration.type(), buf, wrapper.payload()),
                buf -> new ElysiumPayloadWrapper(decodeUnchecked(registration.type(), buf), new CustomPacketPayload.Type<>(registration.type().id()))
        );
        return ElysiumStreamCodecBridge.toVanilla(codec);
    }

    private void handleReceive(String modId, ElysiumPayload payload, ElysiumNetworkContext context) {
        ElysiumNetworkRegistry registry = this.registries.get(modId);
        if (registry == null) return;

        Collection<ElysiumNetworkRegistry.Registration<?>> candidates = context instanceof ClientContext ?
                registry.serverToClientRegistrations() : registry.clientToServerRegistrations();

        for (ElysiumNetworkRegistry.Registration<?> registration : candidates) {
            if (registration.type().id().equals(payload.type().id())) {
                dispatch(registration, payload, context);
                return;
            }
        }
        LOGGER.warn("Received unknown payload '{}' on channel '{}:payload'", payload.type().id(), modId);
    }

    private static <T extends ElysiumPayload> T decodeUnchecked(ElysiumPayloadType<T> type, FriendlyByteBuf buf) {
        return type.codec().decode(buf);
    }

    @SuppressWarnings("unchecked")
    private static <T extends ElysiumPayload> void encodePayloadUnchecked(ElysiumPayloadType<T> type, FriendlyByteBuf buf, ElysiumPayload payload) {
        type.codec().encode(buf, (T) payload);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void dispatch(ElysiumNetworkRegistry.Registration<?> registration, ElysiumPayload payload, ElysiumNetworkContext context) {
        try { ((ElysiumNetworkRegistry.Handler) registration.handler()).receive(payload, context); }
        catch (Throwable t) { LOGGER.error("Payload handler for '{}' threw an exception", registration.type().id(), t); }
    }

    @Override
    public void sendToServer(ElysiumPayload payload) {
        modIdForSend(payload, true);
        CustomPacketPayload.Type<ElysiumPayloadWrapper> type = this.c2sTypes.get(payload.type().id());
        ClientPlayNetworking.send(new ElysiumPayloadWrapper(payload, type));
    }

    @Override
    public void sendToPlayer(Player player, ElysiumPayload payload) {
        if (!(player instanceof ServerPlayer serverPlayer)) throw new IllegalArgumentException("sendToPlayer requires a ServerPlayer.");
        modIdForSend(payload, false);
        CustomPacketPayload.Type<ElysiumPayloadWrapper> type = this.s2cTypes.get(payload.type().id());
        ServerPlayNetworking.send(serverPlayer, new ElysiumPayloadWrapper(payload, type));
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
        if (excluded.getServer() == null) return;
        for (ServerPlayer player : excluded.getServer().getPlayerList().getPlayers()) if (player != excluded) sendToPlayer(player, payload);
    }

    private void modIdForSend(ElysiumPayload payload, boolean toServer) {
        String modId = this.payloadToModId.get(payload.type().id());
        if (modId == null) throw new IllegalStateException("Payload type '" + payload.type().id() + "' has not been registered.");
        ElysiumNetworkRegistry registry = this.registries.get(modId);
        if (registry == null) throw new IllegalStateException("Networking registry for mod id '" + modId + "' is not available.");

        Collection<ElysiumNetworkRegistry.Registration<?>> registrations = toServer ? registry.clientToServerRegistrations() : registry.serverToClientRegistrations();

        for (ElysiumNetworkRegistry.Registration<?> registration : registrations) if (registration.type().id().equals(payload.type().id())) return;
        throw new IllegalArgumentException("Payload type '" + payload.type().id() + "' is not registered for " + (toServer ? "client-to-server" : "server-to-client") + " sending.");
    }

    private static final class ServerContext implements ElysiumNetworkContext {

        private final ServerPlayer player;

        private ServerContext(ServerPlayer player) {
            this.player = player;
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
            INSTANCE.sendToPlayer(this.player, payload);
        }

        @Override
        public void enqueueWork(Runnable task) {
            this.player.getServer().execute(task);
        }
    }

    private static final class ClientContext implements ElysiumNetworkContext {

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
            INSTANCE.sendToServer(payload);
        }

        @Override
        public void enqueueWork(Runnable task) {
            Minecraft.getInstance().execute(task);
        }
    }
}