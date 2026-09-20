package net.jadenxgamer.elysium_api.impl.core.datadriven.block.use_behaviors;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

public enum AfterUseItemEnum implements StringRepresentable {
    NOTHING("nothing"),
    CONSUME("consume"),
    DAMAGE("damage");

    private final String name;
    public static final StringRepresentableCodec<AfterUseItemEnum> CODEC = StringRepresentable.fromEnum(AfterUseItemEnum::values);

    AfterUseItemEnum(String name) {
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
