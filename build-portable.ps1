# Build and ZIP a portable Windows distribution. Run with PowerShell 5.1 or newer.
[CmdletBinding()]
param(
    [string]$JarPath,
    [string]$JPackagePath,
    [string]$OutputDirectory = (Join-Path $PSScriptRoot 'portable-builds')
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

if (-not $JarPath) {
    foreach ($candidate in @(
        (Join-Path $PSScriptRoot 'RELISON\RELISON-gui\target\RELISON-gui.jar'),
        (Join-Path $PSScriptRoot 'RELISON-gui\target\RELISON-gui.jar')
    )) {
        if (Test-Path -LiteralPath $candidate -PathType Leaf) {
            $JarPath = $candidate
            break
        }
    }
}
if (-not $JarPath -or -not (Test-Path -LiteralPath $JarPath -PathType Leaf)) {
    throw 'Build the GUI JAR first, or supply -JarPath pointing to the runnable JAR with dependencies.'
}
$JarPath = (Resolve-Path -LiteralPath $JarPath).Path

if (-not $JPackagePath) {
    $command = Get-Command jpackage.exe -ErrorAction SilentlyContinue
    if ($command) { $JPackagePath = $command.Source }
    if (-not $JPackagePath -and $env:JAVA_HOME) {
        $candidate = Join-Path $env:JAVA_HOME 'bin\jpackage.exe'
        if (Test-Path -LiteralPath $candidate) { $JPackagePath = $candidate }
    }
    if (-not $JPackagePath) {
        foreach ($vendor in @('Java', 'Eclipse Adoptium', 'Microsoft')) {
            $directory = Join-Path $env:ProgramFiles $vendor
            if (Test-Path -LiteralPath $directory) {
                $found = Get-ChildItem -LiteralPath $directory -Filter jpackage.exe -Recurse -ErrorAction SilentlyContinue |
                    Sort-Object FullName -Descending | Select-Object -First 1
                if ($found) { $JPackagePath = $found.FullName; break }
            }
        }
    }
}
if (-not $JPackagePath -or -not (Test-Path -LiteralPath $JPackagePath -PathType Leaf)) {
    throw 'Cannot find jpackage.exe. Supply -JPackagePath with its full path inside your JDK bin folder.'
}
$JPackagePath = (Resolve-Path -LiteralPath $JPackagePath).Path

# Each run uses a new directory so existing distributions are preserved.
$buildName = 'RELISON-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + ([guid]::NewGuid().ToString('N').Substring(0, 6))
$buildDirectory = Join-Path $OutputDirectory $buildName
$inputDirectory = Join-Path $buildDirectory 'input'
$distributionDirectory = Join-Path $buildDirectory 'dist'
New-Item -ItemType Directory -Path $inputDirectory -Force | Out-Null
New-Item -ItemType Directory -Path $distributionDirectory -Force | Out-Null
Copy-Item -LiteralPath $JarPath -Destination (Join-Path $inputDirectory 'RELISON-gui.jar')

Write-Host "Packaging: $JarPath"
Write-Host "Using: $JPackagePath"
& $JPackagePath --type app-image --name RELISON --input $inputDirectory `
    --main-jar RELISON-gui.jar --win-console --dest $distributionDirectory
if ($LASTEXITCODE -ne 0) { throw "jpackage failed with exit code $LASTEXITCODE." }

$appDirectory = Join-Path $distributionDirectory 'RELISON'
$launcher = Join-Path $appDirectory 'RELISON.exe'
if (-not (Test-Path -LiteralPath $launcher)) { throw 'Packaging finished without producing RELISON.exe.' }

@'
RELISON portable for Windows

1. Extract the entire ZIP before running the application.
2. Double-click RELISON.exe. Keep the console window open while using RELISON.
3. If the browser does not open automatically, visit http://localhost:7070.
4. Press Ctrl+C in the console to stop the server when finished.

Java is included. Keep the app and runtime folders beside RELISON.exe.
Use Windows with the same processor architecture as the build JDK.
Libraries, fonts, icons and Earth map boundaries are included for offline use.
Settings > Use online street maps enables optional OpenStreetMap downloads.
'@ | Set-Content -LiteralPath (Join-Path $appDirectory 'README.txt') -Encoding UTF8

$zipPath = Join-Path $buildDirectory 'RELISON-Windows.zip'
Compress-Archive -LiteralPath $appDirectory -DestinationPath $zipPath -CompressionLevel Optimal
Write-Host "Portable launcher: $launcher"
Write-Host "Share this ZIP: $zipPath"
