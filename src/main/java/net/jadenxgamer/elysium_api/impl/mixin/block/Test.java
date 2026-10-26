package net.jadenxgamer.elysium_api.impl.mixin.block;

import net.jadenxgamer.elysium_api.api.extensions.IElysiumBlock;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SpongeBlock;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

// asphodel::mixin -> loader=* -> env=common, file=elysium_api.mixins.json
@Mixin(SpongeBlock.class)
public class Test implements IElysiumBlock {

    @Override
    public void elysiumAppendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        if (level instanceof Level realLevel) {
            tooltip.add(Component.literal("Elysium Level: " + realLevel.dimension().location()));
        } else if (level != null) {
            tooltip.add(Component.literal("Elysium Level: non-Level BlockGetter"));
        } else {
            tooltip.add(Component.literal("Elysium Level: null"));
        }
    }
}