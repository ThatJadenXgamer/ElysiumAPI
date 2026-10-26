package net.jadenxgamer.elysium_api.merge.util;

import net.minecraft.world.item.DyeColor;

public final class ElysiumDyeColors21 {

    private ElysiumDyeColors21() {}

    public static int getTextureDiffuseColor(DyeColor color) {
        return color.getTextureDiffuseColor();
    }

    public static float[] getTextureDiffuseColors(DyeColor color) {
        int packed = color.getTextureDiffuseColor();
        return new float[]{
                ((packed >> 16) & 0xFF) / 255.0F,
                ((packed >> 8) & 0xFF) / 255.0F,
                (packed & 0xFF) / 255.0F
        };
    }
}