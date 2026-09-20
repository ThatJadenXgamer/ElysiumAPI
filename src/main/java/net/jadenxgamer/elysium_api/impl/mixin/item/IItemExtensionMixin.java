package net.jadenxgamer.elysium_api.impl.mixin.item;

import net.jadenxgamer.elysium_api.impl.core.datadriven.item.properties_transformer.ItemPropertiesTransformer;
import net.jadenxgamer.elysium_api.impl.core.datadriven.item.properties_transformer.ItemPropertiesTransformerHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.extensions.IItemExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(IItemExtension.class)
public interface IItemExtensionMixin {

    @Inject(
            method = "getMaxStackSize",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void elysium_api$overrideMaxStackSize(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        ItemPropertiesTransformer transformer = ItemPropertiesTransformerHelper.getTransformer(stack.getItem());
        if (transformer != null && transformer.properties().maxStackSize().isPresent()) {
            cir.setReturnValue(transformer.properties().maxStackSize().get());
        }
    }

    @Inject(
            method = "getMaxDamage",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void elysium_api$overrideMaxDamage(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        ItemPropertiesTransformer transformer = ItemPropertiesTransformerHelper.getTransformer(stack.getItem());
        if (transformer != null && transformer.properties().maxDamage().isPresent()) {
            cir.setReturnValue(transformer.properties().maxDamage().get());
        }
    }

    @Inject(
            method = "getFoodProperties",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void elysium_api$overrideFoodProperties(ItemStack stack, LivingEntity entity, CallbackInfoReturnable<FoodProperties> cir) {
        ItemPropertiesTransformer transformer = ItemPropertiesTransformerHelper.getTransformer(stack.getItem());
        if (transformer != null && transformer.properties().food().isPresent()) {
            cir.setReturnValue(transformer.properties().food().get());
        }
    }

    @Inject(
            method = "getCraftingRemainingItem",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void elysium_api$craftRemainder(ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
        ItemPropertiesTransformer transformer = ItemPropertiesTransformerHelper.getTransformer(stack.getItem());
        if (transformer != null && transformer.properties().craftRemainder().isPresent()) {
            Item remainderItem = transformer.properties().craftRemainder().get().value();
            cir.setReturnValue(new ItemStack(remainderItem));
        }
    }

    @Inject(
            method = "hasCraftingRemainingItem",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void elysium_api$hasCraftRemainder(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        ItemPropertiesTransformer transformer = ItemPropertiesTransformerHelper.getTransformer(stack.getItem());
        if (transformer != null && transformer.properties().craftRemainder().isPresent()) {
            cir.setReturnValue(true);
        }
    }
}