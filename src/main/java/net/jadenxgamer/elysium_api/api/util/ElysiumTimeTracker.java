package net.jadenxgamer.elysium_api.api.util;

import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public final class ElysiumTimeTracker {
    private ElysiumTimeTracker() {}

    public enum TimeSource { SERVER, CLIENT, CURRENT }

    public enum TimeUnit {
        MILLISECONDS(50.0),
        SECONDS(1.0 / 20.0),
        MINUTES(1.0 / (20.0 * 60.0)),
        HOURS(1.0 / (20.0 * 60.0 * 60.0)),
        DAYS(1.0 / (20.0 * 60.0 * 60.0 * 24.0)),
        GAME_DAYS(1.0 / 24000.0),
        GAME_HOURS(1.0 / 1000.0);

        private final double multiplier;

        TimeUnit(double multiplier) { this.multiplier = multiplier; }

        public double convert(long ticks) { return ticks < 0 ? -1.0 : ticks * multiplier; }
    }

    private static long getTicksForSource(TimeSource source) {
        return switch (source) {
            case SERVER -> {
                MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
                yield server != null ? server.getTickCount() : -1L;
            }
            case CLIENT -> {
                if (FMLEnvironment.dist != Dist.CLIENT) yield -1L;
                Minecraft client = Minecraft.getInstance();
                yield client.level != null ? client.level.getGameTime() : -1L;
            }
            case CURRENT -> {
                long clientTicks = getTicksForSource(TimeSource.CLIENT);
                yield clientTicks != -1L ? clientTicks : getTicksForSource(TimeSource.SERVER);
            }
        };
    }

    public static TimeTracker getServer() { return new SourceTimeTracker(TimeSource.SERVER); }
    public static TimeTracker getClient() { return new SourceTimeTracker(TimeSource.CLIENT); }
    public static TimeTracker getCurrent() { return new SourceTimeTracker(TimeSource.CURRENT); }

    public static Timer createTimer(TimeSource source) {
        return new Timer(source);
    }

    public static IntervalTimer createIntervalTimer(long intervalTicks, TimeSource source) {
        return new IntervalTimer(intervalTicks, source);
    }

    public interface TimeTracker {
        long getTicks();

        default double getTime(TimeUnit unit) {
            return unit.convert(getTicks());
        }
    }

    private record SourceTimeTracker(TimeSource source) implements TimeTracker {

        @Override public long getTicks() {
            return ElysiumTimeTracker.getTicksForSource(source);
        }
    }

    public static final class Timer {
        private final TimeSource source;
        private volatile boolean running;
        private long startTime;
        private long accumulated;

        private Timer(TimeSource source) { this.source = source; reset(); }

        public synchronized void start() {
            if (running) return;
            long now = ElysiumTimeTracker.getTicksForSource(source);
            if (now < 0) return;
            this.startTime = now;
            this.running = true;
        }

        public synchronized void pause() {
            if (!running) return;
            long now = ElysiumTimeTracker.getTicksForSource(source);
            if (now < 0) return;
            this.accumulated += (now - this.startTime);
            this.running = false;
        }

        public synchronized void resume() {
            if (running) return;
            long now = ElysiumTimeTracker.getTicksForSource(source);
            if (now < 0) return;
            this.startTime = now;
            this.running = true;
        }

        public synchronized void reset() {
            this.running = false;
            this.startTime = ElysiumTimeTracker.getTicksForSource(source);
            this.accumulated = 0L;
        }

        public synchronized void stop() {
            this.running = false;
            this.startTime = -1L;
            this.accumulated = 0L;
        }

        public synchronized long getElapsedTicks() {
            long now = ElysiumTimeTracker.getTicksForSource(source);
            if (now < 0) return -1L;
            long base = this.running ? (now - this.startTime) : 0L;
            return this.accumulated + base;
        }

        public double getElapsedTime(TimeUnit unit) {
            return unit.convert(getElapsedTicks());
        }

        public boolean isRunning() {
            return this.running;
        }
    }

    public static final class IntervalTimer {
        private final TimeSource source;
        private long intervalTicks;
        private long lastTriggerTick;

        private IntervalTimer(long intervalTicks, TimeSource source) {
            this.source = source;
            this.intervalTicks = intervalTicks;
            this.lastTriggerTick = getTicksForSource(source);
        }

        public boolean check() {
            long current = getTicksForSource(source);
            if (current < 0) return false;
            if (current - lastTriggerTick >= intervalTicks) {
                lastTriggerTick = current;
                return true;
            }
            return false;
        }

        public void reset() {
            long now = getTicksForSource(source);
            if (now >= 0) this.lastTriggerTick = now;
        }

        public void setInterval(long intervalTicks) { this.intervalTicks = intervalTicks; }

        public long getElapsedTicksSinceLastTrigger() {
            long current = getTicksForSource(source);
            return current < 0 ? -1L : current - lastTriggerTick;
        }

        public double getTimeSinceLastTrigger(TimeUnit unit) {
            return unit.convert(getElapsedTicksSinceLastTrigger());
        }

        public long getIntervalTicks() {
            return intervalTicks;
        }
        public TimeSource getSource() {
            return source;
        }
    }
}