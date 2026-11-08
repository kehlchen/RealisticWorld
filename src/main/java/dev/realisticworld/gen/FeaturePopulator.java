package dev.realisticworld.gen;

import dev.realisticworld.Settings;
import org.bukkit.Axis;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Biome;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

/** Biome specific surface and underwater features. Every generated biome has at least one entry. */
final class FeaturePopulator extends BlockPopulator {
    private record Ctx(LimitedRegion r, Random rnd, TerrainSampler s, int sea, int x, int y, int z) {
    }

    private interface Feature {
        void place(Ctx c);
    }

    private record Entry(int oneIn, String kind, Feature f) {
    }

    private static final int MIN = Integer.MIN_VALUE;
    private static final Material[] CORALS = {Material.TUBE_CORAL_BLOCK, Material.BRAIN_CORAL_BLOCK,
            Material.BUBBLE_CORAL_BLOCK, Material.FIRE_CORAL_BLOCK, Material.HORN_CORAL_BLOCK};
    private static final Material[] PRAIRIE = {Material.DANDELION, Material.OXEYE_DAISY, Material.CORNFLOWER,
            Material.POPPY};
    private static final Material[] SUNNY = {Material.DANDELION, Material.DANDELION, Material.OXEYE_DAISY};
    private static final Material[] MIXED = {Material.ALLIUM, Material.AZURE_BLUET, Material.CORNFLOWER,
            Material.LILY_OF_THE_VALLEY, Material.RED_TULIP, Material.ORANGE_TULIP, Material.WHITE_TULIP,
            Material.PINK_TULIP, Material.BLUE_ORCHID, Material.OXEYE_DAISY};
    private static final Material[] MUSHROOMS = {Material.RED_MUSHROOM, Material.BROWN_MUSHROOM};
    private static final Material[] PETALS = {Material.PINK_PETALS};
    private static final Material[] MOSS = {Material.MOSS_CARPET};
    private static final Set<Material> GRASSY = Set.of(Material.GRASS_BLOCK, Material.PODZOL, Material.DIRT,
            Material.MOSS_BLOCK, Material.COARSE_DIRT, Material.ROOTED_DIRT);

    private final RealisticChunkGenerator gen;
    private final Settings st;
    private final Map<Biome, List<Entry>> reg = new HashMap<>();

    FeaturePopulator(RealisticChunkGenerator gen, Settings settings) {
        this.gen = gen;
        this.st = settings;
        register();
    }

    private void add(Biome b, int oneIn, String kind, Feature f) {
        reg.computeIfAbsent(b, k -> new ArrayList<>()).add(new Entry(oneIn, kind, f));
    }

    private static Function<Random, Material> pick(Material... m) {
        return r -> m[r.nextInt(m.length)];
    }

    private void register() {
        Function<Random, Material> stone = pick(Material.STONE, Material.STONE, Material.ANDESITE);
        Function<Random, Material> mossy = pick(Material.MOSSY_COBBLESTONE, Material.MOSSY_COBBLESTONE, Material.COBBLESTONE);
        Function<Random, Material> sandstone = pick(Material.SANDSTONE, Material.SANDSTONE, Material.CUT_SANDSTONE);
        Function<Random, Material> iceRock = pick(Material.PACKED_ICE, Material.PACKED_ICE, Material.BLUE_ICE);
        Function<Random, Material> kopje = pick(Material.GRANITE, Material.ANDESITE, Material.STONE);
        Function<Random, Material> seabed = pick(Material.STONE, Material.GRAVEL, Material.COBBLESTONE);

        // oceans and rivers
        add(Biome.OCEAN, 14, "boulder", c -> boulder(c, 1, 3, pick(Material.GRAVEL, Material.SAND, Material.STONE)));
        add(Biome.DEEP_OCEAN, 12, "boulder", c -> boulder(c, 2, 4, pick(Material.TUFF, Material.DEEPSLATE, Material.GRAVEL)));
        add(Biome.WARM_OCEAN, 3, "sea", FeaturePopulator::reef);
        add(Biome.LUKEWARM_OCEAN, 10, "boulder", c -> boulder(c, 1, 2, pick(Material.SANDSTONE, Material.SAND, Material.CLAY)));
        add(Biome.DEEP_LUKEWARM_OCEAN, 12, "boulder", c -> boulder(c, 2, 3, pick(Material.SANDSTONE, Material.GRAVEL)));
        add(Biome.COLD_OCEAN, 5, "boulder", c -> boulder(c, 1, 3, seabed));
        add(Biome.DEEP_COLD_OCEAN, 10, "boulder", c -> boulder(c, 2, 4, pick(Material.TUFF, Material.STONE)));
        add(Biome.FROZEN_OCEAN, 8, "sea", c -> iceberg(c, 3, 6));
        add(Biome.DEEP_FROZEN_OCEAN, 6, "sea", c -> iceberg(c, 5, 9));
        add(Biome.RIVER, 6, "boulder", c -> boulder(c, 1, 2, pick(Material.GRAVEL, Material.COBBLESTONE, Material.SAND)));
        add(Biome.FROZEN_RIVER, 6, "boulder", c -> boulder(c, 1, 2, pick(Material.PACKED_ICE, Material.GRAVEL)));

        // coasts
        add(Biome.BEACH, 12, "log", c -> fallenLog(c, Material.OAK_LOG, 3, 6));
        add(Biome.SNOWY_BEACH, 6, "boulder", c -> boulder(c, 1, 2, iceRock));
        add(Biome.STONY_SHORE, 6, "tower", c -> tower(c, 6, 12, 1, (t, h) -> t % 4 == 3 ? Material.ANDESITE : Material.STONE));

        // deserts and badlands
        add(Biome.DESERT, 7, "tower", c -> tower(c, 5, 11, 1, (t, h) ->
                t % 3 == 2 ? Material.CUT_SANDSTONE : (t % 3 == 1 ? Material.SANDSTONE : Material.SMOOTH_SANDSTONE)));
        add(Biome.DESERT, 10, "boulder", c -> boulder(c, 1, 3, sandstone));
        add(Biome.DESERT, 220, "oasis", FeaturePopulator::oasis);
        add(Biome.DESERT, 90, "arch", c -> arch(c, (y, h) -> y % 3 == 0 ? Material.CUT_SANDSTONE : Material.SANDSTONE));
        add(Biome.BADLANDS, 70, "arch", c -> arch(c, FeaturePopulator::terracottaBand));
        add(Biome.ERODED_BADLANDS, 60, "arch", c -> arch(c, FeaturePopulator::terracottaBand));
        add(Biome.BADLANDS, 5, "tower", FeaturePopulator::terracottaTower);
        add(Biome.ERODED_BADLANDS, 4, "tower", FeaturePopulator::terracottaTower);
        add(Biome.WOODED_BADLANDS, 8, "tower", FeaturePopulator::terracottaTower);
        add(Biome.SAVANNA, 12, "boulder", c -> boulder(c, 3, 5, kopje));
        add(Biome.SAVANNA_PLATEAU, 10, "boulder", c -> boulder(c, 3, 5, kopje));
        add(Biome.WINDSWEPT_SAVANNA, 12, "boulder", c -> boulder(c, 2, 4, kopje));

        // temperate
        add(Biome.PLAINS, 10, "pond", c -> pond(c, false));
        add(Biome.PLAINS, 4, "flowers", c -> cover(c, 4, 0.35, PRAIRIE, GRASSY));
        add(Biome.SUNFLOWER_PLAINS, 2, "flowers", c -> cover(c, 5, 0.5, SUNNY, GRASSY));
        add(Biome.SUNFLOWER_PLAINS, 14, "pond", c -> pond(c, false));
        add(Biome.FOREST, 6, "log", c -> fallenLog(c, Material.OAK_LOG, 4, 7));
        add(Biome.FOREST, 8, "flowers", c -> cover(c, 3, 0.2, MUSHROOMS, GRASSY));
        add(Biome.FLOWER_FOREST, 2, "flowers", c -> cover(c, 4, 0.5, MIXED, GRASSY));
        add(Biome.FLOWER_FOREST, 12, "log", c -> fallenLog(c, Material.OAK_LOG, 4, 6));
        add(Biome.BIRCH_FOREST, 6, "log", c -> fallenLog(c, Material.BIRCH_LOG, 4, 7));
        add(Biome.BIRCH_FOREST, 12, "flowers", c -> cover(c, 3, 0.2, MUSHROOMS, GRASSY));
        add(Biome.OLD_GROWTH_BIRCH_FOREST, 4, "log", c -> fallenLog(c, Material.BIRCH_LOG, 6, 9));
        add(Biome.DARK_FOREST, 5, "log", c -> fallenLog(c, Material.DARK_OAK_LOG, 4, 8));
        add(Biome.DARK_FOREST, 3, "flowers", c -> cover(c, 4, 0.3, MUSHROOMS, GRASSY));

        // cold forests and plains
        add(Biome.TAIGA, 8, "boulder", c -> boulder(c, 1, 3, mossy));
        add(Biome.TAIGA, 7, "log", c -> fallenLog(c, Material.SPRUCE_LOG, 4, 7));
        add(Biome.OLD_GROWTH_PINE_TAIGA, 5, "boulder", c -> boulder(c, 2, 4, mossy));
        add(Biome.OLD_GROWTH_PINE_TAIGA, 5, "log", c -> fallenLog(c, Material.SPRUCE_LOG, 5, 9));
        add(Biome.OLD_GROWTH_PINE_TAIGA, 10, "flowers", c -> cover(c, 3, 0.25, MUSHROOMS, GRASSY));
        add(Biome.OLD_GROWTH_SPRUCE_TAIGA, 6, "boulder", c -> boulder(c, 2, 4, mossy));
        add(Biome.OLD_GROWTH_SPRUCE_TAIGA, 5, "log", c -> fallenLog(c, Material.SPRUCE_LOG, 5, 9));
        add(Biome.SNOWY_TAIGA, 6, "snow", c -> snowdrift(c, 3, 5));
        add(Biome.SNOWY_TAIGA, 12, "pond", c -> pond(c, true));
        add(Biome.SNOWY_TAIGA, 10, "log", c -> fallenLog(c, Material.SPRUCE_LOG, 4, 7));
        add(Biome.SNOWY_PLAINS, 8, "pond", c -> pond(c, true));
        add(Biome.SNOWY_PLAINS, 5, "snow", c -> snowdrift(c, 3, 6));
        add(Biome.ICE_SPIKES, 6, "pond", c -> pond(c, true));
        add(Biome.ICE_SPIKES, 5, "boulder", c -> boulder(c, 1, 3, iceRock));

        // tropical and wet
        add(Biome.JUNGLE, 4, "log", c -> fallenLog(c, Material.JUNGLE_LOG, 5, 9));
        add(Biome.JUNGLE, 8, "boulder", c -> boulder(c, 1, 3, mossy));
        add(Biome.SPARSE_JUNGLE, 5, "boulder", c -> {
            boulder(c, 2, 4, mossy);
            cover(c, 5, 0.3, MOSS, GRASSY);
        });
        add(Biome.BAMBOO_JUNGLE, 8, "tower", c -> tower(c, 12, 24, 2, (t, h) ->
                t >= h - 2 ? Material.MOSS_BLOCK : (t % 5 == 4 ? Material.ANDESITE : Material.STONE)));
        add(Biome.SWAMP, 8, "log", c -> fallenLog(c, Material.OAK_LOG, 4, 7));
        add(Biome.SWAMP, 6, "flowers", c -> cover(c, 3, 0.25, MUSHROOMS, GRASSY));
        add(Biome.MANGROVE_SWAMP, 8, "log", c -> fallenLog(c, Material.MANGROVE_LOG, 4, 7));

        // highlands
        add(Biome.MEADOW, 2, "flowers", c -> cover(c, 5, 0.45, MIXED, GRASSY));
        add(Biome.MEADOW, 12, "pond", c -> pond(c, false));
        add(Biome.MEADOW, 12, "boulder", c -> boulder(c, 1, 3, stone));
        add(Biome.CHERRY_GROVE, 3, "flowers", c -> cover(c, 5, 0.4, PETALS, GRASSY));
        add(Biome.CHERRY_GROVE, 10, "pond", c -> pond(c, false));
        add(Biome.CHERRY_GROVE, 10, "log", c -> fallenLog(c, Material.CHERRY_LOG, 4, 7));
        add(Biome.GROVE, 10, "boulder", c -> boulder(c, 1, 3, stone));
        add(Biome.GROVE, 8, "log", c -> fallenLog(c, Material.SPRUCE_LOG, 4, 7));
        add(Biome.SNOWY_SLOPES, 6, "snow", c -> snowdrift(c, 3, 6));
        add(Biome.SNOWY_SLOPES, 14, "boulder", c -> boulder(c, 1, 3, stone));
        add(Biome.JAGGED_PEAKS, 8, "boulder", c -> boulder(c, 2, 4, stone));
        add(Biome.FROZEN_PEAKS, 5, "boulder", c -> boulder(c, 2, 4, iceRock));
        add(Biome.STONY_PEAKS, 6, "boulder", c -> boulder(c, 2, 4, pick(Material.ANDESITE, Material.CALCITE, Material.STONE)));
        add(Biome.WINDSWEPT_HILLS, 4, "boulder", c -> boulder(c, 1, 3, stone));
        add(Biome.WINDSWEPT_FOREST, 6, "boulder", c -> boulder(c, 1, 3, stone));
        add(Biome.WINDSWEPT_FOREST, 8, "log", c -> fallenLog(c, Material.SPRUCE_LOG, 4, 7));
        add(Biome.WINDSWEPT_GRAVELLY_HILLS, 3, "boulder", c -> boulder(c, 1, 3, pick(Material.GRAVEL, Material.ANDESITE, Material.STONE)));
    }

    @Override
    public void populate(@NotNull WorldInfo info, @NotNull Random rnd, int cx, int cz, @NotNull LimitedRegion region) {
        TerrainSampler s = gen.sampler(info);
        int sea = st.seaLevel();

        if (st.biomeFeatures()) {
            for (int attempt = 0; attempt < 4; attempt++) {
                int x = cx * 16 + 3 + rnd.nextInt(10), z = cz * 16 + 3 + rnd.nextInt(10);
                Column col = s.sample(x, z);
                List<Entry> entries = reg.get(col.biome());
                if (entries == null) continue;
                int y = s.toBlockHeight(col);
                Ctx c = new Ctx(region, rnd, s, sea, x, y, z);
                for (Entry e : entries) {
                    if (!st.featureEnabled(e.kind)) continue;
                    if (rnd.nextInt(e.oneIn) == 0 && region.isInRegion(x, y, z)) e.f.place(c);
                }
            }
        }

        for (int attempt = 0; attempt < (st.cavePools() ? 2 : 0); attempt++) {
            caveFeature(region, rnd, s, info.getMinHeight(), cx, cz);
        }

        if (st.removeVanillaLava()) removeLava(region, s, info.getMinHeight(), cx, cz);
    }

    /**
     * Vanilla lava lakes ignore biomes and, with a tall world, can appear at any height (for example inside
     * snow mountains). Lava above the deep layer is removed again; deep lava (below y -40) is left alone.
     */
    private void removeLava(LimitedRegion r, TerrainSampler s, int minY, int cx, int cz) {
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                int x = cx * 16 + 2 + i * 4, z = cz * 16 + 2 + j * 4;
                Column col = s.sample(x, z);
                int h = s.toBlockHeight(col);
                for (int y = Math.min(h + 2, 500); y > Math.max(-40, minY); y--) {
                    if (r.isInRegion(x, y, z) && r.getType(x, y, z) == Material.LAVA && !ventLava(r, x, y, z)) {
                        fillLava(r, s, x, y, z);
                    }
                }
            }
        }
    }

    /** Lava directly below a magma block belongs to a magma vent and must stay. */
    private static boolean ventLava(LimitedRegion r, int x, int y, int z) {
        for (int k = 1; k <= 4; k++) {
            if (!r.isInRegion(x, y + k, z)) return false;
            Material t = r.getType(x, y + k, z);
            if (t == Material.MAGMA_BLOCK) return true;
            if (t != Material.LAVA) return false;
        }
        return false;
    }

    private static Material lavaReplacement(Biome b, int y, int h) {
        if (y < h - 2) return Material.STONE;
        if (b == Biome.DESERT) return Material.SAND;
        if (b == Biome.BADLANDS || b == Biome.ERODED_BADLANDS || b == Biome.WOODED_BADLANDS) return Material.RED_SAND;
        if (b == Biome.SNOWY_PLAINS || b == Biome.SNOWY_TAIGA || b == Biome.SNOWY_SLOPES || b == Biome.JAGGED_PEAKS
                || b == Biome.FROZEN_PEAKS || b == Biome.GROVE || b == Biome.ICE_SPIKES) return Material.SNOW_BLOCK;
        if (b == Biome.STONY_PEAKS || b == Biome.WINDSWEPT_HILLS || b == Biome.WINDSWEPT_GRAVELLY_HILLS) return Material.STONE;
        return Material.GRASS_BLOCK;
    }

    private static void fillLava(LimitedRegion r, TerrainSampler s, int x0, int y0, int z0) {
        java.util.ArrayDeque<int[]> queue = new java.util.ArrayDeque<>();
        queue.add(new int[]{x0, y0, z0});
        r.setType(x0, y0, z0, Material.STONE);
        int budget = 4000;
        int[][] dirs = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
        while (!queue.isEmpty() && budget-- > 0) {
            int[] p = queue.poll();
            for (int[] d : dirs) {
                int nx = p[0] + d[0], ny = p[1] + d[1], nz = p[2] + d[2];
                if (!r.isInRegion(nx, ny, nz) || r.getType(nx, ny, nz) != Material.LAVA || ventLava(r, nx, ny, nz)) continue;
                Column col = s.sample(nx, nz);
                r.setType(nx, ny, nz, lavaReplacement(col.biome(), ny, s.toBlockHeight(col)));
                queue.add(new int[]{nx, ny, nz});
            }
        }
        Column col = s.sample(x0, z0);
        r.setType(x0, y0, z0, lavaReplacement(col.biome(), y0, s.toBlockHeight(col)));
    }

    // ---------------------------------------------------------------- helpers

    private static int surfaceY(LimitedRegion r, int x, int z, int hint) {
        for (int y = hint + 6; y >= hint - 6; y--) {
            if (!r.isInRegion(x, y, z)) return MIN;
            Material m = r.getType(x, y, z);
            if (m.isAir() || !m.isSolid()) continue;
            if (Tag.LEAVES.isTagged(m) || Tag.LOGS.isTagged(m)) return MIN;
            return y;
        }
        return MIN;
    }

    private static void set(LimitedRegion r, int x, int y, int z, Material m) {
        if (r.isInRegion(x, y, z)) r.setType(x, y, z, m);
    }

    private static boolean replaceable(Material m) {
        return m.isAir() || m == Material.WATER || !m.isSolid();
    }

    // ---------------------------------------------------------------- features

    private static void boulder(Ctx c, int rMin, int rMax, Function<Random, Material> mat) {
        int r = rMin + c.rnd.nextInt(rMax - rMin + 1);
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -1; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    double dist = dx * dx + dy * dy * 1.3 + dz * dz;
                    if (dist <= r * r + c.rnd.nextInt(2)) {
                        set(c.r, c.x + dx, c.y + dy, c.z + dz, mat.apply(c.rnd));
                    }
                }
            }
        }
    }

    private static void tower(Ctx c, int hMin, int hMax, int rBase, BiFunction<Integer, Integer, Material> pal) {
        int height = hMin + c.rnd.nextInt(hMax - hMin + 1);
        for (int t = 0; t < height; t++) {
            int r = (t < 2 || t >= height - 2) ? rBase + 1 : rBase;
            Material m = pal.apply(t, height);
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dz * dz > r * r + 1) continue;
                    set(c.r, c.x + dx, c.y + t, c.z + dz, m);
                }
            }
        }
    }

    private static void terracottaTower(Ctx c) {
        Material[] pal = {Material.ORANGE_TERRACOTTA, Material.TERRACOTTA, Material.YELLOW_TERRACOTTA,
                Material.WHITE_TERRACOTTA, Material.RED_TERRACOTTA};
        int off = c.rnd.nextInt(pal.length);
        tower(c, 5, 12, 1, (t, h) -> pal[(t + off) % pal.length]);
    }

    private static void fallenLog(Ctx c, Material log, int minLen, int maxLen) {
        int len = minLen + c.rnd.nextInt(maxLen - minLen + 1);
        boolean alongX = c.rnd.nextBoolean();
        BlockData data = log.createBlockData();
        if (data instanceof Orientable o) o.setAxis(alongX ? Axis.X : Axis.Z);
        int placed = 0;
        for (int i = 0; i < len; i++) {
            int px = c.x + (alongX ? i : 0), pz = c.z + (alongX ? 0 : i);
            int sy = surfaceY(c.r, px, pz, c.y);
            if (sy == MIN) break;
            if (!c.r.isInRegion(px, sy + 1, pz) || !replaceable(c.r.getType(px, sy + 1, pz))) break;
            c.r.setBlockData(px, sy + 1, pz, data);
            placed++;
        }
    }

    private static void cover(Ctx c, int radius, double density, Material[] mats, Set<Material> ground) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius || c.rnd.nextDouble() > density) continue;
                int px = c.x + dx, pz = c.z + dz;
                int sy = surfaceY(c.r, px, pz, c.y);
                if (sy == MIN || !ground.contains(c.r.getType(px, sy, pz))) continue;
                Material above = c.r.getType(px, sy + 1, pz);
                if (above.isAir() || above == Material.SHORT_GRASS || above == Material.FERN) {
                    set(c.r, px, sy + 1, pz, mats[c.rnd.nextInt(mats.length)]);
                }
            }
        }
    }

    private static void snowdrift(Ctx c, int rMin, int rMax) {
        int r = rMin + c.rnd.nextInt(rMax - rMin + 1);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > r) continue;
                int hgt = (int) Math.round((1 - dist / r) * 2.5 + c.rnd.nextDouble() * 0.5);
                int px = c.x + dx, pz = c.z + dz;
                int sy = surfaceY(c.r, px, pz, c.y);
                if (sy == MIN) continue;
                for (int k = 1; k <= hgt; k++) {
                    if (c.r.isInRegion(px, sy + k, pz) && replaceable(c.r.getType(px, sy + k, pz))) {
                        c.r.setType(px, sy + k, pz, Material.SNOW_BLOCK);
                    }
                }
            }
        }
    }

    private static void pond(Ctx c, boolean frozen) {
        int r = 3 + c.rnd.nextInt(3);
        if (c.y <= c.sea + 1) return;
        // water level = lowest ground in the outer ring, so no water edge ever hangs over lower terrain
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        // heights are kept for the fill loop below (its cells are a subset of this ring area)
        int side = 2 * r + 3;
        int[] heights = new int[side * side];
        for (int dx = -r - 1; dx <= r + 1; dx++) {
            for (int dz = -r - 1; dz <= r + 1; dz++) {
                double e = (dx * dx) / (double) (r * r) + (dz * dz) / (r * r * 0.5);
                if (e > 1.3) continue;
                int hh = c.s.heightAt(c.x + dx, c.z + dz);
                heights[(dx + r + 1) * side + dz + r + 1] = hh;
                min = Math.min(min, hh);
                max = Math.max(max, hh);
            }
        }
        if (max - min > 1) return;
        int wy = min;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                double e = (dx * dx) / (double) (r * r) + (dz * dz) / (r * r * 0.5);
                if (e > 0.64) continue;
                int px = c.x + dx, pz = c.z + dz;
                int hh = heights[(dx + r + 1) * side + dz + r + 1];
                if (!c.r.isInRegion(px, hh + 3, pz) || !c.r.isInRegion(px, wy - 2, pz)) continue;
                Material above = c.r.getType(px, hh + 1, pz);
                if (Tag.LOGS.isTagged(above) || Tag.LEAVES.isTagged(above)) continue;
                for (int yy = hh + 3; yy > wy; yy--) c.r.setType(px, yy, pz, Material.AIR);
                c.r.setType(px, wy, pz, frozen ? Material.ICE : Material.WATER);
                c.r.setType(px, wy - 1, pz, Material.WATER);
                c.r.setType(px, wy - 2, pz, frozen ? Material.GRAVEL : (c.rnd.nextBoolean() ? Material.CLAY : Material.MUD));
            }
        }
    }

    private static void iceberg(Ctx c, int rMin, int rMax) {
        int a = rMin + c.rnd.nextInt(rMax - rMin + 1);
        int hgt = Math.max(2, (int) (a / 1.5));
        for (int dx = -a; dx <= a; dx++) {
            for (int dz = -a; dz <= a; dz++) {
                for (int dy = -(int) (hgt * 0.8); dy <= hgt; dy++) {
                    double e = (dx * dx + dz * dz) / (double) (a * a) + (dy * dy) / (double) (hgt * hgt);
                    if (e > 1.0) continue;
                    int px = c.x + dx, py = c.sea + dy, pz = c.z + dz;
                    if (!c.r.isInRegion(px, py, pz)) continue;
                    Material cur = c.r.getType(px, py, pz);
                    if (cur == Material.WATER || cur == Material.ICE || cur.isAir()) {
                        Material m = dy >= hgt - 1 ? Material.SNOW_BLOCK
                                : (c.rnd.nextInt(5) == 0 ? Material.BLUE_ICE : Material.PACKED_ICE);
                        c.r.setType(px, py, pz, m);
                    }
                }
            }
        }
    }

    private static void reef(Ctx c) {
        int r = 2 + c.rnd.nextInt(3);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > r) continue;
                int hgt = (int) Math.round((1 - dist / r) * 3 + c.rnd.nextDouble() * 0.8);
                int px = c.x + dx, pz = c.z + dz;
                for (int k = 1; k <= hgt; k++) {
                    if (c.r.isInRegion(px, c.y + k, pz) && c.r.getType(px, c.y + k, pz) == Material.WATER) {
                        c.r.setType(px, c.y + k, pz, CORALS[c.rnd.nextInt(CORALS.length)]);
                    }
                }
            }
        }
    }


    // ---------------------------------------------------------------- oasis and rock arch

    private static final Material[] BANDS = {Material.ORANGE_TERRACOTTA, Material.TERRACOTTA, Material.YELLOW_TERRACOTTA,
            Material.WHITE_TERRACOTTA, Material.RED_TERRACOTTA, Material.BROWN_TERRACOTTA};

    private static Material terracottaBand(int y, int h) {
        return BANDS[Math.floorMod(y, BANDS.length)];
    }

    /** Small pond on flat desert ground with a grass ring, a few short palms and ferns. */
    private static void oasis(Ctx c) {
        if (c.y <= c.sea + 1) return;
        int r = 4 + c.rnd.nextInt(3);
        int ext = r + 3;
        int side = 2 * ext + 1;
        int[] heights = new int[side * side];
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int dx = -ext; dx <= ext; dx++) {
            for (int dz = -ext; dz <= ext; dz++) {
                int hh = c.s.heightAt(c.x + dx, c.z + dz);
                heights[(dx + ext) * side + dz + ext] = hh;
                double e = oasisShape(dx, dz, r);
                if (e <= 1.8) {
                    min = Math.min(min, hh);
                    max = Math.max(max, hh);
                }
            }
        }
        if (max - min > 2 || min <= c.sea) return;
        int wy = min;
        List<int[]> ring = new ArrayList<>();
        for (int dx = -ext; dx <= ext; dx++) {
            for (int dz = -ext; dz <= ext; dz++) {
                double e = oasisShape(dx, dz, r);
                int px = c.x + dx, pz = c.z + dz;
                int hh = heights[(dx + ext) * side + dz + ext];
                if (e <= 1.0) {
                    if (!c.r.isInRegion(px, hh + 3, pz) || !c.r.isInRegion(px, wy - 2, pz)) continue;
                    for (int yy = hh + 3; yy > wy; yy--) c.r.setType(px, yy, pz, Material.AIR);
                    c.r.setType(px, wy, pz, Material.WATER);
                    c.r.setType(px, wy - 1, pz, Material.WATER);
                    c.r.setType(px, wy - 2, pz, c.rnd.nextInt(3) == 0 ? Material.CLAY : Material.SAND);
                } else if (e <= 1.7) {
                    if (!c.r.isInRegion(px, hh + 2, pz)) continue;
                    int sy = surfaceY(c.r, px, pz, hh);
                    if (sy == MIN) continue;
                    set(c.r, px, sy, pz, Material.GRASS_BLOCK);
                    set(c.r, px, sy - 1, pz, Material.DIRT);
                    if (c.r.getType(px, sy + 1, pz).isAir() && c.rnd.nextInt(3) == 0) {
                        set(c.r, px, sy + 1, pz, c.rnd.nextInt(4) == 0 ? Material.FERN : Material.SHORT_GRASS);
                    }
                    if (e > 1.15) ring.add(new int[]{px, sy, pz});
                }
            }
        }
        int palms = 2 + c.rnd.nextInt(3);
        for (int i = 0; i < palms && !ring.isEmpty(); i++) {
            int[] p = ring.remove(c.rnd.nextInt(ring.size()));
            palm(c, p[0], p[1], p[2]);
        }
    }

    private static double oasisShape(int dx, int dz, int r) {
        return (dx * dx) / (double) (r * r) + (dz * dz) / (r * r * 0.7);
    }

    private static void leaf(Ctx c, int x, int y, int z) {
        if (!c.r.isInRegion(x, y, z) || !c.r.getType(x, y, z).isAir()) return;
        BlockData d = Material.JUNGLE_LEAVES.createBlockData();
        if (d instanceof Leaves l) l.setPersistent(true);
        c.r.setBlockData(x, y, z, d);
    }

    private static void palm(Ctx c, int x, int sy, int z) {
        int h = 4 + c.rnd.nextInt(3);
        for (int t = 1; t <= h; t++) {
            if (!c.r.isInRegion(x, sy + t, z) || !replaceable(c.r.getType(x, sy + t, z))) return;
            c.r.setType(x, sy + t, z, Material.JUNGLE_LOG);
        }
        int top = sy + h;
        leaf(c, x, top + 1, z);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                leaf(c, x + dx, top, z + dz);
                leaf(c, x + dx * 2, top, z + dz * 2);
                leaf(c, x + dx * 2, top - 1, z + dz * 2);
            }
        }
    }

    /** Natural stone arch: two feet on flat ground joined by a curved span. */
    private static void arch(Ctx c, BiFunction<Integer, Integer, Material> pal) {
        if (c.y <= c.sea + 1) return;
        int span = 7 + c.rnd.nextInt(5);
        int rise = 6 + c.rnd.nextInt(4);
        boolean alongX = c.rnd.nextBoolean();
        int half = span / 2;
        for (int a : new int[]{-half, half}) {
            for (int w = -1; w <= 1; w++) {
                int hh = c.s.heightAt(c.x + (alongX ? a : w), c.z + (alongX ? w : a));
                if (Math.abs(hh - c.y) > 2 || hh <= c.sea) return;
            }
        }
        for (int a = -half; a <= half; a++) {
            double t = a / (double) half;
            int topY = c.y + (int) Math.round(rise * Math.sqrt(Math.max(0, 1 - t * t)));
            int thick = Math.abs(a) >= half - 1 ? rise : 2;
            for (int w = -1; w <= 1; w++) {
                if (Math.abs(w) == 1 && Math.abs(a) < half - 1 && c.rnd.nextInt(3) == 0) continue;
                int px = c.x + (alongX ? a : w), pz = c.z + (alongX ? w : a);
                int ground = c.s.heightAt(px, pz);
                int from = Math.abs(a) >= half - 1 ? ground - 1 : topY - thick + 1;
                for (int y = from; y <= topY + 1; y++) set(c.r, px, y, pz, pal.apply(y, topY));
            }
        }
    }

    // ---------------------------------------------------------------- caves

    private void caveFeature(LimitedRegion r, Random rnd, TerrainSampler s, int minY, int cx, int cz) {
        int x = cx * 16 + 3 + rnd.nextInt(10), z = cz * 16 + 3 + rnd.nextInt(10);
        Column col = s.sample(x, z);
        int span = (int) col.height() - minY - 40;
        if (span < 10) return;
        int y = minY + 14 + rnd.nextInt(span);
        Biome ub = s.undergroundBiome(col, x, y, z);
        if (ub == null) return;

        int floor = MIN;
        for (int yy = y; yy > y - 14; yy--) {
            if (!r.isInRegion(x, yy, z)) return;
            if (r.getType(x, yy, z).isAir() && r.getType(x, yy - 1, z).isSolid()) {
                floor = yy - 1;
                break;
            }
        }
        if (floor == MIN || !r.getType(x, floor + 1, z).isAir()) return;

        if (ub == Biome.LUSH_CAVES) {
            // small clay-lined pool
            int rad = 1 + rnd.nextInt(2);
            for (int dx = -rad; dx <= rad; dx++) {
                for (int dz = -rad; dz <= rad; dz++) {
                    if (dx * dx + dz * dz > rad * rad + 1) continue;
                    int px = x + dx, pz = z + dz;
                    if (!r.isInRegion(px, floor, pz)) continue;
                    if (r.getType(px, floor, pz).isSolid() && r.getType(px, floor + 1, pz).isAir()) {
                        r.setType(px, floor, pz, Material.WATER);
                        if (r.getType(px, floor - 1, pz).isSolid()) r.setType(px, floor - 1, pz, Material.CLAY);
                    }
                }
            }
        } else if (ub == Biome.DRIPSTONE_CAVES) {
            // dripstone mound with calcite streaks
            int rad = 2 + rnd.nextInt(2);
            for (int dx = -rad; dx <= rad; dx++) {
                for (int dz = -rad; dz <= rad; dz++) {
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    if (dist > rad) continue;
                    int hgt = (int) Math.round((1 - dist / rad) * 2.5);
                    for (int k = 1; k <= hgt; k++) {
                        int px = x + dx, py = floor + k, pz = z + dz;
                        if (r.isInRegion(px, py, pz) && r.getType(px, py, pz).isAir()) {
                            r.setType(px, py, pz, rnd.nextInt(4) == 0 ? Material.CALCITE : Material.DRIPSTONE_BLOCK);
                        }
                    }
                }
            }
        }
    }
}
