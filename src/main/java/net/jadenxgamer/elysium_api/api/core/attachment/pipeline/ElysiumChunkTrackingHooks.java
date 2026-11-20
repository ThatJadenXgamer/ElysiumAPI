package net.jadenxgamer.elysium_api.api.core.attachment.pipeline;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public final class ElysiumChunkTrackingHooks {

    @Nullable private static Tracker tracker;

    private ElysiumChunkTrackingHooks() {
    }

    public static void setTracker(@Nullable Tracker newTracker) {
        tracker = newTracker;
    }

    public static void onChunkStartTracking(ServerPlayer player, ChunkPos pos) {
        if (tracker != null) tracker.onStartTracking(player, pos);
    }

    public static void onChunkStopTracking(ServerPlayer player, ChunkPos pos) {
        if (tracker != null) tracker.onStopTracking(player, pos);
    }

    public interface Tracker {
        void onStartTracking(ServerPlayer player, ChunkPos pos);
        void onStopTracking(ServerPlayer player, ChunkPos pos);
    }
}