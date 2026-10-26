package net.jadenxgamer.elysium_api.merge.mechanics;

import net.jadenxgamer.elysium_api.api.unification.IElysiumExplosion;
import net.minecraft.world.level.Explosion;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Explosion.class)
public abstract class ExplosionMixin21 implements IElysiumExplosion {

    @Override
    public IElysiumExplosion.ElysiumBlockInteraction elysiumGetBlockInteraction() {
        return switch (((Explosion) (Object) this).getBlockInteraction()) {
            case KEEP -> IElysiumExplosion.ElysiumBlockInteraction.KEEP;
            case DESTROY -> IElysiumExplosion.ElysiumBlockInteraction.DESTROY;
            case DESTROY_WITH_DECAY -> IElysiumExplosion.ElysiumBlockInteraction.DESTROY_WITH_DECAY;
            case TRIGGER_BLOCK -> IElysiumExplosion.ElysiumBlockInteraction.TRIGGER_BLOCK;
        };
    }

    @Override
    public float elysiumGetRadius() {
        return ((Explosion) (Object) this).radius();
    }
}