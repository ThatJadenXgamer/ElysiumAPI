package net.jadenxgamer.elysium_api.impl.mixin.brewing;

import net.jadenxgamer.elysium_api.impl.core.datadriven.brewing_recipe.ElysiumBrewingRecipeHelper;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionBrewing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BrewingStandMenu.PotionSlot.class)
public class PotionSlotMixin {

    @Inject(
            method = "mayPlaceItem(Lnet/minecraft/world/item/alchemy/PotionBrewing;Lnet/minecraft/world/item/ItemStack;)Z",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void elysium_api$allowDataMayPlaceItem(PotionBrewing brewing, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (ElysiumBrewingRecipeHelper.isValidInput(stack)) {
            cir.setReturnValue(true);
        }
    }
}