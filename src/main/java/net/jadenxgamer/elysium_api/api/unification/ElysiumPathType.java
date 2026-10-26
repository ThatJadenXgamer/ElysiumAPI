package net.jadenxgamer.elysium_api.api.unification;

/**
 * Version-agnostic mirror of the vanilla pathfinding weight classifications
 * used to evaluate how mobs treat blocks during navigation.
 * <p>
 * The enum values mirror {@code BlockPathTypes} (1.20.1) and {@code PathType}
 * (1.21.1) exactly in name and order. This lets the interface methods
 * {@code elysiumGetBlockPathType} and {@code elysiumGetAdjacentBlockPathType}
 * expose a single signature regardless of which Minecraft version is being
 * built against, with conversions between this type and the underlying
 * vanilla type handled by {@link ElysiumPathTypes}.
 */
public enum ElysiumPathType {
    BLOCKED,
    OPEN,
    WALKABLE,
    TRAPDOOR,
    FENCE,
    LAVA,
    WATER,
    WATER_BORDER,
    RAIL,
    UNPASSABLE_RAIL,
    DANGER_FIRE,
    DAMAGE_FIRE,
    DANGER_OTHER,
    DAMAGE_OTHER,
    DANGER_POWDER_SNOW,
    POWDER_SNOW,
    DOOR_OPEN,
    DOOR_WOOD_CLOSED,
    DOOR_IRON_CLOSED,
    BREACH,
    LEAVES,
    STICKY_HONEY,
    COCOA,
    DAMAGE_CAUTIOUS,
    WALKABLE_DOOR
}