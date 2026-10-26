// asphodel::merge -> version=1.20.1 -> net.jadenxgamer.elysium_api.merge.mechanics.ExplosionMixin20
// asphodel::merge -> version=1.21.1 -> net.jadenxgamer.elysium_api.merge.mechanics.ExplosionMixin21
package net.jadenxgamer.elysium_api.impl.mixin.mechanics;

import net.jadenxgamer.elysium_api.api.unification.IElysiumExplosion;
import net.minecraft.world.level.Explosion;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Explosion.class)
public abstract class ExplosionMixin implements IElysiumExplosion {
}