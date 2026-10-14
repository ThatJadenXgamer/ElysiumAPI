package net.jadenxgamer.elysium_api.impl.mixin.entity;

import net.jadenxgamer.elysium_api.api.interfaces.IElysiumBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    public LivingEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Redirect(
            method = "travel",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;getFriction()F")
    )
    private float elysium$getFriction(Block block) {
        LivingEntity self = (LivingEntity) (Object) this;
        BlockPos pos = this.getBlockPosBelowThatAffectsMyMovement();
        BlockState state = self.level().getBlockState(pos);
        return ((IElysiumBlock) state.getBlock()).elysiumGetFriction(state, self.level(), pos, self);
    }

    @Redirect(
            method = "handleOnClimbable",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z")
    )
    private boolean elysium$isScaffolding(BlockState state, Block block) {
        if (block != Blocks.SCAFFOLDING) return state.is(block);
        LivingEntity self = (LivingEntity) (Object) this;
        return ((IElysiumBlock) state.getBlock()).elysiumIsScaffolding(state, self.level(), self.blockPosition(), self);
    }
}