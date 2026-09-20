package net.jadenxgamer.elysium_api.impl.event;

import com.mojang.brigadier.CommandDispatcher;
import net.jadenxgamer.elysium_api.ElysiumAPI;
import net.jadenxgamer.elysium_api.api.client.screen_flash.ScreenFlash;
import net.jadenxgamer.elysium_api.impl.client.assetdriven.fog_settings.FogSettingsManager;
import net.jadenxgamer.elysium_api.impl.client.assetdriven.lightmap_settings.LightmapSettingsManager;
import net.jadenxgamer.elysium_api.impl.registry.ElysiumBlocks;
import net.jadenxgamer.elysium_api.impl.registry.ElysiumItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@SuppressWarnings("unused")
@Mod.EventBusSubscriber(modid = ElysiumAPI.MOD_ID, value = Dist.CLIENT)
public class ElysiumClientEvents {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            // pre
        } else {
            // post
        }
    }

    @SubscribeEvent
    public static void onClientCommandsRegister(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void fogRender(ViewportEvent.RenderFog event) {
        if (event.getCamera().getFluidInCamera() == FogType.NONE && event.getMode() == FogRenderer.FogMode.FOG_TERRAIN && (event.getCamera().getEntity().getEyeInFluidType() == ForgeMod.EMPTY_TYPE.get())) {
            var settings = ElysiumAPI.FOG_SETTINGS.getSettings(Minecraft.getInstance().player, event.getNearPlaneDistance(), event.getFarPlaneDistance());
            if (settings != null) {
                event.setCanceled(true);
                event.setNearPlaneDistance(settings.getLeft());
                event.setFarPlaneDistance(settings.getRight());
            }
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Pre event) {
        ScreenFlash.handle(event.getGuiGraphics(), event.getPartialTick());
    }

    @Mod.EventBusSubscriber(modid = ElysiumAPI.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModBus {

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

        @SubscribeEvent
        public static void registerReloadListener(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener(new FogSettingsManager());
            event.registerReloadListener(new LightmapSettingsManager());
        }

        @SubscribeEvent
        public static void clientSetup(FMLClientSetupEvent event) {

        }
    }
}