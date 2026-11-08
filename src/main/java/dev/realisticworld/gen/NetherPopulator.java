package dev.realisticworld.gen;

import dev.realisticworld.Settings;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

/** Nether features: basalt spires, ruined blackstone pillars, bone ribs, nylium boulders. */
final class NetherPopulator extends BlockPopulator {
    private static final int MIN = Integer.MIN_VALUE;
    private final NetherGenerator gen;
    private final Settings st;

    NetherPopulator(NetherGenerator gen, Settings st) {
        this.gen = gen;
        this.st = st;
    }

    private static int floorY(LimitedRegion r, int x, int z, int hint) {
        for (int y = Math.min(hint + 8, 118); y >= Math.max(hint - 8, 6); y--) {
            if (!r.isInRegion(x, y, z)) return MIN;
            Material m = r.getType(x, y, z);
            if (m.isAir() || m == Material.LAVA || !m.isSolid()) continue;
            return r.getType(x, y + 1, z).isAir() ? y : MIN;
        }
        return MIN;
    }

    private static void set(LimitedRegion r, int x, int y, int z, Material m) {
        if (y < 6 || y > 118) return;
        if (r.isInRegion(x, y, z)) r.setType(x, y, z, m);
    }

    @Override
    public void populate(@NotNull WorldInfo info, @NotNull Random rnd, int cx, int cz, @NotNull LimitedRegion region) {
        NetherSampler s = gen.sampler(info);
        for (int attempt = 0; attempt < 3; attempt++) {
            int x = cx * 16 + 3 + rnd.nextInt(10), z = cz * 16 + 3 + rnd.nextInt(10);
            NetherSampler.Col c = s.col(x, z);
            int y = floorY(region, x, z, c.floor());
            if (y == MIN || y <= s.lava) continue;
            Biome b = s.biome(c);
            if (b == Biome.BASALT_DELTAS && st.netherSpires() && rnd.nextInt(5) == 0) basaltSpire(region, rnd, x, y, z);
            else if (b == Biome.NETHER_WASTES && st.netherRuins() && rnd.nextInt(14) == 0) ruinedPillar(region, rnd, x, y, z);
            else if (b == Biome.SOUL_SAND_VALLEY && st.netherBones() && rnd.nextInt(9) == 0) boneRib(region, rnd, x, y, z);
            else if ((b == Biome.CRIMSON_FOREST || b == Biome.WARPED_FOREST) && st.netherBoulders() && rnd.nextInt(8) == 0) {
                boulder(region, rnd, x, y, z, b == Biome.CRIMSON_FOREST ? Material.CRIMSON_NYLIUM : Material.WARPED_NYLIUM);
            }
        }
    }

    private static void basaltSpire(LimitedRegion r, Random rnd, int x, int y, int z) {
        int h = 6 + rnd.nextInt(13);
        int rad = 1 + rnd.nextInt(2);
        for (int t = 1; t <= h; t++) {
            int rr = t > h - 3 ? 0 : (t < 4 ? rad : Math.max(0, rad - 1));
            for (int dx = -rr; dx <= rr; dx++) {
                for (int dz = -rr; dz <= rr; dz++) {
                    if (dx * dx + dz * dz > rr * rr + 1) continue;
                    set(r, x + dx, y + t, z + dz, t > h - 2 && rnd.nextInt(3) == 0 ? Material.BLACKSTONE : Material.BASALT);
                }
            }
        }
    }

    private static void ruinedPillar(LimitedRegion r, Random rnd, int x, int y, int z) {
        int h = 5 + rnd.nextInt(10);
        for (int t = 1; t <= h; t++) {
            if (t > h - 3 && rnd.nextInt(3) == 0) continue;
            Material m;
            int roll = rnd.nextInt(20);
            if (roll == 0) m = Material.GILDED_BLACKSTONE;
            else if (roll < 8) m = Material.CRACKED_POLISHED_BLACKSTONE_BRICKS;
            else m = Material.POLISHED_BLACKSTONE_BRICKS;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (Math.abs(dx) + Math.abs(dz) > 1 && t % 4 != 0) continue;
                    set(r, x + dx, y + t, z + dz, m);
                }
            }
        }
    }

    private static void boneRib(LimitedRegion r, Random rnd, int x, int y, int z) {
        int radius = 3 + rnd.nextInt(3);
        boolean alongX = rnd.nextBoolean();
        for (int a = -radius; a <= radius; a++) {
            int h = (int) Math.round(Math.sqrt(Math.max(0, radius * radius - a * a)));
            for (int t = 0; t <= h; t++) {
                if (t < h - 1 && Math.abs(a) < radius - 1) continue;
                set(r, x + (alongX ? a : 0), y + 1 + t, z + (alongX ? 0 : a), Material.BONE_BLOCK);
            }
        }
    }

    private static void boulder(LimitedRegion r, Random rnd, int x, int y, int z, Material nylium) {
        int rad = 2 + rnd.nextInt(2);
        for (int dx = -rad; dx <= rad; dx++) {
            for (int dy = -1; dy <= rad; dy++) {
                for (int dz = -rad; dz <= rad; dz++) {
                    if (dx * dx + dy * dy * 1.3 + dz * dz > rad * rad + rnd.nextInt(2)) continue;
                    set(r, x + dx, y + dy, z + dz, dy == rad || rnd.nextInt(3) == 0 ? nylium : Material.NETHERRACK);
                }
            }
        }
    }
}
