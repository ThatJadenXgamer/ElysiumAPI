package net.jadenxgamer.elysium_api.impl.mixin.item;

import net.jadenxgamer.elysium_api.api.core.extensions.IElysiumItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.extensions.IForgeItem;
import org.spongepowered.asm.mixin.Mixin;

// asphodel::mixin -> loader=forge -> env=common, file=elysium_api.forge.mixins.json
@Mixin(Item.class)
public abstract class ItemMixinL20 implements IElysiumItem, IForgeItem {

    @Override
    public boolean onDroppedByPlayer(ItemStack item, Player player) {
        return elysiumOnDroppedByPlayer(item, player);
    }

    @Override
    public boolean isPiglinCurrency(ItemStack stack) {
        return elysiumIsPiglinCurrency(stack);
    }
}