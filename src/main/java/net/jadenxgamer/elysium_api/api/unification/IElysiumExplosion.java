package net.jadenxgamer.elysium_api.api.unification;

/**
 * A cross-loader and cross-version extension interface for {@link net.minecraft.world.level.Explosion}
 * that exposes version-agnostic access to the block interaction mode and
 * radius. Both values exist on every supported Minecraft version but their
 * access paths differ: 1.21.1 exposes {@code getBlockInteraction()} and
 * {@code radius()} publicly, while 1.20.1 keeps the corresponding fields
 * private and has no {@code TRIGGER_BLOCK} interaction mode at all.
 * <p>
 * This interface flattens those differences so downstream code, particularly
 * {@link net.jadenxgamer.elysium_api.api.extensions.IElysiumBlock}'s unified
 * explosion pipeline, can read either value without version-specific
 * branching. The block interaction is surfaced through the nested
 * {@link ElysiumBlockInteraction} enum so callers never reference
 * {@code Explosion.BlockInteraction} directly and remain insulated from
 * version-specific enum membership.
 * <br>
 * Implemented by per-version mixins into {@code Explosion}.
 */
public interface IElysiumExplosion {

    /**
     * Returns the block interaction mode of this explosion.
     *
     * @return the block interaction mode of this explosion
     */
    ElysiumBlockInteraction elysiumGetBlockInteraction();

    /**
     * Returns the radius of this explosion.
     *
     * @return the radius of this explosion
     */
    float elysiumGetRadius();

    /**
     * Version-agnostic mirror of {@code Explosion.BlockInteraction}. The four
     * values match 1.21.1's set; on 1.20.1, whose enum only defines the first
     * three, {@link #TRIGGER_BLOCK} is unreachable but still present so
     * downstream code can switch on it without version-specific handling.
     * <br>
     * <li> KEEP - The explosion does not interact with blocks at all.
     * <li> DESTROY - Blocks in the explosion's radius are destroyed.
     * <li> DESTROY_WITH_DECAY - Blocks are destroyed and additionally lose their drops to decay.
     * <li> TRIGGER_BLOCK - Blocks in the explosion's radius trigger their interaction behavior rather than being destroyed.
     *
     */
    enum ElysiumBlockInteraction {
        KEEP,
        DESTROY,
        DESTROY_WITH_DECAY,
        TRIGGER_BLOCK // todo: backport trigger block on 1.20.1, right now it does nothing.
    }
}