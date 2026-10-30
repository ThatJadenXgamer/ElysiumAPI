package net.jadenxgamer.elysium_api.impl.mixin.registry;

import net.jadenxgamer.elysium_api.api.registry.ElysiumDataRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// asphodel::mixin -> version=1.21.1, loader=fabric -> env=common, file=elysium_api.fabric.mixins.json
@Mixin(Registries.class)
public class RegistriesMixinF21 {

    @Inject(method = "elementsDirPath", at = @At("HEAD"), cancellable = true)
    private static void elysium$namespacePrefixCustomRegistries(ResourceKey<? extends Registry<?>> key, CallbackInfoReturnable<String> cir) {
        if (ElysiumDataRegistry.shouldNamespacePrefix(key.location())) cir.setReturnValue(key.location().getNamespace() + "/" + key.location().getPath());
    }
}