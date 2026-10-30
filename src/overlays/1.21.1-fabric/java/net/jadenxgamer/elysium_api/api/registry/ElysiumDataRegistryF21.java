package net.jadenxgamer.elysium_api.api.registry;

import com.mojang.serialization.Codec;
import net.jadenxgamer.elysium_api.impl.mixin.registry.RegistryDataLoaderAccessorF21;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ElysiumDataRegistryF21<T> {

    protected final ResourceKey<Registry<T>> registryKey; // asphodel::exclude
    protected final String modId; // asphodel::exclude
    protected final Codec<T> codec; // asphodel::exclude
    @Nullable protected final Codec<T> networkCodec; // asphodel::exclude

    // asphodel::exclude
    protected ElysiumDataRegistryF21(ResourceKey<Registry<T>> registryKey, Codec<T> codec, @Nullable Codec<T> networkCodec) {
        this.registryKey = registryKey;
        this.modId = registryKey.location().getNamespace();
        this.codec = codec;
        this.networkCodec = networkCodec;
    }

    protected void registerToLoader() {
        List<RegistryDataLoader.RegistryData<?>> currentWorldGen = RegistryDataLoaderAccessorF21.elysium$getWorldgenRegistries();
        List<RegistryDataLoader.RegistryData<?>> updatedWorldGen = new ArrayList<>(currentWorldGen);
        updatedWorldGen.add(new RegistryDataLoader.RegistryData<>(this.registryKey, this.codec, false));
        RegistryDataLoaderAccessorF21.elysium$setWorldgenRegistries(List.copyOf(updatedWorldGen));

        if (this.networkCodec != null) {
            List<RegistryDataLoader.RegistryData<?>> currentSynced = RegistryDataLoaderAccessorF21.elysium$getSynchronizedRegistries();
            List<RegistryDataLoader.RegistryData<?>> updatedSynced = new ArrayList<>(currentSynced);
            updatedSynced.add(new RegistryDataLoader.RegistryData<>(this.registryKey, this.networkCodec, false));
            RegistryDataLoaderAccessorF21.elysium$setSynchronizedRegistries(List.copyOf(updatedSynced));
        }
    }
}