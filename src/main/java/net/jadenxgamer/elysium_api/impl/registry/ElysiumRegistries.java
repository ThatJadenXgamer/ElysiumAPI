package net.jadenxgamer.elysium_api.impl.registry;

import net.jadenxgamer.elysium_api.ElysiumAPI;
import net.jadenxgamer.elysium_api.api.registry.ElysiumDataRegistry;
import net.jadenxgamer.elysium_api.impl.core.datadriven.block.BlockSoundTransformer;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

public class ElysiumRegistries {

    public static void init() {
        datapackInit();
    }

    private static void datapackInit() {
        ElysiumDataRegistry.create(Keys.BLOCK_SOUND_TRANSFORMERS, BlockSoundTransformer.CODEC);
    }

    private static <T> ResourceKey<Registry<T>> key(String name) {
        return ResourceKey.createRegistryKey(ElysiumAPI.elysiumPath(name));
    }

    public static final class Keys {
        // Elysium Registries (currently nothing)

        // Data-Driven Registries
        public static final ResourceKey<Registry<BlockSoundTransformer>> BLOCK_SOUND_TRANSFORMERS = key("block/sound_transformers");
    }
}