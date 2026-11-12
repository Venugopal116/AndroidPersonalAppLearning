#!/usr/bin/env bash
set -eu
SDK_PATH="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
if [ -z "$SDK_PATH" ] || [ ! -d "$SDK_PATH" ]; then
    printf '%s\n' 'Android SDK directory is missing. Set ANDROID_SDK_ROOT or ANDROID_HOME on the build runner.' >&2
    exit 1
fi
manager=''
for candidate in \
    "$SDK_PATH/cmdline-tools/latest/bin/sdkmanager" \
    "$SDK_PATH"/cmdline-tools/*/bin/sdkmanager \
    "$SDK_PATH/tools/bin/sdkmanager"; do
    if [ -x "$candidate" ]; then
        manager="$candidate"
        break
    fi
done
if [ -z "$manager" ]; then
    manager=$(command -v sdkmanager || true)
fi
if [ -z "$manager" ]; then
    printf 'Android command-line tools were not found inside %s or on PATH. Check the Codemagic Android SDK image.\n' "$SDK_PATH" >&2
    exit 1
fi
printf 'Using Android SDK manager: %s\n' "$manager"
"$manager" --sdk_root="$SDK_PATH" "platforms;android-35" "build-tools;35.0.0"
printf 'sdk.dir=%s\n' "$SDK_PATH" > local.properties