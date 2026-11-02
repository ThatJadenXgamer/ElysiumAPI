package net.jadenxgamer.elysium_api.api.client.render;

import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.Nullable;

public interface IElysiumBakedModel {
    @Nullable
    RenderType elysium$getRenderType();

    void elysium$setRenderType(@Nullable RenderType renderType);
}