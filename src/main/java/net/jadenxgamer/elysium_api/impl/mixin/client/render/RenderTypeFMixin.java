package net.jadenxgamer.elysium_api.impl.mixin.client.render;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.jadenxgamer.elysium_api.api.client.render.ElysiumRenderTypes;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// asphodel::mixin -> loader=fabric, env=client, file=elysium_api.fabric.mixins.json
@Mixin(RenderType.class)
public abstract class RenderTypeFMixin {

    @Inject(method = "<init>", at = @At("RETURN"))
    private void elysium$captureRenderType(String name, VertexFormat format, VertexFormat.Mode mode,
                                           int bufferSize, boolean affectsCrumbling, boolean sortOnUpload,
                                           Runnable setupState, Runnable clearState, CallbackInfo ci) {
        ElysiumRenderTypes.capture(name, (RenderType) (Object) this);
    }
}