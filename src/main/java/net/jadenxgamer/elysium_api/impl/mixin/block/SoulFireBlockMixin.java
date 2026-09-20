package net.jadenxgamer.elysium_api.impl.mixin.block;

import net.jadenxgamer.elysium_api.api.tags.ElysiumTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.SoulFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoulFireBlock.class)
public abstract class SoulFireBlockMixin extends BaseFireBlock {

    public SoulFireBlockMixin(Properties properties, float damage) {
        super(properties, damage);
    }

    @Inject(
            method = "canSurvive",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void elysium_api$preventIllegalSoulFirePlacement(BlockState pState, LevelReader pLevel, BlockPos pPos, CallbackInfoReturnable<Boolean> cir) {
        BlockPos belowPos = pPos.below();
        BlockState belowState = pLevel.getBlockState(belowPos);
        if (!pState.is(ElysiumTags.Blocks.NON_SOLID_FIRE_SUPPORT) && !belowState.isFaceSturdy(pLevel, belowPos, Direction.UP)) {
            cir.setReturnValue(false);
        }
    }
}
