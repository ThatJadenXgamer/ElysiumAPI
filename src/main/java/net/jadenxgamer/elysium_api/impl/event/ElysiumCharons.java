package net.jadenxgamer.elysium_api.impl.event;

import net.jadenxgamer.elysium_api.ElysiumAPI;
import net.jadenxgamer.elysium_api.api.charon.*;
import net.jadenxgamer.elysium_api.impl.core.biome.MosaicBiomeSource;
import net.jadenxgamer.elysium_api.impl.core.datadriven.block.properties_transformer.BlockPropertiesTransformerHelper;
import net.jadenxgamer.elysium_api.impl.core.datadriven.block.use_behaviors.UseBehaviorImpl;
import net.jadenxgamer.elysium_api.impl.core.datadriven.item.properties_transformer.ItemPropertiesTransformerHelper;
import net.jadenxgamer.elysium_api.impl.core.surface_rules.ElysiumSurfaceRulesManager;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Optional;

@CharonEventBoat(modid = ElysiumAPI.MOD_ID, dist = CharonDist.COMMON)
public class ElysiumCharons {

    @CharonEvent(target = LivingEntity.class, method = "tick", at = @Toll("TAIL"))
    public static void onLivingTick(CharonContext ctx) {
    }

    @CharonEvent(target = ServerPlayerGameMode.class, method = "useItemOn", at = @Toll("HEAD"))
    public static void rightClickBlock(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand, BlockHitResult hitResult, CharonContext ctx) {
        UseBehaviorImpl.init(player, level, stack, hand, hitResult, ctx);
    }

    @CharonEvent(target = MinecraftServer.class, method = "loadLevel", at = @Toll("HEAD"))
    public static void onServerAboutToStart(CharonContext ctx) {
        MinecraftServer server = ctx.getSelf();

        BlockPropertiesTransformerHelper.invalidateCache();
        ItemPropertiesTransformerHelper.invalidateCache();
        RegistryAccess registryAccess = server.registryAccess();

        Registry<LevelStem> levelStems = registryAccess.registryOrThrow(Registries.LEVEL_STEM);
        for (LevelStem dimension : levelStems.stream().toList()) {
            Optional<ResourceKey<LevelStem>> dimensionKey = levelStems.getResourceKey(dimension);

            if (dimensionKey.isPresent() && dimension.generator().getBiomeSource() instanceof MosaicBiomeSource biomeSource) {
                var seed = server.getWorldData().worldGenOptions().seed();
                biomeSource.initialize(seed, dimensionKey.get());
            }

            ChunkGenerator generator = dimension.generator();
            if (dimensionKey.isPresent() && generator instanceof NoiseBasedChunkGenerator noiseGenerator)
                ElysiumSurfaceRulesManager.handleSurfaceRules(dimensionKey.get(), noiseGenerator);
        }
    }
}