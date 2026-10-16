package net.jadenxgamer.elysium_api.impl.mixin.mechanics;

import net.jadenxgamer.elysium_api.api.interfaces.IElysiumBlock;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PistonStructureResolver.class)
public abstract class PistonStructureResolverMixin {

    @Inject(
            method = "isSticky",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private static void elysium$isSticky(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(((IElysiumBlock) state.getBlock()).elysiumIsStickyBlock(state));
    }

    @Inject(
            method = "canStickToEachOther",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private static void elysium$canStickToEachOther(BlockState state1, BlockState state2, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(
            ((IElysiumBlock) state1.getBlock()).elysiumCanStickTo(state1, state2) && ((IElysiumBlock) state2.getBlock()).elysiumCanStickTo(state2, state1)
        );
    }
}