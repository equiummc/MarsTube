# MarsTube 0.0.12 - safe side-by-side Fire TV Cube tester (PowerShell 7)
# Run in: VS Code > Integrated PowerShell 7; unpack CI artifact ZIP first.
# Usage: & "E:\MarsTube\tools\Install-MarsTube-012-FireTV.ps1" -ApkPath "E:\path\to\your\file.apk"
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$ApkPath,

    [string]$Device = '192.168.41.24:5555'
)

$ErrorActionPreference = 'Stop'
$adb = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
if (-not (Test-Path -LiteralPath $adb -PathType Leaf)) {
    throw "ADB nicht gefunden: $adb"
}

$resolved = (Resolve-Path -LiteralPath $ApkPath -ErrorAction Stop).ProviderPath
if (-not (Test-Path -LiteralPath $resolved -PathType Leaf) -or
        [IO.Path]::GetExtension($resolved).ToLowerInvariant() -ne '.apk') {
    throw "Keine gueltige APK-Datei: $ApkPath"
}
if ([IO.Path]::GetFileName($resolved) -notmatch '0[.]0[.]11|stmobile.*debug') {
    throw "APK-Dateiname nicht eindeutig als MarsTube 0.0.12 Debug-Build erkennbar."
}

Write-Host 'APK:' $resolved -ForegroundColor Cyan
Get-FileHash -LiteralPath $resolved -Algorithm SHA256 | Format-List

$state = & $adb -s $Device get-state 2>$null
if ($LASTEXITCODE -ne 0 -or ($state -join '').Trim() -ne 'device') {
    & $adb connect $Device | Out-Host
    if ($LASTEXITCODE -ne 0) { throw 'ADB-Verbindung fehlgeschlagen.' }
}

$model = (& $adb -s $Device shell getprop ro.product.model) -join ''
$maker = (& $adb -s $Device shell getprop ro.product.manufacturer) -join ''
if ($LASTEXITCODE -ne 0 -or $model.Trim() -ne 'AFTR' -or $maker.Trim() -ne 'Amazon') {
    throw "Falsches Zielgeraet: $maker / $model. Erwartet: Amazon Fire TV Cube AFTR."
}

# Separate package: older 0.0.9/0.0.10/0.0.11 tests stay installed.
Write-Host "Installiere MarsTube 0.0.12 auf $Device ..." -ForegroundColor Cyan
& $adb -s $Device install -r $resolved
if ($LASTEXITCODE -ne 0) { throw 'Installation fehlgeschlagen.' }

& $adb -s $Device shell monkey -p de.equiummc.marstube2.firetest012.debug -c android.intent.category.LAUNCHER 1 | Out-Host
if ($LASTEXITCODE -ne 0) { throw 'Start konnte nicht bestaetigt werden.' }
Write-Host 'MarsTube 0.0.12 Startbefehl gesendet. Vorhandene 0.0.9/0.0.10/0.0.11 bleiben erhalten.' -ForegroundColor Green
