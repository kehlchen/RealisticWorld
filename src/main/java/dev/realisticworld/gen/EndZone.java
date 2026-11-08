package dev.realisticworld.gen;

import org.bukkit.block.Biome;

/**
 * Zones of the outer End. A zone decides terrain shape, surface blocks and features. The game only knows
 * the vanilla biomes, so every zone reports a host biome (mobs, sky, chorus and end city generation follow it).
 */
enum EndZone {
    CHORUS_FOREST("chorus_forest", Biome.END_MIDLANDS, true),
    CRYSTAL_FIELDS("crystal_fields", Biome.END_MIDLANDS, true),
    OBSIDIAN_WASTES("obsidian_wastes", Biome.END_BARRENS, true),
    ENDER_DUNES("ender_dunes", Biome.END_BARRENS, true),
    PURPUR_RUINS("purpur_ruins", Biome.END_HIGHLANDS, true),
    VOID_RIFTS("void_rifts", Biome.SMALL_END_ISLANDS, true),
    HIGHLANDS("end_highlands", Biome.END_HIGHLANDS, false),
    MIDLANDS("end_midlands", Biome.END_MIDLANDS, false),
    BARRENS("end_barrens", Biome.END_BARRENS, false),
    SMALL_ISLANDS("small_end_islands", Biome.SMALL_END_ISLANDS, false);

    static final EndZone[] ALL = values();

    /** Config key below end.biomes.weights. */
    final String key;
    final Biome host;
    /** True for the zones that were added on top of the vanilla biomes. */
    final boolean custom;

    EndZone(String key, Biome host, boolean custom) {
        this.key = key;
        this.host = host;
        this.custom = custom;
    }
}
