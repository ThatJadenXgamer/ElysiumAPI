package net.jadenxgamer.elysium_api.impl.mixin.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.jadenxgamer.elysium_api.api.extensions.IElysiumBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntity.class)
public abstract class LivingEntityFMixin extends Entity {

    public LivingEntityFMixin(EntityType<?> entityType, Level level) {
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

    @ModifyExpressionValue(
            method = "playBlockFallSound",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;getSoundType()Lnet/minecraft/world/level/block/SoundType;"
            )
    )
    private SoundType elysium$getSoundTypeStep(SoundType original, @Local(name = "blockState") BlockState state,
                                               @Local(name = "i") int x, @Local(name = "j") int y, @Local(name = "k") int z) {
        BlockPos pos = new BlockPos(x, y, z);
        return ((IElysiumBlock) state.getBlock()).elysiumGetSoundType(state, this.level(), pos, this);
    }

    @Redirect(
            method = "checkFallDamage",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;sendParticles(Lnet/minecraft/core/particles/ParticleOptions;DDDIDDDD)I"
            ),
            require = 0
    )
    private int elysium$addLandingEffects(ServerLevel level, ParticleOptions particle, double x, double y, double z,
                                          int count, double xDist, double yDist, double zDist, double speed,
                                          @Local(argsOnly = true) BlockState state, @Local(argsOnly = true) BlockPos pos) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (((IElysiumBlock) state.getBlock()).elysiumAddLandingEffects(state, level, pos, self, count)) return 0;
        return level.sendParticles(particle, x, y, z, count, xDist, yDist, zDist, speed);
    }
}