package net.jadenxgamer.elysium_api.impl.mixin.client;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

// asphodel::mixin -> loader=*, version=* -> env=client, file=elysium_api.mixins.json
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    // asphodel::patch -> version=1.20.1 -> id=p20
    // asphodel::patch -> version=1.21.1 -> id=p21

    @Shadow
    @Final
    private LightTexture lightTexture;

    // mass*p20 play
//    @org.spongepowered.asm.mixin.injection.Inject(method = "render",
//            at = @org.spongepowered.asm.mixin.injection.At(value = "NEW", target = "(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)Lnet/minecraft/client/gui/GuiGraphics;")
//    )
//    private void elysium_api$setupGUILightmap(float partialTicks, long nanoTime, boolean renderLevel, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
//        if (net.jadenxgamer.elysium_api.api.util.UniversalModLoaderHelper.isModLoaded("polytone")) return;
//        net.jadenxgamer.elysium_api.ElysiumAPI.LIGHTMAP_SETTINGS.setupForGUI(true);
//        lightTexture.turnOnLightLayer();
//    }
//
//    @org.spongepowered.asm.mixin.injection.Inject(
//            method = "render",
//            at = @org.spongepowered.asm.mixin.injection.At(value = "TAIL")
//    )
//    private void elysium_api$resetGUILightmap(float partialTicks, long nanoTime, boolean renderLevel, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
//        if (net.jadenxgamer.elysium_api.api.util.UniversalModLoaderHelper.isModLoaded("polytone")) return;
//        net.jadenxgamer.elysium_api.ElysiumAPI.LIGHTMAP_SETTINGS.setupForGUI(false);
//        lightTexture.turnOnLightLayer();
//    }
    // mass*p20 pause

    // mass*p21 play
//    @org.spongepowered.asm.mixin.injection.Inject(method = "render",
//            at = @org.spongepowered.asm.mixin.injection.At(value = "NEW", target = "(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)Lnet/minecraft/client/gui/GuiGraphics;")
//    )
//    private void elysium_api$setupGUILightmap(net.minecraft.client.DeltaTracker deltaTracker, boolean bl, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
//        if (net.jadenxgamer.elysium_api.api.util.UniversalModLoaderHelper.isModLoaded("polytone")) return;
//        net.jadenxgamer.elysium_api.ElysiumAPI.LIGHTMAP_SETTINGS.setupForGUI(true);
//        lightTexture.turnOnLightLayer();
//    }
//
//    @org.spongepowered.asm.mixin.injection.Inject(
//            method = "render",
//            at = @org.spongepowered.asm.mixin.injection.At(value = "TAIL")
//    )
//    private void elysium_api$resetGUILightmap(net.minecraft.client.DeltaTracker deltaTracker, boolean bl, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
//        if (net.jadenxgamer.elysium_api.api.util.UniversalModLoaderHelper.isModLoaded("polytone")) return;
//        net.jadenxgamer.elysium_api.ElysiumAPI.LIGHTMAP_SETTINGS.setupForGUI(false);
//        lightTexture.turnOnLightLayer();
//    }
    // mass*p21 pause
}