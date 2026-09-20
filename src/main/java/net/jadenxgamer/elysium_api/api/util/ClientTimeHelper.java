package net.jadenxgamer.elysium_api.api.util;

import net.minecraft.client.Minecraft;

public final class ClientTimeHelper {

    private ClientTimeHelper() {}

    public static float getGameTimeDeltaTicks() {
        return Minecraft.getInstance().getDeltaFrameTime();
    }

    public static float getGameTimeDeltaPartialTick(boolean ignoreFreeze) {
        return Minecraft.getInstance().getFrameTime();
    }

    public static float getRealDeltaTick() {
        return Minecraft.getInstance().getDeltaFrameTime();
    }

    public static long getRealTimeDelta() {
        return Minecraft.getInstance().getFrameTimeNs() / 1_000_000L;
    }
}