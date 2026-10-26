package net.jadenxgamer.elysium_api.impl.network;

import io.netty.buffer.Unpooled;
import net.jadenxgamer.elysium_api.ElysiumAPI;
import net.jadenxgamer.elysium_api.api.network.ElysiumNetwork;
import net.jadenxgamer.elysium_api.api.network.ElysiumNetworkContext;
import net.jadenxgamer.elysium_api.api.network.ElysiumNetworkDirection;
import net.jadenxgamer.elysium_api.api.network.ElysiumNetworkRegistry;
import net.jadenxgamer.elysium_api.api.network.payload.ElysiumPayload;
import net.jadenxgamer.elysium_api.api.network.payload.ElysiumPayloadType;
import net.jadenxgamer.elysium_api.api.network.pipeline.ElysiumNetworkBackend;
import net.jadenxgamer.elysium_api.impl.network.message.C2SEnvelope;
import net.jadenxgamer.elysium_api.impl.network.message.S2CEnvelope;
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
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import static net.jadenxgamer.elysium_api.ElysiumAPI.LOGGER;

public final class ElysiumNetwork20L implements ElysiumNetworkBackend {

    private static final ElysiumNetwork20L INSTANCE = new ElysiumNetwork20L();
    private static final String CHANNEL_SUFFIX = "payload";
    private static final String CHANNEL_VERSION = "1";

    private final Map<String, SimpleChannel> channels = new ConcurrentHashMap<>();
    private final Map<String, ElysiumNetworkRegistry> registries = new ConcurrentHashMap<>();
    private final Map<ResourceLocation, String> payloadToModId = new ConcurrentHashMap<>();

    private ElysiumNetwork20L() {
    }

    public static void init() {
        ElysiumNetwork.installBackend(INSTANCE);
    }

    @Override
    public void register(String modId, ElysiumNetworkRegistry registry) {
        ElysiumNetworkRegistry previous = this.registries.putIfAbsent(modId, registry);
        if (previous != null) throw new IllegalStateException("Networking registry for mod id '" + modId + "' has already been registered.");
        for (ElysiumNetworkRegistry.Registration<?> registration : registry.clientToServerRegistrations()) claimPayload(registration.type().id(), modId);
        for (ElysiumNetworkRegistry.Registration<?> registration : registry.serverToClientRegistrations()) claimPayload(registration.type().id(), modId);

        SimpleChannel channel = NetworkRegistry.newSimpleChannel(
                channelNameFor(modId), () -> CHANNEL_VERSION,
                NetworkRegistry.acceptMissingOr(CHANNEL_VERSION),
                NetworkRegistry.acceptMissingOr(CHANNEL_VERSION)
        );
        channel.messageBuilder(C2SEnvelope.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SEnvelope::encode)
                .decoder(C2SEnvelope::decode)
                .consumerMainThread((msg, ctxSupplier) -> handleReceive(modId, msg.typeId(), msg.body(), ctxSupplier, true))
                .add();
        channel.messageBuilder(S2CEnvelope.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CEnvelope::encode)
                .decoder(S2CEnvelope::decode)
                .consumerMainThread((msg, ctxSupplier) -> handleReceive(modId, msg.typeId(), msg.body(), ctxSupplier, false))
                .add();
        this.channels.put(modId, channel);
    }

    private void claimPayload(ResourceLocation payloadId, String modId) {
        String existing = this.payloadToModId.putIfAbsent(payloadId, modId);
        if (existing != null && !existing.equals(modId)) {
            throw new IllegalStateException("Payload id '" + payloadId + "' has been claimed by both mod '" + existing + "' and mod '" + modId + "'.");
        }
    }

    private static ResourceLocation channelNameFor(String modId) {
        return ElysiumAPI.idPath(modId, CHANNEL_SUFFIX);
    }

    private void handleReceive(String modId, ResourceLocation typeId, byte[] body, Supplier<NetworkEvent.Context> ctxSupplier, boolean c2s) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ElysiumNetworkRegistry registry = this.registries.get(modId);
        if (registry == null) return;

        ElysiumNetworkRegistry.Registration<?> matched = null;
        ElysiumPayload payload = null;
        try {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(body));
            Collection<ElysiumNetworkRegistry.Registration<?>> candidates = c2s ? registry.clientToServerRegistrations() : registry.serverToClientRegistrations();
            for (ElysiumNetworkRegistry.Registration<?> registration : candidates) {
                if (registration.type().id().equals(typeId)) {
                    matched = registration;
                    payload = decodeUnchecked(registration.type(), buf);
                    break;
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to decode incoming {} payload on channel '{}'", c2s ? "C2S" : "S2C", channelNameFor(modId), e);
            return;
        }
        if (matched == null) {
            LOGGER.warn("Received unknown {} payload '{}' on channel '{}'", c2s ? "C2S" : "S2C", typeId, channelNameFor(modId));
            return;
        }
        ElysiumNetworkContext context;
        if (c2s) {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            context = new ServerContext(player);
        } else {
            context = new ClientContext(modId);
        }
        dispatch(matched, payload, context);
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
        SimpleChannel channel = this.channels.get(modId);
        channel.sendToServer(new C2SEnvelope(payload.type().id(), encodeBody(payload)));
    }

    @Override
    public void sendToPlayer(Player player, ElysiumPayload payload) {
        if (!(player instanceof ServerPlayer serverPlayer)) throw new IllegalArgumentException("sendToPlayer requires a ServerPlayer.");
        String modId = modIdForSend(payload, false);
        SimpleChannel channel = this.channels.get(modId);
        channel.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new S2CEnvelope(payload.type().id(), encodeBody(payload)));
    }

    @Override
    public void sendToAllPlayers(ElysiumPayload payload) {
        String modId = modIdForSend(payload, false);
        SimpleChannel channel = this.channels.get(modId);
        channel.send(PacketDistributor.ALL.noArg(), new S2CEnvelope(payload.type().id(), encodeBody(payload)));
    }

    @Override
    public void sendToAllPlayersInLevel(ServerLevel level, ElysiumPayload payload) {
        String modId = modIdForSend(payload, false);
        SimpleChannel channel = this.channels.get(modId);
        channel.send(PacketDistributor.DIMENSION.with(level::dimension), new S2CEnvelope(payload.type().id(), encodeBody(payload)));
    }

    @Override
    public void sendToPlayersNear(ServerLevel level, Vec3 center, double radius, ElysiumPayload payload) {
        String modId = modIdForSend(payload, false);
        SimpleChannel channel = this.channels.get(modId);
        channel.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(center.x, center.y, center.z, radius * radius, level.dimension())), new S2CEnvelope(payload.type().id(), encodeBody(payload)));
    }

    @Override
    public void sendToPlayersTrackingChunk(ServerLevel level, ChunkPos chunkPos, ElysiumPayload payload) {
        String modId = modIdForSend(payload, false);
        SimpleChannel channel = this.channels.get(modId);
        channel.send(PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunk(chunkPos.x, chunkPos.z)), new S2CEnvelope(payload.type().id(), encodeBody(payload)));
    }

    @Override
    public void sendToPlayersTrackingBlockEntity(BlockEntity blockEntity, ElysiumPayload payload) {
        if (blockEntity.getLevel() instanceof ServerLevel serverLevel) sendToPlayersTrackingChunk(serverLevel, new ChunkPos(blockEntity.getBlockPos()), payload);
    }

    @Override
    public void sendToPlayersTracking(Entity entity, ElysiumPayload payload) {
        String modId = modIdForSend(payload, false);
        SimpleChannel channel = this.channels.get(modId);
        channel.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), new S2CEnvelope(payload.type().id(), encodeBody(payload)));
    }

    @Override
    public void sendToPlayersTrackingAndSelf(Entity entity, ElysiumPayload payload) {
        String modId = modIdForSend(payload, false);
        SimpleChannel channel = this.channels.get(modId);
        channel.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), new S2CEnvelope(payload.type().id(), encodeBody(payload)));
    }

    @Override
    public void sendToAllPlayersExceptSelf(ServerPlayer excluded, ElysiumPayload payload) {
        modIdForSend(payload, false);
        if (excluded.getServer() == null) return;
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

    private static byte[] encodeBody(ElysiumPayload payload) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        encodePayloadUnchecked(payload, buf);
        byte[] body = new byte[buf.readableBytes()];
        buf.readBytes(body);
        return body;
    }

    @SuppressWarnings("unchecked")
    private static <T extends ElysiumPayload> void encodePayloadUnchecked(T payload, FriendlyByteBuf buf) {
        ElysiumPayloadType<T> type = (ElysiumPayloadType<T>) payload.type();
        type.codec().encode(buf, payload);
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
            INSTANCE.sendToServer(payload);
        }

        @Override
        public void enqueueWork(Runnable task) {
            Minecraft.getInstance().execute(task);
        }
    }
}