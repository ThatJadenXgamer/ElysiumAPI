package net.jadenxgamer.elysium_api.impl.client.render;

import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.Nullable;

public interface IElysiumBlockModel {
    @Nullable
    RenderType elysium$getRenderType();

    void elysium$setRenderType(@Nullable RenderType renderType);
}