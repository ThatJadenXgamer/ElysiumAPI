package net.jadenxgamer.elysium_api.impl.registry;

import net.jadenxgamer.elysium_api.ElysiumAPI;
import net.jadenxgamer.elysium_api.impl.core.block.GlassStructureVoidBlock;
import net.jadenxgamer.elysium_api.impl.core.block.MobBarrierBlock;
import net.jadenxgamer.elysium_api.impl.core.block.SolidStructureVoidBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.GameMasterBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class ElysiumBlocks {

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ElysiumAPI.MOD_ID);

    public static final Supplier<Block> STRUCTURE_STAMP_ANCHOR = registerGameMasterBlock("structure_stamp_anchor", () ->
                    new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).requiresCorrectToolForDrops().strength(-1.0f, 3600000.0F).noLootTable()),
            new Item.Properties().rarity(Rarity.EPIC));

    public static final Supplier<Block> MOB_BARRIER = registerGameMasterBlock("mob_barrier", () ->
                    new MobBarrierBlock(BlockBehaviour.Properties.of().replaceable().strength(-1.0F, 3600000.8F).mapColor(MapColor.NONE).noLootTable().noOcclusion()),
            new Item.Properties().rarity(Rarity.EPIC));

    public static final Supplier<Block> SOLID_STRUCTURE_VOID = registerGameMasterBlock("solid_structure_void", () ->
                    new SolidStructureVoidBlock(BlockBehaviour.Properties.of().strength(-1.0F, 3600000.8F).mapColor(MapColor.NONE).noLootTable()),
            new Item.Properties().rarity(Rarity.EPIC));

    public static final Supplier<Block> GLASS_STRUCTURE_VOID = registerGameMasterBlock("glass_structure_void", () ->
                    new GlassStructureVoidBlock(BlockBehaviour.Properties.of().noOcclusion().strength(-1.0F, 3600000.8F).mapColor(MapColor.NONE).noLootTable()),
            new Item.Properties().rarity(Rarity.EPIC));

    public static void init(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }

    public static <T extends Block> RegistryObject<T> registerGameMasterBlock(String name, Supplier<T> block, Item.Properties properties) {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        ElysiumItems.ITEMS.register(name, () -> new GameMasterBlockItem(toReturn.get(), properties));
        return toReturn;
    }
}