package net.jadenxgamer.elysium_api.impl.mixin.item;

import net.jadenxgamer.elysium_api.impl.core.datadriven.item.properties_transformer.ItemPropertiesTransformer;
import net.jadenxgamer.elysium_api.impl.core.datadriven.item.properties_transformer.ItemPropertiesTransformerHelper;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Shadow public abstract Item getItem();

    @Inject(
            method = "getMaxDamage",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void elysium_api$overrideMaxDamage(CallbackInfoReturnable<Integer> cir) {
        ItemPropertiesTransformer transformer = ItemPropertiesTransformerHelper.getTransformer(getItem());
        if (transformer != null && transformer.properties().maxDamage().isPresent()) {
            cir.setReturnValue(transformer.properties().maxDamage().get());
        }
    }

    @Inject(
            method = "isDamageableItem",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void elysium_api$overrideIsDamageableItem(CallbackInfoReturnable<Boolean> cir) {
        ItemPropertiesTransformer transformer = ItemPropertiesTransformerHelper.getTransformer(getItem());
        if (transformer != null && transformer.properties().maxDamage().isPresent()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(
            method = "getRarity",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void elysium_api$overrideRarity(CallbackInfoReturnable<Rarity> cir) {
        ItemPropertiesTransformer transformer = ItemPropertiesTransformerHelper.getTransformer(getItem());
        if (transformer != null && transformer.properties().rarity().isPresent()) {
            cir.setReturnValue(transformer.properties().rarity().get().toVanillaRarity());
        }
    }

    @Inject(
            method = "getMaxStackSize",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void elysium_api$overrideMaxStackSize(CallbackInfoReturnable<Integer> cir) {
        ItemPropertiesTransformer transformer = ItemPropertiesTransformerHelper.getTransformer(getItem());
        if (transformer != null && transformer.properties().maxStackSize().isPresent()) {
            cir.setReturnValue(transformer.properties().maxStackSize().get());
        }
    }

    @Inject(
            method = "canBeHurtBy",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void elysium_api$overrideFireResistant(DamageSource damageSource, CallbackInfoReturnable<Boolean> cir) {
        ItemPropertiesTransformer transformer = ItemPropertiesTransformerHelper.getTransformer(getItem());
        if (transformer != null && transformer.properties().fireResistant().isPresent()) {
            boolean isFireResistant = transformer.properties().fireResistant().get();
            if (isFireResistant && damageSource.is(DamageTypeTags.IS_FIRE)) {
                cir.setReturnValue(false);
            }
        }
    }
}