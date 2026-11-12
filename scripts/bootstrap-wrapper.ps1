$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$wrapper = Join-Path $root 'gradle/wrapper/gradle-wrapper.jar'
$expected = (Invoke-WebRequest -UseBasicParsing 'https://services.gradle.org/distributions/gradle-8.11.1-wrapper.jar.sha256').Content.Trim()
Invoke-WebRequest -UseBasicParsing 'https://raw.githubusercontent.com/gradle/gradle/v8.11.1/gradle/wrapper/gradle-wrapper.jar' -OutFile $wrapper
$actual = (Get-FileHash $wrapper -Algorithm SHA256).Hash.ToLowerInvariant()
if ($actual -ne $expected) { throw 'Official wrapper checksum mismatch; do not run the wrapper' }
$distribution = (Invoke-WebRequest -UseBasicParsing 'https://services.gradle.org/distributions/gradle-8.11.1-bin.zip.sha256').Content.Trim()
$properties = Get-Content (Join-Path $root 'gradle/wrapper/gradle-wrapper.properties') -Raw
if (-not $properties.Contains("distributionSha256Sum=$distribution")) { throw 'Distribution checksum does not match the published checksum' }
Write-Output "Verified official Gradle wrapper JAR: $actual"
Write-Output "Verified pinned distribution checksum: $distribution"
Write-Output 'No Gradle distribution, SDK or project dependencies were downloaded.'