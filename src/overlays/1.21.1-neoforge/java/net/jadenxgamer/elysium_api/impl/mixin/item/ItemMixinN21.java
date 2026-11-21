package net.jadenxgamer.elysium_api.impl.mixin.item;

import net.jadenxgamer.elysium_api.api.core.extensions.IElysiumItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.extensions.IItemExtension;
import org.spongepowered.asm.mixin.Mixin;

// asphodel::mixin -> loader=neoforge -> env=common, file=elysium_api.neoforge.mixins.json
@Mixin(Item.class)
public abstract class ItemMixinN21 implements IElysiumItem, IItemExtension {

    @Override
    public boolean onDroppedByPlayer(ItemStack item, Player player) {
        return elysiumOnDroppedByPlayer(item, player);
    }

    @Override
    public boolean isPiglinCurrency(ItemStack stack) {
        return elysiumIsPiglinCurrency(stack);
    }
}