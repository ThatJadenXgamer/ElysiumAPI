package net.jadenxgamer.elysium_api.impl.mixin.item;

import net.jadenxgamer.elysium_api.impl.core.datadriven.item.properties_transformer.ItemPropertiesTransformer;
import net.jadenxgamer.elysium_api.impl.core.datadriven.item.properties_transformer.ItemPropertiesTransformerHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public abstract class ItemMixin {

    @Shadow public abstract Item asItem();

    @Inject(
            method = "isRepairable",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void elysium_api$overrideCanRepair(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        ItemPropertiesTransformer transformer = ItemPropertiesTransformerHelper.getTransformer(asItem());
        if (transformer != null && transformer.properties().canRepair().isPresent()) {
            cir.setReturnValue(transformer.properties().canRepair().get());
        }
    }
}