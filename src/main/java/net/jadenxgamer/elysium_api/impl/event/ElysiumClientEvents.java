package net.jadenxgamer.elysium_api.impl.event;

import net.jadenxgamer.elysium_api.ElysiumAPI;
import net.jadenxgamer.elysium_api.impl.registry.ElysiumBlocks;
import net.jadenxgamer.elysium_api.impl.registry.ElysiumItems;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = ElysiumAPI.MOD_ID, value = Dist.CLIENT)
public class ElysiumClientEvents {

    @SubscribeEvent
    public static void addToExistingTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.hasPermissions() && event.getTabKey() == CreativeModeTabs.OP_BLOCKS) {
            event.insertAfter(Items.DEBUG_STICK.getDefaultInstance(), ElysiumItems.PANORAMA_CAMERA.get().getDefaultInstance(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.insertAfter(Items.JIGSAW.getDefaultInstance(), ElysiumBlocks.STRUCTURE_STAMP_ANCHOR.get().asItem().getDefaultInstance(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.insertAfter(Items.BARRIER.getDefaultInstance(), ElysiumBlocks.MOB_BARRIER.get().asItem().getDefaultInstance(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.insertAfter(Items.STRUCTURE_VOID.getDefaultInstance(), ElysiumBlocks.GLASS_STRUCTURE_VOID.get().asItem().getDefaultInstance(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.insertAfter(Items.STRUCTURE_VOID.getDefaultInstance(), ElysiumBlocks.SOLID_STRUCTURE_VOID.get().asItem().getDefaultInstance(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }
    }
}