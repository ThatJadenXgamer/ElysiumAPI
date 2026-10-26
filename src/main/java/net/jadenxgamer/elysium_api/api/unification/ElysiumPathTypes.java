// asphodel::merge -> version=<=1.20.1 -> net.jadenxgamer.elysium_api.merge.util.ElysiumPathTypes20
// asphodel::merge -> version=>=1.21.1 -> net.jadenxgamer.elysium_api.merge.util.ElysiumPathTypes21
package net.jadenxgamer.elysium_api.api.unification;

import org.jetbrains.annotations.Nullable;

/**
 * Utility class for converting between {@link ElysiumPathType} and the
 * version-specific vanilla path type enum {@code BlockPathTypes} on 1.20.1 and
 * {@code PathType} on 1.21.1. The actual implementations are pulled in from
 * version-specific overlays via asphodel merging, so callers always see a
 * single {@code ElysiumPathTypes} with the correct underlying return type.
 */
public final class ElysiumPathTypes {

    private ElysiumPathTypes() {}

    public static <T> @Nullable T toVanilla(@Nullable ElysiumPathType type) {
        throw new UnsupportedOperationException("ElysiumPathTypes.toVanilla is not implemented on this platform");
    }

    public static <T> @Nullable ElysiumPathType fromVanilla(@Nullable T type) {
        throw new UnsupportedOperationException("ElysiumPathTypes.fromVanilla is not implemented on this platform");
    }
}