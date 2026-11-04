package net.jadenxgamer.elysium_api.impl.core.datadriven.block.use_behaviors;

import net.jadenxgamer.elysium_api.api.unification.ElysiumStringRepresentable;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

public enum AfterUseItemEnum implements ElysiumStringRepresentable {
    NOTHING("nothing"),
    CONSUME("consume"),
    DAMAGE("damage");

    private final String name;
    public static final StringRepresentableCodec<AfterUseItemEnum> CODEC = ElysiumStringRepresentable.fromEnum(AfterUseItemEnum::values);

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