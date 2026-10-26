// asphodel::merge -> version=1.20.1 -> net.jadenxgamer.elysium_api.merge.block.ElysiumBlockProperties20
// asphodel::merge -> version=1.21.1 -> net.jadenxgamer.elysium_api.merge.block.ElysiumBlockProperties21
package net.jadenxgamer.elysium_api.api.block;

import net.jadenxgamer.elysium_api.api.registry.ElysiumRegHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * A cross-version extension of {@link BlockBehaviour.Properties} that
 * reconciles the drift between 1.20.1 and 1.21.1's property builders and
 * adds a loader-agnostic lazy loot source backed by an {@link ElysiumRegHolder}.
 */
public class ElysiumBlockProperties extends BlockBehaviour.Properties {

    private ElysiumRegHolder<Block> elysiumLazyLootHolder;

    public ElysiumBlockProperties() {
        super();
    }

    /**
     * Creates a new empty properties' builder.
     *
     * @return a fresh ElysiumBlockProperties instance
     */
    public static ElysiumBlockProperties of() {
        return new ElysiumBlockProperties();
    }

    /**
     * Creates a properties builder copying the legacy field set from the
     * given block. Matches 1.20.1's {@code Properties.copy} exactly on both
     * versions, and matches 1.21.1's deprecated
     * {@code Properties.ofLegacyCopy} on 1.21.1.
     *
     * @param source the block whose properties are copied
     * @return a properties builder carrying the legacy-copy field set
     */
    public static ElysiumBlockProperties ofLegacyCopy(BlockBehaviour source) {
        throw new UnsupportedOperationException("ElysiumBlockProperties.ofLegacyCopy is not implemented on this platform");
    }

    /**
     * Creates a properties builder performing a full copy from the given
     * block, backporting 1.21.1's {@code Properties.ofFullCopy} semantics to
     * every version. In addition to the legacy set, this copies
     * {@code jumpFactor}, {@code isValidSpawn}, {@code isRedstoneConductor},
     * {@code isSuffocating}, {@code isViewBlocking}, {@code hasPostProcess},
     * and {@code drops}.
     *
     * @param source the block whose properties are copied
     * @return a properties builder carrying the full-copy field set
     */
    public static ElysiumBlockProperties ofFullCopy(BlockBehaviour source) {
        throw new UnsupportedOperationException("ElysiumBlockProperties.ofFullCopy is not implemented on this platform");
    }

    /**
     * Disables the terrain particle spawn when this block is broken.
     * Version-agnostic counterpart to {@code noParticlesOnBreak()} on 1.20.1
     * and {@code noTerrainParticles()} for 1.21.1.
     *
     * @return this builder
     */
    public ElysiumBlockProperties noTerrainParticles() {
        throw new UnsupportedOperationException("ElysiumBlockProperties.noTerrainParticles is not implemented on this platform");
    }

    /**
     * Whether terrain particles should spawn when this block is broken.
     * Version-agnostic counterpart to the underlying
     * {@code spawnParticlesOnBreak} or {@code spawnTerrainParticles} field
     * accessor.
     *
     * @return true if terrain particles should spawn on break
     */
    public boolean shouldSpawnTerrainParticles() {
        throw new UnsupportedOperationException("ElysiumBlockProperties.shouldSpawnTerrainParticles is not implemented on this platform");
    }

    /**
     * Sets a lazy loot source backed by the given registry holder. The
     * holder's block is resolved on first access to this block's loot table,
     * rather than eagerly during property construction. This is safe for
     * deferred-registration environments such as Forge, NeoForge and
     * Elysium's own {@code ElysiumRegistry}.
     * <br>
     * Calling this method clears any previously set loot source, matching the
     * "last call wins" semantics of {@code dropsLike}.
     *
     * @param holder the registry holder pointing at the block whose loot table should be inherited
     * @return this builder
     */
    public ElysiumBlockProperties lazyDropsLike(ElysiumRegHolder<Block> holder) {
        throw new UnsupportedOperationException("ElysiumBlockProperties.lazyDropsLike is not implemented on this platform");
    }

    public ElysiumRegHolder<Block> getLazyLootHolder() {
        return this.elysiumLazyLootHolder;
    }

    protected void elysium$setLazyLootHolder(ElysiumRegHolder<Block> holder) {
        this.elysiumLazyLootHolder = holder;
    }
}