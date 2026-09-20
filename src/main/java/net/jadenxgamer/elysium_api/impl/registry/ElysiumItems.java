package net.jadenxgamer.elysium_api.impl.registry;

import net.jadenxgamer.elysium_api.ElysiumAPI;
import net.jadenxgamer.elysium_api.impl.core.item.PanoramaScreenshotItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

public class ElysiumItems {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ElysiumAPI.MOD_ID);

    public static final Supplier<Item> PANORAMA_CAMERA = ITEMS.register("panorama_camera", () ->
            new PanoramaScreenshotItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static void init(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}