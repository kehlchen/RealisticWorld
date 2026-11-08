package dev.realisticworld.gen;

import dev.realisticworld.Settings;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.block.data.BlockData;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.LimitedRegion;

import java.security.MessageDigest;
import java.util.Random;
import java.util.UUID;

public final class PopTest {
    static final class Region implements LimitedRegion {
        final int x0, z0, minY, maxY;
        final Material[] a;
        Region(int cx, int cz, int minY, int maxY) {
            x0 = (cx - 1) * 16; z0 = (cz - 1) * 16; this.minY = minY; this.maxY = maxY;
            a = new Material[48 * 48 * (maxY - minY)];
        }
        int idx(int x, int y, int z) { return ((y - minY) * 48 + (z - z0)) * 48 + (x - x0); }
        public boolean isInRegion(int x, int y, int z) { return x >= x0 && x < x0 + 48 && z >= z0 && z < z0 + 48 && y >= minY && y < maxY; }
        public Material getType(int x, int y, int z) { Material m = a[idx(x, y, z)]; return m == null ? Material.AIR : m; }
        public void setType(int x, int y, int z, Material m) { a[idx(x, y, z)] = m; }
        public void setBlockData(int x, int y, int z, BlockData d) { a[idx(x, y, z)] = d.getMaterial(); }
        public boolean generateTree(Location l, Random r, TreeType t) { return true; }
    }

    public static void main(String[] args) throws Exception {
        int sites = Integer.parseInt(args[0]);
        Settings st = Bench.settingsFromProps();
        int minY = st.minY(), maxY = st.minY() + st.height();
        RealisticChunkGenerator g = new RealisticChunkGenerator(st);
        MessageDigest all = MessageDigest.getInstance("SHA-256");
        Random pr = new Random(99);
        long nsPop = 0;
        for (int i = 0; i < sites; i++) {
            int sx = pr.nextInt(8000) - 4000, sz = pr.nextInt(8000) - 4000;
            long seed = 777L * (i % 3) + 5;
            Bench.W w = new Bench.W(new UUID(0, 200 + i % 3), seed, minY, maxY);
            Region r = new Region(sx, sz, minY, maxY);
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                int cx = sx + dx, cz = sz + dz;
                Bench.Fake d = new Bench.Fake(minY, maxY);
                g.generateNoise(w, new Random(cx * 31L + cz), cx, cz, d);
                g.generateSurface(w, new Random(cx * 37L + cz), cx, cz, d);
                g.generateBedrock(w, new Random(cx * 41L + cz), cx, cz, d);
                g.generateCaves(w, new Random(cx * 43L + cz), cx, cz, d);
                for (int y = minY; y < maxY; y++) for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++)
                    r.a[r.idx(cx * 16 + x, y, cz * 16 + z)] = d.getType(x, y, z);
            }
            BlockPopulator pop = g.getDefaultPopulators(w).get(0);
            long t0 = System.nanoTime();
            pop.populate(w, new Random(seed * 13 + sx * 7 + sz), sx, sz, r);
            nsPop += System.nanoTime() - t0;
            for (Material m : r.a) all.update((byte) (m == null ? 0 : m.ordinal() + 1));
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : all.digest()) sb.append(String.format("%02x", b));
        System.out.printf("populate digest=%s  populate=%.3f ms/chunk%n", sb.substring(0, 16), nsPop / 1e6 / sites);
    }
}
