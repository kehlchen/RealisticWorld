package dev.realisticworld.gen;

import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import java.util.List;

final class EndBiomeProvider extends BiomeProvider {
    private static final class Cache {
        EndSampler sampler;
        long key = Long.MIN_VALUE;
        Biome biome;
    }

    private final EndGenerator gen;
    private final ThreadLocal<Cache> cache = ThreadLocal.withInitial(Cache::new);

    EndBiomeProvider(EndGenerator gen) {
        this.gen = gen;
    }

    @Override
    public @NotNull Biome getBiome(@NotNull WorldInfo info, int x, int y, int z) {
        // the server asks for every height of a column; the End biome does not depend on y
        EndSampler s = gen.sampler(info);
        Cache c = cache.get();
        long key = ((long) x << 32) ^ (z & 0xffffffffL);
        if (c.sampler != s || c.key != key) {
            c.sampler = s;
            c.key = key;
            c.biome = s.biome(x, z);
        }
        return c.biome;
    }

    @Override
    public @NotNull List<Biome> getBiomes(@NotNull WorldInfo info) {
        return List.of(Biome.THE_END, Biome.END_HIGHLANDS, Biome.END_MIDLANDS, Biome.END_BARRENS,
                Biome.SMALL_END_ISLANDS);
    }
}
