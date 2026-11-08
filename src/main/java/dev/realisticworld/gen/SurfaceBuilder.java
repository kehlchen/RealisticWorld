package dev.realisticworld.gen;

import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.generator.ChunkGenerator.ChunkData;

import java.util.Set;

/** Biome specific surface layers: strata, scree, glacier ice, sea floors. Every biome has its own rules. */
final class SurfaceBuilder {
    private static final Material[] BANDS = {
            Material.TERRACOTTA, Material.ORANGE_TERRACOTTA, Material.TERRACOTTA, Material.YELLOW_TERRACOTTA,
            Material.TERRACOTTA, Material.BROWN_TERRACOTTA, Material.WHITE_TERRACOTTA, Material.TERRACOTTA,
            Material.RED_TERRACOTTA, Material.TERRACOTTA, Material.LIGHT_GRAY_TERRACOTTA, Material.ORANGE_TERRACOTTA
    };

    private static final Set<Biome> BADLANDS = Set.of(Biome.BADLANDS, Biome.ERODED_BADLANDS, Biome.WOODED_BADLANDS);
    private static final Set<Biome> FORESTS = Set.of(Biome.FOREST, Biome.FLOWER_FOREST, Biome.BIRCH_FOREST,
            Biome.OLD_GROWTH_BIRCH_FOREST);
    private static final Set<Biome> SAVANNAS = Set.of(Biome.SAVANNA, Biome.SAVANNA_PLATEAU, Biome.WINDSWEPT_SAVANNA);
    private static final Set<Biome> NO_BANKS = Set.of(Biome.DESERT, Biome.BADLANDS, Biome.ERODED_BADLANDS,
            Biome.WOODED_BADLANDS, Biome.BEACH, Biome.SNOWY_BEACH, Biome.STONY_SHORE, Biome.JAGGED_PEAKS,
            Biome.FROZEN_PEAKS, Biome.STONY_PEAKS, Biome.MANGROVE_SWAMP, Biome.SWAMP);

    private enum Kind { NORMAL, BADLAND, DESERT_ROCK }

    private static final class Layer {
        Material top, fill;
        int depth;
        Kind kind = Kind.NORMAL;

        Layer(Material top, Material fill, int depth) {
            set(top, fill, depth);
        }

        void set(Material top, Material fill, int depth) {
            this.top = top;
            this.fill = fill;
            this.depth = depth;
        }
    }

    void apply(TerrainSampler s, int sea, int minY, int cx, int cz, ChunkData d, Column[][] cols) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                Column c = cols[x + 1][z + 1];
                int h = s.toBlockHeight(c);
                double dx = Math.abs(s.toBlockHeight(cols[x + 2][z + 1]) - s.toBlockHeight(cols[x][z + 1]));
                double dz = Math.abs(s.toBlockHeight(cols[x + 1][z + 2]) - s.toBlockHeight(cols[x + 1][z]));
                double slope = Math.max(dx, dz) / 2.0;
                int wx = cx * 16 + x, wz = cz * 16 + z;
                Biome b = c.biome();
                double p = s.patch(wx, wz);
                double p2 = s.patch2(wx, wz);
                int depth = 3 + (int) ((p2 + 1) * 1.5);
                Layer l = layer(b, c, h, sea, slope, p, p2, depth);

                int band = 0;
                boolean bandKnown = false;
                for (int dy = 0; dy < l.depth; dy++) {
                    int y = h - dy;
                    if (y <= minY + 6) break;
                    Material m;
                    if (l.kind == Kind.BADLAND && (dy >= 2 || slope > 0.8)) {
                        if (!bandKnown) {
                            band = s.bandOffset(wx, wz);
                            bandKnown = true;
                        }
                        m = BANDS[Math.floorMod(y + band + 100, BANDS.length)];
                    } else if (l.kind == Kind.DESERT_ROCK) {
                        m = desertRock(y, p);
                    } else {
                        m = dy == 0 ? l.top : l.fill;
                    }
                    d.setBlock(x, y, z, m);
                }

                if (h < sea) freeze(d, x, z, h, sea, b, p);
            }
        }
    }

    /** Ice cover with biome specific thickness. */
    private static void freeze(ChunkData d, int x, int z, int h, int sea, Biome b, double p) {
        if (b == Biome.FROZEN_OCEAN) {
            d.setBlock(x, sea, z, Material.ICE);
            if (p > 0.35 && sea - 1 > h) d.setBlock(x, sea - 1, z, Material.PACKED_ICE);
        } else if (b == Biome.DEEP_FROZEN_OCEAN) {
            d.setBlock(x, sea, z, Material.ICE);
            if (sea - 1 > h) d.setBlock(x, sea - 1, z, Material.PACKED_ICE);
            if (p > 0.4 && sea - 2 > h) d.setBlock(x, sea - 2, z, Material.BLUE_ICE);
        } else if (b == Biome.FROZEN_RIVER) {
            d.setBlock(x, sea, z, Material.ICE);
            if (sea - 1 > h) d.setBlock(x, sea - 1, z, Material.PACKED_ICE);
        }
    }

    private static Material desertRock(int y, double p) {
        int k = Math.floorMod(y, 6);
        if (k == 3) return Material.CUT_SANDSTONE;
        if (k == 4) return Material.SMOOTH_SANDSTONE;
        if (k == 5 && p > 0) return Material.RED_SANDSTONE;
        return Material.SANDSTONE;
    }

    private Layer underwater(Biome b, Column c, int h, int sea, double p, double p2, Layer l) {
        double dep = sea - h;
        boolean warm = c.temp() > 0.25;
        Material m;
        if (b == Biome.RIVER || b == Biome.FROZEN_RIVER) {
            m = p > 0.2 ? Material.GRAVEL : Material.SAND;
            if (p2 > 0.62 && dep < 6) m = Material.CLAY;
        } else if (b == Biome.SWAMP || b == Biome.MANGROVE_SWAMP) {
            m = p2 > 0.55 ? Material.CLAY : Material.MUD;
        } else if (b == Biome.WARM_OCEAN) {
            m = p > 0.5 ? Material.CALCITE : Material.SAND;
        } else if (b == Biome.LUKEWARM_OCEAN) {
            m = p > 0.55 ? Material.CLAY : Material.SAND;
        } else if (b == Biome.DEEP_LUKEWARM_OCEAN) {
            m = p > 0.3 ? Material.GRAVEL : Material.SAND;
        } else if (b == Biome.COLD_OCEAN) {
            m = p > 0 ? Material.GRAVEL : Material.COBBLESTONE;
        } else if (b == Biome.DEEP_COLD_OCEAN) {
            m = p > 0.4 ? Material.TUFF : (p2 > 0.5 ? Material.GRAVEL : Material.STONE);
        } else if (b == Biome.DEEP_OCEAN) {
            m = p > 0.3 ? Material.DEEPSLATE : (p2 > 0.4 ? Material.TUFF : Material.GRAVEL);
        } else if (b == Biome.FROZEN_OCEAN) {
            m = p > 0.1 ? Material.GRAVEL : Material.STONE;
        } else if (b == Biome.DEEP_FROZEN_OCEAN) {
            m = p2 > 0.3 ? Material.STONE : Material.GRAVEL;
        } else if (dep > 20) {
            m = p > 0.1 ? Material.GRAVEL : (warm ? Material.SAND : Material.DIRT);
        } else {
            m = warm ? Material.SAND : (p > 0.3 ? Material.GRAVEL : Material.SAND);
            if (dep < 14 && p2 > 0.62) m = Material.CLAY;
        }
        l.set(m, m, 3);
        return l;
    }

    private Layer layer(Biome b, Column c, int h, int sea, double slope, double p, double p2, int depth) {
        Layer l = new Layer(Material.GRASS_BLOCK, Material.DIRT, depth);
        int exposure = (int) Math.min(48, 4 + slope * 10);

        if (h < sea) return underwater(b, c, h, sea, p, p2, l);

        if (b == Biome.BEACH) {
            l.set(Material.SAND, Material.SAND, 4);
            return l;
        }
        if (b == Biome.SNOWY_BEACH) {
            l.set(p > 0.5 ? Material.PACKED_ICE : Material.SAND, Material.SAND, 4);
            return l;
        }
        if (b == Biome.STONY_SHORE) {
            l.set(p > 0.2 ? Material.GRAVEL : Material.STONE, Material.STONE, Math.max(2, exposure));
            return l;
        }

        if (b == Biome.DESERT) {
            if (slope > 1.0) {
                l.kind = Kind.DESERT_ROCK;
                l.depth = exposure;
            } else if (slope < 0.6 && c.arid() > 0.5 && p2 > 0.15 && p < -0.1) {
                l.set(p2 > 0.45 ? Material.GRAVEL : Material.COARSE_DIRT, Material.SANDSTONE, 3);
            } else {
                Material top = p > 0.55 ? Material.RED_SAND : Material.SAND;
                l.set(top, top, depth + 1);
            }
            return l;
        }

        if (BADLANDS.contains(b)) {
            l.kind = Kind.BADLAND;
            l.depth = Math.max(exposure, 28);
            if (slope < 0.7 && b == Biome.WOODED_BADLANDS) {
                l.kind = Kind.NORMAL;
                l.set(Material.GRASS_BLOCK, Material.DIRT, 3);
            } else if (slope < 0.7) {
                l.top = Material.RED_SAND;
            }
            return l;
        }

        if (b == Biome.SAVANNA_PLATEAU || b == Biome.WINDSWEPT_SAVANNA) {
            if (slope > 0.9) {
                l.kind = Kind.BADLAND;
                l.depth = Math.max(exposure, 12);
            } else {
                l.set(p > 0.0 ? Material.COARSE_DIRT : Material.GRASS_BLOCK,
                        p > 0.0 ? Material.COARSE_DIRT : Material.DIRT, depth);
            }
            return l;
        }

        // snow and ice mountains
        if (b == Biome.JAGGED_PEAKS) {
            if (slope > 1.1) l.set(Material.STONE, Material.STONE, exposure);
            else l.set(Material.SNOW_BLOCK, Material.STONE, 2);
            return l;
        }
        if (b == Biome.FROZEN_PEAKS) {
            if (slope > 1.5) l.set(Material.STONE, Material.STONE, exposure);
            else if (p > 0.78) l.set(Material.BLUE_ICE, Material.PACKED_ICE, 3);
            else if (p > 0.4) l.set(Material.PACKED_ICE, Material.PACKED_ICE, 3);
            else l.set(Material.SNOW_BLOCK, Material.STONE, 2);
            return l;
        }
        if (b == Biome.SNOWY_SLOPES) {
            if (slope > 1.5) l.set(Material.STONE, Material.STONE, exposure);
            else if (p > 0.55) l.set(Material.PACKED_ICE, Material.SNOW_BLOCK, 3);
            else l.set(Material.SNOW_BLOCK, Material.STONE, 2);
            return l;
        }
        if (b == Biome.GROVE) {
            if (slope > 1.5) l.set(Material.STONE, Material.STONE, exposure);
            else l.set(Material.SNOW_BLOCK, Material.DIRT, 3);
            return l;
        }
        if (b == Biome.STONY_PEAKS) {
            Material top;
            if (slope < 0.9 && p2 > 0.3) top = Material.GRAVEL;
            else if (p > 0.5) top = Material.CALCITE;
            else if (p2 > 0.45) top = Material.ANDESITE;
            else top = Material.STONE;
            l.set(top, Material.STONE, Math.max(2, exposure));
            return l;
        }

        // windswept
        if (b == Biome.WINDSWEPT_GRAVELLY_HILLS) {
            l.set(p2 > -0.3 ? Material.GRAVEL : Material.STONE, Material.STONE, Math.max(2, exposure));
            return l;
        }
        if (b == Biome.WINDSWEPT_HILLS || b == Biome.WINDSWEPT_FOREST) {
            if (slope > 0.9 || p > 0.4) l.set(p2 > 0.3 ? Material.GRAVEL : Material.STONE, Material.STONE, Math.max(2, exposure));
            return l;
        }

        // cold and wet lowlands
        if (b == Biome.ICE_SPIKES) {
            l.set(p > 0.4 ? Material.PACKED_ICE : Material.SNOW_BLOCK, Material.PACKED_ICE, 3);
            return l;
        }
        if (b == Biome.SNOWY_PLAINS) {
            if (p > 0.45) l.set(Material.SNOW_BLOCK, Material.DIRT, 2);
            else if (p2 > 0.7) l.top = Material.COARSE_DIRT;
        } else if (b == Biome.SNOWY_TAIGA) {
            if (p > 0.35) l.top = Material.PODZOL;
        } else if (b == Biome.TAIGA) {
            if (p > 0.5) l.top = Material.PODZOL;
            else if (p2 > 0.75) l.top = Material.COARSE_DIRT;
        } else if (b == Biome.OLD_GROWTH_PINE_TAIGA || b == Biome.OLD_GROWTH_SPRUCE_TAIGA) {
            if (p > 0.0) l.top = Material.PODZOL;
            else if (p2 > 0.6) l.top = Material.COARSE_DIRT;
        } else if (b == Biome.FOREST) {
            if (p > 0.6) l.top = Material.ROOTED_DIRT;
        } else if (b == Biome.BIRCH_FOREST || b == Biome.OLD_GROWTH_BIRCH_FOREST) {
            if (p > 0.55) l.top = Material.MOSS_BLOCK;
        } else if (b == Biome.FLOWER_FOREST) {
            if (p2 > 0.8) l.top = Material.COARSE_DIRT;
        } else if (b == Biome.DARK_FOREST) {
            if (p > 0.55) l.top = Material.PODZOL;
            else if (p2 > 0.7) l.top = Material.MOSS_BLOCK;
        } else if (b == Biome.PLAINS || b == Biome.SUNFLOWER_PLAINS) {
            if (p > 0.75) l.top = Material.COARSE_DIRT;
        } else if (b == Biome.MEADOW || b == Biome.CHERRY_GROVE) {
            if (p > 0.8) l.top = Material.MOSS_BLOCK;
        } else if (b == Biome.JUNGLE) {
            if (p > 0.5) l.top = Material.MOSS_BLOCK;
            else if (p2 > 0.7) l.top = Material.MUD;
        } else if (b == Biome.SPARSE_JUNGLE) {
            if (p > 0.55) l.top = Material.COARSE_DIRT;
        } else if (b == Biome.BAMBOO_JUNGLE) {
            if (slope > 0.9) l.set(p > 0.3 ? Material.MOSS_BLOCK : Material.STONE, Material.STONE, exposure);
            else if (p > 0.0) l.top = Material.PODZOL;
        } else if (b == Biome.SAVANNA) {
            if (p > 0.35) l.top = Material.COARSE_DIRT;
        } else if (b == Biome.MANGROVE_SWAMP) {
            l.set(Material.MUD, Material.MUD, 4);
        } else if (b == Biome.SWAMP) {
            if (p > 0.45) l.top = Material.MUD;
        }

        // generic rock exposure on steep terrain
        if (slope > 1.7) {
            l.set(Material.STONE, Material.STONE, exposure);
        } else if (slope > 1.15) {
            l.set(p > -0.1 ? Material.STONE : Material.COARSE_DIRT, p > -0.1 ? Material.STONE : Material.DIRT, exposure);
        } else if (slope > 0.85 && p > 0.35 && c.mountain() > 0.2) {
            l.set(Material.GRAVEL, Material.STONE, 2);
        } else if (c.river() > 0.12 && !NO_BANKS.contains(b)) {
            // river banks
            Material bank = p > 0.1 ? Material.GRAVEL : (c.temp() > 0.2 ? Material.SAND : Material.COARSE_DIRT);
            l.set(bank, bank == Material.COARSE_DIRT ? Material.DIRT : bank, 2);
        }
        return l;
    }
}
