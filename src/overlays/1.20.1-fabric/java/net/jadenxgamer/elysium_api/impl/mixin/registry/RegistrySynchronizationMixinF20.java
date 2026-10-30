package net.jadenxgamer.elysium_api.impl.mixin.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.jadenxgamer.elysium_api.api.registry.ElysiumDataRegistry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistrySynchronization;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.stream.Stream;

// asphodel::mixin -> version=1.20.1, loader=fabric -> env=common, file=elysium_api.fabric.mixins.json
@Mixin(RegistrySynchronization.class)
public class RegistrySynchronizationMixinF20 {

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Inject(method = "getNetworkCodec", at = @At("HEAD"), cancellable = true)
    private static void elysium$getNetworkCodec(ResourceKey registryKey, CallbackInfoReturnable<DataResult> cir) {
        Codec<?> codec = ElysiumDataRegistry.getNetworkCodecFor(registryKey);
        if (codec != null) cir.setReturnValue(DataResult.success(codec));
    }

    @Inject(method = "ownedNetworkableRegistries", at = @At("RETURN"), cancellable = true)
    private static void elysium$ownedNetworkableRegistries(RegistryAccess registryAccess, CallbackInfoReturnable<Stream<RegistryAccess.RegistryEntry<?>>> cir) {
        Stream<RegistryAccess.RegistryEntry<?>> extras = registryAccess.registries()
                .filter(entry -> ElysiumDataRegistry.isNetworkable(entry.key()));
        cir.setReturnValue(Stream.concat(cir.getReturnValue(), extras));
    }
}