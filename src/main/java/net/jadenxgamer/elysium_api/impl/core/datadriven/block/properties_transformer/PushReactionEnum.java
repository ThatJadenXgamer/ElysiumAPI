package net.jadenxgamer.elysium_api.impl.core.datadriven.block.properties_transformer;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.material.PushReaction;
import org.jetbrains.annotations.NotNull;

public enum PushReactionEnum implements StringRepresentable {
    NORMAL("normal"),
    DESTROY("destroy"),
    BLOCK("block"),
    IGNORE("ignore"),
    PUSH_ONLY("push_only");

    private final String name;
    public static final StringRepresentableCodec<PushReactionEnum> CODEC = StringRepresentable.fromEnum(PushReactionEnum::values);

    PushReactionEnum(String name) {
        this.name = name;
    }

    @Override
    public @NotNull String getSerializedName() {
        return name;
    }

    public PushReaction toPushReaction() {
        return switch (this) {
            case NORMAL -> PushReaction.NORMAL;
            case DESTROY -> PushReaction.DESTROY;
            case BLOCK -> PushReaction.BLOCK;
            case IGNORE -> PushReaction.IGNORE;
            case PUSH_ONLY -> PushReaction.PUSH_ONLY;
        };
    }
}