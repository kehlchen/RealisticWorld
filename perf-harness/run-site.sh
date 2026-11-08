#!/bin/sh
# Rendert eine Stronghold-Berglandschaft als PNG und gibt Höhen- und Biom-Kennzahlen aus.
# Nutzung:  ./run-site.sh <Ordner-mit-src/main/java> [seed] [ausgabe.png]
# Config-Werte lassen sich per -D überschreiben, z. B. JAVA_OPTS="-Doverworld.stronghold-mountains.size=1.5".
# Braucht ein JDK 21+ mit javac. Die Bukkit-Klassen werden durch minimale Stubs ersetzt (nicht Paper selbst).
set -e
SRC="${1:?Pfad zu src/main/java}"; SEED="${2:-12345}"; PNG="${3:-site.png}"
HERE="$(cd "$(dirname "$0")" && pwd)"; OUT="$(mktemp -d)"
cd "$HERE" && python3 gen_stubs.py && find src -name '*.java' > "$OUT/stubs.txt"
javac -d "$OUT/stubs" @"$OUT/stubs.txt"
cd "$SRC" && javac -cp "$OUT/stubs" -d "$OUT/cls" $(find dev/realisticworld/gen dev/realisticworld/Settings.java -name '*.java') "$HERE/SiteTest.java"
java $JAVA_OPTS -cp "$OUT/stubs:$OUT/cls" dev.realisticworld.gen.SiteTest "$SEED" "$PNG"
