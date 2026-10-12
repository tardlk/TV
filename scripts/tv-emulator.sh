#!/bin/sh
set -eu

TV_ANDROID_ROOT="${TV_ANDROID_ROOT:-$HOME/.local/share/tv-android}"
export ANDROID_HOME="${ANDROID_HOME:-$TV_ANDROID_ROOT/sdk}"
export ANDROID_AVD_HOME="${ANDROID_AVD_HOME:-$TV_ANDROID_ROOT/avd}"
export ANDROID_ADB_SERVER_PORT="${ANDROID_ADB_SERVER_PORT:-5039}"
export ADB_MDNS_OPENSCREEN=0
export ADB_MDNS_AUTO_CONNECT=0
TV_AVD_NAME="${TV_AVD_NAME:-TV_1080p_API33}"
TV_ADB="$ANDROID_HOME/platform-tools/adb"

case "${1:-start}" in
    start)
        if [ ! -f "$ANDROID_AVD_HOME/$TV_AVD_NAME.ini" ]; then
            echo "Android TV 设备尚未配置，参见 docs/TV_EMULATOR.md" >&2
            exit 1
        fi
        # This host needs the Intel adb server through the existing Rosetta runtime.
        arch -x86_64 "$TV_ADB" -P "$ANDROID_ADB_SERVER_PORT" start-server
        trap '"$TV_ADB" -P "$ANDROID_ADB_SERVER_PORT" kill-server' EXIT
        "$ANDROID_HOME/emulator/emulator" -avd "$TV_AVD_NAME" \
            -no-snapshot -no-audio -no-boot-anim -gpu swiftshader -no-metrics
        ;;
    stop)
        for TV_SERIAL in $("$TV_ADB" -P "$ANDROID_ADB_SERVER_PORT" devices | awk '/^emulator-/ {print $1}'); do
            TV_RUNNING_NAME=$("$TV_ADB" -P "$ANDROID_ADB_SERVER_PORT" -s "$TV_SERIAL" emu avd name | tr -d '\r' | head -n 1)
            if [ "$TV_RUNNING_NAME" = "$TV_AVD_NAME" ]; then
                "$TV_ADB" -P "$ANDROID_ADB_SERVER_PORT" -s "$TV_SERIAL" emu kill
            fi
        done
        "$TV_ADB" -P "$ANDROID_ADB_SERVER_PORT" kill-server
        ;;
    *)
        echo "用法：sh scripts/tv-emulator.sh [start|stop]" >&2
        exit 2
        ;;
esac
