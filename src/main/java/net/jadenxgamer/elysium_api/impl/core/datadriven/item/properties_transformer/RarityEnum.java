package net.jadenxgamer.elysium_api.impl.core.datadriven.item.properties_transformer;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Rarity;
import org.jetbrains.annotations.NotNull;

public enum RarityEnum implements StringRepresentable {
    COMMON("common"),
    UNCOMMON("uncommon"),
    RARE("rare"),
    EPIC("epic");

    private final String name;
    public static final StringRepresentableCodec<RarityEnum> CODEC = StringRepresentable.fromEnum(RarityEnum::values);

    RarityEnum(String name) {
        this.name = name;
    }

    @Override
    public @NotNull String getSerializedName() {
        return name;
    }

    public Rarity toVanillaRarity() {
        return switch (this) {
            case COMMON -> Rarity.COMMON;
            case UNCOMMON -> Rarity.UNCOMMON;
            case RARE -> Rarity.RARE;
            case EPIC -> Rarity.EPIC;
        };
    }
}