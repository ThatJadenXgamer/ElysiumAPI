package net.jadenxgamer.elysium_api.impl.core.biome;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.jadenxgamer.elysium_api.ElysiumAPI;
import net.jadenxgamer.elysium_api.api.util.RegistryAccessHelper;
import net.jadenxgamer.elysium_api.impl.core.datadriven.mosaic.EntryType;
import net.jadenxgamer.elysium_api.impl.core.datadriven.mosaic.MosaicBiomeEntry;
import net.jadenxgamer.elysium_api.impl.core.datadriven.mosaic.SubBiomeBehavior;
import net.jadenxgamer.elysium_api.impl.registry.ElysiumRegistries;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MosaicBiomeSource extends BiomeSource {

    public static final Codec<MosaicBiomeSource> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("grid_cell_size").forGetter(s -> s.gridCellSize),
            Codec.INT.fieldOf("climate_count").forGetter(s -> s.climateCount),
            Codec.FLOAT.optionalFieldOf("jitter_strength", 0.35f).forGetter(s -> s.jitterStrength),
            Codec.BOOL.optionalFieldOf("avoid_diagonal_neighbors", false).forGetter(s -> s.avoidDiagonalNeighbors),

            Codec.DOUBLE.optionalFieldOf("distortion_strength", 24.0).forGetter(s -> s.distortionStrength),
            Codec.DOUBLE.optionalFieldOf("distortion_scale", 0.016).forGetter(s -> s.distortionScale),
            Codec.INT.optionalFieldOf("warp_iterations", 1).forGetter(s -> s.warpIterations),
            Codec.INT.optionalFieldOf("noise_octaves", 3).forGetter(s -> s.noiseOctaves),
            TagKey.codec(Registries.BIOME).optionalFieldOf("auto_populate_entries_from_tag").forGetter(s -> s.autoPopulateEntriesFromTag),
            TagKey.codec(Registries.BIOME).optionalFieldOf("biome_exclusion_tag").forGetter(s -> s.biomeExclusionTag)
    ).apply(instance, MosaicBiomeSource::new));

    private final int gridCellSize;
    private final int climateCount;
    private final float jitterStrength;
    private final boolean avoidDiagonalNeighbors;
    private final Optional<TagKey<Biome>> autoPopulateEntriesFromTag;
    private final Optional<TagKey<Biome>> biomeExclusionTag;

    private final double distortionStrength;
    private final double distortionScale;
    private final int warpIterations;
    private final int noiseOctaves;

    private final double halfCellSize;
    private final double jitterRange;

    private volatile boolean isInitialized = false;
    private long worldSeed;
    private long validClimatesMask = 0L;

    private Set<Holder<Biome>> possibleBiomeSet = Set.of();

    private WeightedBiomeList[] climateEntries;

    private PerlinNoise[][] warpNoisesX;
    private PerlinNoise[][] warpNoisesZ;

    private static final ThreadLocal<ClimateCache> CLIMATE_RESOLUTION_CACHE = ThreadLocal.withInitial(ClimateCache::new);

    public MosaicBiomeSource(int gridCellSize, int climateCount, float jitterStrength, boolean avoidDiagonalNeighbors,
                             double distortionStrength, double distortionScale, int warpIterations, int noiseOctaves,
                             Optional<TagKey<Biome>> autoPopulateEntriesFromTag,
                             Optional<TagKey<Biome>> biomeExclusionTag) {
        this.gridCellSize = gridCellSize;
        this.climateCount = climateCount;
        this.jitterStrength = jitterStrength;
        this.avoidDiagonalNeighbors = avoidDiagonalNeighbors;
        this.autoPopulateEntriesFromTag = autoPopulateEntriesFromTag;
        this.biomeExclusionTag = biomeExclusionTag;

        this.distortionStrength = distortionStrength;
        this.distortionScale = distortionScale;
        this.warpIterations = warpIterations;
        this.noiseOctaves = noiseOctaves;
        this.halfCellSize = gridCellSize / 2.0;
        this.jitterRange = gridCellSize * jitterStrength * 2.0;
        if (this.climateCount > 64) throw new IllegalStateException("climateCount beyond 64 is not supported due to bitmask avoidance");
    }

    @Override
    public Set<Holder<Biome>> possibleBiomes() {
        return this.possibleBiomeSet;
    }

    @Override
    protected Codec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return this.possibleBiomeSet.stream();
    }

    public void initialize(long seed, ResourceKey<LevelStem> dimension) {
        if (isInitialized) return;
        this.worldSeed = seed;

        RandomSource baseRand = RandomSource.create(worldSeed);
        warpNoisesX = new PerlinNoise[warpIterations][noiseOctaves];
        warpNoisesZ = new PerlinNoise[warpIterations][noiseOctaves];

        for (int iteration = 0; iteration < warpIterations; iteration++) {
            for (int octave = 0; octave < noiseOctaves; octave++) {
                long octaveSeed = baseRand.nextLong() ^ (iteration * 73421L) ^ (octave * 19381L);
                RandomSource octaveRandom = RandomSource.create(octaveSeed);
                warpNoisesX[iteration][octave] = PerlinNoise.create(octaveRandom, 1, DoubleList.of(1.0));
                warpNoisesZ[iteration][octave] = PerlinNoise.create(octaveRandom, 1, DoubleList.of(1.0));
            }
        }

        WeightedBiomeList[] entriesByClimate = new WeightedBiomeList[climateCount];
        Map<ResourceKey<Biome>, BiomeEntry> defaultEntryMap = new HashMap<>();
        List<MosaicBiomeEntry> subBiomeEntries = new ArrayList<>();
        Set<ResourceKey<Biome>> assignedBiomeKeys = new HashSet<>();

        RegistryAccessHelper.getServer()
                .flatMap(access -> access.registry(ElysiumRegistries.Keys.MOSAIC_BIOME_ENTRY))
                .ifPresent(registry -> registry.forEach(entry -> {
                    if (!entry.dimension().equals(dimension.location())) return;
                    if (biomeExclusionTag.isPresent() && entry.biome().is(biomeExclusionTag.get())) return;
                    int climateP = entry.climatePoint();
                    if (climateP < 0 || climateP >= climateCount) return;

                    if (entry.type() == EntryType.DEFAULT) {
                        processDefaultEntry(entry, climateP, entriesByClimate, defaultEntryMap, assignedBiomeKeys);
                    } else if (entry.type() == EntryType.SUB_BIOME) {
                        subBiomeEntries.add(entry);
                        entry.biome().unwrapKey().ifPresent(assignedBiomeKeys::add);
                    }
                }));
        tagProvidedEntries(assignedBiomeKeys, entriesByClimate, defaultEntryMap);
        for (MosaicBiomeEntry subEntry : subBiomeEntries) processSubBiomeEntry(subEntry, defaultEntryMap);

        Set<Holder<Biome>> providePossibleBiomes = new HashSet<>();
        for (WeightedBiomeList biomeList : entriesByClimate) {
            if (biomeList == null) continue;
            for (BiomeEntry entry : biomeList.entries) {
                providePossibleBiomes.add(entry.biome);
                if (entry.replacements != null) for (SubBiomeReplacement replacement : entry.replacements)
                    providePossibleBiomes.add(replacement.replacementBiome);
            }
        }
        this.possibleBiomeSet = providePossibleBiomes.stream().distinct().collect(Collectors.toSet());

        boolean hasAnyValidEntry = false;
        long globalMask = 0L;

        for (int i = 0; i < climateCount; i++) {
            if (entriesByClimate[i] != null && !entriesByClimate[i].isEmpty()) {
                hasAnyValidEntry = true;
                globalMask |= (1L << i);
            } else entriesByClimate[i] = null;
        }

        if (!hasAnyValidEntry) throw new IllegalStateException("MosaicBiomeSource for dimension '" + dimension.location() + "' has no entries to populate any of the climate points");

        this.climateEntries = entriesByClimate;
        this.validClimatesMask = globalMask;
        this.isInitialized = true;

        ElysiumAPI.LOGGER.info("MosaicBiomeSource successfully initialized for dimension: '{}'", dimension.location());
        ElysiumAPI.LOGGER.debug(buildDebugInfo(seed, dimension, possibleBiomeSet).toString());
    }

    private void processDefaultEntry(MosaicBiomeEntry entry, int climatePoint, WeightedBiomeList[] entriesByClimate,
                                     Map<ResourceKey<Biome>, BiomeEntry> defaultEntryMap,
                                     Set<ResourceKey<Biome>> assignedBiomeKeys) {
        ResourceKey<Biome> biomeKey = entry.biome().unwrapKey().orElseThrow(() -> new IllegalStateException("Biome has no key"));
        if (entriesByClimate[climatePoint] == null) entriesByClimate[climatePoint] = new WeightedBiomeList();
        BiomeEntry biomeEntry = new BiomeEntry(entry.biome(), entry.weight(), entry.keepWeight(), climatePoint, null);
        entriesByClimate[climatePoint].add(biomeEntry);
        defaultEntryMap.put(biomeKey, biomeEntry);
        assignedBiomeKeys.add(biomeKey);
    }

    private void processSubBiomeEntry(MosaicBiomeEntry entry, Map<ResourceKey<Biome>, BiomeEntry> defaultEntryMap) {
        if (biomeExclusionTag.isPresent() && entry.biome().is(biomeExclusionTag.get())) return;

        MosaicBiomeEntry.SubBiomeType subType = entry.subBiomeSettings().orElseThrow(() -> new IllegalStateException("Sub-biome entry missing sub_biome_type"));
        if (subType.behavior() == SubBiomeBehavior.REPLACE) {
            ResourceKey<Biome> targetKey = ResourceKey.create(Registries.BIOME, subType.replaceBiome());
            BiomeEntry targetEntry = defaultEntryMap.get(targetKey);
            if (targetEntry == null) {
                ElysiumAPI.LOGGER.error("Target biome: {} not found for sub-biome replacement: {}", subType.replaceBiome(), entry.biome().unwrapKey().map(ResourceKey::location).orElse(null));
                return;
            }
            if (targetEntry.climate != entry.climatePoint()) return;
            if (targetEntry.replacements == null) targetEntry.replacements = new ArrayList<>();
            targetEntry.replacements.add(new SubBiomeReplacement(entry.biome(), entry.weight()));
        } else ElysiumAPI.LOGGER.warn("Only \"REPLACE\" sub-biome behavior is supported at the moment.");
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.@NotNull Sampler sampler) {
        double wx = x;
        double wz = z;
        for (int iteration = 0; iteration < warpIterations; iteration++) {
            double dx = fractalNoise(warpNoisesX[iteration], wx, wz, distortionScale) * distortionStrength;
            double dz = fractalNoise(warpNoisesZ[iteration], wx, wz, distortionScale) * distortionStrength;
            wx += dx;
            wz += dz;
        }
        int gridX = Mth.floor(wx / gridCellSize);
        int gridZ = Mth.floor(wz / gridCellSize);

        double minDistanceSq = Double.MAX_VALUE;
        int bestClimate = -1;
        int bestGridX = 0;
        int bestGridZ = 0;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int neighborGridX = gridX + dx;
                int neighborGridZ = gridZ + dz;
                int climate = resolveClimateForGridCell(neighborGridX, neighborGridZ);
                double jitteredX = (neighborGridX * gridCellSize) + halfCellSize + computeJitterOffset(neighborGridX, neighborGridZ, 0);
                double jitteredZ = (neighborGridZ * gridCellSize) + halfCellSize + computeJitterOffset(neighborGridX, neighborGridZ, 1);

                double distSq = (wx - jitteredX) * (wx - jitteredX) + (wz - jitteredZ) * (wz - jitteredZ);
                if (distSq < minDistanceSq) {
                    minDistanceSq = distSq;
                    bestClimate = climate;
                    bestGridX = neighborGridX;
                    bestGridZ = neighborGridZ;
                }
            }
        }

        BiomeEntry selectedEntry = selectBiomeFromClimateEntry(bestClimate, bestGridX, bestGridZ);
        return applySubBiomeReplacement(selectedEntry, bestGridX, bestGridZ);
    }

    private double fractalNoise(PerlinNoise[] noises, double x, double z, double scale) {
        double value = 0.0;
        double amplitude = 1.0;
        double frequency = scale;
        for (PerlinNoise noise : noises) {
            value += noise.getValue(x * frequency, 0, z * frequency) * amplitude;
            amplitude *= 0.5;
            frequency *= 2.0;
        }
        return value;
    }

    private int resolveClimateForGridCell(int gridX, int gridZ) {
        long cellKey = ((long) gridX << 32) | (gridZ & 0xFFFFFFFFL);
        long instanceSalt = System.identityHashCode(this);
        long key = cellKey ^ this.worldSeed ^ (instanceSalt << 32);

        ClimateCache cache = CLIMATE_RESOLUTION_CACHE.get();
        int cached = cache.get(key);
        if (cached != -1) return cached;

        int result = computeClimateWithAvoidance(gridX, gridZ);
        cache.put(key, result);
        return result;
    }

    private int computeClimateWithAvoidance(int gridX, int gridZ) {
        int xMod = Mth.abs(gridX % 2);
        int zMod = Mth.abs(gridZ % 2);

        if (xMod == 0 && zMod == 0) {
            return selectClimateWithAvoidance(gridX, gridZ, 0L, 0L);
        } else if (xMod == 1 && zMod == 0) {
            long mask = toMask(resolveClimateForGridCell(gridX - 1, gridZ)) |
                    toMask(resolveClimateForGridCell(gridX + 1, gridZ));
            return selectClimateWithAvoidance(gridX, gridZ, mask, mask);
        } else if (xMod == 0) {
            long mask = toMask(resolveClimateForGridCell(gridX, gridZ - 1)) |
                    toMask(resolveClimateForGridCell(gridX, gridZ + 1));
            return selectClimateWithAvoidance(gridX, gridZ, mask, mask);
        } else {
            long cardinalMask = toMask(resolveClimateForGridCell(gridX - 1, gridZ)) |
                    toMask(resolveClimateForGridCell(gridX + 1, gridZ)) |
                    toMask(resolveClimateForGridCell(gridX, gridZ - 1)) |
                    toMask(resolveClimateForGridCell(gridX, gridZ + 1));

            if (avoidDiagonalNeighbors) {
                long diagonalMask = toMask(resolveClimateForGridCell(gridX - 1, gridZ - 1)) |
                        toMask(resolveClimateForGridCell(gridX + 1, gridZ - 1)) |
                        toMask(resolveClimateForGridCell(gridX - 1, gridZ + 1)) |
                        toMask(resolveClimateForGridCell(gridX + 1, gridZ + 1));

                long forbiddenMask = cardinalMask | diagonalMask;
                return selectClimateWithAvoidance(gridX, gridZ, forbiddenMask, cardinalMask);
            } else return selectClimateWithAvoidance(gridX, gridZ, cardinalMask, cardinalMask);
        }
    }

    private long toMask(int climate) {
        return (climate >= 0 && climate < climateCount) ? (1L << climate) : 0L;
    }

    private int selectClimateWithAvoidance(int gridX, int gridZ, long forbiddenMask, long cardinalMask) {
        long hash = this.worldSeed + gridX * 1234567L + gridZ * 7654321L;
        hash = (hash ^ (hash >> 16)) * 0x85ebca6bL;

        long availableMask = ~forbiddenMask & this.validClimatesMask;
        if (availableMask != 0L) return pickClimateFromMask(hash, availableMask);

        long cardinalAvailableMask = ~cardinalMask & this.validClimatesMask;
        if (cardinalAvailableMask != 0L) return pickClimateFromMask(hash, cardinalAvailableMask);

        return pickClimateFromMask(hash, this.validClimatesMask);
    }

    private int pickClimateFromMask(long hash, long mask) {
        int count = Long.bitCount(mask);
        int target = (int) ((hash & Long.MAX_VALUE) % count);

        long tempMask = mask;
        for (int i = 0; i < target; i++) tempMask &= tempMask - 1;
        return Long.numberOfTrailingZeros(tempMask);
    }

    private double computeJitterOffset(int gridX, int gridZ, int axis) {
        long hash = this.worldSeed + gridX * 31337L + gridZ * 313373L + axis * 17L;
        hash = (hash ^ (hash >> 16)) * 0x85ebca6bL;
        hash = (hash ^ (hash >> 13)) * 0xc2b2ae35L;
        hash = hash ^ (hash >> 16);
        double value = (double) (hash & 0xFFFFFF) / (double) 0xFFFFFF;
        return (value - 0.5) * jitterRange;
    }

    private BiomeEntry selectBiomeFromClimateEntry(int climate, int gridX, int gridZ) {
        WeightedBiomeList list = climateEntries[climate];
        long hash = this.worldSeed + gridX * 98765L + gridZ * 54321L;
        hash = (hash ^ (hash >> 16)) * 0x85ebca6bL;

        int roll = (int) ((hash & Long.MAX_VALUE) % list.totalWeight);

        for (BiomeEntry entry : list.entries) {
            roll -= entry.weight;
            if (roll < 0) return entry;
        }

        return list.entries.get(list.entries.size() - 1);
    }

    private Holder<Biome> applySubBiomeReplacement(BiomeEntry selectedEntry, int gridX, int gridZ) {
        if (selectedEntry.replacements == null || selectedEntry.replacements.isEmpty()) return selectedEntry.biome;

        int keepWeight = selectedEntry.keepWeight;
        int totalWeight = keepWeight;
        for (SubBiomeReplacement replacement : selectedEntry.replacements) totalWeight += replacement.weight;

        long cellSeed = this.worldSeed;
        cellSeed = cellSeed * 6364136223846793005L + 1442695040888963407L;
        cellSeed += (long) gridX * 374761393L;
        cellSeed = cellSeed * 6364136223846793005L + 1442695040888963407L;
        cellSeed += (long) gridZ * 668265263L;

        int biomeHash = selectedEntry.biome.unwrapKey().map(key -> key.location().hashCode()).orElse(0);
        cellSeed ^= biomeHash;
        RandomSource random = RandomSource.create(cellSeed);
        int roll = random.nextInt(totalWeight);

        if (roll < keepWeight) return selectedEntry.biome;
        roll -= keepWeight;
        for (SubBiomeReplacement replacement : selectedEntry.replacements) {
            roll -= replacement.weight;
            if (roll < 0) return replacement.replacementBiome;
        }

        return selectedEntry.biome;
    }

    private StringBuilder buildDebugInfo(long seed, ResourceKey<LevelStem> dimension, Set<Holder<Biome>> possibleBiomes) {
        StringBuilder debug = new StringBuilder();
        debug.append("MosaicBiomeSource - '").append(dimension.location()).append("'\n");
        debug.append("\tSeed: ").append(seed).append("\n");
        debug.append("\tGrid Cell Size: ").append(gridCellSize).append(", climate count: ").append(climateCount).append("\n");
        debug.append("\tAvoid Diagonal Neighbors: ").append(avoidDiagonalNeighbors).append("\n");
        debug.append("\tPossible Biomes:\n");
        for (Holder<Biome> biome : possibleBiomes) {
            String biomeName = biome.unwrapKey()
                    .map(key -> "'" + key.location() + "'")
                    .orElse("'unknown'");
            debug.append("\t\t").append(biomeName).append("\n");
        }
        return debug;
    }

    private void tagProvidedEntries(Set<ResourceKey<Biome>> assignedBiomeKeys,
                                    WeightedBiomeList[] entriesByClimate,
                                    Map<ResourceKey<Biome>, BiomeEntry> defaultEntryMap) {
        if (autoPopulateEntriesFromTag.isEmpty()) return;
        RegistryAccessHelper.getServer()
                .flatMap(access -> access.registry(Registries.BIOME))
                .ifPresent(biomeRegistry -> biomeRegistry.getTagOrEmpty(autoPopulateEntriesFromTag.get())
                        .forEach(holder -> holder.unwrapKey().ifPresent(key -> {
                            if (biomeExclusionTag.isPresent() && holder.is(biomeExclusionTag.get())) return;
                            if (!assignedBiomeKeys.contains(key)) {
                                int climate = Math.abs(key.location().toString().hashCode()) % climateCount;
                                WeightedBiomeList list = entriesByClimate[climate];
                                int weight = (list == null) ? 1 : Math.max(1, list.totalWeight / list.entries.size());

                                if (list == null) entriesByClimate[climate] = list = new WeightedBiomeList();
                                BiomeEntry newEntry = new BiomeEntry(holder, weight, 50, climate, null);
                                list.add(newEntry);
                                assignedBiomeKeys.add(key);
                                defaultEntryMap.put(key, newEntry);
                            }
                        }))
                );
    }

    private static class WeightedBiomeList {
        final List<BiomeEntry> entries = new ArrayList<>();
        int totalWeight = 0;

        void add(BiomeEntry entry) {
            entries.add(entry);
            totalWeight += entry.weight;
        }

        boolean isEmpty() {
            return entries.isEmpty();
        }
    }

    private static class BiomeEntry {
        final Holder<Biome> biome;
        final int weight;
        final int keepWeight;
        final int climate;
        List<SubBiomeReplacement> replacements;

        BiomeEntry(Holder<Biome> biome, int weight, int keepWeight, int climate, List<SubBiomeReplacement> replacements) {
            this.biome = biome;
            this.weight = weight;
            this.keepWeight = keepWeight;
            this.climate = climate;
            this.replacements = replacements;
        }
    }

    private record SubBiomeReplacement(Holder<Biome> replacementBiome, int weight) {}

    private static class ClimateCache {
        private static final int CAPACITY = 4096;
        private static final int MASK = CAPACITY - 1;
        private final long[] keys = new long[CAPACITY];
        private final int[] values = new int[CAPACITY];

        public ClimateCache() {
            Arrays.fill(keys, Long.MIN_VALUE);
        }

        public int get(long key) {
            long h = key ^ (key >>> 32);
            h ^= (h >>> 16);
            int idx = (int) (h & MASK);

            if (keys[idx] == key) return values[idx];
            return -1;
        }

        public void put(long key, int value) {
            long h = key ^ (key >>> 32);
            h ^= (h >>> 16);
            int idx = (int) (h & MASK);

            keys[idx] = key;
            values[idx] = value;
        }
    }
}