package dev.realisticworld.gen;

import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class RealisticBiomeProvider extends BiomeProvider {
    private static final List<Biome> ALL = List.of(
            Biome.OCEAN, Biome.DEEP_OCEAN, Biome.WARM_OCEAN, Biome.LUKEWARM_OCEAN, Biome.DEEP_LUKEWARM_OCEAN,
            Biome.COLD_OCEAN, Biome.DEEP_COLD_OCEAN, Biome.FROZEN_OCEAN, Biome.DEEP_FROZEN_OCEAN,
            Biome.RIVER, Biome.FROZEN_RIVER, Biome.BEACH, Biome.SNOWY_BEACH, Biome.STONY_SHORE,
            Biome.DESERT, Biome.BADLANDS, Biome.ERODED_BADLANDS, Biome.WOODED_BADLANDS,
            Biome.SAVANNA, Biome.SAVANNA_PLATEAU, Biome.WINDSWEPT_SAVANNA,
            Biome.PLAINS, Biome.SUNFLOWER_PLAINS, Biome.FOREST, Biome.FLOWER_FOREST, Biome.BIRCH_FOREST,
            Biome.OLD_GROWTH_BIRCH_FOREST, Biome.DARK_FOREST, Biome.TAIGA, Biome.OLD_GROWTH_PINE_TAIGA,
            Biome.OLD_GROWTH_SPRUCE_TAIGA, Biome.SNOWY_TAIGA, Biome.SNOWY_PLAINS, Biome.ICE_SPIKES,
            Biome.JUNGLE, Biome.SPARSE_JUNGLE, Biome.BAMBOO_JUNGLE, Biome.SWAMP, Biome.MANGROVE_SWAMP,
            Biome.MEADOW, Biome.GROVE, Biome.SNOWY_SLOPES, Biome.JAGGED_PEAKS, Biome.FROZEN_PEAKS,
            Biome.STONY_PEAKS, Biome.WINDSWEPT_HILLS, Biome.WINDSWEPT_FOREST, Biome.WINDSWEPT_GRAVELLY_HILLS,
            Biome.CHERRY_GROVE, Biome.LUSH_CAVES, Biome.DRIPSTONE_CAVES);

    private static final class Cache {
        TerrainSampler sampler;
        long key = Long.MIN_VALUE;
        Column col;
        Biome surface;
    }

    private final RealisticChunkGenerator gen;
    private final ThreadLocal<Cache> cache = ThreadLocal.withInitial(Cache::new);

    RealisticBiomeProvider(RealisticChunkGenerator gen) {
        this.gen = gen;
    }

    @Override
    public @NotNull Biome getBiome(@NotNull WorldInfo info, int x, int y, int z) {
        TerrainSampler s = gen.sampler(info);
        if (s.sites().suppressed()) {
            // Until the stronghold positions of the world are known the sites must not change any biome:
            // vanilla derives the stronghold ring positions from the biomes, and they must come out the same
            // on every start.
            Column b = s.baseColumn(x, z);
            Biome ub = s.undergroundBiome(b, x, y, z);
            return ub != null ? ub : b.biome();
        }
        Cache c = cache.get();
        long key = ((long) x << 32) ^ (z & 0xffffffffL);
        if (c.sampler != s || c.key != key) {
            c.sampler = s;
            c.key = key;
            c.col = s.sample(x, z);
            c.surface = c.col.biome();
        }
        Biome under = s.undergroundBiome(c.col, x, y, z);
        return under != null ? under : c.surface;
    }

    @Override
    public @NotNull List<Biome> getBiomes(@NotNull WorldInfo info) {
        return ALL;
    }
}
