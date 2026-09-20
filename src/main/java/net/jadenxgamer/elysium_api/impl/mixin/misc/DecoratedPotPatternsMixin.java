package net.jadenxgamer.elysium_api.impl.mixin.misc;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.jadenxgamer.elysium_api.impl.core.misc.forge.ElysiumDataMaps;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.DecoratedPotPattern;
import net.minecraft.world.level.block.entity.DecoratedPotPatterns;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Optional;

@Mixin(DecoratedPotPatterns.class)
public class DecoratedPotPatternsMixin {

    @WrapMethod(
            method = "getPatternFromItem"
    )
    private static ResourceKey<DecoratedPotPattern> elysium_api$getPatternFromItem(Item item, Operation<ResourceKey<DecoratedPotPattern>> original) {
        Holder<Item> holder = BuiltInRegistries.ITEM.wrapAsHolder(item);
        return Optional.ofNullable(holder.getData(ElysiumDataMaps.DECORATED_POT_PATTERNS)).orElse(original.call(item));
    }
}