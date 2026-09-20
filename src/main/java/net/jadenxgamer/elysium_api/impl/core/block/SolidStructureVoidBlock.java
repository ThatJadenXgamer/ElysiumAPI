package net.jadenxgamer.elysium_api.impl.core.block;

import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.NotNull;

public class SolidStructureVoidBlock extends Block {

    public static final EnumProperty<Color> COLOR = EnumProperty.create("color", Color.class);

    public SolidStructureVoidBlock(Properties properties) {
        super(properties);
        this.defaultBlockState().setValue(COLOR, Color.WHITE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        int sum = pos.getX() + pos.getY() + pos.getZ();
        Color color = (sum % 2 == 0) ? Color.WHITE : Color.BLACK;
        return this.defaultBlockState().setValue(COLOR, color);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide) {
            int sum = pos.getX() + pos.getY() + pos.getZ();
            Color expectedColor = (sum % 2 == 0) ? Color.WHITE : Color.BLACK;
            Color currentColor = state.getValue(COLOR);
            if (currentColor != expectedColor) {
                BlockState newState = state.setValue(COLOR, expectedColor);
                level.setBlock(pos, newState, Block.UPDATE_ALL);
            }
        }
    }

    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext useContext) {
        var player = useContext.getPlayer();
        return player != null && player.isShiftKeyDown() && !player.getMainHandItem().is(this.asItem());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(COLOR);
    }

    public enum Color implements StringRepresentable {
        WHITE("white"),
        BLACK("black");

        private final String name;

        Color(String name) {
            this.name = name;
        }

        @Override
        public @NotNull String getSerializedName() {
            return this.name;
        }
    }
}
