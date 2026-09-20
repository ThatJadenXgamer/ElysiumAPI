package net.jadenxgamer.elysium_api.impl.core.datadriven.block.use_behaviors;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

public enum PosEnum implements StringRepresentable {
    NOOP("noop"),
    ABOVE("above"),
    BELOW("below"),
    NORTH("north"),
    EAST("east"),
    SOUTH("south"),
    WEST("west"),
    RANDOM_HORIZONTAL("random_horizontal"),
    RANDOM_VERTICAL("random_vertical");

    private final String name;
    public static final StringRepresentableCodec<PosEnum> CODEC = StringRepresentable.fromEnum(PosEnum::values);

    PosEnum(String name) {
        this.name = name;
    }

    public String toString() {
        return this.name;
    }

    @Override
    public @NotNull String getSerializedName() {
        return name;
    }
}
