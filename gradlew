#!/usr/bin/env sh
set -e

DIR="$(cd "$(dirname "$0")" && pwd)"
DIST_URL="https://services.gradle.org/distributions/gradle-8.4-bin.zip"
GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"
DIST_DIR="$GRADLE_USER_HOME/wrapper/dists/gradle-8.4-bin"

# Gradle 8.4 yüklü mü kontrol et, değilse indir
if [ ! -d "$DIST_DIR" ]; then
    echo "Gradle 8.4 indiriliyor..."
    mkdir -p "$DIST_DIR"
    TEMP_ZIP="$DIST_DIR/gradle-8.4-bin.zip"
    curl -L "$DIST_URL" -o "$TEMP_ZIP"
    unzip -q "$TEMP_ZIP" -d "$DIST_DIR"
    rm "$TEMP_ZIP"
fi

# Bulunan gradle 8.4'ün yürütülebilir yolunu bul
GRADLE_BIN=$(find "$DIST_DIR" -name "gradle" -type f)

exec "$GRADLE_BIN" --no-daemon -p "$DIR" "$@"
