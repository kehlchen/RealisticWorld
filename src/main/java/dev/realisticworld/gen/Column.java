package dev.realisticworld.gen;

import org.bukkit.block.Biome;

/**
 * Terrain and climate values for one block column. Height and biome are final (after biome shaping).
 * {@code site} is the influence (0 to 1) of a stronghold mountain site on this column, 0 everywhere else.
 */
public record Column(
        double height,
        double cont,
        double mountain,
        double ridge,
        double river,
        double temp,
        double humid,
        double variant,
        double arid,
        double dune,
        double site,
        Biome biome) {
}
