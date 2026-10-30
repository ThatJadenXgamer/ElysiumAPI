package net.jadenxgamer.elysium_api.impl.mixin.registry;

import net.minecraft.resources.RegistryDataLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(RegistryDataLoader.class)
public interface RegistryDataLoaderAccessorF21 {

    @Accessor("WORLDGEN_REGISTRIES")
    static List<RegistryDataLoader.RegistryData<?>> elysium$getWorldgenRegistries() {
        throw new AssertionError();
    }

    @Mutable
    @Accessor("WORLDGEN_REGISTRIES")
    static void elysium$setWorldgenRegistries(List<RegistryDataLoader.RegistryData<?>> value) {
        throw new AssertionError();
    }

    @Accessor("SYNCHRONIZED_REGISTRIES")
    static List<RegistryDataLoader.RegistryData<?>> elysium$getSynchronizedRegistries() {
        throw new AssertionError();
    }

    @Mutable
    @Accessor("SYNCHRONIZED_REGISTRIES")
    static void elysium$setSynchronizedRegistries(List<RegistryDataLoader.RegistryData<?>> value) {
        throw new AssertionError();
    }
}