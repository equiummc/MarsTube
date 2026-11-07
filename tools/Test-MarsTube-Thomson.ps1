#requires -Version 7.0
<#
MarsTube 0.0.20 -> 0.0.21 update comparison on Thomson TV.
This test installs the official 0.0.20 signed APK in-place only on
a positively identified Thomson/Expressluck/nagata device. It does not
uninstall apps, clear app data, or touch any other connected TV/emulator.
#>
[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$adb = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
if (-not (Test-Path -LiteralPath $adb -PathType Leaf)) {
    throw 'ADB fehlt im bekannten Android-SDK-Pfad. Bitte die ADB-Diagnose verwenden.'
}

# Android 14 may require developer options > Wireless debugging >
# Pair device with pairing code, then the separate connection IP:port.
function Get-ReadyDevices {
    $lines = @(& $adb devices)
    if ($LASTEXITCODE -ne 0) { throw 'adb devices fehlgeschlagen.' }
    return @($lines | Where-Object { $_ -match '^\S+\s+device(?:\s|$)' -and $_ -notmatch '^emulator-' } |
        ForEach-Object { ($_ -split '\s+')[0] })
}

function Get-TvIdentification([string] $Serial) {
    $model = (@(& $adb -s $Serial shell getprop ro.product.model) -join '').Trim()
    $brand = (@(& $adb -s $Serial shell getprop ro.product.brand) -join '').Trim()
    $manufacturer = (@(& $adb -s $Serial shell getprop ro.product.manufacturer) -join '').Trim()
    $device = (@(& $adb -s $Serial shell getprop ro.product.device) -join '').Trim()
    if ($LASTEXITCODE -ne 0) { throw 'Geraetedaten konnten nicht gelesen werden.' }
    return [pscustomobject]@{
        Serial = $Serial
        Model = $model
        Brand = $brand
        Manufacturer = $manufacturer
        Device = $device
        IsThomson = (($model,$brand,$manufacturer,$device -join ' ') -match 'Thomson|Expressluck|nagata')
    }
}

$ready = @(Get-ReadyDevices)
$thomsons = @($ready | ForEach-Object { Get-TvIdentification $_ } | Where-Object IsThomson)

if ($thomsons.Count -ne 1) {
    Write-Host 'Kein eindeutig erkannter Thomson per ADB verbunden.'
    Write-Host 'Am Thomson: Entwickleroptionen -> Drahtloses Debugging aktivieren.'
    Write-Host 'Wenn das Geraet nicht gekoppelt ist, im TV Kopplungscode anzeigen lassen.'
    $pair = (Read-Host 'Optional: Kopplungs-IP:Port eingeben, sonst leer lassen').Trim()
    if ($pair) {
        & $adb pair $pair
        if ($LASTEXITCODE -ne 0) {
            throw 'ADB-Kopplung fehlgeschlagen. Am Thomson den richtigen Kopplungsport anzeigen lassen.'
        }
    }
    $target = (Read-Host 'Thomson: IP-Adresse und ADB-Verbindungsport (z.B. 192.168.178.50:37123)').Trim()
    if ($target -notmatch '^\d{1,3}(?:\.\d{1,3}){3}:\d{2,5}$') {
        throw 'Ungueltige TV-Adresse. Erforderlich: IP:Port aus Drahtloses Debugging.'
    }
    & $adb connect $target
    if ($LASTEXITCODE -ne 0) { throw 'Verbindung zum Thomson nicht erfolgreich.' }
    $ready = @(Get-ReadyDevices)
    $thomsons = @($ready | ForEach-Object { Get-TvIdentification $_ } | Where-Object IsThomson)
}

if ($thomsons.Count -ne 1) {
    & $adb devices -l
    throw 'Kein eindeutig autorisierter Thomson gefunden. Keine Installation ausgefuehrt.'
}
$tv = $thomsons[0]
Write-Host ('Thomson erkannt: ' + $tv.Model + ' / ' + $tv.Device + ', ADB ' + $tv.Serial)

$serial = $tv.Serial
$package = 'de.equiummc.marstube2'
$dump = @(& $adb -s $serial shell dumpsys package $package)
if ($LASTEXITCODE -ne 0) { throw 'Installierte App-Version konnte nicht ermittelt werden.' }

$current = $null
foreach ($line in $dump) {
    if ($line -match '^\s*versionCode=(\d+)') {
        $current = [int]$Matches[1]
        break
    }
}

if ($null -ne $current) {
    Write-Host ('Bereits vorhandener MarsTube-Versionscode: ' + $current)
    if ($current -ge 12021) {
        throw 'MarsTube 0.0.21 oder neuer ist bereits installiert. Kein Downgrade fuer den Test.'
    }
    if ($current -eq 12020) {
        Write-Host 'MarsTube 0.0.20 ist bereits installiert. Keine Neuinstallation erforderlich.'
        Write-Host 'Jetzt auf dem Thomson: MarsTube -> Einstellungen -> Auf Updates pruefen.'
        return
    }
} else {
    $other = @(& $adb -s $serial shell pm list packages | Where-Object {
        $_ -match '^package:de\.equiummc\.' -and $_ -notmatch '^package:de\.equiummc\.marstube2$'
    })
    if ($other.Count -gt 0) {
        Write-Host 'Aeltere MarsTube-Varianten mit anderen Paketkennungen gefunden:'
        $other | ForEach-Object { Write-Host $_ }
        Write-Host 'Diese Alt-Apps sind unabhaengig vom Produktionspaket.'
        Write-Host 'Sie werden NICHT geloescht oder ueberschrieben.'
    }
    Write-Host 'Das Produktionspaket de.equiummc.marstube2 fehlt. Installiere die offizielle 0.0.20.'
}

$apk = Join-Path $env:TEMP 'MarsTube-0.0.20.apk'
$expected = 'B4E0350FF5FACAA2663200AC8579D967AD5CBC2363AA8BC645718C9131E4BE7C'
if (-not (Test-Path -LiteralPath $apk -PathType Leaf) -or
    (Get-FileHash -LiteralPath $apk -Algorithm SHA256).Hash -ne $expected) {
    Invoke-WebRequest -Uri 'https://github.com/equiummc/MarsTube/releases/download/v0.0.20/MarsTube-0.0.20.apk' -OutFile $apk
}
$actual = (Get-FileHash -LiteralPath $apk -Algorithm SHA256).Hash
if ($actual -ne $expected) { throw 'APK-SHA256 stimmt nicht. Keine Installation.' }

Write-Host 'Original-APK-Pruefsumme bestaetigt. Installiere v0.0.20 nur auf Thomson ...'
& $adb -s $serial install -r $apk
if ($LASTEXITCODE -ne 0) {
    throw 'Installation fehlgeschlagen. Die bisherige App NICHT deinstallieren.'
}

$check = @(& $adb -s $serial shell dumpsys package $package)
if ($LASTEXITCODE -ne 0 -or
    -not ($check | Select-String -Pattern '^\s*versionCode=12020(?:\s|$)')) {
    throw 'Installation abgeschlossen, aber erwarteter Versionscode 12020 nicht bestaetigt.'
}
Write-Host 'ERFOLG: MarsTube 0.0.20 auf Thomson installiert, vorhandene App-Daten bleiben erhalten.'
Write-Host 'JETZT: MarsTube auf Thomson oeffnen -> Einstellungen -> Auf Updates pruefen.'
Write-Host 'Erwartet: Update 0.0.21 verfuegbar. Ergebnis bitte zurueckmelden.'
