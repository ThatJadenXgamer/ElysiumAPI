package net.jadenxgamer.elysium_api.impl.mixin.client;

import net.jadenxgamer.elysium_api.impl.client.KhaosLoadingGradient;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.loading.ForgeLoadingOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ForgeLoadingOverlay.class)
public abstract class ForgeLoadingOverlayMixin {

    @Shadow private long fadeOutStart;
    @Unique private long elysium$gradientStartMillis = -1L;

    @Inject(
            method = "render",
            at = @At("TAIL")
    )
    private void elysium$renderGradient(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        // Forge's startup screen loads way too early for mixins to intercept and is real tricky to inject a normal gradient
        // at least without ours rendering above everything or having theirs render over ours. So the best solution I found was adding the gradient through GL
        long currentMillis = Util.getMillis();
        if (this.elysium$gradientStartMillis == -1L) this.elysium$gradientStartMillis = currentMillis;
        long elapsedMillis = currentMillis - this.elysium$gradientStartMillis;

        float alpha = KhaosLoadingGradient.computeGradientAlpha(elapsedMillis, this.fadeOutStart, -1L, false);
        int width = Minecraft.getInstance().getWindow().getWidth();
        int height = Minecraft.getInstance().getWindow().getHeight();

        KhaosLoadingGradient.drawFramebuffer(width, height, alpha);
        KhaosLoadingGradient.drawFramebufferIcons(width, height, alpha);
    }
}