package net.jadenxgamer.elysium_api.impl.event;

import net.jadenxgamer.elysium_api.ElysiumAPI;
import net.jadenxgamer.elysium_api.impl.core.biome.MosaicBiomeSource;
import net.jadenxgamer.elysium_api.impl.core.datadriven.block.properties_transformer.BlockPropertiesTransformerHelper;
import net.jadenxgamer.elysium_api.impl.core.datadriven.block.use_behaviors.UseBehaviorImpl;
import net.jadenxgamer.elysium_api.impl.core.datadriven.item.properties_transformer.ItemPropertiesTransformerHelper;
import net.jadenxgamer.elysium_api.impl.core.surface_rules.ElysiumSurfaceRulesManager;
import net.jadenxgamer.elysium_api.impl.registry.ElysiumRegistries;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.DataPackRegistryEvent;

import java.util.Optional;

@SuppressWarnings("unused")
@Mod.EventBusSubscriber(modid = ElysiumAPI.MOD_ID)
public class ElysiumEvents {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        BlockPropertiesTransformerHelper.invalidateCache();
        ItemPropertiesTransformerHelper.invalidateCache();
        RegistryAccess registryAccess = event.getServer().registryAccess();

        Registry<LevelStem> levelStems = registryAccess.registryOrThrow(Registries.LEVEL_STEM);
        for (LevelStem dimension : levelStems.stream().toList()) {
            Optional<ResourceKey<LevelStem>> dimensionKey = levelStems.getResourceKey(dimension);

            if (dimensionKey.isPresent() && dimension.generator().getBiomeSource() instanceof MosaicBiomeSource biomeSource) {
                var seed = event.getServer().getWorldData().worldGenOptions().seed();
                biomeSource.initialize(seed, dimensionKey.get());
            }

            ChunkGenerator generator = dimension.generator();
            if (dimensionKey.isPresent() && generator instanceof NoiseBasedChunkGenerator noiseGenerator)
                ElysiumSurfaceRulesManager.handleSurfaceRules(dimensionKey.get(), noiseGenerator);
        }
    }

    @SubscribeEvent
    public static void tickPlayer(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            // pre
        } else {
            // post
        }
    }

    @SubscribeEvent
    public static void rightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        UseBehaviorImpl.init(event);
    }

    @Mod.EventBusSubscriber(modid = ElysiumAPI.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModBus {

        @SubscribeEvent
        public static void modifyDefaultAttributes(EntityAttributeModificationEvent event) {

        }

        @SubscribeEvent
        public static void commonSetup(final FMLCommonSetupEvent event) {

        }

        @SubscribeEvent
        public static void datapackRegistry(DataPackRegistryEvent.NewRegistry event) {
            ElysiumRegistries.datapackInit(event);
        }
    }
}