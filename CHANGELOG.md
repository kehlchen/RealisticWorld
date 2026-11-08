# Changelog

## 1.1.0

Added
- Stronghold mountains (`overworld.stronghold-mountains`): over a share of the strongholds (`chance-percent`, default 30) the terrain is replaced by a fixed mountain layout with taiga valleys, a tree line, snow and peaks, and a small lake. Peaks between Y 100 and 150 by default. The small middle mountain sits over the stronghold's starter staircase.
- No other vanilla structures start inside the landscape (plus `structure-margin`).
- `/rw mountain <list|here|at|stronghold|scan|remove|tp>`; sites are stored per world in `plugins/RealisticWorld/mountains/<world>.yml`.
- `/rw info` shows `mountain-site`.
- Test tool `perf-harness/SiteTest.java` renders a site as a PNG.

Changed
- Config version 8. Older configs are backed up and migrated; the new options use their defaults.
- Version 1.1.0.

Notes
- Only chunks generated after a site exists get the mountains; existing chunks are not changed.
- With no sites the generator produces the same blocks as 1.0.0 (checked with the test harness in `perf-harness/`).

## 1.0.0

Added
- Inland lakes (`overworld.lakes`): amount, size and depth are configurable. Lakes only form in low, flat land, not in mountains, deserts or directly at the coast.
- Desert oases: rare ponds with a grass ring and short palms (`overworld.features.oases`).
- Rock arches in deserts, badlands and eroded badlands (`overworld.features.rock-arches`).
- `/rw locate <biome> [radius]` for overworld biomes, nether biomes and end zones (asynchronous).
- `/rw preview [blocks] [seed|world]` renders a biome map as PNG without generating a world.
- `/rw version`; `/rw info` now also works in the Nether; tab completion for worlds, environments and biome names.
- Config validation at startup: warnings for unknown biome keys, clamped values, invalid size names and height values that are not multiples of 16.

Changed
- Config version 7. Older configs are backed up and migrated; the new options use their defaults.
- Version 1.0.0.

Notes
- Terrain generated with a version before 1.0.0 does not contain lakes; new chunks next to old ones show a seam. Use a new world.
- With `overworld.lakes.enabled`, `overworld.features.oases` and `overworld.features.rock-arches` set to false, the generator produces the same blocks as 0.1.0 (checked with the test harness in `perf-harness/`).


## 1.2

Added 
    - BStats support