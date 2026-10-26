package net.jadenxgamer.elysium_api.impl.mixin;

import net.jadenxgamer.elysium_api.api.extensions.IElysiumBlock;
import net.minecraft.world.level.block.SpongeBlock;
import org.spongepowered.asm.mixin.Mixin;

// asphodel::mixin -> loader=* -> env=common, file=elysium_api.mixins.json
@Mixin(SpongeBlock.class)
public class Test implements IElysiumBlock {

}