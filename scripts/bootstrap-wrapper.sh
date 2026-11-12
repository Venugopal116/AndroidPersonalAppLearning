#!/usr/bin/env bash
set -eu
root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
temp_dir=$(mktemp -d)
trap 'rm -rf "$temp_dir"' EXIT
fetch() {
    curl --proto '=https' --tlsv1.2 --fail --silent --show-error --location --max-redirs 5 "$@"
}
expected=$(fetch https://services.gradle.org/distributions/gradle-8.11.1-wrapper.jar.sha256 | tr -d '[:space:]')
case "$expected" in
    ''|*[!a-f0-9]*) printf '%s\n' 'Invalid official wrapper checksum' >&2; exit 1 ;;
esac
test "${#expected}" -eq 64
fetch https://raw.githubusercontent.com/gradle/gradle/v8.11.1/gradle/wrapper/gradle-wrapper.jar -o "$temp_dir/gradle-wrapper.jar"
if command -v sha256sum >/dev/null 2>&1; then
    actual=$(sha256sum "$temp_dir/gradle-wrapper.jar" | awk '{print $1}')
else
    actual=$(shasum -a 256 "$temp_dir/gradle-wrapper.jar" | awk '{print $1}')
fi
if [ "$actual" != "$expected" ]; then
    printf '%s\n' 'Official wrapper checksum mismatch; refusing to use this artifact' >&2
    exit 1
fi
distribution=$(fetch https://services.gradle.org/distributions/gradle-8.11.1-bin.zip.sha256 | tr -d '[:space:]')
grep -Fqx "distributionSha256Sum=$distribution" "$root/gradle/wrapper/gradle-wrapper.properties"
mv "$temp_dir/gradle-wrapper.jar" "$root/gradle/wrapper/gradle-wrapper.jar"
printf 'Verified official wrapper JAR: %s\n' "$actual"
printf '%s\n' 'Bootstrap complete. Gradle itself has not been run.'