# MARS-ZIEL: PS7
# MarsTube 0.0.14 update-test baseline; 0.0.15 installed afterward FROM FIRE TV.
# Existing firetest014/firetest015 packages are NEVER removed or overwritten.
[CmdletBinding()]
param([string]$Device = '192.168.41.24:5555')
$ErrorActionPreference = 'Stop'
if ($PSVersionTable.PSVersion.Major -lt 7) {
    throw 'Bitte PowerShell 7 verwenden.'
}

$downloads = Join-Path $env:USERPROFILE 'Downloads'
$zip = Get-ChildItem -LiteralPath $downloads -File |
    Where-Object { $_.Name -like 'marstube-014-to-015-update-test-pair*.zip' } |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1

if (-not $zip) {
    throw 'Update-Testpaar-ZIP in Downloads fehlt. Erst GitHub-Actions-Artefakt herunterladen.'
}
Write-Host "ZIP gefunden: $($zip.Name)" -ForegroundColor Cyan

$target = Join-Path $env:TEMP ('MarsTube-UpdateTest-' + [guid]::NewGuid().ToString('N'))
Expand-Archive -LiteralPath $zip.FullName -DestinationPath $target

$apks = @(Get-ChildItem -LiteralPath $target -File -Recurse -Filter 'MarsTube-0.0.14-updatebase.apk')
if ($apks.Count -ne 1) { throw "0.0.14-Testbasis nicht eindeutig: $($apks.Count)" }
$apk = $apks[0].FullName
$manifests = @(Get-ChildItem -LiteralPath $target -File -Recurse -Filter 'SHA256SUMS')
if ($manifests.Count -ne 1) { throw 'SHA256SUMS fehlt oder ist mehrdeutig.' }
$entry = @(Get-Content -LiteralPath $manifests[0].FullName |
    Where-Object { $_ -match '^[a-fA-F0-9]{64}\s+\*?MarsTube-0[.]0[.]14-updatebase[.]apk\s*$' })
if ($entry.Count -ne 1) { throw 'SHA256SUMS hat keinen eindeutigen Eintrag zur 0.0.14-Basis.' }
$expected = ($entry[0] -split '\s+')[0]
$actual = (Get-FileHash -LiteralPath $apk -Algorithm SHA256).Hash
if ($actual -ne $expected) { throw 'APK-Hash stimmt nicht mit dem CI-Testpaar überein.' }

$sdk = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
$adb = Join-Path $sdk 'platform-tools\adb.exe'
$buildTools = Join-Path $sdk 'build-tools'
if (-not (Test-Path -LiteralPath $adb -PathType Leaf) -or
    -not (Test-Path -LiteralPath $buildTools -PathType Container)) {
    throw 'Android SDK/ADB fehlt.'
}
$aapt = Get-ChildItem -LiteralPath $buildTools -File -Recurse -Filter 'aapt.exe' |
    Sort-Object FullName -Descending |
    Select-Object -First 1
if (-not $aapt) { throw 'aapt.exe fehlt.' }

$info = @(& $aapt.FullName dump badging $apk)
$rc = $LASTEXITCODE
$packageLine = $info | Where-Object { $_ -match '^package: ' } | Select-Object -First 1
if ($rc -ne 0 -or -not $packageLine -or
    $packageLine -notmatch "name='de[.]equiummc[.]marstube2[.]updatelab[.]debug'" -or
    $packageLine -notmatch "versionCode='12014'" -or
    $packageLine -notmatch "versionName='0[.]0[.]14'") {
    throw 'Abweichende Test-APK: erwartet 0.0.14 / de.equiummc.marstube2.updatelab.debug.'
}

& $adb connect $Device | Out-Host
if ($LASTEXITCODE -ne 0) { throw 'Verbindung zum Fire TV fehlgeschlagen.' }
$model = ((& $adb -s $Device shell getprop ro.product.model) -join '').Trim()
$maker = ((& $adb -s $Device shell getprop ro.product.manufacturer) -join '').Trim()
if ($LASTEXITCODE -ne 0 -or $maker -ne 'Amazon' -or $model -ne 'AFTR') {
    throw "Falsches Zielgerät: $maker $model."
}

# Prevent accidental downgrade/re-sign attempt when the same lab package is
# already on the device. Old regular MarsTube test packages are left alone.
$existing = ((& $adb -s $Device shell dumpsys package de.equiummc.marstube2.updatelab.debug) -join "`n")
if ($existing -match 'versionCode=12015\b') {
    throw '0.0.15 ist als Update-Lab bereits installiert. Keine 0.0.14-Basis darüber installieren.'
}
if ($existing -match 'versionCode=12014\b') {
    throw 'Eine 0.0.14-Update-Testbasis ist bereits vorhanden. Für fremde CI-Testpaare keine Signatur überinstallieren.'
}

Write-Host 'Installiere einmalig die signierte MarsTube-0.0.14-Update-Testbasis ...' -ForegroundColor Cyan
& $adb -s $Device install -r $apk | Out-Host
if ($LASTEXITCODE -ne 0) { throw 'Installation fehlgeschlagen; keine bestehende MarsTube-Version wurde deinstalliert.' }
& $adb -s $Device shell monkey -p de.equiummc.marstube2.updatelab.debug -c android.intent.category.LAUNCHER 1 | Out-Host
if ($LASTEXITCODE -ne 0) { throw 'Start wurde nicht bestätigt.' }

Write-Host 'Testbasis installiert. Jetzt am Fire TV:' -ForegroundColor Green
Write-Host 'MarsTube 0.0.14 (Update-Test) > Einstellungen > Auf Updates prüfen.'
Write-Host 'Dort soll 0.0.15 angezeigt, heruntergeladen und per Android-Installer bestätigt werden.'
