package dev.realisticworld.gen;

import dev.realisticworld.Settings;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Map;
import java.util.TreeMap;

/** Renders a mountain site (heights and biomes) to a PNG and prints a few checks. Run through run-site.sh. */
public final class SiteTest {
    public static void main(String[] args) throws Exception {
        long seed = args.length > 0 ? Long.parseLong(args[0]) : 12345L;
        String out = args.length > 1 ? args[1] : "site.png";
        Settings st = Settings.from(new Settings.Source() {
            public double number(String p, double d) { String v = System.getProperty(p); return v == null ? d : Double.parseDouble(v); }
            public boolean bool(String p, boolean d) { String v = System.getProperty(p); return v == null ? d : Boolean.parseBoolean(v); }
            public String text(String p, String d) { return d; }
        });
        MountainSites ms = new MountainSites(seed, st);
        ms.add(new MountainSites.Site(4, 4, "stronghold"));
        ms.setSuppressed(false);
        TerrainSampler s = new TerrainSampler(seed, st, -64, 512, ms);

        int half = 460, step = 2, n = half * 2 / step;
        BufferedImage img = new BufferedImage(n, n, BufferedImage.TYPE_INT_RGB);
        int[] hs = new int[n * n];
        Map<String, Integer> biomes = new TreeMap<>();
        int maxH = -999, minInSite = 999;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                int x = -half + i * step + 4, z = -half + j * step + 4;
                Column c = s.sample(x, z);
                hs[j * n + i] = (int) c.height();
                if (c.site() > 0.99) {
                    maxH = Math.max(maxH, (int) c.height());
                    minInSite = Math.min(minInSite, (int) c.height());
                    biomes.merge(TerrainSampler.keyOf(c.biome()), 1, Integer::sum);
                }
            }
        }
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                int x = -half + i * step + 4, z = -half + j * step + 4;
                Column c = s.sample(x, z);
                int h = hs[j * n + i];
                int west = hs[j * n + Math.max(0, i - 1)], north = hs[Math.max(0, j - 1) * n + i];
                double shade = Math.max(0.6, Math.min(1.4, 1.0 + ((west - h) + (north - h)) * 0.10));
                String b = TerrainSampler.keyOf(c.biome());
                int rgb = switch (b) {
                    case "taiga" -> 0x2f6b3a;
                    case "old_growth_pine_taiga", "old_growth_spruce_taiga" -> 0x24562f;
                    case "stony_peaks" -> 0x8a8a8a;
                    case "snowy_slopes" -> 0xe8eef2;
                    case "frozen_peaks", "jagged_peaks" -> 0xffffff;
                    case "river", "frozen_river" -> 0x3a6fd0;
                    case "ocean", "deep_ocean", "cold_ocean", "deep_cold_ocean", "lukewarm_ocean", "warm_ocean" -> 0x1c3f8f;
                    default -> 0xb59a5a;
                };
                if (h < 63) rgb = h < 63 && (b.contains("river") || b.contains("ocean")) ? rgb : rgb;
                int r = (int) Math.min(255, ((rgb >> 16) & 255) * shade), g = (int) Math.min(255, ((rgb >> 8) & 255) * shade),
                        bl = (int) Math.min(255, (rgb & 255) * shade);
                img.setRGB(i, j, (r << 16) | (g << 8) | bl);
            }
        }
        javax.imageio.ImageIO.write(img, "png", new File(out));
        int maxSlope = 0;
        for (int i = 1; i < n; i++) for (int j = 1; j < n; j++) {
            if (s.sample(-half + i * step + 4, -half + j * step + 4).site() < 0.99) continue;
            maxSlope = Math.max(maxSlope, Math.max(Math.abs(hs[j * n + i] - hs[j * n + i - 1]), Math.abs(hs[j * n + i] - hs[(j - 1) * n + i])));
        }
        System.out.println("max height step per " + step + " blocks inside: " + maxSlope);
        System.out.println("height at centre: " + s.heightAt(4, 4));
        System.out.println("site (w=1) height range: " + minInSite + " .. " + maxH);
        System.out.println("biomes inside: " + biomes);
        System.out.println("outer radius: " + (int) ms.outerRadius() + ", radius: " + (int) ms.radius());
        System.out.println("blocks structures: centre chunk=" + ms.blocksStructures(0, 0) + ", chunk 5,5=" + ms.blocksStructures(5, 5)
                + ", chunk 40,0=" + ms.blocksStructures(40, 0));
    }
}
