package net.jadenxgamer.elysium_api.api.network.pipeline;

import net.jadenxgamer.elysium_api.api.network.ElysiumNetworkRegistry;
import net.jadenxgamer.elysium_api.api.network.payload.ElysiumPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus;

/**
 * The internal bridge between the cross-version API and the loader-specific networking
 * implementations.
 * <p>
 * This interface should not be implemented or called by downstream mods. It is used
 * exclusively by Elysium API's internal loader adapters to install the active network
 * backend into {@link net.jadenxgamer.elysium_api.api.network.ElysiumNetwork}.
 */
@ApiStatus.Internal
public interface ElysiumNetworkBackend {

    void register(String modId, ElysiumNetworkRegistry registry);

    void sendToServer(ElysiumPayload payload);

    void sendToPlayer(Player player, ElysiumPayload payload);

    void sendToAllPlayers(ElysiumPayload payload);

    void sendToAllPlayersInLevel(ServerLevel level, ElysiumPayload payload);

    void sendToPlayersNear(ServerLevel level, Vec3 center, double radius, ElysiumPayload payload);

    void sendToPlayersTrackingChunk(ServerLevel level, ChunkPos chunkPos, ElysiumPayload payload);

    void sendToPlayersTrackingBlockEntity(BlockEntity blockEntity, ElysiumPayload payload);

    void sendToPlayersTracking(Entity entity, ElysiumPayload payload);

    void sendToPlayersTrackingAndSelf(Entity entity, ElysiumPayload payload);

    void sendToAllPlayersExceptSelf(ServerPlayer excluded, ElysiumPayload payload);
}