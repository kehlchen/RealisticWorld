#!/bin/sh
# Prüft, dass der optimierte Generator exakt dieselben Blöcke erzeugt wie ein Referenzstand, und misst die Zeit.
# Braucht ein JDK 21+ mit javac. Die Bukkit-Klassen werden durch minimale Stubs ersetzt (nicht Paper selbst).
# Nutzung:  ./run.sh <Ordner-mit-src/main/java> [over|nether|end] [Sites]
# Für einen Vergleich zuerst mit dem alten Stand, dann mit dem neuen laufen lassen und die digest-Zeilen vergleichen.
set -e
SRC="${1:?Pfad zu src/main/java}"; MODE="${2:-over}"; SITES="${3:-24}"
HERE="$(cd "$(dirname "$0")" && pwd)"; OUT="$(mktemp -d)"
cd "$HERE" && python3 gen_stubs.py && find src -name '*.java' > "$OUT/stubs.txt"
javac -d "$OUT/stubs" @"$OUT/stubs.txt"
cd "$SRC" && javac -cp "$OUT/stubs" -d "$OUT/cls" $(find dev/realisticworld/gen dev/realisticworld/Settings.java -name '*.java') "$HERE/Bench.java" "$HERE/PopTest.java"
java -cp "$OUT/stubs:$OUT/cls" dev.realisticworld.gen.Bench "$MODE" "$SITES" 6
