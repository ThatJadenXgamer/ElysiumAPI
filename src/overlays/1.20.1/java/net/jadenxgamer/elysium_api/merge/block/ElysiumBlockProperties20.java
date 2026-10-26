package net.jadenxgamer.elysium_api.merge.block;

import net.jadenxgamer.elysium_api.api.block.ElysiumBlockProperties;
import net.jadenxgamer.elysium_api.api.registry.ElysiumRegHolder;
import net.jadenxgamer.elysium_api.impl.mixin.block.BlockBehaviourAccessor;
import net.jadenxgamer.elysium_api.impl.mixin.block.BlockPropertiesAccessor;
import net.jadenxgamer.elysium_api.impl.mixin.block.BlockPropertiesAccessor20;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class ElysiumBlockProperties20 extends BlockBehaviour.Properties {

    // asphodel::exclude
    private ElysiumRegHolder<Block> elysiumLazyLootHolder;

    // asphodel::exclude
    public ElysiumBlockProperties20() {
        super();
    }

    public static ElysiumBlockProperties ofLegacyCopy(BlockBehaviour source) {
        ElysiumBlockProperties result = ElysiumBlockProperties.of();
        BlockBehaviour.Properties src = ((BlockBehaviourAccessor) source).elysium$getProperties();
        BlockPropertiesAccessor srcAcc = (BlockPropertiesAccessor) src;
        BlockPropertiesAccessor20 srcAcc20 = (BlockPropertiesAccessor20) src;
        BlockPropertiesAccessor resAcc = (BlockPropertiesAccessor) result;
        BlockPropertiesAccessor20 resAcc20 = (BlockPropertiesAccessor20) result;

        resAcc.setDestroyTime(srcAcc.getDestroyTime());
        resAcc.setExplosionResistance(srcAcc.getExplosionResistance());
        resAcc.setHasCollision(srcAcc.getHasCollision());
        resAcc.setIsRandomlyTicking(srcAcc.getIsRandomlyTicking());
        resAcc.setLightEmission(srcAcc.getLightEmission());
        resAcc.setMapColor(srcAcc.getMapColor());
        resAcc.setSoundType(srcAcc.getSoundType());
        resAcc.setFriction(srcAcc.getFriction());
        resAcc.setSpeedFactor(srcAcc.getSpeedFactor());
        resAcc.setDynamicShape(srcAcc.getDynamicShape());
        resAcc.setCanOcclude(srcAcc.getCanOcclude());
        resAcc.setIsAir(srcAcc.getIsAir());
        resAcc.setIgnitedByLava(srcAcc.getIgnitedByLava());
        resAcc.setLiquid(srcAcc.getLiquid());
        resAcc.setForceSolidOff(srcAcc.getForceSolidOff());
        resAcc.setForceSolidOn(srcAcc.getForceSolidOn());
        resAcc.setPushReaction(srcAcc.getPushReaction());
        resAcc.setRequiresCorrectToolForDrops(srcAcc.getRequiresCorrectToolForDrops());
        resAcc20.setOffsetFunction(srcAcc20.getOffsetFunction());
        resAcc20.setSpawnParticlesOnBreak(srcAcc20.getSpawnParticlesOnBreak());
        resAcc.setRequiredFeatures(srcAcc.getRequiredFeatures());
        resAcc.setEmissiveRendering(srcAcc.getEmissiveRendering());
        resAcc.setInstrument(srcAcc.getInstrument());
        resAcc.setReplaceable(srcAcc.getReplaceable());
        return result;
    }

    public static ElysiumBlockProperties ofFullCopy(BlockBehaviour source) {
        ElysiumBlockProperties result = ElysiumBlockProperties.ofLegacyCopy(source);
        BlockBehaviour.Properties src = ((BlockBehaviourAccessor) source).elysium$getProperties();
        BlockPropertiesAccessor srcAcc = (BlockPropertiesAccessor) src;
        BlockPropertiesAccessor20 srcAcc20 = (BlockPropertiesAccessor20) src;
        BlockPropertiesAccessor resAcc = (BlockPropertiesAccessor) result;
        BlockPropertiesAccessor20 resAcc20 = (BlockPropertiesAccessor20) result;

        resAcc.setJumpFactor(srcAcc.getJumpFactor());
        resAcc.setIsRedstoneConductor(srcAcc.getIsRedstoneConductor());
        resAcc.setIsValidSpawn(srcAcc.getIsValidSpawn());
        resAcc.setHasPostProcess(srcAcc.getHasPostProcess());
        resAcc.setIsSuffocating(srcAcc.getIsSuffocating());
        resAcc.setIsViewBlocking(srcAcc.getIsViewBlocking());
        resAcc20.setDrops(srcAcc20.getDrops());
        return result;
    }

    public ElysiumBlockProperties noTerrainParticles() {
        ((BlockPropertiesAccessor20) this).setSpawnParticlesOnBreak(false);
        return (ElysiumBlockProperties) (Object) this;
    }

    public boolean shouldSpawnTerrainParticles() {
        return ((BlockPropertiesAccessor20) this).getSpawnParticlesOnBreak();
    }

    public ElysiumBlockProperties lazyDropsLike(ElysiumRegHolder<Block> holder) {
        this.elysium$setLazyLootHolder(holder);
        ((BlockPropertiesAccessor20) this).setDrops(null);
        return (ElysiumBlockProperties) (Object) this;
    }

    // asphodel::exclude
    protected void elysium$setLazyLootHolder(ElysiumRegHolder<Block> holder) {
        this.elysiumLazyLootHolder = holder;
    }
}