$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$required = @('settings.gradle.kts', 'build.gradle.kts', 'gradle/libs.versions.toml',
    'shared/build.gradle.kts', 'backend/build.gradle.kts', 'app/build.gradle.kts',
    'app/src/main/AndroidManifest.xml', 'app/src/main/res/xml/network_security_config.xml',
    'app/src/main/res/xml/data_extraction_rules.xml',
    'backend/src/main/resources/db/V001__identity.sql', '.github/workflows/verify.yml',
    'gradlew', 'gradlew.bat', 'gradle/wrapper/gradle-wrapper.properties')
foreach ($relative in $required) {
    if (-not (Test-Path (Join-Path $root $relative))) { throw "Missing $relative" }
}
$catalog = Get-Content (Join-Path $root 'gradle/libs.versions.toml') -Raw
if ($catalog -notmatch 'agp = "8.9.2"' -or $catalog -notmatch 'kotlin = "2.1.20"') {
    throw 'Unexpected build baseline'
}
Get-ChildItem $root -Filter '*.xml' -Recurse | ForEach-Object {
    $null = [xml](Get-Content $_.FullName -Raw)
}
$manifest = [xml](Get-Content (Join-Path $root 'app/src/main/AndroidManifest.xml') -Raw)
$namespace = 'http://schemas.android.com/apk/res/android'
$permissions = @($manifest.manifest.'uses-permission' | ForEach-Object { $_.GetAttribute('name', $namespace) })
if ($permissions.Count -ne 2 -or $permissions -notcontains 'android.permission.INTERNET' -or
    $permissions -notcontains 'android.permission.ACCESS_NETWORK_STATE') { throw 'Unexpected foundation permissions' }
if ($manifest.manifest.application.GetAttribute('allowBackup', $namespace) -ne 'false' -or
    $manifest.manifest.application.GetAttribute('usesCleartextTraffic', $namespace) -ne 'false') {
    throw 'Backup or cleartext policy is not fail-closed'
}
$aliases = @{}
$section = ''
foreach ($line in ($catalog -split "`n")) {
    if ($line -match '^\[([^\]]+)\]') { $section = $Matches[1] }
    if ($section -in @('libraries', 'plugins') -and $line -match '^([a-z][a-z0-9-]*)\s*=') {
        $key = $Matches[1].Replace('-', '.')
        if ($section -eq 'plugins') { $key = "plugins.$key" }
        $aliases[$key] = $true
    }
}
Get-ChildItem $root -Filter '*.gradle.kts' -Recurse | ForEach-Object {
    $source = Get-Content $_.FullName -Raw
    foreach ($match in [regex]::Matches($source, 'libs\.([a-zA-Z0-9.]+)')) {
        if (-not $aliases.ContainsKey($match.Groups[1].Value)) { throw "Unknown catalog alias: $($match.Value)" }
    }
}
Get-ChildItem $PSScriptRoot -Filter '*.ps1' | ForEach-Object {
    $tokens = $null
    $errors = $null
    $null = [System.Management.Automation.Language.Parser]::ParseFile($_.FullName, [ref]$tokens, [ref]$errors)
    if ($errors.Count -gt 0) { throw "PowerShell syntax error in $($_.Name): $errors" }
}
$tests = @(Get-ChildItem $root -Filter '*Test.kt' -Recurse)
if ($tests.Count -lt 7) { throw 'Expected foundation test suites are missing' }
Write-Output "PASS: structure, XML, permission/backup/network policy, catalog references, PowerShell syntax; $($tests.Count) Kotlin test suites present."
if (-not (Test-Path (Join-Path $root 'gradle/wrapper/gradle-wrapper.jar'))) {
    Write-Output 'OPEN GATE: official wrapper JAR download blocked locally. Bootstrap in an authorized environment before CLI builds.'
}
Write-Output 'Kotlin compilation, unit/integration/instrumentation execution and cloud deployment remain unverified.'