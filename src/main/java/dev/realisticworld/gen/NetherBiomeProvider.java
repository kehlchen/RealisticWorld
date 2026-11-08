package dev.realisticworld.gen;

import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import java.util.List;

final class NetherBiomeProvider extends BiomeProvider {
    private static final class Cache {
        NetherSampler sampler;
        long key = Long.MIN_VALUE;
        Biome biome;
    }

    private final NetherGenerator gen;
    private final ThreadLocal<Cache> cache = ThreadLocal.withInitial(Cache::new);

    NetherBiomeProvider(NetherGenerator gen) {
        this.gen = gen;
    }

    @Override
    public @NotNull Biome getBiome(@NotNull WorldInfo info, int x, int y, int z) {
        // the server asks for every height of a column; the Nether biome does not depend on y
        NetherSampler s = gen.sampler(info);
        Cache c = cache.get();
        long key = ((long) x << 32) ^ (z & 0xffffffffL);
        if (c.sampler != s || c.key != key) {
            c.sampler = s;
            c.key = key;
            c.biome = s.biomeAt(x, z);
        }
        return c.biome;
    }

    @Override
    public @NotNull List<Biome> getBiomes(@NotNull WorldInfo info) {
        return List.of(Biome.NETHER_WASTES, Biome.SOUL_SAND_VALLEY, Biome.CRIMSON_FOREST, Biome.WARPED_FOREST,
                Biome.BASALT_DELTAS);
    }
}
