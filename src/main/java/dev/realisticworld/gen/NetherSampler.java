package dev.realisticworld.gen;

import dev.realisticworld.Settings;
import org.bukkit.block.Biome;

import static dev.realisticworld.gen.TerrainSampler.clamp;
import static dev.realisticworld.gen.TerrainSampler.smooth;

/** Nether terrain: lava sea, floor with canyons, ceiling with stalactites, pillars and floating islands. */
final class NetherSampler {
    final int lava;
    private final Settings st;
    private final double pillarThr, islandOff;
    private final boolean islands;
    private final double wDelta, wSoul, wCrimson, wWarped, wWastes;

    record Col(int floor, int ceil, double pillar, double temp, double humid, double deltaW, double soulW) {
    }

    private final Noise floorN, ceilN, canyonN, islandN, pillarN, tempN, humN, deltaN, detN, stalN, patchN, patchN2;

    NetherSampler(long seed, Settings settings) {
        this.st = settings;
        this.lava = settings.netherLava();
        this.wDelta = settings.biomeWeight("basalt_deltas");
        this.wSoul = settings.biomeWeight("soul_sand_valley");
        this.wCrimson = settings.biomeWeight("crimson_forest");
        this.wWarped = settings.biomeWeight("warped_forest");
        this.wWastes = settings.biomeWeight("nether_wastes");
        this.pillarThr = settings.netherPillars() <= 0 ? 9 : 0.55 - (settings.netherPillars() - 1) * 0.1;
        this.islands = settings.netherIslands() > 0;
        this.islandOff = (1 - settings.netherIslands()) * 0.2;
        floorN = new Noise(seed * 17 + 1);
        ceilN = new Noise(seed * 17 + 2);
        canyonN = new Noise(seed * 17 + 3);
        islandN = new Noise(seed * 17 + 4);
        pillarN = new Noise(seed * 17 + 5);
        tempN = new Noise(seed * 17 + 6);
        humN = new Noise(seed * 17 + 7);
        deltaN = new Noise(seed * 17 + 8);
        detN = new Noise(seed * 17 + 9);
        stalN = new Noise(seed * 17 + 10);
        patchN = new Noise(seed * 17 + 11);
        patchN2 = new Noise(seed * 17 + 12);
    }

    private double temp(int x, int z) {
        double bs = st.netherBiomeScale();
        return clamp(tempN.fbm(x / (420.0 * bs), z / (420.0 * bs), 3) * 2.1, -1, 1);
    }

    private double humid(int x, int z) {
        double bs = st.netherBiomeScale();
        return clamp(humN.fbm(x / (380.0 * bs) + 300, z / (380.0 * bs), 3) * 2.1, -1, 1);
    }

    private double deltaW(int x, int z) {
        double bs = st.netherBiomeScale();
        return wDelta <= 0 ? 0 : smooth(deltaN.fbm(x / (300.0 * bs) - 500, z / (300.0 * bs), 3), 0.28 + (1 - Math.min(wDelta, 2.5)) * 0.15, 0.5 + (1 - Math.min(wDelta, 2.5)) * 0.15);
    }

    private double soulW(double humid, double deltaW) {
        return wSoul <= 0 ? 0 : smooth(humid, -0.25 - (Math.min(wSoul, 2.5) - 1) * 0.1, -0.55 - (Math.min(wSoul, 2.5) - 1) * 0.1) * (1 - deltaW);
    }

    /** Biome at a column. Needs only the climate noise, not the terrain shape, and gives the same result as biome(col(x, z)). */
    Biome biomeAt(int x, int z) {
        double deltaW = deltaW(x, z);
        double humid = humid(x, z);
        return biomeOf(deltaW, soulW(humid, deltaW), temp(x, z));
    }

    Col col(int x, int z) {
        double temp = temp(x, z);
        double humid = humid(x, z);
        double deltaW = deltaW(x, z);
        double soulW = soulW(humid, deltaW);

        double floor = st.netherFloor() + floorN.fbm(x / 110.0, z / 110.0, 4) * 13 - soulW * 6;
        // deltaW is exactly 0 outside the delta zones; the product would be 0, so the noise is skipped
        if (deltaW != 0) floor += deltaW * (detN.ridged(x / 45.0, z / 45.0, 3) * 18 - 5);
        double cn = Math.abs(canyonN.fbm(x / 260.0, z / 260.0, 2));
        floor -= (1 - smooth(cn, 0, 0.06)) * st.netherCanyon();
        floor = clamp(floor, 8, 80);

        double ceil = st.netherCeiling() - ceilN.fbm(x / 100.0 + 77, z / 100.0, 4) * 12;
        ceil -= Math.max(0, stalN.fbm(x / 14.0, z / 14.0, 2)) * 22;
        ceil = clamp(ceil, floor + 10, 124);

        double pillar = pillarN.fbm(x / 70.0 + 11, z / 70.0, 2);
        return new Col((int) Math.floor(floor), (int) Math.floor(ceil), pillar, temp, humid, deltaW, soulW);
    }

    boolean solid(Col c, int x, int y, int z) {
        if (y <= c.floor || y >= c.ceil) return true;
        if (c.pillar > pillarThr) return true;
        if (islands && y > c.floor + 12 && y < c.ceil - 12) {
            double n = islandN.noise(x / 38.0, y / 15.0, z / 38.0);
            double mid = (c.floor + c.ceil) / 2.0;
            double dv = Math.abs(y - mid) / ((c.ceil - c.floor) / 2.0);
            return n > 0.52 + dv * 0.3 + islandOff;
        }
        return false;
    }

    /** True if the whole column is a pillar (solid at every height). */
    boolean pillarColumn(Col c) {
        return c.pillar > pillarThr;
    }

    boolean hasIslands() {
        return islands;
    }

    void islandColumn(Noise.Vertical v, int x, int z) {
        v.reset(islandN, x / 38.0, z / 38.0);
    }

    /** Island part of {@link #solid}: only valid for floor + 12 < y < ceil - 12. */
    boolean island(Noise.Vertical v, Col c, int y) {
        double n = v.at(y / 15.0);
        double mid = (c.floor + c.ceil) / 2.0;
        double dv = Math.abs(y - mid) / ((c.ceil - c.floor) / 2.0);
        return n > 0.52 + dv * 0.3 + islandOff;
    }

    Biome biome(Col c) {
        return biomeOf(c.deltaW, c.soulW, c.temp);
    }

    private Biome biomeOf(double deltaW, double soulW, double temp) {
        if (deltaW > 0.5) return Biome.BASALT_DELTAS;
        if (soulW > 0.5) return Biome.SOUL_SAND_VALLEY;
        // temperature bands: weights move the thresholds, 0 disables the biome
        double crimsonThr = wCrimson <= 0 ? 9 : 0.3 + (1 - Math.min(wCrimson, 2.5)) * 0.3;
        double warpedThr = wWarped <= 0 ? -9 : -0.3 - (1 - Math.min(wWarped, 2.5)) * 0.3;
        if (wWastes <= 0) {
            // no wastes: split the remaining area between crimson and warped
            if (wCrimson > 0 && (wWarped <= 0 || temp > 0)) return Biome.CRIMSON_FOREST;
            if (wWarped > 0) return Biome.WARPED_FOREST;
            return Biome.NETHER_WASTES;
        }
        if (temp > crimsonThr) return Biome.CRIMSON_FOREST;
        if (temp < warpedThr) return Biome.WARPED_FOREST;
        return Biome.NETHER_WASTES;
    }

    double patch(int x, int z) {
        return patchN.noise(x / 14.0, z / 14.0);
    }

    double patch2(int x, int z) {
        return patchN2.noise(x / 5.0, z / 5.0);
    }
}
