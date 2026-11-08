package dev.realisticworld.gen;

import dev.realisticworld.Settings;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

/** Features of the new End zones: chorus groves, amethyst spires, obsidian spires, purpur ruins, dragon bone ribs. */
final class EndPopulator extends BlockPopulator {
    private static final int NONE = Integer.MIN_VALUE;

    private final EndGenerator gen;
    private final Settings st;
    private final World world;

    EndPopulator(EndGenerator gen, Settings st, World world) {
        this.gen = gen;
        this.st = st;
        this.world = world;
    }

    private record Ctx(LimitedRegion r, int minY, int maxY) {
        void set(int x, int y, int z, Material m) {
            if (y <= minY || y >= maxY - 1) return;
            if (r.isInRegion(x, y, z)) r.setType(x, y, z, m);
        }

        boolean air(int x, int y, int z) {
            return r.isInRegion(x, y, z) && r.getType(x, y, z).isAir();
        }
    }

    private static int groundY(EndSampler s, int x, int z) {
        EndSampler.Col c = s.col(x, z);
        return c.ground() ? c.top() : NONE;
    }

    @Override
    public void populate(@NotNull WorldInfo info, @NotNull Random rnd, int cx, int cz, @NotNull LimitedRegion region) {
        if (!st.endCustomBiomes()) return;
        EndSampler s = gen.sampler(info);
        Ctx c = new Ctx(region, info.getMinHeight(), info.getMaxHeight());
        for (int attempt = 0; attempt < 6; attempt++) {
            int x = cx * 16 + 3 + rnd.nextInt(10), z = cz * 16 + 3 + rnd.nextInt(10);
            if (Math.hypot(x, z) < s.zoneStart()) continue;
            int y = groundY(s, x, z);
            if (y == NONE || !region.isInRegion(x, y, z)) continue;
            Material ground = region.getType(x, y, z);
            if (!ground.isSolid() || !c.air(x, y + 1, z)) continue;

            switch (s.zone(x, z)) {
                case CHORUS_FOREST -> {
                    if (st.endChorus() && ground == Material.END_STONE && rnd.nextInt(100) < 55) {
                        region.generateTree(new Location(world, x, y + 1, z), rnd, TreeType.CHORUS_PLANT);
                    }
                }
                case CRYSTAL_FIELDS -> {
                    if (st.endCrystals() && rnd.nextInt(100) < 18) crystalSpire(c, s, rnd, x, y, z);
                }
                case OBSIDIAN_WASTES -> {
                    if (st.endObsidian() && rnd.nextInt(100) < 15) obsidianSpire(c, rnd, x, y, z);
                }
                case PURPUR_RUINS -> {
                    if (st.endRuins() && rnd.nextInt(100) < 5) colonnade(c, s, rnd, x, y, z);
                }
                case ENDER_DUNES -> {
                    if (st.endBones() && rnd.nextInt(100) < 3) boneRib(c, rnd, x, y, z);
                }
                default -> {
                }
            }
        }
    }

    private static void crystalSpire(Ctx c, EndSampler s, Random rnd, int x, int y, int z) {
        int h = 5 + rnd.nextInt(11);
        int rad = h >= 10 ? 2 : 1;
        for (int t = 1; t <= h; t++) {
            int rr = t <= h / 3 ? rad : (t <= 2 * h / 3 ? Math.max(0, rad - 1) : 0);
            for (int dx = -rr; dx <= rr; dx++) {
                for (int dz = -rr; dz <= rr; dz++) {
                    if (dx * dx + dz * dz > rr * rr + 1) continue;
                    c.set(x + dx, y + t, z + dz, rnd.nextInt(7) == 0 ? Material.CALCITE : Material.AMETHYST_BLOCK);
                }
            }
        }
        c.set(x, y + h + 1, z, Material.AMETHYST_CLUSTER);
        // small clusters on the ground around the spire
        for (int i = 0; i < 4; i++) {
            int px = x + rnd.nextInt(9) - 4, pz = z + rnd.nextInt(9) - 4;
            int gy = groundY(s, px, pz);
            if (gy != NONE && c.air(px, gy + 1, pz)) c.set(px, gy + 1, pz, Material.AMETHYST_CLUSTER);
        }
    }

    private static void obsidianSpire(Ctx c, Random rnd, int x, int y, int z) {
        int h = 7 + rnd.nextInt(14);
        int rad = h >= 14 ? 2 : 1;
        for (int t = 1; t <= h; t++) {
            int rr = t <= h / 3 ? rad : (t < h - 2 ? Math.max(0, rad - 1) : 0);
            for (int dx = -rr; dx <= rr; dx++) {
                for (int dz = -rr; dz <= rr; dz++) {
                    if (dx * dx + dz * dz > rr * rr + 1) continue;
                    c.set(x + dx, y + t, z + dz, rnd.nextInt(6) == 0 ? Material.CRYING_OBSIDIAN : Material.OBSIDIAN);
                }
            }
        }
    }

    /** A row of broken purpur pillars, some of them joined by a beam. */
    private static void colonnade(Ctx c, EndSampler s, Random rnd, int x, int y, int z) {
        int n = 2 + rnd.nextInt(3);
        boolean alongX = rnd.nextBoolean();
        int[] top = new int[n];
        int[] px = new int[n];
        int[] pz = new int[n];
        for (int i = 0; i < n; i++) {
            px[i] = x + (alongX ? i * 4 : 0);
            pz[i] = z + (alongX ? 0 : i * 4);
            int gy = groundY(s, px[i], pz[i]);
            if (gy == NONE) {
                top[i] = NONE;
                continue;
            }
            int h = 4 + rnd.nextInt(6);
            int built = 0;
            for (int t = 1; t <= h; t++) {
                if (t > h - 2 && rnd.nextInt(3) == 0) break;
                c.set(px[i], gy + t, pz[i], Material.PURPUR_PILLAR);
                built = t;
            }
            top[i] = gy + built;
        }
        for (int i = 0; i < n - 1; i++) {
            if (top[i] == NONE || top[i + 1] == NONE) continue;
            if (top[i] < y + 6 || top[i + 1] < y + 6 || rnd.nextInt(3) == 0) continue;
            int by = Math.min(top[i], top[i + 1]);
            for (int k = 0; k <= 4; k++) {
                c.set(px[i] + (alongX ? k : 0), by, pz[i] + (alongX ? 0 : k), Material.PURPUR_BLOCK);
            }
        }
    }

    /** Rib of a long dead dragon. */
    private static void boneRib(Ctx c, Random rnd, int x, int y, int z) {
        int radius = 3 + rnd.nextInt(3);
        boolean alongX = rnd.nextBoolean();
        for (int a = -radius; a <= radius; a++) {
            int h = (int) Math.round(Math.sqrt(Math.max(0, radius * radius - a * a)));
            for (int t = 0; t <= h; t++) {
                if (t < h - 1 && Math.abs(a) < radius - 1) continue;
                c.set(x + (alongX ? a : 0), y + 1 + t, z + (alongX ? 0 : a), Material.BONE_BLOCK);
            }
        }
    }
}
