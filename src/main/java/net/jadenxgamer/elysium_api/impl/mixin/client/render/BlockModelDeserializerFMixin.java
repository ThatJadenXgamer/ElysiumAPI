package net.jadenxgamer.elysium_api.impl.mixin.client.render;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.jadenxgamer.elysium_api.ElysiumAPI;
import net.jadenxgamer.elysium_api.api.client.render.ElysiumRenderTypes;
import net.jadenxgamer.elysium_api.impl.client.render.IElysiumBlockModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Type;

// asphodel::mixin -> loader=fabric, env=client, file=elysium_api.fabric.mixins.json
@Mixin(BlockModel.Deserializer.class)
public abstract class BlockModelDeserializerFMixin {

    @Inject(method = "deserialize(Lcom/google/gson/JsonElement;Ljava/lang/reflect/Type;Lcom/google/gson/JsonDeserializationContext;)Lnet/minecraft/client/renderer/block/model/BlockModel;", at = @At("RETURN"))
    private void elysium$parseRenderType(JsonElement json, Type type, JsonDeserializationContext context, CallbackInfoReturnable<BlockModel> cir) {
        BlockModel model = cir.getReturnValue();
        if (model == null) return;
        if (!json.isJsonObject()) return;
        JsonObject jsonObject = json.getAsJsonObject();

        JsonElement element = jsonObject.get("render_type");
        if (element == null) return;
        ResourceLocation id = elysium$extractRenderTypeId(element);
        if (id == null) return;

        RenderType renderType = ElysiumRenderTypes.get(id);
        if (renderType == null) {
            ElysiumAPI.LOGGER.warn("Unknown render_type '{}' referenced in block model", id);
            return;
        }

        ((IElysiumBlockModel) model).elysium$setRenderType(renderType);
    }

    private static ResourceLocation elysium$extractRenderTypeId(JsonElement element) {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString())
            return ElysiumRenderTypes.tryParseId(element.getAsString());
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            JsonElement type = object.get("type");
            if (type != null && type.isJsonPrimitive() && type.getAsJsonPrimitive().isString())
                return ElysiumRenderTypes.tryParseId(type.getAsString());
        }
        return null;
    }
}