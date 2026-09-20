package net.jadenxgamer.elysium_api.impl.core.datadriven.brewing_recipe;

import net.jadenxgamer.elysium_api.api.util.RegistryAccessHelper;
import net.jadenxgamer.elysium_api.impl.registry.ElysiumRegistries;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class ElysiumBrewingRecipeHelper {

    private ElysiumBrewingRecipeHelper() {}

    private static List<ElysiumBrewingRecipe> getRecipes() {
        return RegistryAccessHelper.getServer()
                .map(access -> access.registryOrThrow(ElysiumRegistries.Keys.BREWING_RECIPES).stream().toList())
                .orElse(List.of());
    }

    public static boolean isIngredient(ItemStack stack) {
        return getRecipes().stream().anyMatch(recipe -> recipe.matchesIngredient(stack));
    }

    public static boolean hasMix(ItemStack input, ItemStack ingredient) {
        return getRecipes().stream().anyMatch(recipe ->
                recipe.matchesInput(input) && recipe.matchesIngredient(ingredient));
    }

    public static boolean isValidInput(ItemStack stack) {
        return getRecipes().stream().anyMatch(recipe -> recipe.matchesInput(stack));
    }

    public static ItemStack mix(ItemStack input, ItemStack ingredient) {
        for (ElysiumBrewingRecipe recipe : getRecipes()) {
            if (recipe.matchesInput(input) && recipe.matchesIngredient(ingredient)) {
                return recipe.createResult(input);
            }
        }
        return ItemStack.EMPTY;
    }
}