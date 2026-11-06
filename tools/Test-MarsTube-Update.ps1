#requires -Version 7.0
<#
.SYNOPSIS
  Read-only MarsTube TV updater diagnostic.
.DESCRIPTION
  Downloads Google's official Platform-Tools if adb is missing, connects to
  exactly one authorized TV, and captures only updater-specific log tags.
  Does NOT install/uninstall anything, clear logs or change app data.
.EXAMPLE
  ./Test-MarsTube-Update.ps1 -TvHost 192.168.178.25:5555
#>
[CmdletBinding()]
param([string] $TvHost = '')

$ErrorActionPreference = 'Stop'
$work = Join-Path $env:TEMP 'MarsTube-Update-Diagnose'
New-Item -ItemType Directory -Path $work -Force | Out-Null

$locations = @(
    (Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'),
    (Join-Path $work 'platform-tools\adb.exe'),
    'C:\platform-tools\adb.exe',
    'E:\platform-tools\adb.exe',
    'E:\MarsTube\platform-tools\adb.exe'
)
if ($env:ANDROID_HOME) {
    $locations += (Join-Path $env:ANDROID_HOME 'platform-tools\adb.exe')
}
$found = Get-Command adb.exe -ErrorAction SilentlyContinue
if ($found) {
    $locations += $found.Source
}
$adb = $locations | Where-Object { Test-Path -LiteralPath $_ -PathType Leaf } | Select-Object -First 1

if (-not $adb) {
    Write-Host 'ADB fehlt. Lade ausschliesslich Googles offizielle Platform-Tools ...'
    $zip = Join-Path $work 'platform-tools-windows.zip'
    Invoke-WebRequest -Uri 'https://dl.google.com/android/repository/platform-tools-latest-windows.zip' -OutFile $zip -ErrorAction Stop
    Expand-Archive -LiteralPath $zip -DestinationPath $work -Force
    $adb = Join-Path $work 'platform-tools\adb.exe'
    if (-not (Test-Path -LiteralPath $adb -PathType Leaf)) {
        throw 'Die offiziellen Platform-Tools wurden nicht korrekt entpackt.'
    }
}

Write-Host 'ADB gefunden:' $adb
& $adb version | Select-Object -First 2
if ($LASTEXITCODE -ne 0) {
    throw 'ADB konnte nicht gestartet werden.'
}

function Get-AuthorizedTvs {
    $result = @(& $adb devices)
    if ($LASTEXITCODE -ne 0) {
        throw 'ADB-Geraeteliste fehlgeschlagen.'
    }
    return @($result | Where-Object { $_ -match '^\S+\s+device$' -and $_ -notmatch '^emulator-' })
}

$ready = @(Get-AuthorizedTvs)
if ($ready.Count -eq 0 -and -not $TvHost) {
    Write-Host 'Kein TV verbunden. Aktiviere ADB-Debugging auf deinem eigenen Fernseher.'
    $TvHost = Read-Host 'TV-IP und ADB-Port (z.B. 192.168.178.25:5555), oder leer zum Abbrechen'
}

if ($TvHost) {
    $destination = $TvHost.Trim()
    if ($destination -notmatch ':') {
        $destination = $destination + ':5555'
    }
    Write-Host 'Verbinde zum angegebenen TV ...'
    & $adb connect $destination
    $ready = @(Get-AuthorizedTvs)
}

if ($ready.Count -ne 1) {
    Write-Host 'Verbundene Geraete:'
    & $adb devices
    throw 'Es muss genau ein autorisierter TV verbunden sein (kein Emulator). Keine Aenderungen vorgenommen.'
}

$serial = ($ready[0] -split '\s+')[0]
Write-Host 'Untersuche MarsTube auf einem autorisierten TV.'
$pkg = @(& $adb -s $serial shell dumpsys package de.equiummc.marstube2)
if ($LASTEXITCODE -ne 0) {
    throw 'App-Paket konnte nicht gelesen werden.'
}
$versions = @($pkg | Select-String 'versionCode=|versionName=' | Select-Object -First 5)
if ($versions.Count -eq 0) {
    throw 'MarsTube de.equiummc.marstube2 ist nicht installiert. Nichts geaendert.'
}
Write-Host 'Installierte Paketversion:'
$versions | ForEach-Object { Write-Host $_.Line.Trim() }

$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$path = Join-Path $work ("MarsTube-Update-Log-" + $stamp + ".txt")

# Marker, not global logcat clear.
& $adb -s $serial shell log -t MarsTubeUpdateProbe "BEGIN_$stamp" | Out-Null

Write-Host ''
Write-Host 'JETZT: MarsTube am TV > Einstellungen > Auf Updates pruefen.'
Write-Host 'Wenn die App ihre Antwort anzeigt, hier ENTER druecken.'
[void](Read-Host 'Weiter')

# Only updater-related tags; no account or video logs.
$logs = @(& $adb -s $serial logcat -d -v time -s 'MarsTubeUpdateProbe:I' 'AppUpdates:V' 'AppVersionChecker:V' 'AppUpdateChecker:V' 'DownloadManager:V' 'MarsTubeUpdate:V' '*:S')
if ($LASTEXITCODE -ne 0) {
    throw 'Logcat-Auslesen fehlgeschlagen. ADB-Freigabe am TV kontrollieren.'
}
$logs | Set-Content -LiteralPath $path -Encoding utf8

$marker = $logs | Select-String "BEGIN_$stamp" | Select-Object -Last 1
$filtered = @($logs | Select-String -Pattern 'manifestLatest|manifestCode|up_to_date|update available|check result|check failed|Checking for updates|Another update|Error in JSON|newer|MarsTubeUpdateProbe')
Write-Host ''
Write-Host '===== AUSWERTUNG ====='
if ($filtered.Count) {
    $filtered | Select-Object -Last 60 | ForEach-Object { Write-Host $_.Line }
} else {
    Write-Host 'Keine passenden Updater-Logzeilen. Eventuell wurde die Abfrage nicht gestartet.'
}
if (-not $marker) {
    Write-Host 'Hinweis: Testmarker nicht gefunden.'
}
Write-Host ''
Write-Host 'Diagnosedatei:' $path
Write-Host 'Keine App wurde installiert, deinstalliert oder zurueckgesetzt.'
