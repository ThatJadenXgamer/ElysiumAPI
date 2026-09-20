package net.jadenxgamer.elysium_api.impl.mixin.enchantment;

import net.jadenxgamer.elysium_api.api.tags.ElysiumTags;
import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEnchantments.Mutable.class)
public class ItemEnchantmentsMutableMixin {

    @Inject(
        method = "set",
        at = @At(value = "HEAD"),
        cancellable = true
    )
    private void elysium_api$skipDisabledEnchantments(Holder<Enchantment> enchantment, int level, CallbackInfo ci) {
        if (enchantment.is(ElysiumTags.Enchantments.DISABLED)) ci.cancel();
    }
}