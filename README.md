# RealisticWorld

Paper-Plugin für Minecraft 26.3, das den Overworld-Generator durch ein klimabasiertes, realistischeres Terrain ersetzt.

## Features

- Kontinente, Küsten, Schelf und Tiefsee über Continentalness-Noise mit Domain Warping
- Hohe, steile Gebirgsketten (Ridged Noise, schmale Übergangszone), Schneegrenze abhängig von Temperatur
- Flüsse mit Talformen und Uferstreifen aus Kies, Sand oder Grobem Dreck
- Klimamodell (Temperatur, Feuchtigkeit, Höhenabkühlung) bestimmt die Vanilla-Biome, daher funktionieren Bäume, Erze, Höhlen und Mobs weiter
- Vanilla-Structures werden generiert; das Plugin liefert dafür die eigene Terrainhöhe (`getBaseHeight`)
- Jedes Biom hat eigene Terrainformen, Oberflächen und Features (siehe unten)
- Lush Caves und Dripstone Caves über 3D-Noise, mit eigenen Höhlen-Features
- Höhenlimit konfigurierbar (Standard: Y -64 bis 511)
- Das Plugin bindet sich selbst an Overworld, Nether und End der Default-Welt (bukkit.yml wird gesetzt)
- Binnenseen in flachem Tiefland (Größe, Menge und Tiefe einstellbar); sie liegen auf Meereshöhe, zählen als Fluss-Biom und bekommen Kies- oder Sandufer
- Eigene Höhlen (Käsehöhlen, Spaghetti-Tunnel, Dolinen) statt Vanilla-Carvern; deren Aquifers passen nicht zum eigenen Terrain und erzeugten Luftblasen im Ozean sowie zufälliges Wasser und Lava
- Biom-Höhenformen werden über einen Gauß-Kernel (ca. 60 Blöcke) ineinander übergeblendet, es entstehen keine Stufen an Biomgrenzen
- Vanilla-Lavaseen oberhalb von Y -40 werden entfernt (abschaltbar); Lava unter Magma-Blöcken bleibt erhalten
- Lava unter Magma-Blöcken (1 bis 2 Blöcke tief, einstellbar): im Nether auf Magma-Flächen und am Lavameer, im Overworld in tiefen Höhlen als Magma-Vents
- Nether: Lavameer, Canyons, Stalaktiten, durchgehende Säulen, schwebende Inseln, Basaltdeltas mit Zacken, Soul-Sand-Täler als Senken, Nylium-Wälder; eigene Features (Basaltspitzen, Blackstone-Ruinen, Knochenrippen)
- End: runde Hauptinsel mit Unterseite, Lücke mit kleinen Inseln, große äußere Inseln mit Terrassen, hängende Spitzen und schwebende Splitter; die äußeren Bereiche sind in Zonen mit eigenem Terrain, eigener Oberfläche und eigenen Features aufgeteilt (siehe unten)

## Biom-Besonderheiten

Meere: Sandbänke (Ocean), Gräben (Deep Ocean), Korallenriffe (Warm), Sandrippel (Lukewarm), Sandterrassen (Deep Lukewarm), Felsbrocken am Boden (Cold), Seeberge (Deep Cold), Eisberge und Packeis (Frozen, Deep Frozen).
Küsten: Dünenrippel und Treibholz (Beach), Eisschollen (Snowy Beach), Steilküste mit Felsnadeln (Stony Shore).
Wüste und Mesa: Dünen, Sandstein-Schichtung, Kiesflächen, Buttes, Hoodoos, seltene Oasen mit Teich und Palmen, Felsbögen (Desert); Felsbögen auch in Badlands und Eroded Badlands; Terrakotta-Terrassen (Badlands, Wooded Badlands); Spitztürme (Eroded Badlands).
Savannen: Kopjes, Plateaus mit Terrakotta-Kanten, windgeformte Terrassen.
Gemäßigt: Teiche und Wildblumenfelder (Plains, Sunflower Plains), umgestürzte Stämme, Pilze, Moos- und Wurzelboden (Forest, Flower Forest, Birch, Old Growth Birch, Dark Forest).
Kalt: Podzol, Moosfelsen, umgestürzte Fichten (Taiga-Varianten); gefrorene Teiche und Schneeverwehungen (Snowy Plains, Snowy Taiga); Eisplateaus (Ice Spikes).
Tropen und Sumpf: Karsttürme (Bamboo Jungle), schroffes Relief und Moosfelsen (Jungle, Sparse Jungle), flache Sümpfe mit Gezeitenkanälen (Swamp, Mangrove Swamp).
Hochland: Blumenwiesen, Teiche (Meadow); Blütenblätter und Teiche (Cherry Grove); Terrassen und Felsbrocken (Grove); Gletschereis und Kare (Snowy Slopes, Frozen Peaks); zerklüftete Grate (Jagged Peaks); Calcit, Andesit und Spitzen (Stony Peaks); Geröllfelder (Windswept-Varianten).
Höhlen: Tonbecken (Lush Caves), Dripstone-Hügel mit Calcit (Dripstone Caves).

## End-Biome

Die Hauptinsel (Drachenkampf) und alles, was näher am Zentrum liegt als `end.main-island.radius` + 100 Blöcke, bleiben unverändert. Weiter außen gibt es sechs zusätzliche Zonen. Minecraft kennt im End nur die fünf Vanilla-Biome; ein Plugin kann keine neuen Biom-IDs registrieren. Jede Zone meldet deshalb ein Vanilla-Biom (Mobs, Chorus und End Cities richten sich danach), F3 zeigt dessen Namen, `/rw info` im End zeigt die Zone.

| Zone | Vanilla-Biom | Terrain und Oberfläche | Features |
|---|---|---|---|
| chorus_forest | End Midlands | flach, End Stone mit Purpur-Flecken | dichte Chorus-Pflanzen |
| crystal_fields | End Midlands | Spitzkuppen, Amethyst und Calcit | Amethyst-Spitzen mit Clustern |
| obsidian_wastes | End Barrens | zerklüftete Grate aus Obsidian, Weinender Obsidian | Obsidian-Säulen |
| ender_dunes | End Barrens | Dünenrippel aus End Stone und Calcit | Rippen eines toten Drachen aus Knochenblöcken |
| purpur_ruins | End Highlands | Terrassen mit End-Stone-Ziegeln und Purpur | zerbrochene Purpursäulen-Reihen mit Balken |
| void_rifts | Small End Islands | schmale Risse durch die Inseln, Obsidian am Rand | keine |

Höhenunterschiede zwischen den Zonen werden weich ineinander übergeblendet, Oberflächen wechseln an der Zonengrenze. Die vier klassischen Zonen (`end_highlands`, `end_midlands`, `end_barrens`, `small_end_islands`) behalten das bisherige glatte Terrain und konkurrieren mit den neuen Zonen um Fläche. End Cities entstehen weiterhin nur dort, wo das Vanilla-Biom End Highlands oder End Midlands ist und die Insel groß genug ist. Mit `end.custom-biomes: false` wird das End außerhalb der Hauptinsel wie vorher erzeugt.

## Stronghold-Berge

Über einem Teil der Strongholds wird das Terrain durch eine feste Berglandschaft ersetzt. Das Layout folgt einer Skizze: ein kleiner Berg in der Mitte, größere Massive im Nordwesten, Osten und Südwesten, kleinere im Norden und Süden und ein kleiner See im Südosten. Der mittlere Berg liegt über der Starttreppe des Strongholds (der Mittelpunkt ist Block 4/4 im Stronghold-Chunk; dort liegt nach bisherigem Wissen die Starttreppe, das ist nicht an einem echten Server geprüft).

- Gipfelhöhen zwischen `peak-min-y` und `peak-max-y` (Standard Y 100 bis 150, absolute Höhe). Der kleine mittlere Berg ist der niedrigste, das große Ostmassiv der höchste.
- In den Tälern liegt Taiga (Taiga, Old Growth Pine und Spruce Taiga). Über `treeline-y` (Standard 98) wachsen keine Bäume mehr: kahler Fels (Stony Peaks), darüber Schnee (Snowy Slopes ab `snow-line-y`) und Gipfel (Frozen und Jagged Peaks).
- Innerhalb der Landschaft und `structure-margin` darum herum starten keine anderen Vanilla-Structures. Der Chunk mit dem Stronghold-Start ist ausgenommen; ein anderes Structure kann in genau diesem einen Chunk theoretisch trotzdem starten. Große Structures von außerhalb können mit einzelnen Teilen hineinragen.
- Die Chance pro Stronghold steht in `overworld.stronghold-mountains.chance-percent` (Standard 30). Sie wird je Stronghold einmal aus dem Weltseed gewürfelt und ändert sich nicht, wenn die Strongholds in anderer Reihenfolge gefunden werden.
- Gefunden werden die Strongholds beim Laden einer Welt über die Server-Abfrage nach dem nächsten Stronghold, von einem Raster aus (bis `scan-radius`, Standard 12000 Blöcke um den Ursprung). Das Ergebnis steht in `plugins/RealisticWorld/mountains/<welt>.yml`.
- Solange die Strongholds einer Welt noch nicht gesucht wurden, meldet der Biom-Provider die Biome so, als gäbe es keine Berge. Vanilla leitet die Stronghold-Positionen aus den Biomen ab; sie sollen bei jedem Start gleich bleiben.

Befehle (`/rw mountain ...`):

- `list` zeigt die Orte, `tp <n>` teleportiert hin
- `here` und `at <x> <z>` legen einen Ort an beliebiger Stelle an
- `stronghold` legt einen Ort über dem nächsten Stronghold an, unabhängig von der Chance
- `scan` sucht die Strongholds erneut und würfelt für neue (bestehende Orte bleiben)
- `remove [x z]` entfernt den nächsten Ort

Die Landschaft entsteht nur in Chunks, die noch nicht generiert sind. Bereits generierte Chunks ändert der Befehl nicht; er meldet, wie viele Chunks des Gebiets schon existieren. Zum Neugenerieren die Region-Dateien des Gebiets löschen.

## Installation

1. `gradle build`, die Jar aus `build/libs` nach `plugins/` kopieren.
2. Frische Welt verwenden: Weltordner löschen, Server starten. Das Plugin trägt `worlds.<level-name>.generator: RealisticWorld` in `bukkit.yml` ein und schreibt das Höhen-Datapack in den Weltordner. Bei der allerersten Welt kann ein zweiter Start nötig sein, falls das Setzen im laufenden Server nicht greift (steht dann im Log).
3. Zusätzliche Welt: `/rw create <name> [normal|nether|end] [seed]`.
4. Nether und End der Default-Welt: eingetragen werden `<level-name>_nether` mit `RealisticWorld:nether` und `<level-name>_the_end` mit `RealisticWorld:end`. Falls die Welten bei deinem Server anders heißen, die Einträge in `bukkit.yml` anpassen.
5. Alte Configs: bei `config-version` unter 3 wird `structures` automatisch auf `true` gesetzt.

## Konfiguration

`config.yml` ist nach Dimension gegliedert (`overworld`, `nether`, `end`) und enthält unter anderem: Kontinentgröße, Land-Anteil, Hügel- und Berghöhe, Gebirgsanteil, Flussbreite, Klima (Biomgröße, Temperatur, Feuchtigkeit), Dünen, Höhlendichte, Dolinen, Lava-Höhe in Höhlen, Magma-Vents, jede Feature-Gruppe einzeln, Nether (Lava-Höhe, Boden- und Deckenhöhe, Canyons, Säulen, Inseln, Magma mit Lava, Features) und End (Hauptinsel, Außeninseln, Splitter, Spitzen). Jede Option steht mit Kommentar in der Datei. Beim Start prüft das Plugin die Config und warnt im Log bei unbekannten Biom-Schlüsseln, Werten außerhalb des erlaubten Bereichs, ungültigen Größenangaben und Höhenwerten, die nicht durch 16 teilbar sind. Alte Configs werden beim Start gesichert (`config-backup-v<N>.yml`), auf das neue Layout gebracht und die bekannten Werte übernommen.

Biome: `end.biome-size` (gleiche Stufen) bestimmt die Größe der End-Zonen. `overworld.climate.biome-size` (small, medium, large, extreme; auch klein, mittel, gross, extrem) bestimmt die Größe der klima-basierten Biomregionen, `nether.biome-size` gilt für den Nether. Unter `overworld.biomes.weights`, `nether.biomes.weights` und `end.biomes.weights` hat jedes Biom ein Spawn-Gewicht (1.0 Standard, 0.5 halb so häufig, 2.0 doppelt so häufig, 0 nie). Die Gewichte verschieben nur den Anteil zwischen Biomen, die zum selben Klima und zur selben Höhe passen. Küsten, Flüsse und Gebirge sind von der Biomgröße nicht betroffen (dafür `continent-size`, `horizontal-scale` und die Berg-Optionen).

Höhen-Datapack: Das Plugin sucht die Vanilla-Dateien im Classloader und in den Jars unter `cache/`, `versions/` und im Serverordner. Falls das nicht klappt, kopiere `data/minecraft/dimension_type/overworld.json` und `pack.mcmeta` aus einem Vanilla-Server-Jar nach `plugins/RealisticWorld/vanilla/` (gleiche relative Pfade).

## Befehle

- `/rw create <name> [normal|nether|end] [seed]`
- `/rw info` zeigt Biom und Terrainwerte an der eigenen Position
- `/rw tp <welt>`
- `/rw mountain <list|here|at|stronghold|scan|remove|tp>` (siehe Stronghold-Berge)

Permission: `realisticworld.admin`

## Hinweise

- Änderungen an `config.yml` wirken erst nach einem Neustart, weil die Generatoren beim Laden des Plugins gebaut werden. Ein `/rw reload` gibt es deshalb nicht.
- Seen verwenden das Biom River (Minecraft erlaubt Plugins keine neuen Biom-IDs). Dort leben daher Lachse statt eigener Seen-Mobs.
- Bereits generierte Chunks einer bestehenden Welt werden nicht umgebaut. Neue Chunks nutzen den Generator, dazwischen entstehen sichtbare Kanten.
- Das Höhenlimit wird über ein Datapack mit überschriebenem Overworld-Dimension-Typ gesetzt. Bestehende Overworlds dürfen ihre Höhe nicht ändern.
- Der Generator passt sich der tatsächlichen Welthöhe an. Ohne Height-Pack (`world-height.enabled: false`) werden die Berge auf -64 bis 319 gestaucht.
- Dungeons, Geoden und Lavaseen sind Vanilla-Decoration und lassen sich per API nicht einzeln abschalten. Lavaseen entfernt das Plugin nachträglich, Dungeons und Geoden bleiben.
- Die Platzierung von Vanilla-Structures nutzt Terrainhöhen aus dem Generator. Einzelne Structures können dennoch leicht schweben oder im Hang stecken.
