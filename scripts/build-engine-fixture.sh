#!/bin/sh
set -eu
TV_ANDROID_ROOT="${TV_ANDROID_ROOT:-$HOME/.local/share/tv-android}"
export JAVA_HOME="${JAVA_HOME:-$TV_ANDROID_ROOT/jdk-17/Contents/Home}"
TV_SDK="${ANDROID_HOME:-$TV_ANDROID_ROOT/sdk}"
TV_FLAVOR="${1:-python64}"
TV_VARIANT="${TV_FLAVOR}Debug"
# Locate the output across supported Android Gradle plugin layouts.
TV_CLASSES=$(find "app/build/intermediates/javac/$TV_VARIANT" -type d -name classes -print | head -n 1)
TV_FIXTURE_DIR=.local-test/engine-fixture
mkdir -p "$TV_FIXTURE_DIR/classes" "$TV_FIXTURE_DIR/dex" app/src/androidTest/assets
"$JAVA_HOME/bin/javac" -source 8 -target 8 -cp "$TV_SDK/platforms/android-35/android.jar:$TV_CLASSES" \
  -d "$TV_FIXTURE_DIR/classes" tests/fixtures/jar/com/github/catvod/spider/*.java
"$JAVA_HOME/bin/jar" cf "$TV_FIXTURE_DIR/classes.jar" -C "$TV_FIXTURE_DIR/classes" .
"$TV_SDK/build-tools/34.0.0/d8" --min-api 28 --lib "$TV_SDK/platforms/android-35/android.jar" \
  --classpath "$TV_CLASSES" --output "$TV_FIXTURE_DIR/dex" "$TV_FIXTURE_DIR/classes.jar"
"$JAVA_HOME/bin/jar" cf app/src/androidTest/assets/engine-smoke.jar -C "$TV_FIXTURE_DIR/dex" classes.dex
