package net.jadenxgamer.elysium_api.impl.mixin.brewing;

import net.jadenxgamer.elysium_api.impl.core.datadriven.brewing_recipe.ElysiumBrewingRecipeHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BrewingStandBlockEntity.class)
public abstract class BrewingStandBlockEntityMixin {

    @Inject(
            method = "canPlaceItem",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void elysium_api$allowDataCanPlaceItem(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (slot < 3 && ElysiumBrewingRecipeHelper.isValidInput(stack)) {
            cir.setReturnValue(true);
        }
    }
}