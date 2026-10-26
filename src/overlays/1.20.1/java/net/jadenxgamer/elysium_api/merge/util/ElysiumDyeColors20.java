package net.jadenxgamer.elysium_api.merge.util;

import net.minecraft.world.item.DyeColor;

public final class ElysiumDyeColors20 {

    private ElysiumDyeColors20() {}

    public static int getTextureDiffuseColor(DyeColor color) {
        float[] rgb = color.getTextureDiffuseColors();
        return 0xFF000000
                | (Math.round(rgb[0] * 255.0F) << 16)
                | (Math.round(rgb[1] * 255.0F) << 8)
                | Math.round(rgb[2] * 255.0F);
    }

    public static float[] getTextureDiffuseColors(DyeColor color) {
        return color.getTextureDiffuseColors();
    }
}