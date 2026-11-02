package net.jadenxgamer.elysium_api.impl.mixin.client.render;

import net.jadenxgamer.elysium_api.api.client.render.IElysiumBakedModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.SimpleBakedModel;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

// asphodel::mixin -> loader=fabric, env=client, file=elysium_api.fabric.mixins.json
@Mixin(SimpleBakedModel.class)
public abstract class SimpleBakedModelFMixin implements IElysiumBakedModel {
    @Unique
    private RenderType elysium$renderType;

    @Override
    @Nullable
    public RenderType elysium$getRenderType() {
        return this.elysium$renderType;
    }

    @Override
    public void elysium$setRenderType(@Nullable RenderType renderType) {
        this.elysium$renderType = renderType;
    }
}