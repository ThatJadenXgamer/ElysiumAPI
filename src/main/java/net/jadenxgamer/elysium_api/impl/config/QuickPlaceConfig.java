package net.jadenxgamer.elysium_api.impl.config;

import net.minecraft.util.StringRepresentable;

public enum QuickPlaceConfig implements StringRepresentable {
    DO_NOTHING("disabled"),
    BEDROCK("bedrock"),
    STROKE("stroke"),
    LINE("line");

    private final String name;

    QuickPlaceConfig(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
