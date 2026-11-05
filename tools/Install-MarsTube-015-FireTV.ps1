# MARS-ZIEL: PS7
# MarsTube 0.0.15 from the GitHub Actions ZIP saved in Downloads.
# Never uninstall any existing package. Confirm the ADB target and APK identity.
[CmdletBinding()]
param(
    [string]$Device = '192.168.41.24:5555'
)

$ErrorActionPreference = 'Stop'
if ($PSVersionTable.PSVersion.Major -lt 7) {
    throw 'Dieses Skript benötigt PowerShell 7 (pwsh).'
}

$downloads = Join-Path $env:USERPROFILE 'Downloads'
$zip = Get-ChildItem -LiteralPath $downloads -File |
    Where-Object { $_.Name -like 'marstube-v0.0.15-universal-back-rail-focus-debug*.zip' } |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1
if (-not $zip) {
    throw 'MarsTube 0.0.15 GitHub-ZIP im Downloads-Ordner nicht gefunden.'
}
Write-Host "ZIP gefunden: $($zip.Name)" -ForegroundColor Cyan

$target = Join-Path $env:TEMP ('MarsTube015-' + [guid]::NewGuid().ToString('N'))
Expand-Archive -LiteralPath $zip.FullName -DestinationPath $target
$apks = @(Get-ChildItem -LiteralPath $target -File -Recurse -Filter '*.apk')
if ($apks.Count -ne 1) {
    throw "Erwartet: genau eine MarsTube-APK. Gefunden: $($apks.Count)"
}
$apk = $apks[0].FullName

$sdk = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
$buildTools = Join-Path $sdk 'build-tools'
$adb = Join-Path $sdk 'platform-tools\adb.exe'
if (-not (Test-Path -LiteralPath $adb -PathType Leaf) -or
    -not (Test-Path -LiteralPath $buildTools -PathType Container)) {
    throw "Android SDK oder ADB fehlt unter $sdk"
}
$aapt = Get-ChildItem -LiteralPath $buildTools -Recurse -File -Filter 'aapt.exe' |
    Sort-Object FullName -Descending |
    Select-Object -First 1
if (-not $aapt) { throw 'aapt.exe wurde nicht gefunden.' }

$badging = @(& $aapt.FullName dump badging $apk)
$code = $LASTEXITCODE
$package = $badging | Where-Object { $_ -match '^package: ' } |
    Select-Object -First 1
if ($code -ne 0 -or -not $package -or
    $package -notmatch "name='de[.]equiummc[.]marstube2[.]firetest015[.]debug'" -or
    $package -notmatch "versionCode='12015'" -or
    $package -notmatch "versionName='0[.]0[.]15'") {
    throw 'APK-Prüfung fehlgeschlagen. Erwartet MarsTube 0.0.15.'
}

& $adb connect $Device | Out-Host
if ($LASTEXITCODE -ne 0) { throw 'Keine ADB-Verbindung.' }
$deviceModel = ((& $adb -s $Device shell getprop ro.product.model) -join '').Trim()
$manufacturer = ((& $adb -s $Device shell getprop ro.product.manufacturer) -join '').Trim()
if ($LASTEXITCODE -ne 0 -or $deviceModel -ne 'AFTR' -or $manufacturer -ne 'Amazon') {
    throw "Falsches Gerät: $manufacturer $deviceModel. Erwartet Fire TV Cube AFTR."
}

Write-Host 'Installiere MarsTube 0.0.15 auf dem Fire TV Cube ...' -ForegroundColor Cyan
& $adb -s $Device install -r $apk | Out-Host
if ($LASTEXITCODE -ne 0) { throw 'APK-Installation fehlgeschlagen.' }
Write-Host 'MarsTube 0.0.15 erfolgreich installiert.' -ForegroundColor Green

& $adb -s $Device shell monkey -p de.equiummc.marstube2.firetest015.debug -c android.intent.category.LAUNCHER 1 | Out-Host
if ($LASTEXITCODE -ne 0) { throw 'Startbefehl fehlgeschlagen.' }
Write-Host 'MarsTube gestartet. BACK, Menübreite und Settings-Fokus jetzt am TV testen.' -ForegroundColor Green
