#!/bin/sh
set -eu
TV_ANDROID_ROOT="${TV_ANDROID_ROOT:-$HOME/.local/share/tv-android}"
export JAVA_HOME="${JAVA_HOME:-$TV_ANDROID_ROOT/jdk-17/Contents/Home}"
export ANDROID_HOME="${ANDROID_HOME:-$TV_ANDROID_ROOT/sdk}"
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-$TV_ANDROID_ROOT/gradle-cache}"
TV_DEFAULT_PYTHON="$TV_ANDROID_ROOT/python-3.8/bin/python3.8"
if [ ! -x "$TV_DEFAULT_PYTHON" ]; then
    TV_DEFAULT_PYTHON="$(command -v python3)"
fi
export TV_BUILD_PYTHON="${TV_BUILD_PYTHON:-$TV_DEFAULT_PYTHON}"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"
cd "$(dirname "$0")/.."
if [ ! -x "$JAVA_HOME/bin/java" ] || [ ! -d "$ANDROID_HOME/platforms/android-33" ]; then
    echo "JDK 或 Android SDK 未就绪，参见 docs/ENVIRONMENT.md" >&2
    exit 1
fi
if [ ! -f local.properties ]; then
    printf 'sdk.dir=%s\n' "$ANDROID_HOME" > local.properties
fi
if [ "$#" -eq 0 ]; then
    set -- :app:assemblePython32Debug
fi
exec sh ./gradlew "$@" --no-daemon
