package net.jadenxgamer.elysium_api.impl.mixin.entity;

import net.jadenxgamer.elysium_api.api.interfaces.IElysiumBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin extends Entity {

    public ItemEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Redirect(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;getFriction()F")
    )
    private float elysium$getFriction(Block block) {
        ItemEntity self = (ItemEntity) (Object) this;
        BlockPos pos = this.getBlockPosBelowThatAffectsMyMovement();
        BlockState state = self.level().getBlockState(pos);
        return ((IElysiumBlock) state.getBlock()).elysiumGetFriction(state, self.level(), pos, self);
    }
}