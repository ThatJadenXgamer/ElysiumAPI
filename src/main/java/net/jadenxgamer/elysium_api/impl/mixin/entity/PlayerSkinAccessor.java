package net.jadenxgamer.elysium_api.impl.mixin.entity;

import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.SurfaceRules;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerSkin.class)
public interface PlayerSkinAccessor {

    @Final
    @Mutable
    @Accessor("capeTexture")
    void elysium_api$setCapeTexture(ResourceLocation texture);
}