package net.jadenxgamer.elysium_api.impl.event;

import com.mojang.blaze3d.systems.RenderSystem;
import net.jadenxgamer.elysium_api.ElysiumAPI;
import net.jadenxgamer.elysium_api.api.charon.*;
import net.jadenxgamer.elysium_api.api.client.screen_flash.ScreenFlash;
import net.jadenxgamer.elysium_api.impl.client.assetdriven.fog_settings.FogSettingsManager;
import net.jadenxgamer.elysium_api.impl.client.assetdriven.lightmap_settings.LightmapSettingsManager;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.world.level.material.FogType;

@CharonEventBoat(modid = ElysiumAPI.MOD_ID, dist = CharonDist.CLIENT)
public class ElysiumClientCharons {

    @CharonEvent(target = Minecraft.class, method = "tick", at = @Toll("HEAD"))
    public static void onClientTickPre(CharonContext ctx) {

    }

    @CharonEvent(target = Minecraft.class, method = "tick", at = @Toll("RETURN"))
    public static void onClientTickPost(CharonContext ctx) {

    }

    @CharonEvent(target = Gui.class, method = "render", at = @Toll("HEAD"))
    public static void onRenderGui(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CharonContext ctx) {
        ScreenFlash.handle(guiGraphics, deltaTracker.getGameTimeDeltaPartialTick(false));
    }

    @CharonPriority(10)
    @CharonEvent(target = FogRenderer.class, method = "setupFog", at = @Toll("RETURN"))
    public static void onFogRender(Camera camera, FogRenderer.FogMode fogMode, float partialTick, CharonContext ctx) {
        if (camera.getFluidInCamera() == FogType.NONE && fogMode == FogRenderer.FogMode.FOG_TERRAIN) {
            BlockPos eyePos = BlockPos.containing(camera.getEntity().getEyePosition(partialTick));
            if (!camera.getEntity().level().getFluidState(eyePos).isEmpty()) return;
            var settings = ElysiumAPI.FOG_SETTINGS.getSettings(Minecraft.getInstance().player, RenderSystem.getShaderFogStart(), RenderSystem.getShaderFogEnd());
            if (settings != null) {
                RenderSystem.setShaderFogStart(settings.getLeft());
                RenderSystem.setShaderFogEnd(settings.getRight());
            }
        }
    }

    @CharonEvent(target = ReloadableResourceManager.class, method = "<init>", at = @Toll("TAIL"))
    public static void registerReloadListeners(PackType packType, CharonContext ctx) {
        if (packType != PackType.CLIENT_RESOURCES) return;
        ReloadableResourceManager resourceManager = ctx.getSelf();
        resourceManager.registerReloadListener(new FogSettingsManager());
        resourceManager.registerReloadListener(new LightmapSettingsManager());
    }
}