package net.jadenxgamer.elysium_api.api.client.screen_flash;

import com.mojang.blaze3d.systems.RenderSystem;
import net.jadenxgamer.elysium_api.ElysiumAPI;
import net.jadenxgamer.elysium_api.impl.networking.to_client.ScreenFlashPayload;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public class ScreenFlash {

    private static final ResourceLocation TEXTURE = ElysiumAPI.elysiumPath("textures/misc/screen_flash.png");
    private static int fadeInDuration = 0;
    private static int holdDuration = 0;
    private static int fadeOutDuration = 0;
    private static boolean playing = false;
    private static float startTime = -1;
    private static boolean firstPersonOnly = true;
    private static int flashColor = 0xFFFFFFFF;

    /**
     * Triggers a customizable Screen Flash on the <u>client</u> screen
     * @param fadeIn duration in ticks for the ScreenFlash to fade into the screen
     * @param hold duration in ticks to hold at maximum opacity after fade-in
     * @param fadeOut duration in ticks for the ScreenFlash to fade away and be removed
     * @param color ARGB color value (0xAARRGGBB) for the flash
     * @param firstPersonOnly if true the screen flash will only play when in first person camera
     * @param force if a screen flash is already playing it overwrites it and plays the new one
     */
    public static void triggerScreenFlash(int fadeIn, int hold, int fadeOut, int color, boolean firstPersonOnly, boolean force) {
        if (playing && !force) return;
        if (force) stopScreenFlash();

        ScreenFlash.fadeInDuration = fadeIn;
        ScreenFlash.holdDuration = hold;
        ScreenFlash.fadeOutDuration = fadeOut;
        ScreenFlash.flashColor = color;
        ScreenFlash.playing = true;
        ScreenFlash.firstPersonOnly = firstPersonOnly;
        ScreenFlash.startTime = -1;
    }

    /**
     * Triggers a customizable Screen Flash for given ServerPlayer
     * @param player the player that will receive the ScreenFlashPayload
     * @param fadeIn duration in ticks for the ScreenFlash to fade into the screen
     * @param hold duration in ticks to hold at maximum opacity after fade-in
     * @param fadeOut duration in ticks for the ScreenFlash to fade away and be removed
     * @param color ARGB color value (0xAARRGGBB) for the flash
     * @param firstPersonOnly if true the screen flash will only play when in first person camera
     * @param force if a screen flash is already playing it overwrites it and plays the new one
     */
    public static void triggerScreenFlash(ServerPlayer player, int fadeIn, int hold, int fadeOut, int color, boolean firstPersonOnly, boolean force) {
        PacketDistributor.sendToPlayer(player, new ScreenFlashPayload(fadeIn, hold, fadeOut, color, firstPersonOnly, force));
    }

    /**
     * Forcefully stop the current ScreenFlash
     */
    public static void stopScreenFlash() {
        ScreenFlash.playing = false;
        ScreenFlash.fadeInDuration = 0;
        ScreenFlash.holdDuration = 0;
        ScreenFlash.fadeOutDuration = 0;
        ScreenFlash.startTime = -1;
        ScreenFlash.firstPersonOnly = true;
        ScreenFlash.flashColor = 0xFFFFFFFF;
    }

    public static void handle(GuiGraphics guiGraphics, float partialTick) {
        if (!playing) return;

        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) {
            stopScreenFlash();
            return;
        }

        if (startTime < 0) {
            startTime = client.level.getGameTime() + partialTick;
        }

        float currentTime = client.level.getGameTime() + partialTick;
        float elapsed = currentTime - startTime;
        float totalDuration = fadeInDuration + holdDuration + fadeOutDuration;

        if (client.options.getCameraType() != CameraType.FIRST_PERSON && firstPersonOnly) {
            if (elapsed >= totalDuration) {
                stopScreenFlash();
            }
            return;
        }

        if (elapsed >= totalDuration) {
            stopScreenFlash();
            return;
        }

        float opacity;
        if (elapsed < fadeInDuration) {
            opacity = elapsed / fadeInDuration;
        } else if (elapsed < fadeInDuration + holdDuration) {
            opacity = 1.0f;
        } else {
            float fadeOutStart = fadeInDuration + holdDuration;
            opacity = 1f - (elapsed - fadeOutStart) / fadeOutDuration;
        }

        renderScreenFlash(guiGraphics, client, opacity);
    }

    private static void renderScreenFlash(GuiGraphics guiGraphics, Minecraft client, float opacity) {
        float screenEffectIntensity = client.options.screenEffectScale().get().floatValue();
        float actualOpacity = opacity * screenEffectIntensity;

        if (actualOpacity <= 0.001f) return;

        float alpha = (float) ((flashColor >> 24) & 0xFF) / 255.0f;
        float red = (float) ((flashColor >> 16) & 0xFF) / 255.0f;
        float green = (float) ((flashColor >> 8) & 0xFF) / 255.0f;
        float blue = (float) (flashColor & 0xFF) / 255.0f;
        float finalAlpha = alpha * actualOpacity;

        int width = client.getWindow().getGuiScaledWidth();
        int height = client.getWindow().getGuiScaledHeight();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        guiGraphics.setColor(red, green, blue, finalAlpha);
        guiGraphics.blit(TEXTURE, 0, 0, width, height, 0, 0, 2, 2, 2, 2);

        RenderSystem.disableBlend();
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}