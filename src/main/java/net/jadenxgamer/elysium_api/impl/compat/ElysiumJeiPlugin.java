package net.jadenxgamer.elysium_api.impl.compat;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.vanilla.IJeiBrewingRecipe;
import mezz.jei.api.recipe.vanilla.IVanillaRecipeFactory;
import mezz.jei.api.registration.IRecipeRegistration;
import net.jadenxgamer.elysium_api.ElysiumAPI;
import net.jadenxgamer.elysium_api.impl.core.datadriven.brewing_recipe.ElysiumBrewingRecipe;
import net.jadenxgamer.elysium_api.impl.registry.ElysiumRegistries;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public class ElysiumJeiPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return ElysiumAPI.elysiumPath("brewing_recipes");
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        IVanillaRecipeFactory factory = registration.getVanillaRecipeFactory();
        List<IJeiBrewingRecipe> jeiRecipes = new ArrayList<>();

        Minecraft minecraft = Minecraft.getInstance();
        ClientPacketListener connection = minecraft.getConnection();

        if (connection != null) {
            Registry<ElysiumBrewingRecipe> registry = connection.registryAccess()
                    .registry(ElysiumRegistries.Keys.BREWING_RECIPES)
                    .orElse(null);

            if (registry != null) {
                for (ElysiumBrewingRecipe recipe : registry) {
                    ItemStack input = new ItemStack(recipe.inputItem());
                    if (!recipe.inputComponents().isEmpty()) input.applyComponents(recipe.inputComponents());
                    ItemStack ingredient = new ItemStack(recipe.ingredient());
                    ItemStack output = recipe.createResult(input);

                    IJeiBrewingRecipe jeiRecipe = factory.createBrewingRecipe(List.of(ingredient), input, output);
                    jeiRecipes.add(jeiRecipe);
                }
            }
        }

        if (!jeiRecipes.isEmpty()) registration.addRecipes(RecipeTypes.BREWING, jeiRecipes);
    }
}