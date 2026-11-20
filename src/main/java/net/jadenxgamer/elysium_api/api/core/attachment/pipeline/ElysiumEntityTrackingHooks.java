package net.jadenxgamer.elysium_api.api.core.attachment.pipeline;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public final class ElysiumEntityTrackingHooks {

    @Nullable private static Tracker tracker;

    private ElysiumEntityTrackingHooks() {}

    public static void setTracker(@Nullable Tracker newTracker) {
        tracker = newTracker;
    }

    public static void onPlayerStartTracking(ServerPlayer player, Entity entity) {
        if (tracker != null) tracker.onStartTracking(player, entity);
    }

    public static void onPlayerStopTracking(ServerPlayer player, Entity entity) {
        if (tracker != null) tracker.onStopTracking(player, entity);
    }

    public interface Tracker {
        void onStartTracking(ServerPlayer player, Entity entity);
        void onStopTracking(ServerPlayer player, Entity entity);
    }
}