package net.jadenxgamer.elysium_api.impl.mixin.enchantment;

import net.jadenxgamer.elysium_api.api.tags.ElysiumTags;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.stream.Stream;

@Mixin(EnchantmentHelper.class)
public class EnchantmentHelperMixin {

    @Inject(
        method = "getAvailableEnchantmentResults",
        at = @At(value = "RETURN"),
        cancellable = true
    )
    private static void elysium_api$filterDisabledEnchantments(int level, ItemStack stack, Stream<Holder<Enchantment>> possibleEnchantments, CallbackInfoReturnable<List<EnchantmentInstance>> cir) {
        List<EnchantmentInstance> list = cir.getReturnValue();
        list.removeIf(instance -> instance.enchantment.is(ElysiumTags.Enchantments.DISABLED));
        cir.setReturnValue(list);
    }
}