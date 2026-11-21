package net.jadenxgamer.elysium_api.impl.mixin.item;

import net.jadenxgamer.elysium_api.api.core.extensions.IElysiumItem;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;

// asphodel::mixin -> loader=* -> env=common, file=elysium_api.mixins.json
@Mixin(Item.class)
public abstract class ItemMixin implements IElysiumItem {}