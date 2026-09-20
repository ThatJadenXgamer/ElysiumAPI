package net.jadenxgamer.elysium_api.impl.core.datadriven.block.use_behaviors;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

public enum UseBehaviorTypeEnum implements StringRepresentable {
    PLACE("place"),
    PLACE_ITSELF("place_itself"),
    DROP("drop"),
    DROP_ITSELF("drop_itself"),
    FEATURE("feature"),
    INSERT_STACK("insert_stack");

    private final String name;
    public static final StringRepresentableCodec<UseBehaviorTypeEnum> CODEC = StringRepresentable.fromEnum(UseBehaviorTypeEnum::values);

    UseBehaviorTypeEnum(String name) {
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
