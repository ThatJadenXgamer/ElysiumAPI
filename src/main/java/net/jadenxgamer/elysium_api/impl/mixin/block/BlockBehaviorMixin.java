package net.jadenxgamer.elysium_api.impl.mixin.block;

import net.jadenxgamer.elysium_api.api.event.BlockOnPlaceEvent;
import net.jadenxgamer.elysium_api.api.util.RegistryAccessHelper;
import net.jadenxgamer.elysium_api.impl.registry.ElysiumRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.class)
public abstract class BlockBehaviorMixin {

    @Inject(
            method = "getSoundType",
            at = @At("HEAD"),
            cancellable = true
    )
    private void elysium_api$soundTransformer(BlockState state, CallbackInfoReturnable<SoundType> cir) {
        RegistryAccessHelper.getServer()
                .flatMap(access -> access.registryOrThrow(ElysiumRegistries.Keys.BLOCK_SOUND_TRANSFORMERS).stream()
                        .filter(s -> s.blocks().contains(state.getBlockHolder()))
                        .findFirst())
                .ifPresent(transformer -> cir.setReturnValue(transformer.toSoundType()));
    }

    @Inject(
            method = "onPlace",
            at = @At("HEAD")
    )
    private void elysium_api$onPlaceHook(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston, CallbackInfo ci) {
        BlockOnPlaceEvent event = new BlockOnPlaceEvent(state, level, pos, oldState, movedByPiston);
        NeoForge.EVENT_BUS.post(event);
    }
}