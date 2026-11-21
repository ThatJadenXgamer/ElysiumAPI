package net.jadenxgamer.elysium_api.impl.mixin;

import net.jadenxgamer.elysium_api.api.core.extensions.IElysiumItem;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SaddleItem;
import org.spongepowered.asm.mixin.Mixin;

// asphodel::mixin -> loader=* -> env=common, file=elysium_api.mixins.json
@Mixin(SaddleItem.class)
public class TestItem implements IElysiumItem {

    @Override
    public boolean elysiumOnDroppedByPlayer(ItemStack stack, Player player) {
        player.level().playSound(null, player.getOnPos(), SoundEvents.ALLAY_AMBIENT_WITH_ITEM, SoundSource.NEUTRAL, 1.0F, 1.0F);
        if (player.level().isClientSide) System.out.println("TestItem.onDroppedByPlayer CLIENT");
        else System.out.println("TestItem.onDroppedByPlayer SERVER");
        return false;
    }

    @Override
    public boolean elysiumIsPiglinCurrency(ItemStack stack) {
        return true;
    }
}