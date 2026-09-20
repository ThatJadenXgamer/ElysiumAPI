package net.jadenxgamer.elysium_api.impl.core.misc.forge;

import net.jadenxgamer.elysium_api.ElysiumAPI;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.DecoratedPotPattern;
import net.neoforged.neoforge.registries.datamaps.DataMapType;

public class ElysiumDataMaps {

    public static final DataMapType<Item, ResourceKey<DecoratedPotPattern>> DECORATED_POT_PATTERNS = DataMapType.builder(
            ResourceLocation.fromNamespaceAndPath(ElysiumAPI.MOD_ID, "decorated_pot_patterns"),
            Registries.ITEM, ResourceKey.codec(Registries.DECORATED_POT_PATTERN)
    ).build();

}