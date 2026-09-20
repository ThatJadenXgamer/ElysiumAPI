package net.jadenxgamer.elysium_api.impl.mixin.client;

import net.jadenxgamer.elysium_api.impl.client.conditional_resource.ConditionalAssetsHelper;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.util.Unit;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(ReloadableResourceManager.class)
public abstract class ReloadableResourceManagerMixin {

    @Inject(method = "createReload", at = @At("HEAD"))
    private void onReloadStart(Executor backgroundExecutor, Executor gameExecutor, CompletableFuture<Unit> waitingFor, List<PackResources> resourcePacks, CallbackInfoReturnable<ReloadInstance> cir) {
        // afaik there wasn't any event that fired early enough to invalidate caches without it occurring after the cache was populated
        if (FMLEnvironment.dist == Dist.CLIENT) ConditionalAssetsHelper.invalidateCache();
    }
}