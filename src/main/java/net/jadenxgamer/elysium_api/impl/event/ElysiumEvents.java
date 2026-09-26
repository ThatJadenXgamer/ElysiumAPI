package net.jadenxgamer.elysium_api.impl.event;

import net.jadenxgamer.elysium_api.ElysiumAPI;
import net.jadenxgamer.elysium_api.impl.networking.ElysiumPayloads;
import net.jadenxgamer.elysium_api.impl.registry.ElysiumRegistries;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = ElysiumAPI.MOD_ID)
public class ElysiumEvents {

    @SubscribeEvent
    public static void registerPayloads(final RegisterPayloadHandlersEvent event) {
        ElysiumPayloads.registerPayloads(event);
    }

    @SubscribeEvent
    public static void datapackRegistry(DataPackRegistryEvent.NewRegistry event) {
        ElysiumRegistries.datapackInit(event);
    }
}