package net.jadenxgamer.elysium_api.merge.mechanics;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.jadenxgamer.elysium_api.api.extensions.IElysiumBlock;
import net.jadenxgamer.elysium_api.api.unification.IElysiumExplosion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Explosion.class)
public abstract class ExplosionMixin20 implements IElysiumExplosion {

    @Shadow @Final private Explosion.BlockInteraction blockInteraction;

    @Shadow @Final private float radius;

    @Shadow
    private static void addBlockDrops(ObjectArrayList<Pair<ItemStack, BlockPos>> dropPositionArray, ItemStack stack, BlockPos pos) {
        throw new AssertionError();
    }

    @Redirect(
            method = "finalizeExplosion",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;",
                    ordinal = 0
            )
    )
    private BlockState elysium$redirectGetBlockState(Level level, BlockPos pos, @Local(name = "objectArrayList") ObjectArrayList<Pair<ItemStack, BlockPos>> objectArrayList) {
        BlockState state = level.getBlockState(pos);
        if (!state.isAir()) {
            ((IElysiumBlock) state.getBlock()).elysiumOnExplosionHit(
                    state, level, pos, (Explosion) (Object) this,
                    (stack, dropPos) -> addBlockDrops(objectArrayList, stack, dropPos)
            );
        }
        return Blocks.AIR.defaultBlockState();
    }

    @Override
    public IElysiumExplosion.ElysiumBlockInteraction elysiumGetBlockInteraction() {
        return switch (this.blockInteraction) {
            case KEEP -> IElysiumExplosion.ElysiumBlockInteraction.KEEP;
            case DESTROY -> IElysiumExplosion.ElysiumBlockInteraction.DESTROY;
            case DESTROY_WITH_DECAY -> IElysiumExplosion.ElysiumBlockInteraction.DESTROY_WITH_DECAY;
        };
    }

    @Override
    public float elysiumGetRadius() {
        return this.radius;
    }
}