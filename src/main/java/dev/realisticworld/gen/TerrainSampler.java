package dev.realisticworld.gen;

import dev.realisticworld.Settings;
import org.bukkit.block.Biome;

import java.util.concurrent.ConcurrentHashMap;

/** Pure function of (seed, x, z): height, climate and biome. Thread safe. */
public final class TerrainSampler {
    private final Settings st;
    private final int minY, maxY, sea;
    private final Noise warpA, warpB, contN, hillN, hillN2, mountN, ridgeN, detailN,
            tempN, humN, varN, duneN, riverN, caveN, patchN, patchN2, bandN, lakeN;
    private final TerrainShaper shaper;
    private final Noise caveA, caveB, caveC, sinkN, ventN;
    private final ConcurrentHashMap<Long, Biome> cellBiomes = new ConcurrentHashMap<>();
    private final MountainSites sites;

    /** Sampler without any mountain sites (previews of a plain seed). */
    public TerrainSampler(long seed, Settings settings, int minY, int maxY) {
        this(seed, settings, minY, maxY, newPlainSites(seed, settings));
    }

    private static MountainSites newPlainSites(long seed, Settings settings) {
        MountainSites ms = new MountainSites(seed, settings);
        ms.setSuppressed(false);
        return ms;
    }

    public TerrainSampler(long seed, Settings settings, int minY, int maxY, MountainSites sites) {
        this.sites = sites;
        this.st = settings;
        this.minY = minY;
        this.maxY = maxY;
        this.sea = settings.seaLevel();
        warpA = new Noise(seed * 31 + 1);
        warpB = new Noise(seed * 31 + 2);
        contN = new Noise(seed * 31 + 3);
        hillN = new Noise(seed * 31 + 4);
        hillN2 = new Noise(seed * 31 + 5);
        mountN = new Noise(seed * 31 + 6);
        ridgeN = new Noise(seed * 31 + 7);
        detailN = new Noise(seed * 31 + 8);
        tempN = new Noise(seed * 31 + 9);
        humN = new Noise(seed * 31 + 10);
        varN = new Noise(seed * 31 + 11);
        duneN = new Noise(seed * 31 + 12);
        riverN = new Noise(seed * 31 + 13);
        caveN = new Noise(seed * 31 + 14);
        patchN = new Noise(seed * 31 + 15);
        patchN2 = new Noise(seed * 31 + 16);
        bandN = new Noise(seed * 31 + 17);
        lakeN = new Noise(seed * 31 + 18);
        shaper = new TerrainShaper(seed, sea, minY);
        loadWeights();
        caveA = new Noise(seed * 31 + 41);
        caveB = new Noise(seed * 31 + 42);
        caveC = new Noise(seed * 31 + 43);
        sinkN = new Noise(seed * 31 + 44);
        ventN = new Noise(seed * 31 + 45);
    }

    public MountainSites sites() {
        return sites;
    }

    /** Must be called after the site list changed: cached cell biomes were computed with the old list. */
    public void sitesChanged() {
        cellBiomes.clear();
    }

    public static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : Math.min(v, hi);
    }

    public static double smooth(double x, double a, double b) {
        double t = clamp((x - a) / (b - a), 0, 1);
        return t * t * (3 - 2 * t);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    /** Unshaped terrain and climate for one column (biome is null). */
    private Column baseSample(int bx, int bz) {
        return baseSample(bx, bz, true);
    }

    private Column baseSample(int bx, int bz, boolean useSites) {
        double sc = st.horizontalScale();
        double x = bx / sc, z = bz / sc;
        double wx = x + 90 * warpA.fbm(x / 600.0, z / 600.0, 3);
        double wz = z + 90 * warpB.fbm(x / 600.0, z / 600.0, 3);

        // continentalness: ocean < -0.08 < land
        double cs = 2600.0 * st.continentSize();
        double cont = clamp(contN.fbm(wx / cs, wz / cs, 5) + st.landBias(), -1, 1);
        double base = cont >= -0.08 ? 2 + (cont + 0.08) * 34 : 2 + (cont + 0.08) * 170;
        base = Math.max(base, Math.max(-75, minY - sea + 14));

        double inland = smooth(cont, -0.05, 0.35);
        double oceanDamp = smooth(cont, -0.2, -0.05);
        double hills = (hillN.fbm(wx / 180.0, wz / 180.0, 4) * (3 + 16 * inland)
                + hillN2.fbm(wx / 45.0, wz / 45.0, 2) * 1.8 * inland) * oceanDamp * st.hillIntensity();

        // mountains: narrow mask transition + ridged crests = tall and steep
        double mm = mountN.fbm(wx / 1500.0 + 100, wz / 1500.0 - 100, 3);
        double lo = 0.19 - 0.07 * st.mountainCoverage();
        double mask = smooth(mm, lo, lo + 0.14) * smooth(cont, 0.0, 0.22);
        double ridge = ridgeN.ridged(wx / 640.0, wz / 640.0, 6);
        double peak = mask * (0.16 + 0.84 * ridge);
        double avail = Math.min(st.mountainHeight(), maxY - 24 - sea);
        double mtn = Math.pow(peak, 1.2) * avail * st.mountainIntensity()
                + mask * detailN.fbm(wx / 70.0, wz / 70.0, 3) * 9;

        double h = sea + base + hills + mtn;

        // climate
        double ks = st.climateScale();
        double temp = clamp(tempN.fbm(wx / (3400.0 * ks), wz / (3400.0 * ks), 3) * 2.1 + st.temperatureBias(), -1, 1);
        double humid = clamp(humN.fbm(wx / (2800.0 * ks) + 500, wz / (2800.0 * ks) - 500, 3) * 2.1 + st.humidityBias(), -1, 1);
        double variant = varN.fbm(wx / (260.0 * ks), wz / (260.0 * ks), 2);
        double arid = smooth(temp, 0.35, 0.7) * smooth(humid, 0.05, -0.35);

        // desert dunes: elongated, asymmetric crests
        double dune = 0;
        if (st.dunes() && arid > 0.02) {
            double low = 1 - smooth(mask, 0.1, 0.5);
            double dn = duneN.noise((wx * 0.82 + wz * 0.57) / 48.0, (-wx * 0.57 + wz * 0.82) / 150.0);
            double crest = 1 - Math.abs(dn);
            double d = Math.pow(crest, 2.2) * 9 + duneN.fbm(wx / 90.0, wz / 90.0, 2) * 3;
            dune = d * arid * low * smooth(cont, 0.0, 0.1) * st.duneHeight();
            h += dune;
        }

        // rivers
        double landOk = smooth(cont, -0.06, 0.0);
        double rs = 1300.0 * st.riverScale();
        double rn = Math.abs(riverN.fbm(wx / rs, wz / rs, 2));
        double rw = 0.03 * st.riverWidth();
        double factor = landOk * (1 - arid * 0.85) * (1 - smooth(mask, 0.5, 1.0) * 0.6);
        double river = (1 - smooth(rn, 0, rw)) * factor;
        double valley = (1 - smooth(rn, 0, rw * 4)) * factor;
        h = lerp(h, Math.min(h, sea + 4 + (h - sea) * 0.25), valley * 0.65);
        h = lerp(h, Math.min(h, sea - 4), smooth(river, 0.0, 1.0));

        // inland lakes: basins on low, flat land, filled with water up to sea level (they use the river
        // field, so they get the river biome and gravel/sand banks)
        if (st.lakes()) {
            double ls = 620.0 * st.lakeSize();
            double ln = lakeN.fbm(wx / ls + 700, wz / ls - 700, 3);
            double thr = LAKE_THRESHOLD - 0.12 * st.lakeAmount();
            double ok = smooth(cont, 0.12, 0.3) * (1 - smooth(mask, 0.0, 0.2)) * (1 - arid * 0.9)
                    * (1 - smooth(h - sea, 12, 30));
            double lake = smooth(ln, thr, thr + 0.1) * ok;
            if (lake > 0) {
                double bottom = sea - 2 - st.lakeDepth() * smooth(ln, thr + 0.05, thr + 0.3);
                h = lerp(h, Math.min(h, bottom), lake);
                river = Math.max(river, lake);
            }
        }

        // stronghold mountain site: replaces the terrain with the site layout, fading out at its edge
        double siteW = 0, cont2 = cont, mask2 = mask, arid2 = arid, dune2 = dune;
        if (useSites) {
            MountainSites.Hit hit = sites.eval(bx, bz);
            if (hit != null && hit.w > 0) {
                siteW = hit.w;
                h = lerp(h, hit.height, siteW);
                river = lerp(river, hit.lake, siteW);
                cont2 = lerp(cont, Math.max(cont, 0.5), siteW);
                mask2 = lerp(mask, hit.relief, siteW);
                arid2 = arid * (1 - siteW);
                dune2 = dune * (1 - siteW);
            }
        }

        h = clamp(h, minY + 12, maxY - 12);
        return new Column(h, cont2, mask2, ridge, river, temp, humid, variant, arid2, dune2, siteW, null);
    }

    static final double LAKE_THRESHOLD = 0.38;

    /** Biome of a column without computing the blended height (cheap, used by /rw locate and /rw preview). */
    public Biome biomeAt(int bx, int bz) {
        return classify(baseSample(bx, bz));
    }

    /** Full column including biome. */
    public Column sample(int bx, int bz) {
        return build(bx, bz, null, 0, 0, true);
    }

    /**
     * Same as {@link #sample} but without biome classification (biome is null). The height and all other fields
     * are identical. Used where only the height is needed (neighbour columns, structure placement, ponds).
     */
    Column sampleNoBiome(int bx, int bz) {
        return build(bx, bz, null, 0, 0, false);
    }

    private Column build(int bx, int bz, Biome[][] grid, int gx0, int gz0, boolean withBiome) {
        Column base0 = baseSample(bx, bz);
        Biome biome = withBiome ? classify(base0) : null;
        double shaped = clamp(blendedShape(bx, bz, base0, grid, gx0, gz0), minY + 12, maxY - 12);
        // inside a mountain site the layout height is used as is (no biome shaping on top of it)
        if (base0.site() > 0) shaped = lerp(shaped, base0.height(), base0.site());
        return new Column(shaped, base0.cont(), base0.mountain(), base0.ridge(), base0.river(), base0.temp(),
                base0.humid(), base0.variant(), base0.arid(), base0.dune(), base0.site(), biome);
    }

    /**
     * Column as if there were no mountain sites: unshaped height, biome from the base terrain. Used by the biome
     * provider while the stronghold positions of a world are being determined.
     */
    public Column baseColumn(int bx, int bz) {
        Column b = baseSample(bx, bz, false);
        return new Column(b.height(), b.cont(), b.mountain(), b.ridge(), b.river(), b.temp(), b.humid(),
                b.variant(), b.arid(), b.dune(), 0, classify(b));
    }

    /**
     * Columns of one chunk plus a one block border, index [x + 1][z + 1] for block offsets -1..16. The 16x16
     * interior has biomes; the border only carries heights (its biome is null). All cell biomes of the blend
     * kernel are looked up once per chunk instead of once per column.
     */
    public Column[][] sampleChunk(int cx, int cz) {
        Biome[][] grid = new Biome[2 * KR + 3][2 * KR + 3];
        int gx0 = cx - KR - 1, gz0 = cz - KR - 1;
        Column[][] cols = new Column[18][18];
        for (int x = 0; x < 18; x++) {
            for (int z = 0; z < 18; z++) {
                boolean inner = x >= 1 && x <= 16 && z >= 1 && z <= 16;
                cols[x][z] = build(cx * 16 + x - 1, cz * 16 + z - 1, grid, gx0, gz0, inner);
            }
        }
        return cols;
    }

    private Biome cellBiome(int ix, int iz) {
        long key = ((long) ix << 32) ^ (iz & 0xffffffffL);
        Biome b = cellBiomes.get(key);
        if (b == null) {
            b = classify(baseSample(ix * 16 + 8, iz * 16 + 8));
            if (cellBiomes.size() > 400_000) cellBiomes.clear();
            cellBiomes.put(key, b);
        }
        return b;
    }

    // Blend kernel: gaussian with sigma 30 blocks over the cells (16x16 blocks, centre at +8) around a column.
    // The weight only depends on the offset of the column inside its cell, so it is computed once for all
    // 16 x 16 offsets. Entries below 0.02 are dropped, order is the same as in the original double loop.
    private static final int KR = 4;
    private static final int[] K_OFF = new int[16 * 16 + 1];
    private static final byte[] K_DX;
    private static final byte[] K_DZ;
    private static final double[] K_W;

    static {
        int total = 0;
        byte[] dxs = new byte[16 * 16 * 81], dzs = new byte[16 * 16 * 81];
        double[] ws = new double[16 * 16 * 81];
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                K_OFF[lx * 16 + lz] = total;
                for (int dx = -KR; dx <= KR; dx++) {
                    for (int dz = -KR; dz <= KR; dz++) {
                        double px = dx * 16 + 8 - lx, pz = dz * 16 + 8 - lz;
                        double w = Math.exp(-(px * px + pz * pz) / (2 * 30.0 * 30.0));
                        if (w < 0.02) continue;
                        dxs[total] = (byte) dx;
                        dzs[total] = (byte) dz;
                        ws[total++] = w;
                    }
                }
            }
        }
        K_OFF[16 * 16] = total;
        K_DX = java.util.Arrays.copyOf(dxs, total);
        K_DZ = java.util.Arrays.copyOf(dzs, total);
        K_W = java.util.Arrays.copyOf(ws, total);
    }

    private Biome cell(Biome[][] grid, int gx0, int gz0, int ix, int iz) {
        if (grid == null) return cellBiome(ix, iz);
        Biome b = grid[ix - gx0][iz - gz0];
        if (b == null) grid[ix - gx0][iz - gz0] = b = cellBiome(ix, iz);
        return b;
    }

    /**
     * Biome shaping is applied as a weighted blend of the shaping functions of all biomes around the column
     * (gaussian kernel, sigma 30 blocks), so heights change smoothly across biome borders instead of in steps.
     */
    private double blendedShape(int x, int z, Column c, Biome[][] grid, int gx0, int gz0) {
        int cx0 = Math.floorDiv(x, 16), cz0 = Math.floorDiv(z, 16);
        int k = (x - cx0 * 16) * 16 + (z - cz0 * 16);
        Biome[] bs = new Biome[16];
        double[] ws = new double[16];
        int n = 0;
        double total = 0;
        for (int e = K_OFF[k], end = K_OFF[k + 1]; e < end; e++) {
            double w = K_W[e];
            Biome b = cell(grid, gx0, gz0, cx0 + K_DX[e], cz0 + K_DZ[e]);
            int slot = -1;
            for (int i = 0; i < n; i++) {
                if (bs[i] == b) {
                    slot = i;
                    break;
                }
            }
            if (slot < 0) {
                if (n < 16) {
                    slot = n++;
                    bs[slot] = b;
                } else {
                    slot = 15;
                }
            }
            ws[slot] += w;
            total += w;
        }
        if (total <= 0) return c.height();
        double sum = 0;
        for (int i = 0; i < n; i++) {
            if (ws[i] > 0) sum += ws[i] * shaper.shape(bs[i], x, z, c);
        }
        return sum / total;
    }

    /** Own cave system (replaces vanilla carvers, whose aquifers do not match this terrain). */
    public boolean carve(int x, int y, int z, int h) {
        int cap = h - (h < sea ? 10 : 5);
        if (y > cap || y < minY + 6 || y > sea + 120) return false;
        double depth = smooth(cap - y, 0, 14);
        double dens = st.caveDensity();
        if (dens <= 0) return false;
        double cheese = caveC.noise(x / 42.0, y / 26.0, z / 42.0);
        if (cheese > 0.5 + (1 - depth) * 0.25 - (dens - 1) * 0.08) return true;
        double a = caveA.noise(x / 38.0, y / 30.0, z / 38.0);
        double b = caveB.noise(x / 38.0 + 100, y / 30.0, z / 38.0 - 100);
        double w = (0.055 + 0.03 * depth) * dens;
        return Math.abs(a) < w && Math.abs(b) < w;
    }

    /**
     * Per-call scratch for cave carving of one column after another. Creates exactly the same decisions as
     * {@link #carve(int, int, int, int)} but reuses the x/z part of the 3D noise along the column.
     */
    public final class CaveScan {
        private final Noise.Vertical vc = new Noise.Vertical(), va = new Noise.Vertical(), vb = new Noise.Vertical();
        private int cap, limit;
        private final double dens = st.caveDensity();

        /** Starts a column; the highest y that can be carved by noise is {@link #limit()}. */
        public void column(int x, int z, int h) {
            cap = h - (h < sea ? 10 : 5);
            limit = dens <= 0 ? Integer.MIN_VALUE : Math.min(cap, sea + 120);
            if (dens <= 0) return;
            vc.reset(caveC, x / 42.0, z / 42.0);
            va.reset(caveA, x / 38.0, z / 38.0);
            vb.reset(caveB, x / 38.0 + 100, z / 38.0 - 100);
        }

        public int limit() {
            return limit;
        }

        public boolean carve(int y) {
            if (y > limit || y < minY + 6) return false;
            double depth = smooth(cap - y, 0, 14);
            double cheese = vc.at(y / 26.0);
            if (cheese > 0.5 + (1 - depth) * 0.25 - (dens - 1) * 0.08) return true;
            double w = (0.055 + 0.03 * depth) * dens;
            if (!(Math.abs(va.at(y / 30.0)) < w)) return false;
            return Math.abs(vb.at(y / 30.0)) < w;
        }
    }

    public CaveScan caveScan() {
        return new CaveScan();
    }

    /** Magma vent patches in deep caves: small clusters where the cave floor becomes magma over lava. */
    public boolean ventAt(int x, int z) {
        double t = 0.85 - 0.35 * st.ventChance();
        return ventN.noise(x / 9.0, z / 9.0) > t;
    }

    /** Sinkholes: rare vertical shafts from the surface into the cave level (land only). */
    public boolean sinkColumn(int x, int z, int h) {
        return h > sea + 2 && sinkN.noise(x / 60.0, z / 60.0) > 0.82;
    }

    public int toBlockHeight(Column c) {
        return (int) Math.floor(c.height());
    }

    public int heightAt(int x, int z) {
        return toBlockHeight(sampleNoBiome(x, z));
    }

    public double patch(int x, int z) {
        return patchN.noise(x / 18.0, z / 18.0);
    }

    public double patch2(int x, int z) {
        return patchN2.noise(x / 6.0, z / 6.0);
    }

    public int bandOffset(int x, int z) {
        return (int) Math.round(bandN.noise(x / 40.0, z / 40.0) * 3);
    }

    public double caveNoise(int x, int y, int z) {
        return caveN.noise(x / 55.0, y / 35.0, z / 55.0);
    }

    public Biome undergroundBiome(Column c, int x, int y, int z) {
        if (y < minY + 10 || y > c.height() - 18) return null;
        double n = caveNoise(x, y, z);
        double wl = wLush, wd = wDrip;
        if (wl > 0 && n > 0.42 + (1 - Math.min(wl, 2.5)) * 0.15 && c.humid() > -0.2 && c.temp() > -0.6) return Biome.LUSH_CAVES;
        if (wd > 0 && n < -(0.45 + (1 - Math.min(wd, 2.5)) * 0.15)) return Biome.DRIPSTONE_CAVES;
        return null;
    }

    public Biome biome(Column c) {
        return c.biome();
    }

    // ------------------------------------------------------------------ biome classification

    private static final Object[][] KEYS = {
            {Biome.OCEAN, "ocean"}, {Biome.DEEP_OCEAN, "deep_ocean"}, {Biome.WARM_OCEAN, "warm_ocean"},
            {Biome.LUKEWARM_OCEAN, "lukewarm_ocean"}, {Biome.DEEP_LUKEWARM_OCEAN, "deep_lukewarm_ocean"},
            {Biome.COLD_OCEAN, "cold_ocean"}, {Biome.DEEP_COLD_OCEAN, "deep_cold_ocean"},
            {Biome.FROZEN_OCEAN, "frozen_ocean"}, {Biome.DEEP_FROZEN_OCEAN, "deep_frozen_ocean"},
            {Biome.RIVER, "river"}, {Biome.FROZEN_RIVER, "frozen_river"}, {Biome.BEACH, "beach"},
            {Biome.SNOWY_BEACH, "snowy_beach"}, {Biome.STONY_SHORE, "stony_shore"}, {Biome.DESERT, "desert"},
            {Biome.BADLANDS, "badlands"}, {Biome.ERODED_BADLANDS, "eroded_badlands"},
            {Biome.WOODED_BADLANDS, "wooded_badlands"}, {Biome.SAVANNA, "savanna"},
            {Biome.SAVANNA_PLATEAU, "savanna_plateau"}, {Biome.WINDSWEPT_SAVANNA, "windswept_savanna"},
            {Biome.PLAINS, "plains"}, {Biome.SUNFLOWER_PLAINS, "sunflower_plains"}, {Biome.FOREST, "forest"},
            {Biome.FLOWER_FOREST, "flower_forest"}, {Biome.BIRCH_FOREST, "birch_forest"},
            {Biome.OLD_GROWTH_BIRCH_FOREST, "old_growth_birch_forest"}, {Biome.DARK_FOREST, "dark_forest"},
            {Biome.TAIGA, "taiga"}, {Biome.OLD_GROWTH_PINE_TAIGA, "old_growth_pine_taiga"},
            {Biome.OLD_GROWTH_SPRUCE_TAIGA, "old_growth_spruce_taiga"}, {Biome.SNOWY_TAIGA, "snowy_taiga"},
            {Biome.SNOWY_PLAINS, "snowy_plains"}, {Biome.ICE_SPIKES, "ice_spikes"}, {Biome.JUNGLE, "jungle"},
            {Biome.SPARSE_JUNGLE, "sparse_jungle"}, {Biome.BAMBOO_JUNGLE, "bamboo_jungle"}, {Biome.SWAMP, "swamp"},
            {Biome.MANGROVE_SWAMP, "mangrove_swamp"}, {Biome.MEADOW, "meadow"}, {Biome.GROVE, "grove"},
            {Biome.SNOWY_SLOPES, "snowy_slopes"}, {Biome.JAGGED_PEAKS, "jagged_peaks"},
            {Biome.FROZEN_PEAKS, "frozen_peaks"}, {Biome.STONY_PEAKS, "stony_peaks"},
            {Biome.WINDSWEPT_HILLS, "windswept_hills"}, {Biome.WINDSWEPT_FOREST, "windswept_forest"},
            {Biome.WINDSWEPT_GRAVELLY_HILLS, "windswept_gravelly_hills"}, {Biome.CHERRY_GROVE, "cherry_grove"},
            {Biome.LUSH_CAVES, "lush_caves"}, {Biome.DRIPSTONE_CAVES, "dripstone_caves"},
    };

    private static final java.util.Map<String, Biome> BY_KEY = new java.util.LinkedHashMap<>();
    private static final java.util.Map<Biome, String> TO_KEY = new java.util.HashMap<>();

    static {
        for (Object[] k : KEYS) {
            BY_KEY.put((String) k[1], (Biome) k[0]);
            TO_KEY.put((Biome) k[0], (String) k[1]);
        }
    }

    /** Config style key of an overworld biome (for example "old_growth_birch_forest"), or "unknown". */
    public static String keyOf(Biome b) {
        return TO_KEY.getOrDefault(b, "unknown");
    }

    /** Overworld biome for a user typed name (minecraft: prefix, spaces and dashes are accepted), or null. */
    public static Biome biomeByKey(String name) {
        if (name == null) return null;
        String k = name.trim().toLowerCase(java.util.Locale.ROOT);
        if (k.startsWith("minecraft:")) k = k.substring(10);
        return BY_KEY.get(k.replace(' ', '_').replace('-', '_'));
    }

    /** All overworld biome keys, in config order. */
    public static java.util.Set<String> biomeKeys() {
        return java.util.Collections.unmodifiableSet(BY_KEY.keySet());
    }

    private final java.util.Map<Biome, Double> userWeights = new java.util.HashMap<>();
    private double wLush = 1.0, wDrip = 1.0;

    private void loadWeights() {
        for (Object[] k : KEYS) userWeights.put((Biome) k[0], st.biomeWeight((String) k[1]));
        wLush = w(Biome.LUSH_CAVES);
        wDrip = w(Biome.DRIPSTONE_CAVES);
    }

    /** User weight of a biome (1.0 = default, 0 = never). */
    private double w(Biome b) {
        return userWeights.getOrDefault(b, 1.0);
    }

    /** 1 inside [lo, hi], falling to 0 over {@code soft} outside. */
    private static double bump(double x, double lo, double hi, double soft) {
        double out = Math.max(Math.max(lo - x, x - hi), 0);
        return Math.max(0, 1 - out / soft);
    }

    /** Weighted choice among biomes; u (0..1) comes from a smooth noise field, so choices form coherent patches. */
    private final class Pick {
        final Biome[] b = new Biome[24];
        final double[] wt = new double[24];
        int n;
        double total;

        Pick add(Biome biome, double base) {
            double v = base * w(biome);
            if (v > 1e-6 && n < b.length) {
                b[n] = biome;
                wt[n++] = v;
                total += v;
            }
            return this;
        }

        boolean empty() {
            return total <= 1e-4;
        }

        Biome choose(double u) {
            double target = u * total, acc = 0;
            for (int i = 0; i < n; i++) {
                acc += wt[i];
                if (target < acc) return b[i];
            }
            return b[n - 1];
        }
    }

    private static final class Cand {
        final Biome biome;
        final double tLo, tHi, hLo, hHi, soft, base, relMax;

        Cand(Biome biome, double tLo, double tHi, double hLo, double hHi, double soft, double base, double relMax) {
            this.biome = biome;
            this.tLo = tLo;
            this.tHi = tHi;
            this.hLo = hLo;
            this.hHi = hHi;
            this.soft = soft;
            this.base = base;
            this.relMax = relMax;
        }
    }

    private static Cand cand(Biome b, double tLo, double tHi, double hLo, double hHi, double soft, double base) {
        return new Cand(b, tLo, tHi, hLo, hHi, soft, base, 1e9);
    }

    /** Lowland biomes: climate range (temperature, humidity) and relative default frequency. */
    private static final Cand[] LOWLAND = {
            cand(Biome.SNOWY_TAIGA, -1.0, -0.5, -0.1, 1.0, 0.15, 1.0),
            cand(Biome.SNOWY_PLAINS, -1.0, -0.5, -1.0, 0.1, 0.15, 1.0),
            cand(Biome.ICE_SPIKES, -1.0, -0.5, -1.0, 0.0, 0.15, 0.12),
            cand(Biome.TAIGA, -0.55, -0.1, -0.3, 0.35, 0.15, 0.55),
            cand(Biome.OLD_GROWTH_PINE_TAIGA, -0.5, -0.1, 0.0, 1.0, 0.15, 0.25),
            cand(Biome.OLD_GROWTH_SPRUCE_TAIGA, -0.5, -0.1, 0.15, 1.0, 0.15, 0.2),
            cand(Biome.PLAINS, -0.6, 0.5, -1.0, -0.15, 0.2, 1.0),
            cand(Biome.SUNFLOWER_PLAINS, -0.1, 0.45, -0.5, 0.0, 0.15, 0.3),
            cand(Biome.FOREST, -0.3, 0.35, -0.15, 0.7, 0.25, 1.3),
            cand(Biome.FLOWER_FOREST, 0.0, 0.35, -0.1, 0.5, 0.15, 0.25),
            cand(Biome.BIRCH_FOREST, -0.25, 0.3, -0.1, 0.5, 0.2, 0.7),
            cand(Biome.OLD_GROWTH_BIRCH_FOREST, -0.1, 0.3, 0.1, 0.6, 0.15, 0.2),
            cand(Biome.DARK_FOREST, -0.1, 0.35, 0.3, 1.0, 0.15, 0.7),
            new Cand(Biome.SWAMP, -0.1, 0.45, 0.5, 1.0, 0.15, 0.8, 12),
            cand(Biome.SAVANNA, 0.25, 1.0, -0.5, 0.15, 0.2, 1.0),
            cand(Biome.SAVANNA_PLATEAU, 0.3, 1.0, -0.4, 0.1, 0.15, 0.25),
            cand(Biome.WOODED_BADLANDS, 0.3, 0.7, -1.0, -0.4, 0.15, 0.3),
            cand(Biome.SPARSE_JUNGLE, 0.3, 1.0, 0.05, 0.5, 0.15, 0.8),
            cand(Biome.JUNGLE, 0.35, 1.0, 0.4, 1.0, 0.15, 1.0),
            cand(Biome.BAMBOO_JUNGLE, 0.4, 1.0, 0.4, 1.0, 0.15, 0.25),
            new Cand(Biome.MANGROVE_SWAMP, 0.3, 1.0, 0.4, 1.0, 0.15, 0.8, 8),
            cand(Biome.DESERT, 0.4, 1.0, -1.0, -0.05, 0.15, 2.6),
            cand(Biome.BADLANDS, 0.45, 1.0, -1.0, -0.5, 0.15, 0.8),
    };

    /** Biomes inside a mountain site: taiga valleys, bare rock above the tree line, snow and peaks on top. */
    private Biome siteBiome(Column c) {
        double h = c.height();
        double u = 1.0 / (1.0 + Math.exp(-c.variant() * 5.2));
        if (h < sea) {
            // the lake is a river biome; water at the outer edge of a site on ocean is ocean
            if (c.river() > 0.35) return c.temp() < -0.5 ? Biome.FROZEN_RIVER : Biome.RIVER;
            return ocean(c.temp(), sea - h > 28, u);
        }
        double treeLine = st.mountainTreeline() + c.variant() * 5;
        double snowLine = st.mountainSnowline() + c.variant() * 5;
        if (h < treeLine) {
            Pick p = new Pick().add(Biome.TAIGA, 1.0).add(Biome.OLD_GROWTH_PINE_TAIGA, 0.25)
                    .add(Biome.OLD_GROWTH_SPRUCE_TAIGA, 0.2);
            return p.empty() ? Biome.TAIGA : p.choose(u);
        }
        if (h < snowLine) return w(Biome.STONY_PEAKS) > 0 ? Biome.STONY_PEAKS : Biome.WINDSWEPT_HILLS;
        if (h < snowLine + 14) return w(Biome.SNOWY_SLOPES) > 0 ? Biome.SNOWY_SLOPES : Biome.FROZEN_PEAKS;
        Pick p = new Pick().add(Biome.JAGGED_PEAKS, 1).add(Biome.FROZEN_PEAKS, 1);
        return p.empty() ? Biome.FROZEN_PEAKS : p.choose(u);
    }

    private Biome classify(Column c) {
        if (c.site() > 0.5) return siteBiome(c);
        double h = c.height();
        double rel = h - sea;
        double tAlt = c.temp() - Math.max(0, rel) * 0.0042;
        double tLow = c.temp() - Math.min(Math.max(0, rel), 120) * 0.0030;
        double hum = c.humid();
        double u = 1.0 / (1.0 + Math.exp(-c.variant() * 5.2));

        if (h < sea) {
            if (c.river() > 0.35 && c.cont() > -0.08) {
                return tAlt < -0.5 ? Biome.FROZEN_RIVER : Biome.RIVER;
            }
            return ocean(tAlt, sea - h > 28, u);
        }

        if (rel < 3 && c.cont() < -0.045) {
            if (c.mountain() > 0.3 && w(Biome.STONY_SHORE) > 0) return Biome.STONY_SHORE;
            Biome beach = tAlt < -0.5 ? Biome.SNOWY_BEACH : Biome.BEACH;
            if (w(beach) > 0) return beach;
        }

        if (c.mountain() > 0.35 && rel > 30) return mountain(c, rel, tAlt, hum, u);
        return lowland(c, rel, tLow, hum, u);
    }

    private Biome ocean(double t, boolean deep, double u) {
        Pick p = new Pick();
        if (!deep) {
            p.add(Biome.WARM_OCEAN, bump(t, 0.6, 1.0, 0.15)).add(Biome.LUKEWARM_OCEAN, bump(t, 0.2, 0.65, 0.15))
                    .add(Biome.OCEAN, bump(t, -0.4, 0.25, 0.15)).add(Biome.COLD_OCEAN, bump(t, -0.7, -0.3, 0.15))
                    .add(Biome.FROZEN_OCEAN, bump(t, -1.0, -0.6, 0.15));
        } else {
            p.add(Biome.DEEP_LUKEWARM_OCEAN, bump(t, 0.2, 1.0, 0.15)).add(Biome.DEEP_OCEAN, bump(t, -0.4, 0.25, 0.15))
                    .add(Biome.DEEP_COLD_OCEAN, bump(t, -0.7, -0.3, 0.15))
                    .add(Biome.DEEP_FROZEN_OCEAN, bump(t, -1.0, -0.6, 0.15));
        }
        if (!p.empty()) return p.choose(u);
        if (t > 0.65) return Biome.WARM_OCEAN;
        if (t > 0.25) return deep ? Biome.DEEP_LUKEWARM_OCEAN : Biome.LUKEWARM_OCEAN;
        if (t > -0.35) return deep ? Biome.DEEP_OCEAN : Biome.OCEAN;
        if (t > -0.65) return deep ? Biome.DEEP_COLD_OCEAN : Biome.COLD_OCEAN;
        return deep ? Biome.DEEP_FROZEN_OCEAN : Biome.FROZEN_OCEAN;
    }

    private Biome mountain(Column c, double rel, double tAlt, double hum, double u) {
        double snowLine = 150 + c.temp() * 110;
        if (tAlt > 0.42) {
            if (rel > 130 && w(Biome.STONY_PEAKS) > 0) return Biome.STONY_PEAKS;
            Pick p = new Pick().add(Biome.ERODED_BADLANDS, bump(hum, -1.0, -0.1, 0.2))
                    .add(Biome.WINDSWEPT_SAVANNA, bump(hum, -0.3, 1.0, 0.2));
            return p.empty() ? Biome.WINDSWEPT_SAVANNA : p.choose(u);
        }
        if (rel > snowLine + 40) {
            Pick p = new Pick().add(Biome.JAGGED_PEAKS, 1).add(Biome.FROZEN_PEAKS, 1);
            return p.empty() ? Biome.FROZEN_PEAKS : p.choose(u);
        }
        if (rel > snowLine) return w(Biome.SNOWY_SLOPES) > 0 ? Biome.SNOWY_SLOPES : Biome.FROZEN_PEAKS;
        if (rel > snowLine - 50 && hum > 0 && w(Biome.GROVE) > 0) return Biome.GROVE;
        if (rel > 120 && w(Biome.STONY_PEAKS) > 0) return Biome.STONY_PEAKS;
        boolean alpine = rel < 80 && hum > -0.2 && c.mountain() < 0.6;
        Pick p = new Pick();
        if (alpine) p.add(Biome.MEADOW, 1.0).add(Biome.CHERRY_GROVE, 0.4);
        p.add(Biome.WINDSWEPT_FOREST, bump(hum, 0.1, 1.0, 0.2)).add(Biome.WINDSWEPT_HILLS, alpine ? 0.3 : 1.0)
                .add(Biome.WINDSWEPT_GRAVELLY_HILLS, alpine ? 0.1 : 0.4);
        return p.empty() ? Biome.WINDSWEPT_HILLS : p.choose(u);
    }

    private Biome lowland(Column c, double rel, double t, double hum, double u) {
        Pick p = new Pick();
        for (Cand k : LOWLAND) {
            if (rel > k.relMax) continue;
            double suit = bump(t, k.tLo, k.tHi, k.soft) * bump(hum, k.hLo, k.hHi, k.soft);
            if (suit > 0.02) p.add(k.biome, k.base * suit);
        }
        if (!p.empty()) return p.choose(u);
        return legacyLowland(rel, t, hum, c.variant());
    }

    /** Old decision table; only used if every candidate is disabled or out of range. */
    private Biome legacyLowland(double rel, double tAlt, double hum, double v) {
        double t = tAlt;
        if (t < -0.5) {
            if (hum > -0.1) return Biome.SNOWY_TAIGA;
            return v > 0.55 ? Biome.ICE_SPIKES : Biome.SNOWY_PLAINS;
        }
        if (t < -0.12) {
            if (hum < -0.3) return Biome.PLAINS;
            if (hum < 0.25) return Biome.TAIGA;
            return v > 0.1 ? Biome.OLD_GROWTH_SPRUCE_TAIGA : Biome.OLD_GROWTH_PINE_TAIGA;
        }
        if (t < 0.3) {
            if (hum < -0.45) return Biome.PLAINS;
            if (hum < -0.15) return v > 0.4 ? Biome.SUNFLOWER_PLAINS : Biome.PLAINS;
            if (hum < 0.2) return v > 0.3 ? Biome.FLOWER_FOREST : (v > 0.0 ? Biome.FOREST : Biome.BIRCH_FOREST);
            if (hum < 0.55) return v > 0.3 ? Biome.DARK_FOREST : (v < -0.3 ? Biome.OLD_GROWTH_BIRCH_FOREST : Biome.FOREST);
            return rel < 12 ? Biome.SWAMP : Biome.DARK_FOREST;
        }
        if (t < 0.42) {
            if (hum < -0.4) return v > 0.2 ? Biome.WOODED_BADLANDS : Biome.SAVANNA;
            if (hum < 0.1) return v > 0.35 ? Biome.SAVANNA_PLATEAU : Biome.SAVANNA;
            if (hum < 0.45) return Biome.SPARSE_JUNGLE;
            return rel < 10 ? Biome.MANGROVE_SWAMP : Biome.JUNGLE;
        }
        if (hum < -0.55 && v > -0.1) return Biome.BADLANDS;
        if (hum < -0.05) return Biome.DESERT;
        if (hum < 0.2) return v > 0.3 ? Biome.SAVANNA_PLATEAU : Biome.SAVANNA;
        if (hum < 0.5) return Biome.SPARSE_JUNGLE;
        if (rel < 8 && hum > 0.4) return Biome.MANGROVE_SWAMP;
        return v > 0.15 ? Biome.BAMBOO_JUNGLE : Biome.JUNGLE;
    }
}
