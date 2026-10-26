package net.jadenxgamer.elysium_api.api.util;

import net.jadenxgamer.elysium_api.api.unification.IElysiumExplosion;
import net.minecraft.world.level.Explosion;

/**
 * Facade over {@link IElysiumExplosion} that hides the interface cast
 * from callers, mirroring how vanilla code reads {@code Explosion}'s private
 * fields directly. Used primarily by
 * {@link net.jadenxgamer.elysium_api.api.extensions.IElysiumBlock}'s unified
 * explosion pipeline so its default implementations remain version-agnostic
 * without needing to import or cast the extension interface at every
 * callsite.
 */
public final class ElysiumExplosionHelper {

    private ElysiumExplosionHelper() {}

    /**
     * Returns the block interaction mode of the given explosion.
     *
     * @param explosion the explosion being queried
     * @return the block interaction mode of the explosion
     */
    public static IElysiumExplosion.ElysiumBlockInteraction elysiumGetBlockInteraction(Explosion explosion) {
        return ((IElysiumExplosion) explosion).elysiumGetBlockInteraction();
    }

    /**
     * Returns the radius of the given explosion.
     *
     * @param explosion the explosion being queried
     * @return the radius of the explosion
     */
    public static float elysiumGetRadius(Explosion explosion) {
        return ((IElysiumExplosion) explosion).elysiumGetRadius();
    }
}