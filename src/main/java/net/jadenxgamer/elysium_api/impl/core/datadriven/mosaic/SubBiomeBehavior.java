package net.jadenxgamer.elysium_api.impl.core.datadriven.mosaic;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum SubBiomeBehavior implements StringRepresentable {
    REPLACE("replace"),
    PATCH("patch"),
    EDGE("edge");

    private final String name;

    SubBiomeBehavior(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public static final Codec<SubBiomeBehavior> CODEC = StringRepresentable.fromEnum(SubBiomeBehavior::values);
}