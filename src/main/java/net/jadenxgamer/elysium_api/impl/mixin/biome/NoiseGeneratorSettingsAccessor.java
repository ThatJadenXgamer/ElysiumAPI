package net.jadenxgamer.elysium_api.impl.mixin.biome;

import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.SurfaceRules;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(NoiseGeneratorSettings.class)
public interface NoiseGeneratorSettingsAccessor {

    @Final
    @Mutable
    @Accessor("surfaceRule")
    void elysium_api$setSurfaceRule(SurfaceRules.RuleSource rule);
}