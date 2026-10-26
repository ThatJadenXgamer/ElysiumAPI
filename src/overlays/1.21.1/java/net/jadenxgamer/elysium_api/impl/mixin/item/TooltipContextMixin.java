package net.jadenxgamer.elysium_api.impl.mixin.item;

import net.jadenxgamer.elysium_api.impl.util.ElysiumTooltipContextStorage;
import net.jadenxgamer.elysium_api.impl.util.IElysiumTooltipContext;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// asphodel::mixin -> version=1.21.1 -> env=common, file=elysium_api.21.mixins.json
@Mixin(Item.TooltipContext.class)
public interface TooltipContextMixin extends IElysiumTooltipContext {

    @Inject(
            method = "of(Lnet/minecraft/world/level/Level;)Lnet/minecraft/world/item/Item$TooltipContext;",
            at = @At("RETURN")
    )
    private static void elysium$captureLevel(Level level, CallbackInfoReturnable<Item.TooltipContext> cir) {
        if (level == null) return;
        ElysiumTooltipContextStorage.capture(cir.getReturnValue(), level);
    }

    @Override
    default @Nullable Level elysium$getLevel() {
        return ElysiumTooltipContextStorage.get((Item.TooltipContext) this);
    }
}