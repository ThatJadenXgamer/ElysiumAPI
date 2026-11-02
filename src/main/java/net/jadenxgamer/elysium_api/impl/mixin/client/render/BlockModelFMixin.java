package net.jadenxgamer.elysium_api.impl.mixin.client.render;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.jadenxgamer.elysium_api.api.client.render.IElysiumBakedModel;
import net.jadenxgamer.elysium_api.impl.client.render.IElysiumBlockModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.resources.model.BakedModel;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

// asphodel::mixin -> loader=fabric, env=client, file=elysium_api.fabric.mixins.json
@Mixin(BlockModel.class)
public abstract class BlockModelFMixin implements IElysiumBlockModel {
    // asphodel::patch -> version=1.20.1, id=p20
    // asphodel::patch -> version=1.21.1, id=p21

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

    // mass*p20 play
//    @ModifyReturnValue(
//            method = "bake(Lnet/minecraft/client/resources/model/ModelBaker;Ljava/util/function/Function;Lnet/minecraft/client/resources/model/ModelState;Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/resources/model/BakedModel;",
//            at = @At("RETURN"),
//            require = 0
//    )
//    private BakedModel elysium$attachRenderType20(BakedModel original) {
//        return elysium$transferRenderType(original);
//    }
    // mass*p20 pause

    // mass*p21 play
//    @ModifyReturnValue(
//            method = "bake(Lnet/minecraft/client/resources/model/ModelBaker;Ljava/util/function/Function;Lnet/minecraft/client/resources/model/ModelState;)Lnet/minecraft/client/resources/model/BakedModel;",
//            at = @At("RETURN"),
//            require = 0
//    )
//    private BakedModel elysium$attachRenderType21(BakedModel original) {
//        return elysium$transferRenderType(original);
//    }
    // mass*p21 pause

    @Unique
    private BakedModel elysium$transferRenderType(BakedModel original) {
        if (original instanceof IElysiumBakedModel elysiumModel && this.elysium$renderType != null)
            elysiumModel.elysium$setRenderType(this.elysium$renderType);
        return original;
    }
}