package net.jadenxgamer.elysium_api.impl.mixin.enchantment;

import net.jadenxgamer.elysium_api.api.tags.ElysiumTags;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Holder;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.Set;

@Mixin(CreativeModeTab.class)
public class CreativeModeTabMixin {

    @Shadow private Collection<ItemStack> displayItems;

    @Shadow private Set<ItemStack> displayItemsSearchTab;

    @Inject(
            method = "buildContents",
            at = @At(value = "TAIL")
    )
    private void removeDisabledEnchantmentBooks(CallbackInfo ci) {
        elysium_api$filterDisabledEnchantments(displayItems);
        elysium_api$filterDisabledEnchantments(displayItemsSearchTab);
    }

    @Unique
    private void elysium_api$filterDisabledEnchantments(Collection<ItemStack> stacks) {
        stacks.removeIf(stack -> {
            ItemEnchantments stored = stack.get(DataComponents.STORED_ENCHANTMENTS);
            if (stored != null) for (Holder<Enchantment> holder : stored.keySet())
                if (holder.is(ElysiumTags.Enchantments.DISABLED)) return true;
            return false;
        });
    }
}