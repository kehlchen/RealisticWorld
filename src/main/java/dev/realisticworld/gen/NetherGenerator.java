package dev.realisticworld.gen;

import dev.realisticworld.Settings;
import org.bukkit.HeightMap;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class NetherGenerator extends ChunkGenerator {
    private static final int ROOF = 127;

    private final Settings settings;
    private final Map<UUID, NetherSampler> samplers = new ConcurrentHashMap<>();
    private final BiomeProvider provider = new NetherBiomeProvider(this);
    private final NetherPopulator populator;

    public NetherGenerator(Settings settings) {
        this.settings = settings;
        this.populator = new NetherPopulator(this, settings);
    }

    /** Config style biome key at a position (nether_wastes, soul_sand_valley, ...). */
    public String biomeKeyAt(WorldInfo w, int x, int z) {
        Biome b = sampler(w).biomeAt(x, z);
        if (b == Biome.SOUL_SAND_VALLEY) return "soul_sand_valley";
        if (b == Biome.CRIMSON_FOREST) return "crimson_forest";
        if (b == Biome.WARPED_FOREST) return "warped_forest";
        if (b == Biome.BASALT_DELTAS) return "basalt_deltas";
        return "nether_wastes";
    }

    public static java.util.List<String> biomeKeys() {
        return dev.realisticworld.Settings.biomeKeys("nether");
    }

    NetherSampler sampler(WorldInfo w) {
        return samplers.computeIfAbsent(w.getUID(), id -> new NetherSampler(w.getSeed(), settings));
    }

    @Override
    public void generateNoise(@NotNull WorldInfo w, @NotNull Random rnd, int cx, int cz, @NotNull ChunkData d) {
        NetherSampler s = sampler(w);
        int minY = w.getMinHeight();
        int top = Math.min(ROOF, w.getMaxHeight() - 1);
        Noise.Vertical iv = new Noise.Vertical();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int wx = cx * 16 + x, wz = cz * 16 + z;
                NetherSampler.Col c = s.col(wx, wz);
                // same result as testing s.solid(c, wx, y, wz) for every y, but written as runs
                if (s.pillarColumn(c)) {
                    d.setRegion(x, minY, z, x + 1, top + 1, z + 1, Material.NETHERRACK);
                    continue;
                }
                if (c.floor() >= minY) d.setRegion(x, minY, z, x + 1, Math.min(c.floor(), top) + 1, z + 1, Material.NETHERRACK);
                if (c.ceil() <= top) d.setRegion(x, Math.max(c.ceil(), minY), z, x + 1, top + 1, z + 1, Material.NETHERRACK);
                int lo = Math.max(minY, c.floor() + 1), hi = Math.min(top, c.ceil() - 1);
                if (lo <= hi && lo <= s.lava) d.setRegion(x, lo, z, x + 1, Math.min(hi, s.lava) + 1, z + 1, Material.LAVA);
                if (s.hasIslands()) {
                    int iLo = Math.max(lo, c.floor() + 13), iHi = Math.min(hi, c.ceil() - 13);
                    if (iLo <= iHi) {
                        s.islandColumn(iv, wx, wz);
                        for (int y = iLo; y <= iHi; y++) {
                            if (s.island(iv, c, y)) d.setBlock(x, y, z, Material.NETHERRACK);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void generateSurface(@NotNull WorldInfo w, @NotNull Random rnd, int cx, int cz, @NotNull ChunkData d) {
        NetherSampler s = sampler(w);
        int minY = Math.max(5, w.getMinHeight() + 5);
        int top = Math.min(ROOF - 6, w.getMaxHeight() - 7);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int wx = cx * 16 + x, wz = cz * 16 + z;
                Biome b = s.biomeAt(wx, wz);
                double p = s.patch(wx, wz), p2 = s.patch2(wx, wz);
                // sliding window over (down, cur, up): one getType per height instead of three.
                // Writes only touch y, y - 1 and y - 2, so cur and up are still unmodified when they are read.
                Material down = d.getType(x, minY - 1, z);
                Material cur = d.getType(x, minY, z);
                for (int y = minY; y <= top; y++) {
                    Material up = d.getType(x, y + 1, z);
                    Material after = cur;
                    if (cur == Material.NETHERRACK) {
                        if (up.isAir() || up == Material.LAVA) {
                            Material m = floorMaterial(b, y, s.lava, p, p2);
                            d.setBlock(x, y, z, m);
                            after = m;
                            if (m == Material.MAGMA_BLOCK && settings.magmaEnabled()) {
                                // lava directly under magma blocks (configurable depth)
                                int depth = settings.magmaMinDepth() + rnd.nextInt(settings.magmaMaxDepth() - settings.magmaMinDepth() + 1);
                                for (int k = 1; k <= depth; k++) {
                                    if (d.getType(x, y - k, z) == Material.NETHERRACK || d.getType(x, y - k, z) == Material.BASALT
                                            || d.getType(x, y - k, z) == Material.BLACKSTONE) {
                                        d.setBlock(x, y - k, z, Material.LAVA);
                                    }
                                }
                            } else {
                                Material sub = subMaterial(b, m);
                                for (int k = 1; k <= 2; k++) {
                                    if (d.getType(x, y - k, z) == Material.NETHERRACK) d.setBlock(x, y - k, z, sub);
                                }
                            }
                        } else if (down.isAir()) {
                            Material m = ceilMaterial(b, p);
                            if (m != Material.NETHERRACK) {
                                d.setBlock(x, y, z, m);
                                after = m;
                            }
                        }
                    }
                    down = after;
                    cur = up;
                }
            }
        }
    }

    private Material floorMaterial(Biome b, int y, int lava, double p, double p2) {
        boolean shore = y <= lava + 2;
        boolean magmaOk = settings.magmaEnabled() && settings.magmaPatches() > 0
                && (b == Biome.NETHER_WASTES || b == Biome.BASALT_DELTAS);
        if (magmaOk && !shore && p > 0.85 - 0.35 * settings.magmaPatches()) return Material.MAGMA_BLOCK;
        if (b == Biome.CRIMSON_FOREST) return shore ? Material.NETHERRACK : Material.CRIMSON_NYLIUM;
        if (b == Biome.WARPED_FOREST) return shore ? Material.NETHERRACK : Material.WARPED_NYLIUM;
        if (b == Biome.SOUL_SAND_VALLEY) return p > 0 ? Material.SOUL_SAND : Material.SOUL_SOIL;
        if (b == Biome.BASALT_DELTAS) {
            if (shore) return p2 > 0.2 ? Material.MAGMA_BLOCK : Material.BASALT;
            return p > 0.3 ? Material.BLACKSTONE : Material.BASALT;
        }
        if (shore) return p > 0.2 ? Material.BLACKSTONE : (p2 > 0.6 ? Material.MAGMA_BLOCK : Material.NETHERRACK);
        if (p > 0.6) return Material.SOUL_SAND;
        if (p2 > 0.7) return Material.GRAVEL;
        return Material.NETHERRACK;
    }

    private static Material subMaterial(Biome b, Material top) {
        if (b == Biome.SOUL_SAND_VALLEY) return Material.SOUL_SOIL;
        if (b == Biome.BASALT_DELTAS) return Material.BASALT;
        return Material.NETHERRACK;
    }

    private static Material ceilMaterial(Biome b, double p) {
        if (b == Biome.BASALT_DELTAS) return p > 0 ? Material.BLACKSTONE : Material.BASALT;
        return Material.NETHERRACK;
    }

    @Override
    public void generateBedrock(@NotNull WorldInfo w, @NotNull Random rnd, int cx, int cz, @NotNull ChunkData d) {
        int minY = w.getMinHeight();
        int top = Math.min(ROOF, w.getMaxHeight() - 1);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int i = 0; i < 5; i++) {
                    if (i == 0 || rnd.nextDouble() < (5 - i) / 5.0 * 0.8) {
                        d.setBlock(x, minY + i, z, Material.BEDROCK);
                        d.setBlock(x, top - i, z, Material.BEDROCK);
                    }
                }
            }
        }
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
        return settings.structures();
    }

    @Override
    public int getBaseHeight(@NotNull WorldInfo w, @NotNull Random rnd, int x, int z, @NotNull HeightMap heightMap) {
        NetherSampler s = sampler(w);
        return Math.max(s.col(x, z).floor(), s.lava) + 1;
    }

    @Override
    public @Nullable BiomeProvider getDefaultBiomeProvider(@NotNull WorldInfo w) {
        return provider;
    }

    @Override
    public @NotNull List<BlockPopulator> getDefaultPopulators(@NotNull World world) {
        return List.of(populator);
    }
}
