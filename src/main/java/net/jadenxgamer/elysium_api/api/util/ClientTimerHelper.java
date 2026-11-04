package net.jadenxgamer.elysium_api.api.util;

import net.minecraft.client.Minecraft;

public final class ClientTimerHelper {

    // asphodel::patch -> loader=*, version=1.21.1, id=p21

    private ClientTimerHelper() {}

    /**
     * Obtain the game-time delta in ticks for the current frame that works across 1.20.1 and 1.21.1.
     * @return the number of game ticks that have elapsed since the previous frame.
     */
    public static float getGameTimeDeltaTicks() {
        // return Minecraft.getInstance().getTimer().getGameTimeDeltaTicks(); *p21
        return Minecraft.getInstance().getDeltaFrameTime(); // remove*p21
    }

    /**
     * Obtain the game-time partial tick for the current frame that works across 1.20.1 and 1.21.1.
     * @param runsNormally whether the game is currently ticking normally.
     * @return the fractional progress through the current game tick.
     */
    public static float getGameTimeDeltaPartialTick(boolean runsNormally) {
        // return Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(runsNormally); *p21
        return Minecraft.getInstance().getFrameTime(); // remove*p21
    }

    /**
     * Obtain the real-time delta in ticks for the current frame that works across 1.20.1 and 1.21.1.
     * @return the number of ticks elapsed in real time since the previous frame, falling back to the game-time delta on 1.20.1.
     */
    public static float getRealtimeDeltaTicks() {
        // return Minecraft.getInstance().getTimer().getRealtimeDeltaTicks(); *p21
        return Minecraft.getInstance().getDeltaFrameTime(); // remove*p21
    }
}