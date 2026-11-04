package net.jadenxgamer.elysium_api.impl.client.assetdriven;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

public enum FogLightSettingsType implements StringRepresentable {
    GLOBAL("global"),
    BIOME("biome"),
    DIMENSION("dimension"),
    NOT_BIOME("not_biome"),
    NOT_DIMENSION("not_dimension");

    private final String name;

    FogLightSettingsType(String name) {
        this.name = name;
    }

    @Override
    public @NotNull String getSerializedName() {
        return this.name;
    }

    public static FogLightSettingsType byName(String name, FogLightSettingsType fallback) {
        for (FogLightSettingsType type : values()) {
            if (type.name.equals(name)) return type;
        }
        return fallback;
    }
}