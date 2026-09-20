package net.jadenxgamer.elysium_api.impl.core.datadriven.brewing_recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public record ElysiumBrewingRecipe(Item inputItem, DataComponentPatch inputComponents, Item ingredient,
                                   Item outputItem, DataComponentPatch outputComponents, boolean copyInputComponents) {

    public static final Codec<ElysiumBrewingRecipe> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("input_item").forGetter(ElysiumBrewingRecipe::inputItem),
            DataComponentPatch.CODEC.optionalFieldOf("input_components", DataComponentPatch.EMPTY).forGetter(ElysiumBrewingRecipe::inputComponents),
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("ingredient").forGetter(ElysiumBrewingRecipe::ingredient),
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("output_item").forGetter(ElysiumBrewingRecipe::outputItem),
            DataComponentPatch.CODEC.optionalFieldOf("output_components", DataComponentPatch.EMPTY).forGetter(ElysiumBrewingRecipe::outputComponents),
            Codec.BOOL.optionalFieldOf("copy_input_components", false).forGetter(ElysiumBrewingRecipe::copyInputComponents)
    ).apply(instance, ElysiumBrewingRecipe::new));

    public boolean matchesInput(ItemStack stack) {
        if (!stack.is(inputItem)) return false;
        if (inputComponents.isEmpty()) return true;

        ItemStack copy = stack.copy();
        copy.applyComponents(inputComponents);
        return stack.getComponents().equals(copy.getComponents());
    }

    public boolean matchesIngredient(ItemStack stack) {
        return stack.is(ingredient);
    }

    public ItemStack createResult(ItemStack input) {
        ItemStack result = outputItem.getDefaultInstance();
        if (copyInputComponents) result.applyComponents(input.getComponents());
        if (!outputComponents.isEmpty()) result.applyComponents(outputComponents);
        return result;
    }
}