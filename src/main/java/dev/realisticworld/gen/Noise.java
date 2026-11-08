package dev.realisticworld.gen;

import java.util.Random;

/** Seeded gradient noise (improved Perlin) with fBm and ridged helpers. */
public final class Noise {
    private final int[] p = new int[512];

    public Noise(long seed) {
        Random r = new Random(seed);
        int[] a = new int[256];
        for (int i = 0; i < 256; i++) a[i] = i;
        for (int i = 255; i > 0; i--) {
            int j = r.nextInt(i + 1);
            int t = a[i];
            a[i] = a[j];
            a[j] = t;
        }
        for (int i = 0; i < 512; i++) p[i] = a[i & 255];
    }

    private static double fade(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private static double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    private static double grad2(int h, double x, double y) {
        switch (h & 7) {
            case 0: return x + y;
            case 1: return -x + y;
            case 2: return x - y;
            case 3: return -x - y;
            case 4: return x;
            case 5: return -x;
            case 6: return y;
            default: return -y;
        }
    }

    private static double grad3(int h, double x, double y, double z) {
        h &= 15;
        double u = h < 8 ? x : y;
        double v = h < 4 ? y : (h == 12 || h == 14 ? x : z);
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }

    /** 2D noise, roughly -1..1. */
    public double noise(double x, double y) {
        int xi = (int) Math.floor(x);
        int yi = (int) Math.floor(y);
        double xf = x - xi;
        double yf = y - yi;
        xi &= 255;
        yi &= 255;
        double u = fade(xf);
        double v = fade(yf);
        int aa = p[p[xi] + yi];
        int ab = p[p[xi] + yi + 1];
        int ba = p[p[xi + 1] + yi];
        int bb = p[p[xi + 1] + yi + 1];
        double x1 = lerp(u, grad2(aa, xf, yf), grad2(ba, xf - 1, yf));
        double x2 = lerp(u, grad2(ab, xf, yf - 1), grad2(bb, xf - 1, yf - 1));
        return lerp(v, x1, x2);
    }

    /** 3D noise, roughly -1..1. */
    public double noise(double x, double y, double z) {
        int xi = (int) Math.floor(x);
        int yi = (int) Math.floor(y);
        int zi = (int) Math.floor(z);
        x -= xi;
        y -= yi;
        z -= zi;
        xi &= 255;
        yi &= 255;
        zi &= 255;
        double u = fade(x), v = fade(y), w = fade(z);
        int a = p[xi] + yi, aa = p[a] + zi, ab = p[a + 1] + zi;
        int b = p[xi + 1] + yi, ba = p[b] + zi, bb = p[b + 1] + zi;
        return lerp(w,
                lerp(v,
                        lerp(u, grad3(p[aa], x, y, z), grad3(p[ba], x - 1, y, z)),
                        lerp(u, grad3(p[ab], x, y - 1, z), grad3(p[bb], x - 1, y - 1, z))),
                lerp(v,
                        lerp(u, grad3(p[aa + 1], x, y, z - 1), grad3(p[ba + 1], x - 1, y, z - 1)),
                        lerp(u, grad3(p[ab + 1], x, y - 1, z - 1), grad3(p[bb + 1], x - 1, y - 1, z - 1))));
    }

    /**
     * Evaluates {@link #noise(double, double, double)} for many y values at a fixed (x, z). The lattice work that
     * only depends on x and z, and the permutation lookups of the current lattice cell, are reused between calls.
     * The result is bit-identical to noise(x, y, z). Not thread safe: use one instance per thread and call.
     */
    public static final class Vertical {
        private Noise n;
        private int xi, zi;
        private double xf, zf, u, w, xf1, zf1;
        private int cellY = Integer.MIN_VALUE;
        private int g000, g100, g010, g110, g001, g101, g011, g111;

        public void reset(Noise noise, double x, double z) {
            n = noise;
            xi = (int) Math.floor(x);
            zi = (int) Math.floor(z);
            xf = x - xi;
            zf = z - zi;
            xi &= 255;
            zi &= 255;
            u = fade(xf);
            w = fade(zf);
            xf1 = xf - 1;
            zf1 = zf - 1;
            cellY = Integer.MIN_VALUE;
        }

        public double at(double y) {
            int[] p = n.p;
            int yi = (int) Math.floor(y);
            double yf = y - yi;
            if (yi != cellY) {
                cellY = yi;
                int yh = yi & 255;
                int a = p[xi] + yh, aa = p[a] + zi, ab = p[a + 1] + zi;
                int b = p[xi + 1] + yh, ba = p[b] + zi, bb = p[b + 1] + zi;
                g000 = p[aa];
                g100 = p[ba];
                g010 = p[ab];
                g110 = p[bb];
                g001 = p[aa + 1];
                g101 = p[ba + 1];
                g011 = p[ab + 1];
                g111 = p[bb + 1];
            }
            double v = fade(yf);
            double yf1 = yf - 1;
            return lerp(w,
                    lerp(v,
                            lerp(u, grad3(g000, xf, yf, zf), grad3(g100, xf1, yf, zf)),
                            lerp(u, grad3(g010, xf, yf1, zf), grad3(g110, xf1, yf1, zf))),
                    lerp(v,
                            lerp(u, grad3(g001, xf, yf, zf1), grad3(g101, xf1, yf, zf1)),
                            lerp(u, grad3(g011, xf, yf1, zf1), grad3(g111, xf1, yf1, zf1))));
        }
    }

    /** fBm, normalised and stretched so values spread over about -1..1. */
    public double fbm(double x, double y, int octaves) {
        double sum = 0, amp = 1, norm = 0, f = 1;
        for (int i = 0; i < octaves; i++) {
            sum += noise(x * f + i * 17.31, y * f - i * 9.77) * amp;
            norm += amp;
            amp *= 0.5;
            f *= 2.0;
        }
        double v = sum / norm * 1.6;
        return v < -1 ? -1 : Math.min(v, 1);
    }

    /** Ridged multifractal, 0..1 (sharp crests). */
    public double ridged(double x, double y, int octaves) {
        double sum = 0, amp = 1, norm = 0, f = 1, weight = 1;
        for (int i = 0; i < octaves; i++) {
            double n = noise(x * f + i * 31.7, y * f + i * 13.1) * 1.4;
            n = 1 - Math.min(1, Math.abs(n));
            n *= n;
            n *= weight;
            weight = Math.max(0, Math.min(1, n * 2));
            sum += n * amp;
            norm += amp;
            amp *= 0.5;
            f *= 2.0;
        }
        return sum / norm;
    }
}
