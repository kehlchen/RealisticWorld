package dev.realisticworld.gen;

import dev.realisticworld.Settings;
import org.bukkit.HeightMap;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/** Test and benchmark harness. Runs the real generator classes against array backed fake chunk data. */
public final class Bench {
    static final class Fake implements ChunkGenerator.ChunkData {
        final int minY, maxY;
        final Material[] a;
        long writes;

        Fake(int minY, int maxY) {
            this.minY = minY;
            this.maxY = maxY;
            this.a = new Material[16 * 16 * (maxY - minY)];
        }

        int idx(int x, int y, int z) {
            return (y - minY) * 256 + z * 16 + x;
        }

        public int getMinHeight() { return minY; }
        public int getMaxHeight() { return maxY; }

        public void setBlock(int x, int y, int z, Material m) {
            if (x < 0 || x > 15 || z < 0 || z > 15 || y < minY || y >= maxY) return;
            a[idx(x, y, z)] = m;
            writes++;
        }

        public void setRegion(int x0, int y0, int z0, int x1, int y1, int z1, Material m) {
            for (int y = y0; y < y1; y++)
                for (int x = x0; x < x1; x++)
                    for (int z = z0; z < z1; z++)
                        setBlock(x, y, z, m);
        }

        public Material getType(int x, int y, int z) {
            if (x < 0 || x > 15 || z < 0 || z > 15 || y < minY || y >= maxY) return Material.AIR;
            Material m = a[idx(x, y, z)];
            return m == null ? Material.AIR : m;
        }

        void digest(MessageDigest md) {
            for (Material m : a) md.update((byte) (m == null ? 0 : m.ordinal() + 1));
        }
    }

    record W(UUID id, long seed, int minY, int maxY) implements World {
        public UUID getUID() { return id; }
        public long getSeed() { return seed; }
        public int getMinHeight() { return minY; }
        public int getMaxHeight() { return maxY; }
    }

    /** Settings from -Dcfg=path=value,path=value (defaults for everything else). */
    static Settings settingsFromProps() {
        String cfg = System.getProperty("cfg");
        if (cfg == null) return Settings.defaults();
        java.util.Map<String, String> m = new java.util.HashMap<>();
        for (String kv : cfg.split(",")) { String[] p = kv.split("="); m.put(p[0], p[1]); }
        return Settings.from(new Settings.Source() {
            public double number(String path, double def) { return m.containsKey(path) ? Double.parseDouble(m.get(path)) : def; }
            public boolean bool(String path, boolean def) { return m.containsKey(path) ? Boolean.parseBoolean(m.get(path)) : def; }
            public String text(String path, String def) { return m.getOrDefault(path, def); }
        });
    }

    static final boolean CAVES_FIRST = "cb".equals(System.getProperty("order"));
    static final String[] STEPS = {"noise", "surface", "bedrock", "caves", "biomes"};

    static String hex(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (byte x : b) sb.append(String.format("%02x", x));
        return sb.toString();
    }

    public static void main(String[] args) throws Exception {
        String mode = args[0];                 // over | nether | end
        int sites = Integer.parseInt(args[1]); // number of 3x3 chunk sites
        int warm = Integer.parseInt(args[2]);  // warm-up sites (not timed)
        int threads = args.length > 3 ? Integer.parseInt(args[3]) : 1;
        boolean timed = threads == 1;

        Settings st = settingsFromProps();
        ChunkGenerator gen;
        int minY, maxY;
        switch (mode) {
            case "nether" -> { gen = new NetherGenerator(st); minY = 0; maxY = 256; }
            case "end" -> { gen = new EndGenerator(st); minY = 0; maxY = 256; }
            default -> { gen = new RealisticChunkGenerator(st); minY = st.minY(); maxY = st.minY() + st.height(); }
        }
        final int fMin = minY, fMax = maxY;
        List<int[]> list = new ArrayList<>();
        Random pr = new Random(4711);
        for (int i = 0; i < warm + sites; i++) {
            boolean endNear = mode.equals("end") && i % 4 == 0;
            int range = mode.equals("end") ? 400 : 6000;
            list.add(new int[]{endNear ? pr.nextInt(40) - 20 : pr.nextInt(range) - range / 2, endNear ? pr.nextInt(40) - 20 : pr.nextInt(range) - range / 2, i % 3});
        }
        long[] stepNs = new long[STEPS.length];
        AtomicLong writes = new AtomicLong();
        List<String> digests = Collections.synchronizedList(new ArrayList<>());
        final ChunkGenerator g = gen;
        BiomeProvider bp = g.getDefaultBiomeProvider(new W(new UUID(1, 1), 1, minY, maxY));

        Runnable[] jobs = new Runnable[threads];
        for (int t = 0; t < threads; t++) {
            final int tt = t;
            jobs[t] = () -> {
                try {
                    MessageDigest md = MessageDigest.getInstance("SHA-256");
                    for (int i = tt; i < list.size(); i += threads) {
                        boolean measure = timed && i >= warm;
                        int[] s = list.get(i);
                        W w = new W(new UUID(0, 100 + s[2]), 1000L * s[2] + 12345, fMin, fMax);
                        // sites are empty in this test; with the biome suppression off the output must equal the old generator
                        if (g instanceof RealisticChunkGenerator rg) rg.sites(w).setSuppressed(false);
                        for (int dx = 0; dx < 3; dx++) {
                            for (int dz = 0; dz < 3; dz++) {
                                int cx = s[0] + dx, cz = s[1] + dz;
                                Fake d = new Fake(fMin, fMax);
                                long t0 = System.nanoTime();
                                g.generateNoise(w, new Random(w.seed ^ (cx * 341873128712L) ^ (cz * 132897987541L)), cx, cz, d);
                                long t1 = System.nanoTime();
                                g.generateSurface(w, new Random(w.seed + 1 ^ (cx * 341873128712L) ^ (cz * 132897987541L)), cx, cz, d);
                                long t2 = System.nanoTime();
                                Random rb = new Random(w.seed + 2 ^ (cx * 341873128712L) ^ (cz * 132897987541L));
                                Random rc = new Random(w.seed + 3 ^ (cx * 341873128712L) ^ (cz * 132897987541L));
                                long t3, t4;
                                if (CAVES_FIRST) {
                                    g.generateCaves(w, rc, cx, cz, d);
                                    t3 = System.nanoTime();
                                    g.generateBedrock(w, rb, cx, cz, d);
                                    t4 = System.nanoTime();
                                    long tmp = t3; t3 = t2 + (t4 - tmp); t4 = t3 + (tmp - t2);
                                } else {
                                    g.generateBedrock(w, rb, cx, cz, d);
                                    t3 = System.nanoTime();
                                    g.generateCaves(w, rc, cx, cz, d);
                                    t4 = System.nanoTime();
                                }
                                // biome lookups like the server: every 4x4x4 cell of the chunk
                                MessageDigest bd = MessageDigest.getInstance("SHA-256");
                                int bh = 0;
                                for (int qx = 0; qx < 4; qx++)
                                    for (int qz = 0; qz < 4; qz++)
                                        for (int qy = fMin >> 2; qy < fMax >> 2; qy++)
                                            bh = bh * 31 + bp.getBiome(w, cx * 16 + qx * 4, qy * 4, cz * 16 + qz * 4).toString().hashCode();
                                long t5 = System.nanoTime();
                                if (measure) {
                                    synchronized (stepNs) {
                                        stepNs[0] += t1 - t0;
                                        stepNs[1] += t2 - t1;
                                        stepNs[2] += t3 - t2;
                                        stepNs[3] += t4 - t3;
                                        stepNs[4] += t5 - t4;
                                    }
                                    writes.addAndGet(d.writes);
                                }
                                MessageDigest cd = MessageDigest.getInstance("SHA-256");
                                d.digest(cd);
                                cd.update(Integer.toString(bh).getBytes());
                                digests.add(cx + "," + cz + "," + s[2] + ":" + hex(cd.digest()));
                            }
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            };
        }
        if (threads == 1) jobs[0].run();
        else {
            Thread[] ts = new Thread[threads];
            for (int t = 0; t < threads; t++) { ts[t] = new Thread(jobs[t]); ts[t].start(); }
            for (Thread t : ts) t.join();
        }
        Collections.sort(digests);
        MessageDigest all = MessageDigest.getInstance("SHA-256");
        for (String s : digests) all.update(s.getBytes());
        System.out.println("mode=" + mode + " chunks=" + digests.size() + " digest=" + hex(all.digest()));
        if (timed) {
            int n = sites * 9;
            StringBuilder sb = new StringBuilder("ms/chunk:");
            long tot = 0;
            for (int i = 0; i < STEPS.length; i++) {
                sb.append(String.format(" %s=%.3f", STEPS[i], stepNs[i] / 1e6 / n));
                tot += stepNs[i];
            }
            sb.append(String.format(" TOTAL=%.3f writes/chunk=%d", tot / 1e6 / n, writes.get() / n));
            System.out.println(sb);
        }
    }
}
