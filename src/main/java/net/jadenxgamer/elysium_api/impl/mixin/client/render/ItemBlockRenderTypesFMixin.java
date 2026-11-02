package net.jadenxgamer.elysium_api.impl.mixin.client.render;

import net.jadenxgamer.elysium_api.api.client.render.IElysiumBakedModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// asphodel::mixin -> loader=fabric, env=client, file=elysium_api.fabric.mixins.json
@Mixin(ItemBlockRenderTypes.class)
public abstract class ItemBlockRenderTypesFMixin {

    @Inject(method = "getChunkRenderType", at = @At("HEAD"), cancellable = true)
    private static void elysium$chunkRenderType(BlockState state, CallbackInfoReturnable<RenderType> cir) {
        RenderType renderType = elysium$resolveRenderType(state);
        if (renderType != null) {
            cir.setReturnValue(renderType);
        }
    }

    @Inject(method = "getMovingBlockRenderType", at = @At("HEAD"), cancellable = true)
    private static void elysium$movingBlockRenderType(BlockState state, CallbackInfoReturnable<RenderType> cir) {
        RenderType renderType = elysium$resolveRenderType(state);
        if (renderType != null) {
            cir.setReturnValue(renderType);
        }
    }

    @Inject(
        method = "getRenderType(Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/client/renderer/RenderType;",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void elysium$itemRenderType(ItemStack stack, boolean cull, CallbackInfoReturnable<RenderType> cir) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) return;
        BlockState state = blockItem.getBlock().defaultBlockState();
        RenderType renderType = elysium$resolveRenderType(state);
        if (renderType != null) cir.setReturnValue(renderType);
    }

    private static RenderType elysium$resolveRenderType(BlockState state) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) return null;
        BlockRenderDispatcher dispatcher = minecraft.getBlockRenderer();
        if (dispatcher == null) return null;
        BakedModel model = dispatcher.getBlockModel(state);
        if (!(model instanceof IElysiumBakedModel elysiumModel)) return null;

        return elysiumModel.elysium$getRenderType();
    }
}