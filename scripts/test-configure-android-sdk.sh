#!/usr/bin/env bash
set -eu
root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
temp_dir=$(mktemp -d)
trap 'rm -rf "$temp_dir"' EXIT
cd "$temp_dir"
for layout in cmdline-tools/latest/bin cmdline-tools/19.0/bin tools/bin; do
    sdk="$temp_dir/sdk with spaces"
    mkdir -p "$sdk/$layout"
    printf '#!/bin/sh\nprintf "%%s\\n" "$@"\n' > "$sdk/$layout/sdkmanager"
    chmod +x "$sdk/$layout/sdkmanager"
    output=$(ANDROID_SDK_ROOT="$sdk" ANDROID_HOME='' "$BASH" "$root/scripts/configure-android-sdk.sh")
    printf '%s\n' "$output" | grep -Fx -- "--sdk_root=$sdk"
    printf '%s\n' "$output" | grep -Fx 'platforms;android-35'
    printf '%s\n' "$output" | grep -Fx 'build-tools;35.0.0'
    grep -Fx "sdk.dir=$sdk" local.properties
    rm "$sdk/$layout/sdkmanager"
done
PATH="$temp_dir/no-tools" ANDROID_SDK_ROOT='' ANDROID_HOME="$sdk" "$BASH" "$root/scripts/configure-android-sdk.sh" > missing-tool.log 2>&1 && {
    printf '%s\n' 'Expected missing-tool failure' >&2
    exit 1
}
grep -F 'Android command-line tools were not found' missing-tool.log
PATH="$temp_dir/no-tools" ANDROID_SDK_ROOT='' ANDROID_HOME='' "$BASH" "$root/scripts/configure-android-sdk.sh" > missing-sdk.log 2>&1 && {
    printf '%s\n' 'Expected missing-SDK failure' >&2
    exit 1
}
grep -F 'Android SDK directory is missing' missing-sdk.log
mkdir -p "$temp_dir/path-tools"
printf '#!/bin/sh\nprintf "%%s\\n" "$@"\n' > "$temp_dir/path-tools/sdkmanager"
chmod +x "$temp_dir/path-tools/sdkmanager"
output=$(PATH="$temp_dir/path-tools" ANDROID_SDK_ROOT='' ANDROID_HOME="$sdk" "$BASH" "$root/scripts/configure-android-sdk.sh")
printf '%s\n' "$output" | grep -Fx -- "--sdk_root=$sdk"
printf '%s\n' 'PASS: SDK-local/PATH tool discovery, ANDROID_HOME fallback, paths with spaces, arguments, properties and failure diagnostics. No SDK packages installed.'