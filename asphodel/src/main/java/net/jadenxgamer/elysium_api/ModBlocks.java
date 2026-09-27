package net.jadenxgamer.elysium_api;

import net.jadenxgamer.elysium_api.api.registry.ElysiumRegHolder;
import net.jadenxgamer.elysium_api.api.registry.ElysiumRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public class ModBlocks {

    public static final ElysiumRegistry<Block> BLOCKS = new ElysiumRegistry<>(Registries.BLOCK, "elysium_api");

    public static final ElysiumRegHolder<Block> EXAMPLE_BLOCK = BLOCKS.register("example_block", () ->
            new Block(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(1.5F, 6.0F).sound(SoundType.STONE)));

    public static final ElysiumRegHolder<Block> EXAMPLE_RUBY_SLAB = BLOCKS.register("example_ruby_slab", () ->
            new SlabBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(5.0F, 6.0F).sound(SoundType.METAL)));

    public static void init() {
        BLOCKS.initialize();
    }
}