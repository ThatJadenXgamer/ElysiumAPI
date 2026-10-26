package net.jadenxgamer.elysium_api.merge.util;

import net.jadenxgamer.elysium_api.api.unification.ElysiumPathType;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import org.jetbrains.annotations.Nullable;

public final class ElysiumPathTypes20 {

    private ElysiumPathTypes20() {}

    @SuppressWarnings("unchecked")
    public static <T> @Nullable T toVanilla(@Nullable ElysiumPathType type) {
        return type != null ? (T) BlockPathTypes.valueOf(type.name()) : null;
    }

    @SuppressWarnings("unchecked")
    public static <T> @Nullable ElysiumPathType fromVanilla(@Nullable T type) {
        return type != null ? ElysiumPathType.valueOf(((BlockPathTypes) (Object) type).name()) : null;
    }
}