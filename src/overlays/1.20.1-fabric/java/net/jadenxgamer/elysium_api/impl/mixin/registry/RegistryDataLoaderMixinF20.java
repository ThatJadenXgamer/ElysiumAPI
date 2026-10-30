package net.jadenxgamer.elysium_api.impl.mixin.registry;

import net.jadenxgamer.elysium_api.api.registry.ElysiumDataRegistry;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RegistryDataLoader.class)
public class RegistryDataLoaderMixinF20 {

    @Inject(method = "registryDirPath", at = @At("RETURN"), cancellable = true)
    private static void elysium$namespacePrefixCustomRegistries(ResourceLocation location, CallbackInfoReturnable<String> cir) {
        if (ElysiumDataRegistry.shouldNamespacePrefix(location)) cir.setReturnValue(location.getNamespace() + "/" + location.getPath());
    }
}