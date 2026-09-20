package net.jadenxgamer.elysium_api.impl.util;

import net.jadenxgamer.elysium_api.impl.registry.ElysiumBlocks;
import net.minecraft.world.level.block.Block;

import java.util.Set;

public class StructureHelper {
    public static final Set<Block> IGNORE_BLOCK_SUPPLIERS = Set.of(
            ElysiumBlocks.SOLID_STRUCTURE_VOID.get(),
            ElysiumBlocks.GLASS_STRUCTURE_VOID.get()
    );
}
