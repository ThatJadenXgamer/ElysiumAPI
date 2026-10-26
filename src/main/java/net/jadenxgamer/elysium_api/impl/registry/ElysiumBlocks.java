package net.jadenxgamer.elysium_api.impl.registry;

import net.jadenxgamer.elysium_api.ElysiumAPI;
import net.jadenxgamer.elysium_api.api.block.ElysiumBlockProperties;
import net.jadenxgamer.elysium_api.api.registry.ElysiumRegistry;
import net.jadenxgamer.elysium_api.impl.core.block.GlassStructureVoidBlock;
import net.jadenxgamer.elysium_api.impl.core.block.MobBarrierBlock;
import net.jadenxgamer.elysium_api.impl.core.block.SolidStructureVoidBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.GameMasterBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.MapColor;

import java.util.function.Supplier;

public class ElysiumBlocks {

    public static final ElysiumRegistry<Block> BLOCKS = new ElysiumRegistry<>(Registries.BLOCK, ElysiumAPI.MOD_ID);

    public static final Supplier<Block> STRUCTURE_STAMP_ANCHOR = registerGameMasterBlock("structure_stamp_anchor", () ->
            new Block(ElysiumBlockProperties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).requiresCorrectToolForDrops().strength(-1.0f, 3600000.0F).noLootTable()),
            new Item.Properties().rarity(Rarity.EPIC));

    public static final Supplier<Block> MOB_BARRIER = registerGameMasterBlock("mob_barrier", () ->
            new MobBarrierBlock(ElysiumBlockProperties.of().replaceable().strength(-1.0F, 3600000.8F).mapColor(MapColor.NONE).noLootTable().noOcclusion()),
            new Item.Properties().rarity(Rarity.EPIC));

    public static final Supplier<Block> SOLID_STRUCTURE_VOID = registerGameMasterBlock("solid_structure_void", () ->
            new SolidStructureVoidBlock(ElysiumBlockProperties.of().strength(-1.0F, 3600000.8F).mapColor(MapColor.NONE).noLootTable()),
            new Item.Properties().rarity(Rarity.EPIC));

    public static final Supplier<Block> GLASS_STRUCTURE_VOID = registerGameMasterBlock("glass_structure_void", () ->
            new GlassStructureVoidBlock(ElysiumBlockProperties.of().noOcclusion().strength(-1.0F, 3600000.8F).mapColor(MapColor.NONE).noLootTable()),
            new Item.Properties().rarity(Rarity.EPIC));

    public static void init() {
        BLOCKS.initialize();
    }

    public static <T extends Block> Supplier<T> registerGameMasterBlock(String name, Supplier<T> block, Item.Properties properties) {
        Supplier<T> toReturn = BLOCKS.register(name, block);
        ElysiumItems.ITEMS.register(name, () -> new GameMasterBlockItem(toReturn.get(), properties));
        return toReturn;
    }
}