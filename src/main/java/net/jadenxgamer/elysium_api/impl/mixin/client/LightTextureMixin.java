package net.jadenxgamer.elysium_api.impl.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import net.jadenxgamer.elysium_api.ElysiumAPI;
import net.jadenxgamer.elysium_api.impl.client.assetdriven.lightmap_settings.LightmapSettingsManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.dimension.DimensionType;
import net.neoforged.fml.ModList;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightTexture.class)
public abstract class LightTextureMixin {

    @Unique
    private static final Vector3f elysium_api$skyMultiplier = new Vector3f(1.0f, 1.0f, 1.0f);
    @Unique
    private static final Vector3f elysium_api$blockMultiplier = new Vector3f(1.0f, 1.0f, 1.0f);
    @Unique
    private static float elysium_api$ambientBrightness = 0.0f;

    @Inject(method = "updateLightTexture", at = @At("HEAD"))
    private void elysium_api$updateLightmapMultipliers(float partialTicks, CallbackInfo ci) {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            var settings = ElysiumAPI.LIGHTMAP_SETTINGS.getSettings(player);
            elysium_api$skyMultiplier.set(settings.getLeft());
            elysium_api$blockMultiplier.set(settings.getMiddle());
            elysium_api$ambientBrightness = settings.getRight();
        } else {
            elysium_api$skyMultiplier.set(1.0f, 1.0f, 1.0f);
            elysium_api$blockMultiplier.set(1.0f, 1.0f, 1.0f);
            elysium_api$ambientBrightness = 0.0f;
        }
    }

    @Inject(
            method = "updateLightTexture",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/joml/Vector3f;add(Lorg/joml/Vector3fc;)Lorg/joml/Vector3f;",
                    shift = At.Shift.BEFORE
            )
    )
    private void elysium_api$applyLightmapColors(float partialTicks, CallbackInfo ci, @Local(name = "vector3f1") Vector3f vector3f1, @Local(name = "vector3f2") Vector3f vector3f2) {
        vector3f1.mul(elysium_api$blockMultiplier);
        vector3f2.mul(elysium_api$skyMultiplier);
    }

    @WrapOperation(
            method = "getBrightness",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/dimension/DimensionType;ambientLight()F")
    )
    private static float elysium_api$getBrightness(DimensionType instance, Operation<Float> original) {
        return instance.ambientLight() + elysium_api$ambientBrightness;
    }

    @Inject(
            method = "turnOnLightLayer",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    public void elysium_api$useGuiLightmap(CallbackInfo ci) {
        if (ModList.get().isLoaded("polytone")) return;
        if (ElysiumAPI.LIGHTMAP_SETTINGS.isGui()) {
            RenderSystem.setShaderTexture(2, LightmapSettingsManager.GUI_LIGHTMAP);
            Minecraft.getInstance().getTextureManager().bindForSetup(LightmapSettingsManager.GUI_LIGHTMAP);
            RenderSystem.texParameter(3553, 10241, 9729);
            RenderSystem.texParameter(3553, 10240, 9729);
            ci.cancel();
        }
    }
}