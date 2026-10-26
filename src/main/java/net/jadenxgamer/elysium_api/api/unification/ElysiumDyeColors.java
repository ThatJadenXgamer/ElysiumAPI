// asphodel::merge -> version=<=1.20.1 -> net.jadenxgamer.elysium_api.merge.util.ElysiumDyeColors20
// asphodel::merge -> version=>=1.21.1 -> net.jadenxgamer.elysium_api.merge.util.ElysiumDyeColors21
package net.jadenxgamer.elysium_api.api.unification;

import net.minecraft.world.item.DyeColor;

/**
 * Utility class for reading the texture diffuse color of a {@link DyeColor} in a version-agnostic way.
 * <p>
 * Vanilla 1.20.1 exposes {@code getTextureDiffuseColors()} returning a
 * {@code float[]} of RGB components in {@code [0, 1]}; vanilla 1.21.1
 * replaced that with {@code getTextureDiffuseColor()} returning a packed
 * opaque ARGB {@code int}. Both forms are exposed here so mods can choose
 * whichever representation fits their use case, with the packed ARGB form
 * treated as canonical since it's more user-friendly and modern versions use it.
 */
public final class ElysiumDyeColors {

    private ElysiumDyeColors() {}

    /**
     * Returns the diffuse color as a packed opaque ARGB integer, matching the
     * representation used by 1.21.1's {@code getTextureDiffuseColor()}.
     *
     * @param color the dye color
     * @return the packed ARGB color, with alpha set to {@code 0xFF}
     */
    public static int getTextureDiffuseColor(DyeColor color) {
        throw new UnsupportedOperationException("ElysiumDyeColors.getTextureDiffuseColor is not implemented on this platform");
    }

    /**
     * Returns the diffuse color as an RGB triple of floats in {@code [0, 1]},
     * matching the representation used by 1.20.1's
     * {@code getTextureDiffuseColors()}.
     *
     * @param color the dye color
     * @return a three-element float array containing red, green, and blue
     */
    public static float[] getTextureDiffuseColors(DyeColor color) {
        throw new UnsupportedOperationException("ElysiumDyeColors.getTextureDiffuseColors is not implemented on this platform");
    }
}