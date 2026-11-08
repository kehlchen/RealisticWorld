package dev.realisticworld.gen;

import dev.realisticworld.Settings;
import org.bukkit.block.Biome;

import static dev.realisticworld.gen.TerrainSampler.smooth;

/**
 * End terrain: rounded main island, a void gap with debris, large outer islands with terraces and hanging spikes.
 * Beyond {@link #zoneStart()} the outer End is divided into zones (see {@link EndZone}); the main island and its
 * surroundings are never touched by them.
 */
final class EndSampler {
    /** top < bottom means no ground in this column. */
    record Col(int top, int bottom, double dens, double dist, boolean shards) {
        boolean ground() {
            return top >= bottom;
        }
    }

    /** Zone at a position plus whether the column lies on the rim of a rift. */
    record Spot(EndZone zone, boolean rim) {
    }

    private static final Col VOID = new Col(-1, 0, 0, 0, false);

    private static final int N = EndZone.ALL.length;
    private static final int CRYSTAL = EndZone.CRYSTAL_FIELDS.ordinal();
    private static final int OBSIDIAN = EndZone.OBSIDIAN_WASTES.ordinal();
    private static final int DUNES = EndZone.ENDER_DUNES.ordinal();
    private static final int RUINS = EndZone.PURPUR_RUINS.ordinal();
    private static final int RIFTS = EndZone.VOID_RIFTS.ordinal();
    private static final int CHORUS = EndZone.CHORUS_FOREST.ordinal();

    private final Noise shapeN, detN, outerN, smallN, shardN, stalN, terrN;
    private final Noise[] zoneN = new Noise[N];
    private final Noise crystalN, obsidianN, duneN, duneW, riftN, riftW, patchN, patchN2;

    private final Settings st;
    private final double thr;
    private final int start;
    private final int zoneStart;
    private final double wHigh, wMid, wBarren, wSmall;
    private final boolean custom;
    private final boolean[] on = new boolean[N];
    private final double[] bias = new double[N];
    private final double zoneScale;

    EndSampler(long seed, Settings settings) {
        this.st = settings;
        this.start = settings.endOuterStart();
        this.zoneStart = settings.endRadius() + 100;
        this.wHigh = settings.biomeWeight("end_highlands");
        this.wMid = settings.biomeWeight("end_midlands");
        this.wBarren = settings.biomeWeight("end_barrens");
        this.wSmall = settings.biomeWeight("small_end_islands");
        this.thr = settings.endOuterAmount() <= 0 ? 9 : 0.12 + (1 - settings.endOuterAmount()) * 0.15;
        shapeN = new Noise(seed * 13 + 1);
        detN = new Noise(seed * 13 + 2);
        outerN = new Noise(seed * 13 + 3);
        smallN = new Noise(seed * 13 + 4);
        shardN = new Noise(seed * 13 + 5);
        stalN = new Noise(seed * 13 + 6);
        terrN = new Noise(seed * 13 + 7);

        this.custom = settings.endCustomBiomes();
        this.zoneScale = 240.0 * settings.endBiomeScale();
        for (int i = 0; i < N; i++) {
            EndZone z = EndZone.ALL[i];
            double w = settings.biomeWeight(z.key);
            on[i] = w > 0;
            bias[i] = on[i] ? 0.45 * Math.log(Math.min(w, 10)) : 0;
            zoneN[i] = new Noise(seed * 13 + 20 + i);
        }
        crystalN = new Noise(seed * 13 + 40);
        obsidianN = new Noise(seed * 13 + 41);
        duneN = new Noise(seed * 13 + 42);
        duneW = new Noise(seed * 13 + 43);
        riftN = new Noise(seed * 13 + 44);
        riftW = new Noise(seed * 13 + 45);
        patchN = new Noise(seed * 13 + 46);
        patchN2 = new Noise(seed * 13 + 47);
    }

    /** Distance from the centre from which zones (surface, features, outer island shapes) apply. */
    int zoneStart() {
        return zoneStart;
    }

    private static double terrace(double h, double base, double step, double w) {
        double t = (h - base) / step;
        double f = Math.floor(t);
        return base + step * (f + smooth(t - f, 0.5 - w, 0.5 + w));
    }

    double outerDensity(int x, int z) {
        double d = Math.sqrt((double) x * x + (double) z * z);
        return outerN.fbm(x / 380.0, z / 380.0, 3) * smooth(d, start, start + 400);
    }

    // ---- zones ------------------------------------------------------------------------------------------

    private double score(int i, int x, int z) {
        return zoneN[i].fbm(x / zoneScale + i * 41.0, z / zoneScale - i * 23.0, 2) + bias[i];
    }

    /** Softmax weights of all zones at a position, used to blend terrain shapes smoothly across zone borders. */
    private double[] influence(int x, int z) {
        double[] w = new double[N];
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < N; i++) {
            if (!on[i]) continue;
            w[i] = score(i, x, z);
            if (w[i] > max) max = w[i];
        }
        if (max == Double.NEGATIVE_INFINITY) return w;
        double sum = 0;
        for (int i = 0; i < N; i++) {
            w[i] = on[i] ? Math.exp(5 * (w[i] - max)) : 0;
            sum += w[i];
        }
        for (int i = 0; i < N; i++) w[i] /= sum;
        return w;
    }

    private static int argmax(double[] w) {
        int best = -1;
        double bw = 0;
        for (int i = 0; i < N; i++) {
            if (w[i] > bw) {
                bw = w[i];
                best = i;
            }
        }
        return best;
    }

    /** Zone and rift rim flag at a position. Only meaningful at or beyond {@link #zoneStart()}. */
    Spot spot(int x, int z) {
        double[] w = influence(x, z);
        int b = argmax(w);
        if (b < 0) return new Spot(EndZone.BARRENS, false);
        boolean rim = b == RIFTS && riftMask(x, z) > 1 - 0.18 * smooth(w[RIFTS], 0.3, 0.6);
        return new Spot(EndZone.ALL[b], rim);
    }

    EndZone zone(int x, int z) {
        return spot(x, z).zone();
    }

    private double riftMask(int x, int z) {
        double wx = x + riftW.noise(x / 55.0, z / 55.0) * 20;
        double wz = z + riftW.noise(x / 55.0 + 40, z / 55.0 - 40) * 20;
        return 1 - Math.abs(riftN.noise(wx / 120.0, wz / 120.0) * 1.7);
    }

    private double dune(int x, int z) {
        double wx = x + duneW.noise(x / 70.0, z / 70.0) * 25;
        double r = Math.max(0, 1 - Math.abs(duneN.noise(wx / 34.0, z / 52.0) * 1.5));
        return r * r;
    }

    /** Terrain height of an outer island after the zone shapes are blended in. */
    private double shapeTop(double[] w, int x, int z, double base) {
        double delta = 0;
        if (w[CHORUS] > 0.02) delta += w[CHORUS] * 1.5;
        if (w[CRYSTAL] > 0.02) delta += w[CRYSTAL] * (crystalN.ridged(x / 40.0, z / 40.0, 2) * 9 - 2);
        if (w[OBSIDIAN] > 0.02) delta += w[OBSIDIAN] * (obsidianN.ridged(x / 32.0, z / 32.0, 3) * 15 - 5);
        if (w[DUNES] > 0.02) delta += w[DUNES] * (dune(x, z) * 8 - 3);
        if (w[RUINS] > 0.02) delta += w[RUINS] * (terrace(base, 58, 8, 0.3) - base);
        return base + delta;
    }

    private boolean riftCarved(double[] w, int x, int z) {
        double rw = smooth(w[RIFTS], 0.3, 0.6);
        return rw > 0 && riftMask(x, z) > 1 - 0.07 * rw;
    }

    /** Small scale noise for surface patches. */
    double patch(int x, int z) {
        return patchN.fbm(x / 9.0, z / 9.0, 2);
    }

    double patch2(int x, int z) {
        return patchN2.fbm(x / 16.0 + 99, z / 16.0, 2);
    }

    // ---- columns ----------------------------------------------------------------------------------------

    private double hang(int x, int z) {
        return Math.max(0, stalN.fbm(x / 10.0, z / 10.0, 2)) * 10 * st.endSpikes();
    }

    Col col(int x, int z) {
        double d = Math.sqrt((double) x * x + (double) z * z);
        // R is endRadius +- 16 (fbm is clamped to -1..1), so beyond endRadius + 96 neither d < R nor
        // d < R + 80 can hold and the noise is not needed
        double R = d < st.endRadius() + 96 ? st.endRadius() + 16 * shapeN.fbm(x / 70.0, z / 70.0, 2)
                : Double.NEGATIVE_INFINITY;

        if (d < R) {
            double hang = hang(x, z);
            double t = 1 - (d / R) * (d / R);
            double topY = st.endHeight() + detN.fbm(x / 45.0, z / 45.0, 3) * 3 + (1 - t) * 2;
            double bottom = topY - 58 * Math.pow(t, 0.65) - hang * (1 - t);
            return new Col((int) topY, (int) bottom, 1, d, true);
        }

        boolean nearMain = d < R + 80;
        if (d > start) {
            double v = outerDensity(x, z);
            if (v > thr) {
                double hang = hang(x, z);
                double k = Math.min(1, (v - thr) / 0.5);
                double topY = 58 + 16 * smooth(k, 0, 1) + terrN.fbm(x / 60.0, z / 60.0, 3) * 9;
                topY += (terrace(topY, 56, 6, 0.2) - topY) * smooth(v, 0.4, 0.6);
                if (custom) {
                    double[] w = influence(x, z);
                    topY = shapeTop(w, x, z, topY);
                    if (riftCarved(w, x, z)) return new Col(-1, 0, 0, d, true);
                }
                double depth = 10 + 85 * Math.pow(k, 0.8);
                return new Col((int) topY, (int) (topY - depth - hang), v, d, true);
            }
        }

        // small floating islands, in the gap and between the outer islands
        double sm = smallN.fbm(x / 120.0 + 50, z / 120.0, 2);
        double sthr = (d < start ? 0.72 : 0.6) + (1 - Math.min(wSmall, 2.5)) * 0.1;
        if (wSmall > 0 && sm > sthr && d > 130) {
            double hang = hang(x, z);
            double topY = 62 + detN.fbm(x / 30.0, z / 30.0, 2) * 6;
            double bottom = topY - 8 - (sm - sthr) * 90 - hang;
            return new Col((int) topY, (int) bottom, 0.1, d, true);
        }
        return nearMain ? new Col(-1, 0, 0, d, true) : VOID;
    }

    void shardColumn(Noise.Vertical v, int x, int z) {
        v.reset(shardN, x / 24.0, z / 24.0);
    }

    boolean shard(Noise.Vertical v, int y) {
        return v.at(y / 16.0) > 0.74;
    }

    /** Floating shards of end stone around islands. */
    boolean shard(int x, int y, int z) {
        return shardN.noise(x / 24.0, y / 16.0, z / 24.0) > 0.74;
    }

    Biome biome(int x, int z) {
        double d = Math.sqrt((double) x * x + (double) z * z);
        if (d < start) return d < st.endRadius() + 100 ? Biome.THE_END : Biome.SMALL_END_ISLANDS;
        if (!custom) return legacyBiome(x, z);
        double od = outerDensity(x, z);
        if (od <= thr) return Biome.SMALL_END_ISLANDS;
        Biome host = zone(x, z).host;
        // end cities only on the cores of big islands
        if (od <= thr + 0.18 && (host == Biome.END_HIGHLANDS || host == Biome.END_MIDLANDS)) return Biome.END_BARRENS;
        return host;
    }

    /** Biome selection of the plugin before the new zones existed (used when end.custom-biomes is false). */
    private Biome legacyBiome(int x, int z) {
        double v = outerDensity(x, z) - (thr - 0.12);
        if (wHigh > 0 && v > 0.45 + (1 - Math.min(wHigh, 2.5)) * 0.15) return Biome.END_HIGHLANDS;
        if (wMid > 0 && v > 0.3 + (1 - Math.min(wMid, 2.5)) * 0.1) return Biome.END_MIDLANDS;
        if (wBarren > 0 && v > 0.12 + (1 - Math.min(wBarren, 2.5)) * 0.08) return Biome.END_BARRENS;
        return Biome.SMALL_END_ISLANDS;
    }
}
