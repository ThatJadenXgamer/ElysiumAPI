package net.jadenxgamer.elysium_api.impl.network;

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
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static net.jadenxgamer.elysium_api.ElysiumAPI.LOGGER;

public final class ElysiumNetwork21N implements ElysiumNetworkBackend {

    private static final ElysiumNetwork21N INSTANCE = new ElysiumNetwork21N();
    private static final String PAYLOAD_VERSION = "1";

    private final Map<String, ElysiumNetworkRegistry> registries = new ConcurrentHashMap<>();
    private final Map<ResourceLocation, String> payloadToModId = new ConcurrentHashMap<>();
    private final Map<ResourceLocation, CustomPacketPayload.Type<ElysiumPayloadWrapper>> c2sTypes = new ConcurrentHashMap<>();
    private final Map<ResourceLocation, CustomPacketPayload.Type<ElysiumPayloadWrapper>> s2cTypes = new ConcurrentHashMap<>();

    private ElysiumNetwork21N() {
    }

    public static void init(IEventBus modBus) {
        ElysiumNetwork.installBackend(INSTANCE);
        modBus.addListener(ElysiumNetwork21N::onRegisterPayloadHandlers);
    }

    private static void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PAYLOAD_VERSION);
        for (Map.Entry<String, ElysiumNetworkRegistry> entry : INSTANCE.registries.entrySet()) {
            String modId = entry.getKey();
            ElysiumNetworkRegistry registry = entry.getValue();
            for (ElysiumNetworkRegistry.Registration<?> registration : registry.clientToServerRegistrations()) registerC2S(registrar, modId, registration);
            for (ElysiumNetworkRegistry.Registration<?> registration : registry.serverToClientRegistrations()) registerS2C(registrar, modId, registration);
        }
    }

    private static void registerC2S(PayloadRegistrar registrar, String modId, ElysiumNetworkRegistry.Registration<?> registration) {
        INSTANCE.claimPayload(registration.type().id(), modId);
        CustomPacketPayload.Type<ElysiumPayloadWrapper> type = new CustomPacketPayload.Type<>(registration.type().id());
        INSTANCE.c2sTypes.put(registration.type().id(), type);
        StreamCodec<? super FriendlyByteBuf, ElysiumPayloadWrapper> codec = wrapCodec(registration);
        registrar.playToServer(type, codec, (wrapper, context) ->
                context.enqueueWork(() -> {
                    Player player = context.player();
                    if (!(player instanceof ServerPlayer serverPlayer)) return;
                    INSTANCE.handleReceive(modId, wrapper.payload(), new ServerContext(serverPlayer));
                }));
    }

    private static void registerS2C(PayloadRegistrar registrar, String modId, ElysiumNetworkRegistry.Registration<?> registration) {
        INSTANCE.claimPayload(registration.type().id(), modId);
        CustomPacketPayload.Type<ElysiumPayloadWrapper> type = new CustomPacketPayload.Type<>(registration.type().id());
        INSTANCE.s2cTypes.put(registration.type().id(), type);
        StreamCodec<? super FriendlyByteBuf, ElysiumPayloadWrapper> codec = wrapCodec(registration);
        registrar.playToClient(type, codec, (wrapper, context) ->
                context.enqueueWork(() ->
                        INSTANCE.handleReceive(modId, wrapper.payload(), new ClientContext())));
    }

    @Override
    public void register(String modId, ElysiumNetworkRegistry registry) {
        ElysiumNetworkRegistry previous = this.registries.putIfAbsent(modId, registry);
        if (previous != null) throw new IllegalStateException("Networking registry for mod id '" + modId + "' has already been registered.");
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
        PacketDistributor.sendToServer(new ElysiumPayloadWrapper(payload, type));
    }

    @Override
    public void sendToPlayer(Player player, ElysiumPayload payload) {
        if (!(player instanceof ServerPlayer serverPlayer)) throw new IllegalArgumentException("sendToPlayer requires a ServerPlayer.");
        modIdForSend(payload, false);
        CustomPacketPayload.Type<ElysiumPayloadWrapper> type = this.s2cTypes.get(payload.type().id());
        PacketDistributor.sendToPlayer(serverPlayer, new ElysiumPayloadWrapper(payload, type));
    }

    @Override
    public void sendToAllPlayers(ElysiumPayload payload) {
        modIdForSend(payload, false);
        CustomPacketPayload.Type<ElysiumPayloadWrapper> type = this.s2cTypes.get(payload.type().id());
        PacketDistributor.sendToAllPlayers(new ElysiumPayloadWrapper(payload, type));
    }

    @Override
    public void sendToAllPlayersInLevel(ServerLevel level, ElysiumPayload payload) {
        modIdForSend(payload, false);
        CustomPacketPayload.Type<ElysiumPayloadWrapper> type = this.s2cTypes.get(payload.type().id());
        PacketDistributor.sendToPlayersInDimension(level, new ElysiumPayloadWrapper(payload, type));
    }

    @Override
    public void sendToPlayersNear(ServerLevel level, Vec3 center, double radius, ElysiumPayload payload) {
        modIdForSend(payload, false);
        CustomPacketPayload.Type<ElysiumPayloadWrapper> type = this.s2cTypes.get(payload.type().id());
        PacketDistributor.sendToPlayersNear(level, null, center.x, center.y, center.z, radius, new ElysiumPayloadWrapper(payload, type));
    }

    @Override
    public void sendToPlayersTrackingChunk(ServerLevel level, ChunkPos chunkPos, ElysiumPayload payload) {
        modIdForSend(payload, false);
        CustomPacketPayload.Type<ElysiumPayloadWrapper> type = this.s2cTypes.get(payload.type().id());
        PacketDistributor.sendToPlayersTrackingChunk(level, chunkPos, new ElysiumPayloadWrapper(payload, type));
    }

    @Override
    public void sendToPlayersTrackingBlockEntity(BlockEntity blockEntity, ElysiumPayload payload) {
        if (blockEntity.getLevel() instanceof ServerLevel serverLevel) sendToPlayersTrackingChunk(serverLevel, new ChunkPos(blockEntity.getBlockPos()), payload);
    }

    @Override
    public void sendToPlayersTracking(Entity entity, ElysiumPayload payload) {
        modIdForSend(payload, false);
        CustomPacketPayload.Type<ElysiumPayloadWrapper> type = this.s2cTypes.get(payload.type().id());
        PacketDistributor.sendToPlayersTrackingEntity(entity, new ElysiumPayloadWrapper(payload, type));
    }

    @Override
    public void sendToPlayersTrackingAndSelf(Entity entity, ElysiumPayload payload) {
        modIdForSend(payload, false);
        CustomPacketPayload.Type<ElysiumPayloadWrapper> type = this.s2cTypes.get(payload.type().id());
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, new ElysiumPayloadWrapper(payload, type));
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