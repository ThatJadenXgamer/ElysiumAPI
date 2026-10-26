package net.jadenxgamer.elysium_api.impl.mixin.entity;

import com.llamalad7.mixinextras.sugar.Local;
import net.jadenxgamer.elysium_api.api.extensions.IElysiumBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EnderDragon.class)
public abstract class EnderDragonFMixin {

    @Redirect(
            method = "checkWalls",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/tags/TagKey;)Z"
            ),
            require = 0
    )
    private boolean elysium$is(BlockState state, TagKey<Block> tag, @Local(name = "blockPos") BlockPos pos) {
        if (tag == BlockTags.DRAGON_IMMUNE) {
            EnderDragon self = (EnderDragon) (Object) this;
            return !((IElysiumBlock) state.getBlock()).elysiumCanEntityDestroy(state, self.level(), pos, self);
        }
        return state.is(tag);
    }
}