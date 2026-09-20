package net.jadenxgamer.elysium_api.impl.mixin.entity;

import net.jadenxgamer.elysium_api.api.tags.ElysiumTags;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(
            method = "getDamageAfterArmorAbsorb",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hurtArmor(Lnet/minecraft/world/damagesource/DamageSource;F)V"),
            cancellable = true
    )
    private void elysium_api$preventArmorDamage(DamageSource pDamageSource, float pDamageAmount, CallbackInfoReturnable<Float> cir) {
        // makes it so some damage types do not take away armor durability
        if (pDamageSource.is(ElysiumTags.DamageTypes.CANT_DAMAGE_ARMOR)) {
            pDamageAmount = CombatRules.getDamageAfterAbsorb(((LivingEntity) (Object) this), pDamageAmount, pDamageSource, ((LivingEntity) (Object) this).getArmorValue(), (float) ((LivingEntity) (Object) this).getAttributeValue(Attributes.ARMOR_TOUGHNESS));
            cir.setReturnValue(pDamageAmount);
        }
    }
}
