package net.jadenxgamer.elysium_api.impl.core.datadriven.mosaic;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum EntryType implements StringRepresentable {
    DEFAULT("default"),
    SUB_BIOME("sub_biome");

    private final String name;

    EntryType(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public static final Codec<EntryType> CODEC = StringRepresentable.fromEnum(EntryType::values);
}