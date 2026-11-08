package dev.realisticworld.gen;

import dev.realisticworld.Settings;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RealisticChunkGenerator extends ChunkGenerator {
    private record Key(UUID world, int cx, int cz) {
    }

    private final Settings settings;
    private final Map<UUID, TerrainSampler> samplers = new ConcurrentHashMap<>();
    private final SurfaceBuilder surface = new SurfaceBuilder();
    private final RealisticBiomeProvider provider = new RealisticBiomeProvider(this);
    private final FeaturePopulator populator;

    /** Small cache so noise/surface/bedrock steps share one sampling pass per chunk. */
    private static final class Entry {
        final Column[][] cols;
        /** Steps that still have to read these columns; the entry is dropped when the last one is done. */
        final java.util.concurrent.atomic.AtomicInteger left;

        Entry(Column[][] cols, int users) {
            this.cols = cols;
            this.left = new java.util.concurrent.atomic.AtomicInteger(users);
        }
    }

    private final Map<Key, Entry> cache = java.util.Collections.synchronizedMap(
            new LinkedHashMap<>(64, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Key, Entry> e) {
                    return size() > 256;
                }
            });

    public RealisticChunkGenerator(Settings settings) {
        this.settings = settings;
        this.populator = new FeaturePopulator(this, settings);
    }

    public Settings settings() {
        return settings;
    }

    public TerrainSampler sampler(WorldInfo w) {
        return samplers.computeIfAbsent(w.getUID(),
                id -> new TerrainSampler(w.getSeed(), settings, w.getMinHeight(), w.getMaxHeight(),
                        new MountainSites(w.getSeed(), settings)));
    }

    public MountainSites sites(WorldInfo w) {
        return sampler(w).sites();
    }

    /** Users of the cached columns per chunk: noise, surface and (if enabled) caves. */
    private int users() {
        return settings.caves() ? 3 : 2;
    }

    private Column[][] columns(WorldInfo w, TerrainSampler s, int cx, int cz) {
        Entry e = cache.get(new Key(w.getUID(), cx, cz));
        if (e != null) return e.cols;
        Column[][] cols = s.sampleChunk(cx, cz);
        cache.put(new Key(w.getUID(), cx, cz), new Entry(cols, users()));
        return cols;
    }

    /** Called when a step is done with the columns of a chunk. A late or repeated call only costs a recompute. */
    private void release(WorldInfo w, int cx, int cz) {
        Key key = new Key(w.getUID(), cx, cz);
        Entry e = cache.get(key);
        if (e != null && e.left.decrementAndGet() <= 0) cache.remove(key);
    }

    @Override
    public void generateNoise(@NotNull WorldInfo w, @NotNull Random rnd, int cx, int cz, @NotNull ChunkData d) {
        TerrainSampler s = sampler(w);
        Column[][] cols = columns(w, s, cx, cz);
        int minY = w.getMinHeight();
        int sea = settings.seaLevel();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int h = s.toBlockHeight(cols[x + 1][z + 1]);
                // y < 0 is deepslate, y >= 0 stone; every block is written once
                int stoneFrom = Math.max(minY, 0);
                if (minY < 0) d.setRegion(x, minY, z, x + 1, Math.min(h + 1, 0), z + 1, Material.DEEPSLATE);
                if (h + 1 > stoneFrom) d.setRegion(x, stoneFrom, z, x + 1, h + 1, z + 1, Material.STONE);
                for (int y = Math.max(0, minY); y <= Math.min(h, 7); y++) {
                    if (rnd.nextInt(8) >= y) d.setBlock(x, y, z, Material.DEEPSLATE);
                }
                if (h < sea) d.setRegion(x, h + 1, z, x + 1, sea + 1, z + 1, Material.WATER);
            }
        }
        release(w, cx, cz);
    }

    @Override
    public void generateSurface(@NotNull WorldInfo w, @NotNull Random rnd, int cx, int cz, @NotNull ChunkData d) {
        TerrainSampler s = sampler(w);
        surface.apply(s, settings.seaLevel(), w.getMinHeight(), cx, cz, d, columns(w, s, cx, cz));
        release(w, cx, cz);
    }

    @Override
    public void generateBedrock(@NotNull WorldInfo w, @NotNull Random rnd, int cx, int cz, @NotNull ChunkData d) {
        int minY = w.getMinHeight();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int i = 0; i < 5; i++) {
                    if (i == 0 || rnd.nextDouble() < (5 - i) / 5.0 * 0.8) d.setBlock(x, minY + i, z, Material.BEDROCK);
                }
            }
        }
    }

    /**
     * Own caves instead of vanilla carvers: the vanilla aquifers are computed from vanilla terrain noise and
     * produce air pockets in oceans, random water and lava in this terrain.
     */
    @Override
    public void generateCaves(@NotNull WorldInfo w, @NotNull Random rnd, int cx, int cz, @NotNull ChunkData d) {
        if (!settings.caves()) return;
        TerrainSampler s = sampler(w);
        Column[][] cols = columns(w, s, cx, cz);
        int minY = w.getMinHeight();
        int lavaLevel = settings.caveLavaLevel();
        TerrainSampler.CaveScan scan = s.caveScan();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int h = s.toBlockHeight(cols[x + 1][z + 1]);
                int wx = cx * 16 + x, wz = cz * 16 + z;
                boolean sink = settings.sinkholes() && s.sinkColumn(wx, wz, h);
                scan.column(wx, wz, h);
                // without a sinkhole nothing above scan.limit() can be carved
                int yTop = sink ? h : Math.min(h, scan.limit());
                for (int y = minY + 6; y <= yTop; y++) {
                    if (!(sink && y >= h - 35) && !scan.carve(y)) continue;
                    Material m = d.getType(x, y, z);
                    if (m == Material.BEDROCK || m == Material.WATER || m == Material.LAVA
                            || m == Material.ICE || m.isAir()) continue;
                    d.setBlock(x, y, z, y <= lavaLevel ? Material.LAVA : Material.AIR);
                }
                if (settings.ventsEnabled() && s.ventAt(wx, wz)) placeVent(d, rnd, x, z, minY + 8, Math.min(h - 6, 40));
            }
        }
        release(w, cx, cz);
    }

    /** Magma block on a cave floor with 1-2 (configurable) blocks of lava directly below. */
    private void placeVent(ChunkData d, Random rnd, int x, int z, int yMin, int yMax) {
        for (int y = yMin; y <= yMax; y++) {
            Material floor = d.getType(x, y, z);
            if (floor.isAir() || floor == Material.WATER || floor == Material.LAVA || floor == Material.BEDROCK) continue;
            if (!d.getType(x, y + 1, z).isAir()) continue;
            int depth = settings.ventMinDepth() + rnd.nextInt(settings.ventMaxDepth() - settings.ventMinDepth() + 1);
            d.setBlock(x, y, z, Material.MAGMA_BLOCK);
            for (int k = 1; k <= depth; k++) {
                Material below = d.getType(x, y - k, z);
                if (below.isAir() || below == Material.BEDROCK || y - k <= yMin - 2) break;
                d.setBlock(x, y - k, z, Material.LAVA);
            }
            return;
        }
    }

    // vanilla steps that still run on top of the custom terrain
    @Override
    public boolean shouldGenerateCaves(@NotNull WorldInfo w, @NotNull Random r, int cx, int cz) {
        return false;
    }

    @Override
    public boolean shouldGenerateDecorations(@NotNull WorldInfo w, @NotNull Random r, int cx, int cz) {
        return true;
    }

    @Override
    public boolean shouldGenerateMobs(@NotNull WorldInfo w, @NotNull Random r, int cx, int cz) {
        return true;
    }

    @Override
    public boolean shouldGenerateStructures(@NotNull WorldInfo w, @NotNull Random r, int cx, int cz) {
        // no other structures inside a mountain site; the stronghold itself starts in its own chunk
        return settings.structures() && !sampler(w).sites().blocksStructures(cx, cz);
    }

    /**
     * Vanilla uses this to place structures. Without it villages and temples would be placed using the
     * vanilla noise height and end up floating or buried.
     */
    @Override
    public int getBaseHeight(@NotNull WorldInfo w, @NotNull Random rnd, int x, int z, @NotNull HeightMap heightMap) {
        int h = sampler(w).heightAt(x, z) + 1;
        if (heightMap.name().startsWith("OCEAN_FLOOR")) return h;
        return Math.max(h, settings.seaLevel() + 1);
    }

    @Override
    public @Nullable BiomeProvider getDefaultBiomeProvider(@NotNull WorldInfo w) {
        return provider;
    }

    @Override
    public @NotNull List<BlockPopulator> getDefaultPopulators(@NotNull World world) {
        return List.of(populator);
    }

    @Override
    public @Nullable Location getFixedSpawnLocation(@NotNull World world, @NotNull Random random) {
        TerrainSampler s = sampler(world);
        int sea = settings.seaLevel();
        for (int r = 0; r <= 4000; r += 32) {
            for (int i = 0; i < Math.max(1, r / 16); i++) {
                double a = i * 2 * Math.PI / Math.max(1, r / 16);
                int x = (int) (Math.cos(a) * r), z = (int) (Math.sin(a) * r);
                Column c = s.sample(x, z);
                Biome b = c.biome();
                int h = s.toBlockHeight(c);
                if (h > sea + 1 && h < sea + 40 && c.mountain() < 0.1 && c.river() < 0.05 && b != Biome.BEACH) {
                    return new Location(world, x + 0.5, h + 1, z + 0.5);
                }
            }
        }
        return new Location(world, 0.5, sea + 2, 0.5);
    }
}
