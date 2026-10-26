package net.jadenxgamer.elysium_api.impl.mixin.block;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// asphodel::mixin -> version=1.20.1 -> env=common, file=elysium_api.mixins.json
@Mixin(BlockBehaviour.Properties.class)
public interface BlockPropertiesAccessor21 {
    @Accessor("drops") ResourceKey<LootTable> getDrops();
    @Accessor("drops") void setDrops(ResourceKey<LootTable> value);
    @Accessor("offsetFunction") BlockBehaviour.OffsetFunction getOffsetFunction();
    @Accessor("offsetFunction") void setOffsetFunction(BlockBehaviour.OffsetFunction value);
    @Accessor("spawnTerrainParticles") boolean getSpawnTerrainParticles();
    @Accessor("spawnTerrainParticles") void setSpawnTerrainParticles(boolean value);
}