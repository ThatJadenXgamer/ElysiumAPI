package net.jadenxgamer.elysium_api.merge.block;

import net.jadenxgamer.elysium_api.api.block.ElysiumBlockProperties;
import net.jadenxgamer.elysium_api.api.registry.ElysiumRegHolder;
import net.jadenxgamer.elysium_api.impl.mixin.block.BlockPropertiesAccessor;
import net.jadenxgamer.elysium_api.impl.mixin.block.BlockPropertiesAccessor21;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class ElysiumBlockProperties21 extends BlockBehaviour.Properties {

    // asphodel::exclude
    private ElysiumRegHolder<Block> elysiumLazyLootHolder;

    // asphodel::exclude
    public ElysiumBlockProperties21() {
        super();
    }

    public static ElysiumBlockProperties ofLegacyCopy(BlockBehaviour source) {
        ElysiumBlockProperties result = ElysiumBlockProperties.of();
        BlockBehaviour.Properties src = source.properties();
        BlockPropertiesAccessor srcAcc = (BlockPropertiesAccessor) src;
        BlockPropertiesAccessor21 srcAcc21 = (BlockPropertiesAccessor21) src;
        BlockPropertiesAccessor resAcc = (BlockPropertiesAccessor) result;
        BlockPropertiesAccessor21 resAcc21 = (BlockPropertiesAccessor21) result;

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
        resAcc21.setOffsetFunction(srcAcc21.getOffsetFunction());
        resAcc21.setSpawnTerrainParticles(srcAcc21.getSpawnTerrainParticles());
        resAcc.setRequiredFeatures(srcAcc.getRequiredFeatures());
        resAcc.setEmissiveRendering(srcAcc.getEmissiveRendering());
        resAcc.setInstrument(srcAcc.getInstrument());
        resAcc.setReplaceable(srcAcc.getReplaceable());
        return result;
    }

    public static ElysiumBlockProperties ofFullCopy(BlockBehaviour source) {
        ElysiumBlockProperties result = ElysiumBlockProperties.ofLegacyCopy(source);
        BlockBehaviour.Properties src = source.properties();
        BlockPropertiesAccessor srcAcc = (BlockPropertiesAccessor) src;
        BlockPropertiesAccessor21 srcAcc21 = (BlockPropertiesAccessor21) src;
        BlockPropertiesAccessor resAcc = (BlockPropertiesAccessor) result;
        BlockPropertiesAccessor21 resAcc21 = (BlockPropertiesAccessor21) result;

        resAcc.setJumpFactor(srcAcc.getJumpFactor());
        resAcc.setIsRedstoneConductor(srcAcc.getIsRedstoneConductor());
        resAcc.setIsValidSpawn(srcAcc.getIsValidSpawn());
        resAcc.setHasPostProcess(srcAcc.getHasPostProcess());
        resAcc.setIsSuffocating(srcAcc.getIsSuffocating());
        resAcc.setIsViewBlocking(srcAcc.getIsViewBlocking());
        resAcc21.setDrops(srcAcc21.getDrops());
        return result;
    }

    public ElysiumBlockProperties noTerrainParticles() {
        ((BlockPropertiesAccessor21) this).setSpawnTerrainParticles(false);
        return (ElysiumBlockProperties) (Object) this;
    }

    public boolean shouldSpawnTerrainParticles() {
        return ((BlockPropertiesAccessor21) this).getSpawnTerrainParticles();
    }

    public ElysiumBlockProperties lazyDropsLike(ElysiumRegHolder<Block> holder) {
        this.elysium$setLazyLootHolder(holder);
        ((BlockPropertiesAccessor21) this).setDrops(null);
        return (ElysiumBlockProperties) (Object) this;
    }

    // asphodel::exclude
    protected void elysium$setLazyLootHolder(ElysiumRegHolder<Block> holder) {
        this.elysiumLazyLootHolder = holder;
    }
}