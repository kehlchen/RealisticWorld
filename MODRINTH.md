# RealisticWorld

RealisticWorld replaces the default world generator of a Paper server with a climate based terrain generator. It aims for continents with coasts and shelves, tall mountain ranges with a snow line, rivers and lakes, and biomes that follow temperature and humidity instead of random noise cells. All biomes are vanilla biomes, so trees, ores, mobs and structures keep working.

Requires Paper 26.3 and Java 25. Use it on a new world: chunks that already exist keep their old terrain and show a visible seam next to new ones.

## What it changes

Overworld
- Continents, coasts, continental shelf and deep ocean from continentalness noise
- Steep mountain ranges up to the configured height limit (default Y -64 to 511 through a generated data pack for new worlds)
- Rivers with valleys and banks, inland lakes
- Biome borders blend smoothly, there are no steps between biomes
- Every biome has its own terrain shape, surface layers and small features (boulders, fallen logs, ponds, reefs, icebergs, dunes, hoodoos, oases, rock arches)
- Own cave system; lava below magma blocks in deep caves
- Stronghold mountains: over a configurable share of strongholds (default 30 percent) the terrain becomes a mountain landscape with a small peak above the starter staircase, larger massifs around it, taiga valleys, a tree line, snow-capped peaks (default Y 100 to 150) and a small lake. No other structures start inside it.

Nether
- Lava sea, canyons, stalactites, pillars, floating islands, basalt deltas, soul sand valleys, nylium forests
- Lava directly below magma blocks

End
- The main island and the dragon fight stay unchanged
- Six additional zones further out with their own terrain, surface blocks and features. The game only knows the vanilla End biomes, so each zone reports a vanilla host biome.

## Configuration

`config.yml` is split by dimension and every option is commented. Among others: continent size, land share, hill and mountain height, river width, biome size (small, medium, large, extreme), a spawn weight for every single biome, lake amount, cave density, and switches for each feature group. Changes take effect after a restart and only for newly generated chunks.

## Commands

Permission `realisticworld.admin` (default: operators), alias `/rw`.

- `/rw create <name> [normal|nether|end] [seed]` creates a world with the generator
- `/rw info` shows biome and terrain values at your position
- `/rw locate <biome> [radius]` finds the nearest biome
- `/rw preview [blocks] [seed|world]` writes a biome map PNG to `plugins/RealisticWorld/previews/`
- `/rw mountain <list|here|at|stronghold|scan|remove|tp>` manages the stronghold mountain sites
- `/rw tp <world>`, `/rw version`

## Known limitations

- Lakes use the river biome.
- Dungeons and geodes are vanilla decoration and cannot be disabled by a plugin; vanilla lava lakes are removed afterwards.
- Structures use the terrain height of the generator, individual ones can still float slightly or sit in a slope.
- Stronghold mountains only appear in chunks that are generated after the stronghold was looked up; they rely on the server's stronghold lookup (`World#locateNearestStructure`).
- `/rw locate` samples a grid and can miss very small biome patches.
