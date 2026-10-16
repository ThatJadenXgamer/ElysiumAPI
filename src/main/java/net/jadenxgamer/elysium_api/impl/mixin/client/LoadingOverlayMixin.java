package net.jadenxgamer.elysium_api.impl.mixin.client;

import net.jadenxgamer.elysium_api.impl.client.KhaosLoadingGradient;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LoadingOverlay.class)
public abstract class LoadingOverlayMixin {

    @Shadow private long fadeOutStart;
    @Shadow private long fadeInStart;
    @Shadow @Final private boolean fadeIn;
    @Unique private long elysium$gradientStartMillis = -1L;

    @Inject(
            method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/systems/RenderSystem;disableDepthTest()V",
                    remap = false
            ),
            require = 1
    )
    private void elysium$renderGradient(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        long currentMillis = Util.getMillis();
        if (this.elysium$gradientStartMillis == -1L) this.elysium$gradientStartMillis = currentMillis;
        long elapsedMillis = currentMillis - this.elysium$gradientStartMillis;
        float alpha = KhaosLoadingGradient.computeGradientAlpha(elapsedMillis, this.fadeOutStart, this.fadeInStart, this.fadeIn);

        KhaosLoadingGradient.drawGui(guiGraphics, alpha);
        KhaosLoadingGradient.drawGuiIcons(guiGraphics, alpha);
    }
}