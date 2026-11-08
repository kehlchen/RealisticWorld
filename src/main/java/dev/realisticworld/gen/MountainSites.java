package dev.realisticworld.gen;

import dev.realisticworld.Settings;

import java.util.Arrays;
import java.util.Collection;

import static dev.realisticworld.gen.TerrainSampler.clamp;
import static dev.realisticworld.gen.TerrainSampler.smooth;

/**
 * Mountain sites: places where the terrain is replaced by a fixed layout of mountain massifs with taiga valleys
 * (the layout follows a hand drawn sketch: a small mountain in the middle, larger massifs around it, a lake).
 * A site is a block position; the height, lake and influence at any column are a pure function of the seed, the
 * site list and the settings, so every chunk sees the same terrain. Thread safe: the site list is an immutable
 * snapshot that is swapped as a whole.
 */
public final class MountainSites {
    /** One mountain site. {@code source} is "stronghold", "command" or similar; x/z is the centre in blocks. */
    public record Site(int x, int z, String source) {
        public boolean overStronghold() {
            return source.startsWith("stronghold");
        }

        public int chunkX() {
            return Math.floorDiv(x, 16);
        }

        public int chunkZ() {
            return Math.floorDiv(z, 16);
        }
    }

    /** Template values at one column. */
    public static final class Hit {
        /** Influence 0 to 1 (1 inside the layout, fading to 0 over the edge blend). */
        public double w;
        /** Absolute terrain height of the site layout. */
        public double height;
        /** Lake mask 0 to 1. */
        public double lake;
        /** Share of the possible mountain height (0 valley floor, 1 highest crest). */
        public double relief;
    }

    // Layout in drawing pixels, origin = the small middle mountain: centre x, centre z, radius x, radius z,
    // rotation in degrees, height factor (0 = lowest allowed peak, 1 = highest allowed peak).
    private static final double[][] MASSIFS = {
            {0, 0, 70, 75, -10, 0.05},       // small mountain in the middle (over the starter staircase)
            {-83, -142, 165, 82, -8, 0.80},  // large massif north west
            {175, -108, 100, 150, 10, 1.00}, // very large massif east
            {-223, 50, 112, 108, 0, 0.75},   // large massif south west
            {-295, 0, 45, 60, 0, 0.45},      // hump west of it
            {65, -278, 70, 34, 0, 0.35},     // small massif north
            {40, 118, 77, 40, 0, 0.40},      // small massif south
    };
    private static final double[] COS = new double[MASSIFS.length], SIN = new double[MASSIFS.length];

    static {
        for (int i = 0; i < MASSIFS.length; i++) {
            double r = Math.toRadians(MASSIFS[i][4]);
            COS[i] = Math.cos(r);
            SIN[i] = Math.sin(r);
        }
    }

    // lake south of the eastern massif (drawn in blue)
    private static final double LAKE_X = 163, LAKE_Z = 83, LAKE_RX = 35, LAKE_RZ = 40;
    /** Blocks per drawing pixel at size 1.0. */
    private static final double BLOCKS_PER_PIXEL = 0.8;
    /** Radius of the layout in drawing pixels. */
    private static final double RADIUS_PX = 360;

    private final long seed;
    private final Settings st;
    private final int sea;
    private final Noise warpA, warpB, ridgeN, detailN, floorN;
    private volatile Site[] sites = new Site[0];
    private volatile boolean suppressed;

    public MountainSites(long seed, Settings settings) {
        this.seed = seed;
        this.st = settings;
        this.sea = settings.seaLevel();
        warpA = new Noise(seed * 31 + 61);
        warpB = new Noise(seed * 31 + 62);
        ridgeN = new Noise(seed * 31 + 63);
        detailN = new Noise(seed * 31 + 64);
        floorN = new Noise(seed * 31 + 65);
        // Biomes of the sites stay hidden from the biome provider until the stronghold positions of the world
        // are known (see RealisticBiomeProvider); terrain heights are never hidden.
        this.suppressed = settings.mountainsEnabled();
    }

    // ------------------------------------------------------------------ site list

    public Site[] sites() {
        return sites;
    }

    public synchronized void setSites(Collection<Site> list) {
        sites = list.toArray(new Site[0]);
    }

    /** Adds a site unless one already exists in the same chunk. Returns the site that is now there. */
    public synchronized Site add(Site s) {
        for (Site o : sites) {
            if (o.chunkX() == s.chunkX() && o.chunkZ() == s.chunkZ()) return o;
        }
        Site[] n = Arrays.copyOf(sites, sites.length + 1);
        n[n.length - 1] = s;
        sites = n;
        return s;
    }

    public synchronized boolean remove(Site s) {
        java.util.List<Site> l = new java.util.ArrayList<>(Arrays.asList(sites));
        boolean r = l.remove(s);
        sites = l.toArray(new Site[0]);
        return r;
    }

    /** While true the biome provider reports biomes as if there were no sites. */
    public boolean suppressed() {
        return suppressed;
    }

    public void setSuppressed(boolean v) {
        suppressed = v;
    }

    // ------------------------------------------------------------------ geometry

    private double scale() {
        return BLOCKS_PER_PIXEL * st.mountainSize();
    }

    /** Radius (blocks) inside which the layout has full influence. */
    public double radius() {
        return RADIUS_PX * scale();
    }

    /** Radius (blocks) where the influence has faded to zero. */
    public double outerRadius() {
        return radius() + st.mountainEdgeBlend();
    }

    /** True if vanilla structures must not start in this chunk (the stronghold's own start chunk is allowed). */
    public boolean blocksStructures(int cx, int cz) {
        Site[] arr = sites;
        if (arr.length == 0) return false;
        double reach = outerRadius() + st.mountainStructureMargin();
        double bx = cx * 16 + 8, bz = cz * 16 + 8;
        for (Site s : arr) {
            double dx = bx - s.x(), dz = bz - s.z();
            if (dx * dx + dz * dz > reach * reach) continue;
            if (s.overStronghold() && cx == s.chunkX() && cz == s.chunkZ()) continue;
            return true;
        }
        return false;
    }

    private long hash(Site s) {
        long h = seed ^ (s.x() * 0x9E3779B97F4A7C15L) ^ (s.z() * 0xC2B2AE3D27D4EB4FL);
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        return h;
    }

    /** Template values at a column, or null if no site reaches it. */
    public Hit eval(int bx, int bz) {
        Site[] arr = sites;
        if (arr.length == 0) return null;
        double k = scale();
        double outer = outerRadius();
        Hit best = null;
        for (Site s : arr) {
            double dx = bx - s.x(), dz = bz - s.z();
            double d2 = dx * dx + dz * dz;
            if (d2 > outer * outer) continue;
            Hit h = shape(s, dx, dz, k, Math.sqrt(d2));
            if (best == null || h.w > best.w) best = h;
        }
        return best;
    }

    private Hit shape(Site s, double dx, double dz, double k, double dist) {
        Hit hit = new Hit();
        hit.w = 1 - smooth(dist, RADIUS_PX * k, RADIUS_PX * k + st.mountainEdgeBlend());
        long hs = hash(s);
        double ox = (hs & 1023) * 3.7, oz = ((hs >>> 10) & 1023) * 3.7;

        double rx = dx, rz = dz;
        if (st.mountainRotate()) {
            int q = (int) ((hs >>> 20) & 3);
            for (int i = 0; i < q; i++) {
                double t = rx;
                rx = -rz;
                rz = t;
            }
            if (((hs >>> 22) & 1) == 1) rx = -rx;
        }
        double px = rx / k, pz = rz / k;
        double wpx = px + 16 * warpA.fbm(px / 90 + ox, pz / 90 + oz, 3);
        double wpz = pz + 16 * warpB.fbm(px / 90 + ox + 50, pz / 90 + oz - 50, 3);

        double floor = st.mountainValleyY() + 4 * floorN.fbm(dx / 110.0 + ox, dz / 110.0 + oz, 2);
        double ridge = ridgeN.ridged(wpx / 55 + ox, wpz / 55 + oz, 4);
        int peakMin = st.mountainPeakMin(), peakMax = st.mountainPeakMax();

        double acc4 = 0;
        for (int i = 0; i < MASSIFS.length; i++) {
            double[] m = MASSIFS[i];
            double a = wpx - m[0], b = wpz - m[1];
            double u = (a * COS[i] + b * SIN[i]) / m[2];
            double v = (-a * SIN[i] + b * COS[i]) / m[3];
            double d = Math.sqrt(u * u + v * v);
            if (d >= 1) continue;
            double t = clamp((1 - d) / 0.72, 0, 1);
            double prof = t * t * (3 - 2 * t);
            double jitter = ((hs >>> (i * 7)) & 255) / 127.5 - 1;
            double hf = clamp(m[5] + (i == 0 ? Math.abs(jitter) * 0.1 : jitter * 0.18), 0, 1);
            double amp = Math.max(1, peakMin + (peakMax - peakMin) * hf - floor);
            double hi = amp * prof * (0.9 + 0.1 * ridge);
            double q = hi * hi;
            acc4 += q * q;
        }
        double comb = Math.sqrt(Math.sqrt(acc4));
        double h = floor + comb + detailN.fbm(px / 18 + ox, pz / 18 + oz, 3) * 3.5 * Math.min(1, comb / 20);
        h = Math.min(h, peakMax);

        double lu = (wpx - LAKE_X) / LAKE_RX, lv = (wpz - LAKE_Z) / LAKE_RZ;
        double lake = 1 - smooth(Math.sqrt(lu * lu + lv * lv), 0.55, 1.0);
        if (lake > 0) {
            double bottom = sea - 5;
            h = h + (bottom - h) * smooth(lake, 0, 0.5);
        }
        hit.height = h;
        hit.lake = lake;
        hit.relief = clamp(comb / Math.max(1, peakMax - st.mountainValleyY()), 0, 1);
        return hit;
    }
}
