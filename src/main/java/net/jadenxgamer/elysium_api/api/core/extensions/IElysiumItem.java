package net.jadenxgamer.elysium_api.api.core.extensions;

import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * A cross-loader and cross-version extension interface for {@code Item} that
 * unifies the previously fragmented item and stack APIs into a single
 * consistent surface.
 * <p>
 * Both Forge and NeoForge ship their own extension interfaces with overlapping
 * but incompatible methods, while Fabric exposes no equivalent hooks at all and
 * vanilla itself inlines many item interactions directly at their callsites.
 * This interface collapses all three cases into one set of {@code elysium*}
 * methods that are consulted regardless of which loader or Minecraft version is
 * running underneath.
 * <p>
 * In practice this means mod developers writing a {@code Item} subclass can override
 * any one of these methods and have that override take effect on Fabric, Forge, and
 * NeoForge, on both 1.20.1 and 1.21.1. Our unified methods are wired up through
 * three complementary pipelines:
 * <ul>
 *     <li>On NeoForge and Forge, the loader's own extension interface methods
 *     delegate here, so mods written against either loader's established
 *     conventions continue to work without any changes. Additionally, some methods on
 *     Forge-side got modernized to receive parity from Neo's implementations and standards.</li>
 *     <li>On Fabric, vanilla callsites that would otherwise never consult these
 *     behaviors are patched to route through the same methods, granting
 *     Fabric parity with the extension-only features that were previously
 *     exclusive to Forge-family loaders.</li>
 * </ul>
 * On every loader, vanilla behavior is preserved by default, so
 * overriding an {@code elysium*} method is purely additive.</li>
 * Furthermore it may be safer to do so on Asphodel in some cases where the vanilla
 * implementation of some methods may differ slightly across versions.
 */
public interface IElysiumItem {

    private Item self() {
        return (Item) this;
    }

    /**
     * Called when a player attempts to drop the item into the world, returning false from this will prevent the item
     * from being removed from the players inventory and spawning in the world
     * <br>
     * NOTE: There was a client-side desync issue with the original Forge/Neo implementation that Elysium fixes,
     * attempting to drop an item with the result of this method being false will not cause it to disappear on the client screen.
     * As a side effect of this fix, this method is executed on the client side and server side as well, as opposed to only the latter.
     * <br>
     * <li>Original Analog: {@code ItemStack.onDroppedByPlayer}
     * <li>Purpose: Elysium expanded Forge and NeoForge extension parity.
     *
     * @param stack the item stack, before the item is removed
     * @param player the player that dropped the item
     * @return true if the item must be dropped into the world
     */
    default boolean elysiumOnDroppedByPlayer(ItemStack stack, Player player) {
        return true;
    }

    /**
     * Defines whether Piglins can barter an item in exchange when admiring it.
     * <br>
     * NOTE: This method does not make Piglins pathfind to this item, nor play the admiring pose
     * when they're holding it unless specified in the {@code minecraft:piglin_loved} item tag.
     * <br>
     * <li>Original Analog: {@code ItemStack.isPiglinCurrency}
     * <li>Purpose: Forge and NeoForge extension parity.
     *
     * @param stack the current item stack
     * @return If this item can be used as a bartering currency for Piglins
     */
    default boolean elysiumIsPiglinCurrency(ItemStack stack) {
        return stack.getItem() == PiglinAi.BARTERING_ITEM;
    }
}