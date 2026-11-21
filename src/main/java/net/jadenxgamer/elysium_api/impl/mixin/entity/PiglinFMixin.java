package net.jadenxgamer.elysium_api.impl.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.jadenxgamer.elysium_api.api.core.extensions.IElysiumItem;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// asphodel::mixin -> loader=fabric -> env=common, file=elysium_api.fabric.mixins.json
@Mixin(Piglin.class)
public abstract class PiglinFMixin {

    @WrapOperation(
            method = "holdInOffHand(Lnet/minecraft/world/item/ItemStack;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"
            )
    )
    private boolean elysium$holdInOffHand(ItemStack instance, Item item, Operation<Boolean> original) {
        return ((IElysiumItem) instance.getItem()).elysiumIsPiglinCurrency(instance);
    }
}