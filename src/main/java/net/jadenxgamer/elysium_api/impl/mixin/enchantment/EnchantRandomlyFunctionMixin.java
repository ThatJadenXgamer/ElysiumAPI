package net.jadenxgamer.elysium_api.impl.mixin.enchantment;

import net.jadenxgamer.elysium_api.api.tags.ElysiumTags;
import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.List;
import java.util.stream.Collectors;

@Mixin(EnchantRandomlyFunction.class)
public class EnchantRandomlyFunctionMixin {

    @ModifyArg(
            method = "run",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/Util;getRandomSafe(Ljava/util/List;Lnet/minecraft/util/RandomSource;)Ljava/util/Optional;"),
            index = 0
    )
    private List<Holder<Enchantment>> elysium_api$filterDisabledEnchantments(List<Holder<Enchantment>> selections) {
        return selections.stream().filter(holder -> !holder.is(ElysiumTags.Enchantments.DISABLED)).collect(Collectors.toList());
    }
}