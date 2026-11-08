package dev.realisticworld;

/** All options of config.yml. Generated from one table so every option is read exactly once. */
public final class Settings {
    /** Minimal config access so the generators do not depend on the Bukkit configuration classes. */
    public interface Source {
        double number(String path, double def);

        boolean bool(String path, boolean def);

        String text(String path, String def);
    }

    private static final String[] OVERWORLD_BIOMES = {"ocean", "deep_ocean", "warm_ocean", "lukewarm_ocean", "deep_lukewarm_ocean", "cold_ocean", "deep_cold_ocean", "frozen_ocean", "deep_frozen_ocean", "river", "frozen_river", "beach", "snowy_beach", "stony_shore", "desert", "badlands", "eroded_badlands", "wooded_badlands", "savanna", "savanna_plateau", "windswept_savanna", "plains", "sunflower_plains", "forest", "flower_forest", "birch_forest", "old_growth_birch_forest", "dark_forest", "taiga", "old_growth_pine_taiga", "old_growth_spruce_taiga", "snowy_taiga", "snowy_plains", "ice_spikes", "jungle", "sparse_jungle", "bamboo_jungle", "swamp", "mangrove_swamp", "meadow", "grove", "snowy_slopes", "jagged_peaks", "frozen_peaks", "stony_peaks", "windswept_hills", "windswept_forest", "windswept_gravelly_hills", "cherry_grove", "lush_caves", "dripstone_caves"};
    private static final String[] NETHER_BIOMES = {"nether_wastes", "soul_sand_valley", "crimson_forest", "warped_forest", "basalt_deltas"};
    private static final String[] END_BIOMES = {"end_highlands", "end_midlands", "end_barrens", "small_end_islands", "chorus_forest", "crystal_fields", "obsidian_wastes", "ender_dunes", "purpur_ruins", "void_rifts"};
    private final java.util.Map<String, Double> weights = new java.util.HashMap<>();
    private final double biomeSizeMult, netherSizeMult, endSizeMult;

    private final boolean structures;
    private final boolean tallWorld;
    private final int minYRaw;
    private final int heightRaw;
    private final int seaLevel;
    private final double horizontalScale;
    private final double continentSize;
    private final double landBias;
    private final double hillIntensity;
    private final double mountainHeight;
    private final double mountainIntensity;
    private final double mountainCoverage;
    private final double riverWidth;
    private final double riverScale;
    private final double climateScale;
    private final double temperatureBias;
    private final double humidityBias;
    private final boolean dunes;
    private final double duneHeight;
    private final boolean caves;
    private final double caveDensity;
    private final boolean sinkholes;
    private final int caveLavaLevel;
    private final boolean ventsEnabled;
    private final double ventChance;
    private final int ventMinRaw;
    private final int ventMaxRaw;
    private final boolean boulders;
    private final boolean hoodoos;
    private final boolean biomeFeatures;
    private final boolean removeVanillaLava;
    private final boolean ponds;
    private final boolean logs;
    private final boolean flowers;
    private final boolean snowDrifts;
    private final boolean seaFeatures;
    private final boolean cavePools;
    private final boolean lakes;
    private final double lakeAmount;
    private final double lakeSize;
    private final int lakeDepth;
    private final boolean mtEnabled;
    private final double mtChance;
    private final double mtSize;
    private final int mtPeakMin;
    private final int mtPeakMax;
    private final int mtValleyY;
    private final int mtTreeline;
    private final int mtSnowline;
    private final int mtEdgeBlend;
    private final int mtStructureMargin;
    private final boolean mtRotate;
    private final int mtScanRadius;
    private final boolean oases;
    private final boolean arches;
    private final boolean netherEnabled;
    private final int netherLava;
    private final int netherFloor;
    private final int netherCeiling;
    private final double netherCanyon;
    private final double netherPillars;
    private final double netherIslands;
    private final double netherBiomeScale;
    private final boolean magmaEnabled;
    private final double magmaPatches;
    private final int magmaMinRaw;
    private final int magmaMaxRaw;
    private final boolean netherSpires;
    private final boolean netherRuins;
    private final boolean netherBones;
    private final boolean netherBoulders;
    private final boolean endEnabled;
    private final int endRadius;
    private final int endHeight;
    private final int endOuterStart;
    private final double endOuterAmount;
    private final boolean endShards;
    private final double endSpikes;
    private final boolean endCustomBiomes;
    private final double endBiomeScale;
    private final boolean endChorus;
    private final boolean endCrystals;
    private final boolean endObsidian;
    private final boolean endBones;
    private final boolean endRuins;

    private static double sizeMult(String v) {
        switch (v == null ? "medium" : v.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "small": case "klein": return 0.5;
            case "large": case "gross": case "groß": return 2.0;
            case "extreme": case "extrem": return 4.0;
            default: return 1.0;
        }
    }

    private Settings(Source s) {
        biomeSizeMult = sizeMult(s.text("overworld.climate.biome-size", "medium"));
        netherSizeMult = sizeMult(s.text("nether.biome-size", "medium"));
        endSizeMult = sizeMult(s.text("end.biome-size", "medium"));
        for (String k : OVERWORLD_BIOMES) weights.put(k, Math.max(0, Math.min(10, s.number("overworld.biomes.weights." + k, 1.0))));
        for (String k : NETHER_BIOMES) weights.put(k, Math.max(0, Math.min(10, s.number("nether.biomes.weights." + k, 1.0))));
        for (String k : END_BIOMES) weights.put(k, Math.max(0, Math.min(10, s.number("end.biomes.weights." + k, 1.0))));
        structures = s.bool("structures", true);
        tallWorld = s.bool("world-height.enabled", true);
        minYRaw = (int) s.number("world-height.min-y", -64);
        heightRaw = (int) s.number("world-height.height", 576);
        seaLevel = (int) Math.max(1, Math.min(300, s.number("overworld.sea-level", 63)));
        horizontalScale = Math.max(0.25, Math.min(8, s.number("overworld.terrain.horizontal-scale", 1.0)));
        continentSize = Math.max(0.2, Math.min(6, s.number("overworld.terrain.continent-size", 1.0)));
        landBias = Math.max(-0.6, Math.min(0.6, s.number("overworld.terrain.land-bias", 0.0)));
        hillIntensity = Math.max(0, Math.min(5, s.number("overworld.terrain.hill-intensity", 1.0)));
        mountainHeight = Math.max(50, Math.min(2000, s.number("overworld.terrain.mountain-height", 300)));
        mountainIntensity = Math.max(0.1, Math.min(3, s.number("overworld.terrain.mountain-intensity", 1.0)));
        mountainCoverage = Math.max(0.2, Math.min(2.5, s.number("overworld.terrain.mountain-coverage", 1.0)));
        riverWidth = Math.max(0.2, Math.min(5, s.number("overworld.terrain.river-width", 1.0)));
        riverScale = Math.max(0.3, Math.min(5, s.number("overworld.terrain.river-scale", 1.0)));
        climateScale = Math.max(0.3, Math.min(6, s.number("overworld.climate.scale", 1.0)));
        temperatureBias = Math.max(-1, Math.min(1, s.number("overworld.climate.temperature-bias", 0.0)));
        humidityBias = Math.max(-1, Math.min(1, s.number("overworld.climate.humidity-bias", 0.0)));
        dunes = s.bool("overworld.desert.dunes", true);
        duneHeight = Math.max(0, Math.min(4, s.number("overworld.desert.dune-height", 1.0)));
        caves = s.bool("overworld.caves.enabled", true);
        caveDensity = Math.max(0, Math.min(3, s.number("overworld.caves.density", 1.0)));
        sinkholes = s.bool("overworld.caves.sinkholes", true);
        caveLavaLevel = (int) Math.max(-2000, Math.min(2000, s.number("overworld.caves.lava-level", -54)));
        ventsEnabled = s.bool("overworld.magma-vents.enabled", true);
        ventChance = Math.max(0, Math.min(1, s.number("overworld.magma-vents.chance", 0.5)));
        ventMinRaw = (int) Math.max(1, Math.min(4, s.number("overworld.magma-vents.min-depth", 1)));
        ventMaxRaw = (int) Math.max(1, Math.min(4, s.number("overworld.magma-vents.max-depth", 2)));
        boulders = s.bool("overworld.features.boulders", true);
        hoodoos = s.bool("overworld.features.hoodoos", true);
        biomeFeatures = s.bool("overworld.features.biome-extras", true);
        removeVanillaLava = s.bool("overworld.features.remove-vanilla-lava", true);
        ponds = s.bool("overworld.features.ponds", true);
        logs = s.bool("overworld.features.fallen-logs", true);
        flowers = s.bool("overworld.features.flower-patches", true);
        snowDrifts = s.bool("overworld.features.snow-drifts", true);
        seaFeatures = s.bool("overworld.features.reefs-and-icebergs", true);
        cavePools = s.bool("overworld.features.cave-features", true);
        lakes = s.bool("overworld.lakes.enabled", true);
        lakeAmount = Math.max(0, Math.min(3, s.number("overworld.lakes.amount", 1.0)));
        lakeSize = Math.max(0.3, Math.min(4, s.number("overworld.lakes.size", 1.0)));
        lakeDepth = (int) Math.max(3, Math.min(24, s.number("overworld.lakes.depth", 9)));
        mtEnabled = s.bool("overworld.stronghold-mountains.enabled", true);
        mtChance = Math.max(0, Math.min(100, s.number("overworld.stronghold-mountains.chance-percent", 30))) / 100.0;
        mtSize = Math.max(0.4, Math.min(3, s.number("overworld.stronghold-mountains.size", 1.0)));
        int pMin = (int) Math.max(seaLevel + 20, Math.min(600, s.number("overworld.stronghold-mountains.peak-min-y", 100)));
        int pMax = (int) Math.max(pMin, Math.min(700, s.number("overworld.stronghold-mountains.peak-max-y", 150)));
        mtPeakMin = pMin;
        mtPeakMax = pMax;
        mtValleyY = (int) Math.max(seaLevel + 2, Math.min(pMin - 15, s.number("overworld.stronghold-mountains.valley-y", seaLevel + 5)));
        mtTreeline = (int) Math.max(mtValleyY + 6, Math.min(700, s.number("overworld.stronghold-mountains.treeline-y", 98)));
        mtSnowline = (int) Math.max(mtTreeline + 6, Math.min(700, s.number("overworld.stronghold-mountains.snow-line-y", 122)));
        mtEdgeBlend = (int) Math.max(16, Math.min(300, s.number("overworld.stronghold-mountains.edge-blend", 80)));
        mtStructureMargin = (int) Math.max(0, Math.min(400, s.number("overworld.stronghold-mountains.structure-margin", 32)));
        mtRotate = s.bool("overworld.stronghold-mountains.random-rotation", false);
        mtScanRadius = (int) Math.max(2000, Math.min(40000, s.number("overworld.stronghold-mountains.scan-radius", 12000)));
        oases = s.bool("overworld.features.oases", true);
        arches = s.bool("overworld.features.rock-arches", true);
        netherEnabled = s.bool("nether.enabled", true);
        netherLava = (int) Math.max(5, Math.min(100, s.number("nether.lava-level", 32)));
        netherFloor = (int) Math.max(15, Math.min(70, s.number("nether.floor-height", 34)));
        netherCeiling = (int) Math.max(80, Math.min(122, s.number("nether.ceiling-height", 108)));
        netherCanyon = Math.max(0, Math.min(50, s.number("nether.canyon-depth", 26)));
        netherPillars = Math.max(0, Math.min(3, s.number("nether.pillar-amount", 1.0)));
        netherIslands = Math.max(0, Math.min(3, s.number("nether.island-amount", 1.0)));
        netherBiomeScale = Math.max(0.3, Math.min(6, s.number("nether.biome-scale", 1.0)));
        magmaEnabled = s.bool("nether.magma-lava.enabled", true);
        magmaPatches = Math.max(0, Math.min(1, s.number("nether.magma-lava.patches", 0.5)));
        magmaMinRaw = (int) Math.max(1, Math.min(4, s.number("nether.magma-lava.min-depth", 1)));
        magmaMaxRaw = (int) Math.max(1, Math.min(4, s.number("nether.magma-lava.max-depth", 2)));
        netherSpires = s.bool("nether.features.basalt-spires", true);
        netherRuins = s.bool("nether.features.ruined-pillars", true);
        netherBones = s.bool("nether.features.bone-ribs", true);
        netherBoulders = s.bool("nether.features.nylium-boulders", true);
        endEnabled = s.bool("end.enabled", true);
        endRadius = (int) Math.max(40, Math.min(200, s.number("end.main-island.radius", 90)));
        endHeight = (int) Math.max(40, Math.min(100, s.number("end.main-island.height", 64)));
        endOuterStart = (int) Math.max(300, Math.min(3000, s.number("end.outer-islands.start-distance", 700)));
        endOuterAmount = Math.max(0, Math.min(3, s.number("end.outer-islands.amount", 1.0)));
        endShards = s.bool("end.shards", true);
        endSpikes = Math.max(0, Math.min(3, s.number("end.hanging-spikes", 1.0)));
        endCustomBiomes = s.bool("end.custom-biomes", true);
        endBiomeScale = Math.max(0.3, Math.min(6, s.number("end.biome-scale", 1.0))) * endSizeMult;
        endChorus = s.bool("end.features.chorus-groves", true);
        endCrystals = s.bool("end.features.crystal-spires", true);
        endObsidian = s.bool("end.features.obsidian-spires", true);
        endBones = s.bool("end.features.bone-ribs", true);
        endRuins = s.bool("end.features.purpur-ruins", true);
    }

    public static Settings from(Source s) {
        return new Settings(s);
    }

    public static Settings defaults() {
        return new Settings(new Source() {
            public double number(String path, double def) {
                return def;
            }

            public boolean bool(String path, boolean def) {
                return def;
            }

            public String text(String path, String def) {
                return def;
            }
        });
    }

    public boolean structures() {
        return structures;
    }

    public boolean tallWorld() {
        return tallWorld;
    }

    public int seaLevel() {
        return seaLevel;
    }

    public double horizontalScale() {
        return horizontalScale;
    }

    public double continentSize() {
        return continentSize;
    }

    public double landBias() {
        return landBias;
    }

    public double hillIntensity() {
        return hillIntensity;
    }

    public double mountainHeight() {
        return mountainHeight;
    }

    public double mountainIntensity() {
        return mountainIntensity;
    }

    public double mountainCoverage() {
        return mountainCoverage;
    }

    public double riverWidth() {
        return riverWidth;
    }

    public double riverScale() {
        return riverScale;
    }

    /** Climate noise scale: biome-size preset times the fine tuning multiplier. */
    public double climateScale() {
        return climateScale * biomeSizeMult;
    }

    /** Relative spawn weight of a biome (key = lower case biome name); 1.0 = default, 0 = never. */
    public double biomeWeight(String key) {
        return weights.getOrDefault(key, 1.0);
    }

    public double temperatureBias() {
        return temperatureBias;
    }

    public double humidityBias() {
        return humidityBias;
    }

    public boolean dunes() {
        return dunes;
    }

    public double duneHeight() {
        return duneHeight;
    }

    public boolean caves() {
        return caves;
    }

    public double caveDensity() {
        return caveDensity;
    }

    public boolean sinkholes() {
        return sinkholes;
    }

    public int caveLavaLevel() {
        return caveLavaLevel;
    }

    public boolean ventsEnabled() {
        return ventsEnabled;
    }

    public double ventChance() {
        return ventChance;
    }

    public boolean boulders() {
        return boulders;
    }

    public boolean hoodoos() {
        return hoodoos;
    }

    public boolean biomeFeatures() {
        return biomeFeatures;
    }

    public boolean removeVanillaLava() {
        return removeVanillaLava;
    }

    public boolean ponds() {
        return ponds;
    }

    public boolean logs() {
        return logs;
    }

    public boolean flowers() {
        return flowers;
    }

    public boolean snowDrifts() {
        return snowDrifts;
    }

    public boolean seaFeatures() {
        return seaFeatures;
    }

    public boolean cavePools() {
        return cavePools;
    }

    public boolean lakes() {
        return lakes && lakeAmount > 0;
    }

    /** Relative amount of inland lakes (1.0 = default). */
    public double lakeAmount() {
        return lakeAmount;
    }

    public double lakeSize() {
        return lakeSize;
    }

    /** Maximum depth of inland lakes below sea level. */
    public int lakeDepth() {
        return lakeDepth;
    }

    public boolean oases() {
        return oases;
    }

    // --- stronghold mountains ---

    public boolean mountainsEnabled() {
        return mtEnabled;
    }

    /** Chance (0 to 1) that a stronghold gets the mountain terrain. */
    public double mountainChance() {
        return mtChance;
    }

    public double mountainSize() {
        return mtSize;
    }

    public int mountainPeakMin() {
        return mtPeakMin;
    }

    public int mountainPeakMax() {
        return mtPeakMax;
    }

    public int mountainValleyY() {
        return mtValleyY;
    }

    public int mountainTreeline() {
        return mtTreeline;
    }

    public int mountainSnowline() {
        return mtSnowline;
    }

    public int mountainEdgeBlend() {
        return mtEdgeBlend;
    }

    public int mountainStructureMargin() {
        return mtStructureMargin;
    }

    public boolean mountainRotate() {
        return mtRotate;
    }

    public int mountainScanRadius() {
        return mtScanRadius;
    }

    public boolean arches() {
        return arches;
    }

    /** Biome weight keys per dimension (overworld, nether, end); used to detect typos in config.yml. */
    public static java.util.List<String> biomeKeys(String dimension) {
        switch (dimension) {
            case "overworld": return java.util.List.of(OVERWORLD_BIOMES);
            case "nether": return java.util.List.of(NETHER_BIOMES);
            case "end": return java.util.List.of(END_BIOMES);
            default: return java.util.List.of();
        }
    }

    /** True if the value is one of the accepted biome-size names. */
    public static boolean validSize(String v) {
        if (v == null) return false;
        switch (v.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "small": case "klein": case "medium": case "mittel": case "large": case "gross": case "groß":
            case "extreme": case "extrem": return true;
            default: return false;
        }
    }

    public boolean netherEnabled() {
        return netherEnabled;
    }

    public int netherLava() {
        return netherLava;
    }

    public int netherFloor() {
        return netherFloor;
    }

    public int netherCeiling() {
        return netherCeiling;
    }

    public double netherCanyon() {
        return netherCanyon;
    }

    public double netherPillars() {
        return netherPillars;
    }

    public double netherIslands() {
        return netherIslands;
    }

    public double netherBiomeScale() {
        return netherBiomeScale * netherSizeMult;
    }

    public boolean magmaEnabled() {
        return magmaEnabled;
    }

    public double magmaPatches() {
        return magmaPatches;
    }

    public boolean netherSpires() {
        return netherSpires;
    }

    public boolean netherRuins() {
        return netherRuins;
    }

    public boolean netherBones() {
        return netherBones;
    }

    public boolean netherBoulders() {
        return netherBoulders;
    }

    public boolean endEnabled() {
        return endEnabled;
    }

    public int endRadius() {
        return endRadius;
    }

    public int endHeight() {
        return endHeight;
    }

    public int endOuterStart() {
        return endOuterStart;
    }

    public double endOuterAmount() {
        return endOuterAmount;
    }

    public boolean endShards() {
        return endShards;
    }

    public double endSpikes() {
        return endSpikes;
    }

    /** New End zones (chorus forest, crystal fields, ...) on or off; off restores the previous End outside the main island. */
    public boolean endCustomBiomes() {
        return endCustomBiomes;
    }

    /** Size of the End zones: biome-size preset times the fine tuning multiplier. */
    public double endBiomeScale() {
        return endBiomeScale;
    }

    public boolean endChorus() {
        return endChorus;
    }

    public boolean endCrystals() {
        return endCrystals;
    }

    public boolean endObsidian() {
        return endObsidian;
    }

    public boolean endBones() {
        return endBones;
    }

    public boolean endRuins() {
        return endRuins;
    }

    /** World height values rounded to what the dimension type allows (multiples of 16). */
    public int minY() {
        int v = Math.floorDiv(minYRaw, 16) * 16;
        return Math.max(-2032, Math.min(2016, v));
    }

    public int height() {
        int h = Math.max(384, Math.min(4064, Math.floorDiv(heightRaw, 16) * 16));
        return Math.min(h, 2032 - minY());
    }

    public int ventMinDepth() {
        return Math.min(ventMinRaw, ventMaxRaw);
    }

    public int ventMaxDepth() {
        return Math.max(ventMinRaw, ventMaxRaw);
    }

    public int magmaMinDepth() {
        return Math.min(magmaMinRaw, magmaMaxRaw);
    }

    public int magmaMaxDepth() {
        return Math.max(magmaMinRaw, magmaMaxRaw);
    }

    /** Maps a feature kind (see FeaturePopulator) to its config switch. */
    public boolean featureEnabled(String kind) {
        switch (kind) {
            case "boulder": return boulders;
            case "tower": return hoodoos;
            case "pond": return ponds;
            case "log": return logs;
            case "flowers": return flowers;
            case "snow": return snowDrifts;
            case "sea": return seaFeatures;
            case "oasis": return oases;
            case "arch": return arches;
            default: return true;
        }
    }
}
