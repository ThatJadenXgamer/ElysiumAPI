package net.jadenxgamer.elysium_api.api.registry;

import com.mojang.serialization.Codec;
import net.jadenxgamer.elysium_api.impl.mixin.registry.RegistryDataLoaderAccessorF20;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ElysiumDataRegistryF20<T> {

    private static final ConcurrentHashMap<ResourceKey<? extends Registry<?>>, Codec<?>> REGISTERED_NETWORK_CODECS = new ConcurrentHashMap<>();

    @ApiStatus.Internal
    public static Codec<?> getNetworkCodecFor(ResourceKey<? extends Registry<?>> key) {
        return REGISTERED_NETWORK_CODECS.get(key);
    }

    @ApiStatus.Internal
    public static boolean isNetworkable(ResourceKey<? extends Registry<?>> key) {
        return REGISTERED_NETWORK_CODECS.containsKey(key);
    }

    protected final ResourceKey<Registry<T>> registryKey; // asphodel::exclude
    protected final String modId; // asphodel::exclude
    protected final Codec<T> codec; // asphodel::exclude
    @Nullable protected final Codec<T> networkCodec; // asphodel::exclude

    // asphodel::exclude
    protected ElysiumDataRegistryF20(ResourceKey<Registry<T>> registryKey, Codec<T> codec, @Nullable Codec<T> networkCodec) {
        this.registryKey = registryKey;
        this.modId = registryKey.location().getNamespace();
        this.codec = codec;
        this.networkCodec = networkCodec;
    }

    protected void registerToLoader() {
        List<RegistryDataLoader.RegistryData<?>> currentWorldGen = RegistryDataLoaderAccessorF20.elysium$getWorldgenRegistries();
        List<RegistryDataLoader.RegistryData<?>> updatedWorldGen = new ArrayList<>(currentWorldGen);
        updatedWorldGen.add(new RegistryDataLoader.RegistryData<>(this.registryKey, this.codec));
        RegistryDataLoaderAccessorF20.elysium$setWorldgenRegistries(List.copyOf(updatedWorldGen));

        if (this.networkCodec != null) {
            REGISTERED_NETWORK_CODECS.put(this.registryKey, this.networkCodec);
        }
    }
}