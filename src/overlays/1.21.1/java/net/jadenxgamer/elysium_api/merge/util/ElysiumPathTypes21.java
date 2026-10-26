package net.jadenxgamer.elysium_api.merge.util;

import net.jadenxgamer.elysium_api.api.unification.ElysiumPathType;
import net.minecraft.world.level.pathfinder.PathType;
import org.jetbrains.annotations.Nullable;

public final class ElysiumPathTypes21 {

    private ElysiumPathTypes21() {}

    @SuppressWarnings("unchecked")
    public static <T> @Nullable T toVanilla(@Nullable ElysiumPathType type) {
        return type != null ? (T) PathType.valueOf(type.name()) : null;
    }

    @SuppressWarnings("unchecked")
    public static <T> @Nullable ElysiumPathType fromVanilla(@Nullable T type) {
        return type != null ? ElysiumPathType.valueOf(((PathType) (Object) type).name()) : null;
    }
}