package net.jadenxgamer.elysium_api.impl.mixin.entity.ai;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.jadenxgamer.elysium_api.api.core.extensions.IElysiumItem;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// asphodel::mixin -> loader=fabric -> env=common, file=elysium_api.fabric.mixins.json
@Mixin(PiglinAi.class)
public abstract class PiglinAIFMixin {

    @WrapOperation(
            method = "isBarterCurrency",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"
            )
    )
    private static boolean elysium$isBarterCurrency(ItemStack instance, Item item, Operation<Boolean> original) {
        return ((IElysiumItem) instance.getItem()).elysiumIsPiglinCurrency(instance);
    }
}