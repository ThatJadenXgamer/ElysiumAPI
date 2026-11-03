package net.jadenxgamer.elysium_api.impl.config;

import net.jadenxgamer.elysium_api.api.config.ConfigParseException;
import net.jadenxgamer.elysium_api.api.config.ElysiumConfigEntry;
import net.jadenxgamer.elysium_api.api.config.ConfigLoadType;
import net.jadenxgamer.elysium_api.api.network.ElysiumNetwork;
import net.jadenxgamer.elysium_api.api.util.UniversalModLoaderHelper;
import net.jadenxgamer.elysium_api.mod.network.payload.client.ConfigSyncPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@ApiStatus.Internal
public final class ConfigRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger("Elysium-API");
    private static final Path CONFIG_ROOT = UniversalModLoaderHelper.getConfigDirectory();
    private static final Map<String, Map<ConfigLoadType, ConfigFile.Spec>> SPECS = new HashMap<>();

    private ConfigRegistry() {
    }

    public static void register(String modId, Class<?> configClass) {
        List<ConfigFile.Spec> scanned = ConfigFile.Spec.scan(modId, configClass, CONFIG_ROOT);
        Map<ConfigLoadType, ConfigFile.Spec> byType = SPECS.computeIfAbsent(modId, ignored -> new HashMap<>());

        for (ConfigFile.Spec spec : scanned) {
            if (byType.containsKey(spec.loadType())) {
                throw new IllegalStateException("Duplicate config registration for mod '" + modId + "' and load type " + spec.loadType());
            }
            byType.put(spec.loadType(), spec);
            ConfigFile.loadFrom(spec, spec.globalFile());
        }
    }

    public static void reload(String modId) {
        withSpecs(modId, byType -> byType.values().forEach(ConfigRegistry::reloadSpec));
    }

    public static void reload(String modId, ConfigLoadType type) {
        withSpec(modId, type, ConfigRegistry::reloadSpec);
    }

    public static void save(String modId) {
        withSpecs(modId, byType -> byType.values().forEach(ConfigFile::save));
    }

    public static void save(String modId, ConfigLoadType type) {
        withSpec(modId, type, ConfigFile::save);
    }

    public static void onServerStarting(MinecraftServer server) {
        Path overrideDir = server.getWorldPath(LevelResource.ROOT).resolve("serverconfig");
        forEachServerSpec(spec -> {
            Path overrideFile = overrideDir.resolve(spec.modId()).resolve("server.css");
            Path target = Files.exists(overrideFile) ? overrideFile : spec.globalFile();
            ConfigFile.loadFrom(spec, target);
        });
    }

    public static void onServerStopping(MinecraftServer server) {
        revertAllServerSpecsToGlobal();
    }

    public static void onClientDisconnected() {
        revertAllServerSpecsToGlobal();
    }

    public static void syncToPlayer(ServerPlayer player) {
        forEachServerSpec(spec -> {
            if (spec.entries().isEmpty()) return;
            ElysiumNetwork.sendToPlayer(player, new ConfigSyncPayload(spec.modId(), collectServerValues(spec)));
        });
    }

    public static void broadcastSync(String modId) {
        withSpec(modId, ConfigLoadType.SERVER, spec -> {
            if (spec.entries().isEmpty()) return;
            ElysiumNetwork.sendToAllPlayers(new ConfigSyncPayload(modId, collectServerValues(spec)));
        });
    }

    public static void applyServerSync(String modId, Map<String, String> values, boolean pureClient) {
        withSpec(modId, ConfigLoadType.SERVER, spec -> {
            for (ElysiumConfigEntry<?> entry : spec.entries()) {
                String key = entry.category() + "." + entry.name();
                String raw = values.get(key);
                if (raw == null) continue;
                try {
                    entry.applyServerSync(raw);
                } catch (ConfigParseException e) {
                    LOGGER.warn("Failed to apply server-synced value for {}.{}: {}", entry.category(), entry.name(), e.getMessage());
                }
            }

            if (pureClient) spec.clearActiveFile();
        });
    }

    private static void withSpecs(String modId, Consumer<Map<ConfigLoadType, ConfigFile.Spec>> action) {
        Map<ConfigLoadType, ConfigFile.Spec> byType = SPECS.get(modId);
        if (byType != null) action.accept(byType);
    }

    private static void withSpec(String modId, ConfigLoadType type, Consumer<ConfigFile.Spec> action) {
        withSpecs(modId, byType -> {
            ConfigFile.Spec spec = byType.get(type);
            if (spec != null) action.accept(spec);
        });
    }

    private static void forEachServerSpec(Consumer<ConfigFile.Spec> action) {
        for (Map<ConfigLoadType, ConfigFile.Spec> byType : SPECS.values()) {
            ConfigFile.Spec spec = byType.get(ConfigLoadType.SERVER);
            if (spec != null) action.accept(spec);
        }
    }

    private static void revertAllServerSpecsToGlobal() {
        forEachServerSpec(spec -> ConfigFile.loadFrom(spec, spec.globalFile()));
    }

    private static void reloadSpec(ConfigFile.Spec spec) {
        Path target = spec.activeFile() != null ? spec.activeFile() : spec.globalFile();
        ConfigFile.loadFrom(spec, target);
    }

    private static Map<String, String> collectServerValues(ConfigFile.Spec serverSpec) {
        Map<String, String> values = new LinkedHashMap<>();
        for (ElysiumConfigEntry<?> entry : serverSpec.entries()) values.put(entry.category() + "." + entry.name(), entry.emitForSync());
        return values;
    }
}