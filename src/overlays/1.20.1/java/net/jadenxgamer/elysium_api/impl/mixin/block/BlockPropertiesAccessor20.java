package net.jadenxgamer.elysium_api.impl.mixin.block;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Optional;

// asphodel::mixin -> version=1.20.1 -> env=common, file=elysium_api.mixins.json
@Mixin(BlockBehaviour.Properties.class)
public interface BlockPropertiesAccessor20 {
    @Accessor("drops") ResourceLocation getDrops();
    @Accessor("drops") void setDrops(ResourceLocation value);
    @Accessor("offsetFunction") Optional<BlockBehaviour.OffsetFunction> getOffsetFunction();
    @Accessor("offsetFunction") void setOffsetFunction(Optional<BlockBehaviour.OffsetFunction> value);
    @Accessor("spawnParticlesOnBreak") boolean getSpawnParticlesOnBreak();
    @Accessor("spawnParticlesOnBreak") void setSpawnParticlesOnBreak(boolean value);
}