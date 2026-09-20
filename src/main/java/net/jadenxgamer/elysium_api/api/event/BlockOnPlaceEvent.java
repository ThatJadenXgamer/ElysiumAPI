package net.jadenxgamer.elysium_api.api.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.Event;

public class BlockOnPlaceEvent extends Event {
    private final BlockState state;
    private final Level level;
    private final BlockPos pos;
    private final BlockState oldState;
    private final boolean movedByPiston;

    public BlockOnPlaceEvent(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        this.state = state;
        this.level = level;
        this.pos = pos;
        this.oldState = oldState;
        this.movedByPiston = movedByPiston;
    }

    /**
     * {@return state of the block being placed}
     */
    public BlockState getState() {
        return state;
    }

    /**
     * {@return level that the block is being placed in}
     */
    public Level getLevel() {
        return level;
    }

    /**
     * {@return position at which the block was placed at}
     */
    public BlockPos getPos() {
        return pos;
    }

    /**
     * {@return the original state at the current block position prior to the new state placement}
     */
    public BlockState getOldState() {
        return oldState;
    }

    /**
     * {@return weather placement occurred due to a piston}
     */
    public boolean isMovedByPiston() {
        return movedByPiston;
    }
}