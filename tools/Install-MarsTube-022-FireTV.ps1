#requires -Version 7.0
<#
.SYNOPSIS
  Install official signed MarsTube 0.0.22 IN PLACE on ONE recognized Amazon Fire TV.
.DESCRIPTION
  Uses Google ADB, validates published SHA256SUMS, verifies target identity and
  version, and preserves Android app data. Does not uninstall or downgrade.
.EXAMPLE
  pwsh -NoProfile -File .\Install-MarsTube-022-FireTV.ps1
.EXAMPLE
  pwsh -NoProfile -File .\Install-MarsTube-022-FireTV.ps1 -TvHost 192.168.178.70:5555
#>
[CmdletBinding()]
param([string] $TvHost = '')
$ErrorActionPreference = 'Stop'
$adb = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
$package = 'de.equiummc.marstube2'
$version = '0.0.22'
$code = 12022

if (-not (Test-Path -LiteralPath $adb -PathType Leaf)) {
    throw 'ADB fehlt: Android SDK Platform-Tools auf diesem Windows-PC nicht gefunden.'
}

function Get-ConnectedFireTv {
    $listing = @(& $adb devices)
    if ($LASTEXITCODE -ne 0) { throw 'ADB-Geraete konnten nicht gelesen werden.' }
    $serials = @($listing | Where-Object {
        $_ -match '^\S+\s+device(?:\s|$)' -and $_ -notmatch '^emulator-'
    } | ForEach-Object { ($_ -split '\s+')[0] })
    $fireTvs = @()
    foreach ($serial in $serials) {
        $maker = (@(& $adb -s $serial shell getprop ro.product.manufacturer) -join '').Trim()
        $brand = (@(& $adb -s $serial shell getprop ro.product.brand) -join '').Trim()
        $model = (@(& $adb -s $serial shell getprop ro.product.model) -join '').Trim()
        $device = (@(& $adb -s $serial shell getprop ro.product.device) -join '').Trim()
        if (($maker -match '(?i)amazon') -or ($brand -match '(?i)amazon') -or
            ($model -match '(?i)fire tv') -or ($device -match '^AFT')) {
            $fireTvs += ,([pscustomobject]@{
                Serial = $serial
                Model = $model
                Manufacturer = $maker
            })
        }
    }
    return $fireTvs
}

$fireTvs = @(Get-ConnectedFireTv)
if ($fireTvs.Count -ne 1) {
    if ([string]::IsNullOrWhiteSpace($TvHost)) {
        Write-Host 'Kein eindeutiger Fire TV verbunden. ADB-Debugging in den Fire-TV-Entwickleroptionen aktivieren.'
        $TvHost = (Read-Host 'Fire-TV-IP:5555 eingeben (oder leer zum Abbrechen)').Trim()
    }
    if (-not [string]::IsNullOrWhiteSpace($TvHost)) {
        if ($TvHost -notmatch '^\d{1,3}(?:\.\d{1,3}){3}:\d{2,5}$') {
            throw 'Ungueltiges ADB-Verbindungsziel. Bitte IP:Port eingeben.'
        }
        & $adb connect $TvHost
        $fireTvs = @(Get-ConnectedFireTv)
    }
}

if ($fireTvs.Count -ne 1) {
    & $adb devices -l
    throw 'Genau ein autorisierter Amazon Fire TV muss erreichbar sein. Keine Installation erfolgt.'
}

$target = $fireTvs[0]
$serial = $target.Serial
Write-Host "Zielgeraet: $($target.Manufacturer) $($target.Model) [$serial]"
$installed = @(& $adb -s $serial shell dumpsys package $package)
if ($LASTEXITCODE -ne 0) { throw 'Installierte App konnte nicht geprueft werden.' }
$versionLine = @($installed | Select-String '^\s*versionCode=(\d+)' | Select-Object -First 1)
$installedCode = $null
if ($versionLine.Count -gt 0 -and $versionLine[0].Line -match 'versionCode=(\d+)') {
    $installedCode = [int]$Matches[1]
}

if ($null -ne $installedCode) {
    Write-Host "Installierte Versionskennung: $installedCode"
    if ($installedCode -gt $code) { throw 'Eine neuere App ist installiert. Kein Downgrade.' }
    if ($installedCode -eq $code) {
        Write-Host 'MarsTube 0.0.22 ist schon installiert. Kein erneuter Installationsversuch.'
        return
    }
}

$baseUrl = "https://github.com/equiummc/MarsTube/releases/download/v$version"
$work = Join-Path $env:TEMP 'MarsTube-022-FireTV'
New-Item -ItemType Directory -Path $work -Force | Out-Null
$checksums = Join-Path $work 'SHA256SUMS'
$apk = Join-Path $work "MarsTube-$version.apk"

Write-Host 'Lade die signierte Original-APK und ihre veroeffentlichte SHA256-Pruefsumme ...'
Invoke-WebRequest -Uri "$baseUrl/SHA256SUMS" -OutFile $checksums -ErrorAction Stop
$sumLine = @(Get-Content -LiteralPath $checksums | Where-Object {
    $_ -match "^[0-9a-fA-F]{64}\s+\*?MarsTube-0\.0\.22\.apk$"
})
if ($sumLine.Count -ne 1) {
    throw 'Keine eindeutige APK-Pruefsumme in SHA256SUMS. Abbruch.'
}
$expected = ($sumLine[0] -split '\s+')[0].ToUpperInvariant()
if (-not (Test-Path -LiteralPath $apk -PathType Leaf) -or
    (Get-FileHash -LiteralPath $apk -Algorithm SHA256).Hash -ne $expected) {
    Invoke-WebRequest -Uri "$baseUrl/MarsTube-$version.apk" -OutFile $apk -ErrorAction Stop
}
$actual = (Get-FileHash -LiteralPath $apk -Algorithm SHA256).Hash
if ($actual -ne $expected) {
    throw 'SHA-256 der APK stimmt nicht mit dem offiziellen Release ueberein. Keine Installation.'
}
Write-Host 'APK-SHA256 bestaetigt. Installiere ausschliesslich auf dem erkannten Fire TV ...'
& $adb -s $serial install -r $apk
if ($LASTEXITCODE -ne 0) {
    throw 'ADB-Installation fehlgeschlagen. Keine App deinstallieren und keine App-Daten loeschen.'
}
$after = @(& $adb -s $serial shell dumpsys package $package)
if ($LASTEXITCODE -ne 0 -or
    -not ($after | Select-String -Pattern '^\s*versionCode=12022(?:\s|$)')) {
    throw 'Android bestaetigt nach der Installation nicht die Versionskennung 12022.'
}
Write-Host 'ERFOLG: MarsTube 0.0.22 auf dem Amazon Fire TV; bestehende App-Daten erhalten.'
Write-Host 'Die automatische Update-Funktion testen wir mit der darauffolgenden Version.'
