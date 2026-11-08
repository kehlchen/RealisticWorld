package dev.realisticworld.tools;

import dev.realisticworld.gen.Column;
import dev.realisticworld.gen.TerrainSampler;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Renders a top down biome map of the overworld generator to a PNG file, so config changes can be judged
 * without generating a world. Uses only the sampler, never touches the world.
 */
public final class Preview {
    private static final Map<String, Integer> COLORS = new HashMap<>();

    static {
        put(0x1f3f7a, "ocean", "cold_ocean");
        put(0x14305f, "deep_ocean", "deep_cold_ocean");
        put(0x2f7fb8, "warm_ocean");
        put(0x2a6fa8, "lukewarm_ocean");
        put(0x1d578f, "deep_lukewarm_ocean");
        put(0x8fb4d9, "frozen_ocean");
        put(0x6f93b8, "deep_frozen_ocean");
        put(0x3f76e4, "river");
        put(0xa7c6ef, "frozen_river");
        put(0xe8d9a0, "beach");
        put(0xf2f2f2, "snowy_beach");
        put(0x8a8a8a, "stony_shore");
        put(0xf2d27a, "desert");
        put(0xd5632a, "badlands");
        put(0xb8501f, "eroded_badlands");
        put(0xc2703a, "wooded_badlands");
        put(0xb9b052, "savanna");
        put(0xa6a04a, "savanna_plateau");
        put(0x9c9a5c, "windswept_savanna");
        put(0x8fbd57, "plains");
        put(0xc6d84a, "sunflower_plains");
        put(0x3f8f3a, "forest");
        put(0x6fb84a, "flower_forest");
        put(0x7fa650, "birch_forest");
        put(0x6f9a46, "old_growth_birch_forest");
        put(0x254d22, "dark_forest");
        put(0x2f6f5a, "taiga");
        put(0x2a5f4c, "old_growth_pine_taiga");
        put(0x23503f, "old_growth_spruce_taiga");
        put(0x7fa59a, "snowy_taiga");
        put(0xe6eef2, "snowy_plains");
        put(0xaee0ee, "ice_spikes");
        put(0x1f8f2a, "jungle");
        put(0x4fa63a, "sparse_jungle");
        put(0x7fc040, "bamboo_jungle");
        put(0x4a6b3a, "swamp");
        put(0x3f5f3f, "mangrove_swamp");
        put(0x8fd06a, "meadow");
        put(0xe9a6c0, "cherry_grove");
        put(0x9fc0b0, "grove");
        put(0xdfe9ee, "snowy_slopes");
        put(0xcfd8e0, "jagged_peaks");
        put(0xe9f3fa, "frozen_peaks");
        put(0x9a9a9a, "stony_peaks");
        put(0x7a8a6a, "windswept_hills");
        put(0x5a7a5a, "windswept_forest");
        put(0x8a8f86, "windswept_gravelly_hills");
        put(0x7a9a4a, "lush_caves");
        put(0x8a7a60, "dripstone_caves");
    }

    private static void put(int rgb, String... keys) {
        for (String k : keys) COLORS.put(k, rgb);
    }

    private Preview() {
    }

    public static int color(String key) {
        return COLORS.getOrDefault(key, 0xff00ff);
    }

    /**
     * Writes the map and returns the file. {@code blocks} is the edge length of the area in blocks, centred on
     * (centerX, centerZ); the image has at most {@code maxPixels} pixels per side.
     */
    public static File render(TerrainSampler s, int seaLevel, int centerX, int centerZ, int blocks, int maxPixels,
                              File out) throws IOException {
        int px = Math.min(maxPixels, blocks);
        double step = blocks / (double) px;
        int[] heights = new int[px * px];
        int[] rgb = new int[px * px];
        int x0 = centerX - blocks / 2, z0 = centerZ - blocks / 2;
        for (int i = 0; i < px; i++) {
            for (int j = 0; j < px; j++) {
                Column c = s.sample((int) (x0 + i * step), (int) (z0 + j * step));
                heights[j * px + i] = (int) Math.floor(c.height());
                rgb[j * px + i] = color(TerrainSampler.keyOf(c.biome()));
            }
        }
        BufferedImage img = new BufferedImage(px, px, BufferedImage.TYPE_INT_RGB);
        for (int i = 0; i < px; i++) {
            for (int j = 0; j < px; j++) {
                int h = heights[j * px + i];
                int base = rgb[j * px + i];
                double shade = 1.0;
                if (h >= seaLevel) {
                    int west = heights[j * px + Math.max(0, i - 1)];
                    int north = heights[Math.max(0, j - 1) * px + i];
                    // light from the north west; the slope is relative to the pixel size
                    double slope = ((west - h) + (north - h)) / Math.max(1.0, step);
                    shade = Math.max(0.55, Math.min(1.35, 1.0 + slope * 0.12 + (h - seaLevel) / 900.0));
                } else {
                    shade = Math.max(0.6, 1.0 - (seaLevel - h) / 140.0);
                }
                img.setRGB(i, j, scale(base, shade));
            }
        }
        File parent = out.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) throw new IOException("Cannot create " + parent);
        if (!javax.imageio.ImageIO.write(img, "png", out)) throw new IOException("No PNG writer available");
        return out;
    }

    private static int scale(int rgb, double f) {
        int r = (int) Math.min(255, ((rgb >> 16) & 255) * f);
        int g = (int) Math.min(255, ((rgb >> 8) & 255) * f);
        int b = (int) Math.min(255, (rgb & 255) * f);
        return (r << 16) | (g << 8) | b;
    }
}
