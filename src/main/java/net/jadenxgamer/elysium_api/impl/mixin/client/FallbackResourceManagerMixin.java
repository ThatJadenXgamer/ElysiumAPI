package net.jadenxgamer.elysium_api.impl.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.jadenxgamer.elysium_api.impl.client.conditional_resource.ConditionalAssetsHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.FallbackResourceManager;
import net.minecraft.server.packs.resources.IoSupplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.io.InputStream;

@Mixin(FallbackResourceManager.class)
public abstract class FallbackResourceManagerMixin {

    @WrapOperation(
            method = "getResource",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/packs/PackResources;getResource(Lnet/minecraft/server/packs/PackType;Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/server/packs/resources/IoSupplier;")
    )
    private IoSupplier<InputStream> elysium_api$wrapGetResource(PackResources pack, PackType packType, ResourceLocation location, Operation<IoSupplier<InputStream>> original) {
        if (ConditionalAssetsHelper.isAssetHidden(pack, packType, location)) return null;
        return original.call(pack, packType, location);
    }
    
    @WrapOperation(
            method = "getResourceStack",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/packs/PackResources;getResource(Lnet/minecraft/server/packs/PackType;Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/server/packs/resources/IoSupplier;")
    )
    private IoSupplier<InputStream> elysium_api$wrapGetResourceForStack(PackResources pack, PackType packType, ResourceLocation location, Operation<IoSupplier<InputStream>> original) {
        if (ConditionalAssetsHelper.isAssetHidden(pack, packType, location)) return null;
        return original.call(pack, packType, location);
    }
    
    @WrapOperation(
            method = "listResources",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/packs/PackResources;listResources(Lnet/minecraft/server/packs/PackType;Ljava/lang/String;Ljava/lang/String;Lnet/minecraft/server/packs/PackResources$ResourceOutput;)V")
    )
    private void elysium_api$wrapListResources(PackResources pack, PackType packType, String namespace, String path, PackResources.ResourceOutput originalOutput, Operation<Void> original) {
        PackResources.ResourceOutput filteredOutput = (location, supplier) -> {
            if (!ConditionalAssetsHelper.isAssetHidden(pack, packType, location)) originalOutput.accept(location, supplier);
        };
        original.call(pack, packType, namespace, path, filteredOutput);
    }

    @WrapOperation(
            method = "listPackResources",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/packs/PackResources;listResources(Lnet/minecraft/server/packs/PackType;Ljava/lang/String;Ljava/lang/String;Lnet/minecraft/server/packs/PackResources$ResourceOutput;)V")
    )
    private void elysium_api$wrapListPackResources(PackResources pack, PackType packType, String namespace, String path, PackResources.ResourceOutput originalOutput, Operation<Void> original) {
        PackResources.ResourceOutput filteredOutput = (location, supplier) -> {
            if (!ConditionalAssetsHelper.isAssetHidden(pack, packType, location)) originalOutput.accept(location, supplier);
        };
        original.call(pack, packType, namespace, path, filteredOutput);
    }
}