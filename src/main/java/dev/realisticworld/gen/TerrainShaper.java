package dev.realisticworld.gen;

import org.bukkit.block.Biome;

import static dev.realisticworld.gen.TerrainSampler.smooth;

/** Biome specific height changes, applied after the biome has been chosen from the base terrain. */
final class TerrainShaper {
    private final Noise roll, rough, spire, chan, bar, ripple;
    private final int sea, minY;

    TerrainShaper(long seed, int sea, int minY) {
        this.sea = sea;
        this.minY = minY;
        roll = new Noise(seed * 31 + 21);
        rough = new Noise(seed * 31 + 22);
        spire = new Noise(seed * 31 + 23);
        chan = new Noise(seed * 31 + 24);
        bar = new Noise(seed * 31 + 25);
        ripple = new Noise(seed * 31 + 26);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static double terrace(double h, double base, double step, double w) {
        double t = (h - base) / step;
        double f = Math.floor(t);
        double s = smooth(t - f, 0.5 - w, 0.5 + w);
        return base + step * (f + s);
    }

    private double roll(int x, int z, double f, int o) {
        return roll.fbm(x / f, z / f, o);
    }

    private double rough(int x, int z, double f, int o) {
        return rough.fbm(x / f, z / f, o);
    }

    double shape(Biome b, int x, int z, Column c) {
        double h0 = c.height();
        double h = h0 < sea ? water(b, x, z, h0) : land(b, x, z, h0, c);
        return Math.max(h, minY + 12);
    }

    private double water(Biome b, int x, int z, double h0) {
        double top = sea - 2;
        double h = h0;
        if (b == Biome.OCEAN) {
            // sand bars and shoals
            double bars = smooth(bar.fbm(x / 160.0, z / 160.0, 2), 0.3, 0.55);
            h = Math.max(h0, Math.min(top, h0 + bars * 14));
        } else if (b == Biome.DEEP_OCEAN) {
            // narrow trenches
            double tr = 1 - smooth(Math.abs(rough.fbm(x / 700.0, z / 700.0, 2)), 0.0, 0.08);
            h = h0 - tr * 22;
        } else if (b == Biome.WARM_OCEAN) {
            // reef mounds
            double reef = smooth(spire.fbm(x / 48.0, z / 48.0, 2), 0.25, 0.5);
            h = Math.max(h0, Math.min(sea - 1.5, h0 + reef * 8));
        } else if (b == Biome.LUKEWARM_OCEAN) {
            // sand ripples
            h = h0 + ripple.noise(x / 14.0, z / 14.0) * 1.5 + ripple.fbm(x / 60.0, z / 60.0, 2) * 2;
            h = Math.min(h, Math.max(h0, top));
        } else if (b == Biome.DEEP_LUKEWARM_OCEAN) {
            // terraced sand shelves
            h = lerp(h0, terrace(h0, sea, 5, 0.2), 0.8);
        } else if (b == Biome.COLD_OCEAN) {
            h = h0 + rough.fbm(x / 30.0, z / 30.0, 2) * 3;
            h = Math.min(h, Math.max(h0, top));
        } else if (b == Biome.DEEP_COLD_OCEAN) {
            // seamounts
            double m = smooth(bar.fbm(x / 220.0 + 40, z / 220.0, 2), 0.35, 0.55);
            h = Math.max(h0, Math.min(sea - 8, h0 + m * 32));
        } else if (b == Biome.FROZEN_OCEAN) {
            h = h0 + ripple.fbm(x / 70.0, z / 70.0, 2) * 2;
            h = Math.min(h, Math.max(h0, top));
        }
        return Math.min(h, sea - 1.0);
    }

    private double land(Biome b, int x, int z, double h0, Column c) {
        double h = h0;
        boolean coastal = false;

        if (b == Biome.PLAINS) {
            h += roll(x, z, 320, 2) * 3;
        } else if (b == Biome.SUNFLOWER_PLAINS) {
            h += roll(x, z, 260, 2) * 4.5;
        } else if (b == Biome.FOREST || b == Biome.FLOWER_FOREST || b == Biome.BIRCH_FOREST
                || b == Biome.OLD_GROWTH_BIRCH_FOREST) {
            h += roll(x, z, 140, 3) * 5;
        } else if (b == Biome.DARK_FOREST) {
            h += roll(x, z, 110, 3) * 7;
        } else if (b == Biome.TAIGA || b == Biome.OLD_GROWTH_PINE_TAIGA || b == Biome.OLD_GROWTH_SPRUCE_TAIGA
                || b == Biome.SNOWY_TAIGA) {
            h += (spire.ridged(x / 200.0, z / 200.0, 3) - 0.4) * 14;
        } else if (b == Biome.SNOWY_PLAINS) {
            h += roll(x, z, 200, 3) * 4;
        } else if (b == Biome.ICE_SPIKES) {
            h = sea + 2 + (h0 - sea) * 0.3;
            coastal = true;
        } else if (b == Biome.JUNGLE) {
            h += rough(x, z, 70, 3) * 9 + roll(x, z, 200, 2) * 6;
        } else if (b == Biome.SPARSE_JUNGLE) {
            h += rough(x, z, 90, 3) * 6;
        } else if (b == Biome.BAMBOO_JUNGLE) {
            // karst hills
            h += smooth(spire.fbm(x / 55.0, z / 55.0, 2), 0.3, 0.5) * 22 + rough(x, z, 80, 2) * 3;
        } else if (b == Biome.SWAMP || b == Biome.MANGROVE_SWAMP) {
            // flat wetland with tidal channels
            double cn = Math.abs(chan.fbm(x / 110.0, z / 110.0, 2));
            h = lerp(h0, sea + 1.2, 0.85);
            h = lerp(h, sea - 2.5, 1 - smooth(cn, 0.04, 0.10));
            coastal = true;
        } else if (b == Biome.MEADOW) {
            h += roll(x, z, 200, 3) * 6;
        } else if (b == Biome.CHERRY_GROVE) {
            h = lerp(h0, terrace(h0, sea, 6, 0.3), 0.6);
        } else if (b == Biome.GROVE) {
            h = lerp(h0, terrace(h0, sea, 5, 0.3), 0.5);
        } else if (b == Biome.SNOWY_SLOPES) {
            h += rough(x, z, 90, 3) * 8;
        } else if (b == Biome.JAGGED_PEAKS) {
            h += (c.ridge() - 0.45) * 36;
        } else if (b == Biome.FROZEN_PEAKS) {
            h = lerp(h0, terrace(h0, sea, 12, 0.2), 0.5);
        } else if (b == Biome.STONY_PEAKS) {
            h += smooth(spire.fbm(x / 70.0, z / 70.0, 2), 0.35, 0.6) * 16;
        } else if (b == Biome.WINDSWEPT_HILLS) {
            h += rough(x, z, 60, 3) * 8;
        } else if (b == Biome.WINDSWEPT_FOREST) {
            h += rough(x, z, 60, 3) * 6;
        } else if (b == Biome.WINDSWEPT_GRAVELLY_HILLS) {
            h += rough(x, z, 50, 3) * 10;
        } else if (b == Biome.DESERT) {
            // isolated buttes
            h += smooth(spire.fbm(x / 200.0, z / 200.0, 2), 0.45, 0.55) * 10 * c.arid();
        } else if (b == Biome.BADLANDS || b == Biome.WOODED_BADLANDS) {
            h = lerp(h0, terrace(h0, sea, 7, 0.18), 0.9);
        } else if (b == Biome.ERODED_BADLANDS) {
            h = lerp(h0, terrace(h0, sea, 7, 0.18), 0.5)
                    + smooth(spire.fbm(x / 45.0, z / 45.0, 2), 0.3, 0.55) * 30;
        } else if (b == Biome.SAVANNA) {
            h += roll(x, z, 260, 2) * 3;
        } else if (b == Biome.SAVANNA_PLATEAU) {
            h = terrace(h0, sea, 14, 0.12) + 8;
        } else if (b == Biome.WINDSWEPT_SAVANNA) {
            h = lerp(h0, terrace(h0, sea, 8, 0.2), 0.8);
        } else if (b == Biome.BEACH) {
            h = Math.max(sea + 0.2, h0 + ripple.noise(x / 9.0, z / 9.0) * 1.2);
            coastal = true;
        } else if (b == Biome.SNOWY_BEACH) {
            h = Math.max(sea + 0.2, h0 + ripple.noise(x / 9.0, z / 9.0) * 0.9);
            coastal = true;
        } else if (b == Biome.STONY_SHORE) {
            h = h0 + 6 + (rough.fbm(x / 30.0, z / 30.0, 2) + 1) * 3;
            coastal = true;
        }

        if (!coastal) {
            // keep shorelines where the base terrain put them
            h = h0 + (h - h0) * smooth(h0 - sea, 1.5, 10);
            h = Math.max(h, sea + 0.5);
        }
        return h;
    }
}
