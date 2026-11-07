#!/usr/bin/env bash
# Compila e abre a aplicação Swing. Requer JDK 17+.
set -euo pipefail
cd "$(dirname "$0")/.."
JAVA_BIN="${JAVA_HOME:+$JAVA_HOME/bin/}java"
JAVAC_BIN="${JAVA_HOME:+$JAVA_HOME/bin/}javac"
if ! "$JAVAC_BIN" -version >/dev/null 2>&1; then
  echo "Configure JAVA_HOME para um JDK 17 ou superior." >&2
  exit 1
fi
rm -rf build/main
mkdir -p build/main
find src -name '*.java' -print0 | xargs -0 "$JAVAC_BIN" -encoding UTF-8 --release 17 -d build/main
exec "$JAVA_BIN" -cp build/main controller.Main
