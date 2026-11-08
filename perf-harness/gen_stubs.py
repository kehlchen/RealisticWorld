import os

mats = "AIR ALLIUM AMETHYST_BLOCK AMETHYST_CLUSTER ANDESITE AZURE_BLUET BASALT BEDROCK BIRCH_LOG BLACKSTONE BLUE_ICE BLUE_ORCHID BONE_BLOCK BRAIN_CORAL_BLOCK BROWN_MUSHROOM BROWN_TERRACOTTA BUBBLE_CORAL_BLOCK CALCITE CHERRY_LOG CLAY COARSE_DIRT COBBLESTONE CORNFLOWER CRACKED_POLISHED_BLACKSTONE_BRICKS CRIMSON_NYLIUM CRYING_OBSIDIAN CUT_SANDSTONE DANDELION DARK_OAK_LOG DEEPSLATE DIRT DRIPSTONE_BLOCK END_STONE END_STONE_BRICKS FERN FIRE_CORAL_BLOCK GILDED_BLACKSTONE GRANITE GRASS_BLOCK GRAVEL HORN_CORAL_BLOCK ICE JUNGLE_LOG JUNGLE_LEAVES LAVA LIGHT_GRAY_TERRACOTTA LILY_OF_THE_VALLEY MAGMA_BLOCK MANGROVE_LOG MOSSY_COBBLESTONE MOSS_BLOCK MOSS_CARPET MUD NETHERRACK OAK_LOG OBSIDIAN ORANGE_TERRACOTTA ORANGE_TULIP OXEYE_DAISY PACKED_ICE PINK_PETALS PINK_TULIP PODZOL POLISHED_BLACKSTONE_BRICKS POPPY PURPUR_BLOCK PURPUR_PILLAR RED_MUSHROOM RED_SAND RED_SANDSTONE RED_TERRACOTTA RED_TULIP ROOTED_DIRT SAND SANDSTONE SHORT_GRASS SMOOTH_SANDSTONE SNOW_BLOCK SOUL_SAND SOUL_SOIL SPRUCE_LOG STONE TERRACOTTA TUBE_CORAL_BLOCK TUFF WARPED_NYLIUM WATER WHITE_TERRACOTTA WHITE_TULIP YELLOW_TERRACOTTA".split()
biomes = "BADLANDS BAMBOO_JUNGLE BASALT_DELTAS BEACH BIRCH_FOREST CHERRY_GROVE COLD_OCEAN CRIMSON_FOREST DARK_FOREST DEEP_COLD_OCEAN DEEP_FROZEN_OCEAN DEEP_LUKEWARM_OCEAN DEEP_OCEAN DESERT DRIPSTONE_CAVES END_BARRENS END_HIGHLANDS END_MIDLANDS ERODED_BADLANDS FLOWER_FOREST FOREST FROZEN_OCEAN FROZEN_PEAKS FROZEN_RIVER GROVE ICE_SPIKES JAGGED_PEAKS JUNGLE LUKEWARM_OCEAN LUSH_CAVES MANGROVE_SWAMP MEADOW NETHER_WASTES OCEAN OLD_GROWTH_BIRCH_FOREST OLD_GROWTH_PINE_TAIGA OLD_GROWTH_SPRUCE_TAIGA PLAINS RIVER SAVANNA SAVANNA_PLATEAU SMALL_END_ISLANDS SNOWY_BEACH SNOWY_PLAINS SNOWY_SLOPES SNOWY_TAIGA SOUL_SAND_VALLEY SPARSE_JUNGLE STONY_PEAKS STONY_SHORE SUNFLOWER_PLAINS SWAMP TAIGA THE_END WARM_OCEAN WARPED_FOREST WINDSWEPT_FOREST WINDSWEPT_GRAVELLY_HILLS WINDSWEPT_HILLS WINDSWEPT_SAVANNA WOODED_BADLANDS".split()
nonsolid = ["AIR", "WATER", "LAVA", "SHORT_GRASS", "FERN", "DANDELION", "POPPY", "ALLIUM", "AZURE_BLUET",
            "BLUE_ORCHID", "CORNFLOWER", "LILY_OF_THE_VALLEY", "ORANGE_TULIP", "OXEYE_DAISY", "PINK_TULIP",
            "RED_TULIP", "WHITE_TULIP", "MOSS_CARPET", "PINK_PETALS", "RED_MUSHROOM", "BROWN_MUSHROOM",
            "AMETHYST_CLUSTER"]


def w(path, txt):
    path = os.path.join("src", path)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        f.write(txt)


w("org/jetbrains/annotations/NotNull.java", "package org.jetbrains.annotations;\npublic @interface NotNull {}\n")
w("org/jetbrains/annotations/Nullable.java", "package org.jetbrains.annotations;\npublic @interface Nullable {}\n")

w("org/bukkit/Material.java", """package org.bukkit;
import org.bukkit.block.data.BlockData;
public enum Material {
  %s;
  public boolean isAir() { return this == AIR; }
  public boolean isSolid() {
    switch (this) { case %s: return false; default: return true; }
  }
  public BlockData createBlockData() { return new org.bukkit.block.data.Orientable(this); }
}
""" % (", ".join(mats), ": case ".join(nonsolid)))

w("org/bukkit/block/Biome.java", """package org.bukkit.block;
public final class Biome {
  private final String name;
  private Biome(String n) { name = n; }
  public String toString() { return name; }
%s
}
""" % "\n".join("  public static final Biome %s = new Biome(\"%s\");" % (b, b) for b in biomes))

w("org/bukkit/block/data/BlockData.java", "package org.bukkit.block.data;\npublic interface BlockData { org.bukkit.Material getMaterial(); }\n")
w("org/bukkit/block/data/Orientable.java", """package org.bukkit.block.data;
public class Orientable implements BlockData {
  private final org.bukkit.Material m; private org.bukkit.Axis axis = org.bukkit.Axis.Y;
  public Orientable(org.bukkit.Material m) { this.m = m; }
  public org.bukkit.Material getMaterial() { return m; }
  public void setAxis(org.bukkit.Axis a) { axis = a; }
  public org.bukkit.Axis getAxis() { return axis; }
}
""")
w("org/bukkit/block/data/type/Leaves.java", "package org.bukkit.block.data.type;\npublic interface Leaves extends org.bukkit.block.data.BlockData { void setPersistent(boolean p); }\n")
w("org/bukkit/Axis.java", "package org.bukkit;\npublic enum Axis { X, Y, Z }\n")
w("org/bukkit/HeightMap.java", "package org.bukkit;\npublic enum HeightMap { MOTION_BLOCKING, MOTION_BLOCKING_NO_LEAVES, OCEAN_FLOOR, OCEAN_FLOOR_WG, WORLD_SURFACE, WORLD_SURFACE_WG }\n")
w("org/bukkit/TreeType.java", "package org.bukkit;\npublic enum TreeType { CHORUS_PLANT }\n")
w("org/bukkit/Location.java", """package org.bukkit;
public class Location {
  public final World world; public final double x, y, z;
  public Location(World w, double x, double y, double z) { world = w; this.x = x; this.y = y; this.z = z; }
  public int getBlockX() { return (int) Math.floor(x); }
  public int getBlockY() { return (int) Math.floor(y); }
  public int getBlockZ() { return (int) Math.floor(z); }
}
""")
w("org/bukkit/Tag.java", """package org.bukkit;
public class Tag<T extends Enum<T>> {
  private final java.util.function.Predicate<T> p;
  private Tag(java.util.function.Predicate<T> p) { this.p = p; }
  public boolean isTagged(T t) { return p.test(t); }
  public static final Tag<Material> LOGS = new Tag<>(m -> m.name().endsWith("_LOG"));
  public static final Tag<Material> LEAVES = new Tag<>(m -> m.name().endsWith("_LEAVES"));
}
""")
w("org/bukkit/generator/WorldInfo.java", """package org.bukkit.generator;
public interface WorldInfo {
  java.util.UUID getUID();
  long getSeed();
  int getMinHeight();
  int getMaxHeight();
}
""")
w("org/bukkit/World.java", "package org.bukkit;\npublic interface World extends org.bukkit.generator.WorldInfo { }\n")
w("org/bukkit/generator/BiomeProvider.java", """package org.bukkit.generator;
import org.bukkit.block.Biome;
public abstract class BiomeProvider {
  public abstract Biome getBiome(WorldInfo info, int x, int y, int z);
  public abstract java.util.List<Biome> getBiomes(WorldInfo info);
}
""")
w("org/bukkit/generator/LimitedRegion.java", """package org.bukkit.generator;
import org.bukkit.Material;
public interface LimitedRegion {
  boolean isInRegion(int x, int y, int z);
  Material getType(int x, int y, int z);
  void setType(int x, int y, int z, Material m);
  void setBlockData(int x, int y, int z, org.bukkit.block.data.BlockData d);
  boolean generateTree(org.bukkit.Location l, java.util.Random r, org.bukkit.TreeType t);
}
""")
w("org/bukkit/generator/BlockPopulator.java", """package org.bukkit.generator;
public abstract class BlockPopulator {
  public void populate(WorldInfo worldInfo, java.util.Random random, int chunkX, int chunkZ, LimitedRegion limitedRegion) { }
}
""")
w("org/bukkit/generator/ChunkGenerator.java", """package org.bukkit.generator;
import org.bukkit.*;
import org.bukkit.block.Biome;
import java.util.List;
import java.util.Random;
public abstract class ChunkGenerator {
  public interface ChunkData {
    int getMinHeight();
    int getMaxHeight();
    void setBlock(int x, int y, int z, Material m);
    void setRegion(int xMin, int yMin, int zMin, int xMax, int yMax, int zMax, Material m);
    Material getType(int x, int y, int z);
  }
  public void generateNoise(WorldInfo w, Random r, int cx, int cz, ChunkData d) { }
  public void generateSurface(WorldInfo w, Random r, int cx, int cz, ChunkData d) { }
  public void generateBedrock(WorldInfo w, Random r, int cx, int cz, ChunkData d) { }
  public void generateCaves(WorldInfo w, Random r, int cx, int cz, ChunkData d) { }
  public boolean shouldGenerateCaves(WorldInfo w, Random r, int cx, int cz) { return false; }
  public boolean shouldGenerateDecorations(WorldInfo w, Random r, int cx, int cz) { return false; }
  public boolean shouldGenerateMobs(WorldInfo w, Random r, int cx, int cz) { return false; }
  public boolean shouldGenerateStructures(WorldInfo w, Random r, int cx, int cz) { return false; }
  public int getBaseHeight(WorldInfo w, Random r, int x, int z, HeightMap h) { return 0; }
  public BiomeProvider getDefaultBiomeProvider(WorldInfo w) { return null; }
  public List<BlockPopulator> getDefaultPopulators(World w) { return List.of(); }
  public Location getFixedSpawnLocation(World w, Random r) { return null; }
}
""")
print("stubs written")
