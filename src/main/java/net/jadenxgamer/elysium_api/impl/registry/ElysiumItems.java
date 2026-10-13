package net.jadenxgamer.elysium_api.impl.registry;

import net.jadenxgamer.elysium_api.ElysiumAPI;
import net.jadenxgamer.elysium_api.api.registry.ElysiumRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;

public class ElysiumItems {

    public static final ElysiumRegistry<Item> ITEMS = new ElysiumRegistry<>(Registries.ITEM, ElysiumAPI.MOD_ID);

    public static void init() {
        ITEMS.initialize();
    }
}