# Install the SIGNED-PAIR v0.0.12 TV baseline. v0.0.13 updates from MarsTube UI.
# Window: VS Code -> integrated PowerShell 7. Requires unextracted pair ZIP in Downloads.
# No installed MarsTube 0.0.9-0.0.12 version is removed.
[CmdletBinding()]
param([string]$Device = "192.168.41.24:5555")
$ErrorActionPreference = "Stop"
$download = Join-Path $env:USERPROFILE "Downloads"
$zip = Get-ChildItem -LiteralPath $download -File -Filter "marstube-v013-update-lab-pair*.zip" |
    Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $zip) { throw "0.0.13-Update-Testpaar-ZIP fehlt in Downloads." }
Write-Host "ZIP gefunden: $($zip.Name)" -ForegroundColor Cyan
$dest = Join-Path $env:TEMP ("MarsTube-UpdateLab-" + [guid]::NewGuid().ToString("N"))
Expand-Archive -LiteralPath $zip.FullName -DestinationPath $dest
$matches = @(Get-ChildItem -LiteralPath $dest -Recurse -Filter "MarsTube-0.0.12-updatelab.apk" -File)
if ($matches.Count -ne 1) { throw "Genau eine 0.0.12-Lab-APK erwartet; gefunden: $($matches.Count)" }
$apk = $matches[0].FullName
$hashes = @(Get-ChildItem -LiteralPath $dest -Recurse -Filter "SHA256SUMS" -File)
if ($hashes.Count -ne 1) { throw "SHA256SUMS fehlt oder ist mehrdeutig." }
$hashLine = @(Get-Content -LiteralPath $hashes[0].FullName |
    Where-Object { $_ -match '^[A-Fa-f0-9]{64}\s+\*?(?:dist/)?MarsTube-0[.]0[.]12-updatelab[.]apk\s*$' })
if ($hashLine.Count -ne 1) { throw "SHA256-Eintrag für 0.0.12 nicht eindeutig." }
$expected = ($hashLine[0] -split '\s+')[0]
$actual = (Get-FileHash -LiteralPath $apk -Algorithm SHA256).Hash
if ($actual -ne $expected) { throw "ZIP-APK SHA256 stimmt nicht mit dem Build-Manifest überein." }
$sdk = Join-Path $env:LOCALAPPDATA "Android\Sdk"
$adb = Join-Path $sdk "platform-tools\adb.exe"
if (-not (Test-Path -LiteralPath $adb -PathType Leaf)) { throw "ADB fehlt: $adb" }
$aapt = Get-ChildItem -LiteralPath (Join-Path $sdk "build-tools") -Recurse -File -Filter "aapt.exe" |
    Sort-Object FullName -Descending | Select-Object -First 1
if (-not $aapt) { throw "aapt.exe nicht gefunden." }
$badge = (& $aapt.FullName dump badging $apk | Select-String "^package: ").Line
if ($LASTEXITCODE -ne 0 -or -not $badge -or
    $badge -notmatch "name='de[.]equiummc[.]marstube2[.]updatelab[.]debug'" -or
    $badge -notmatch "versionCode='12012'") {
    throw "Falsche APK. Erwartet MarsTube 0.0.12 UpdateLab."
}
& $adb connect $Device | Out-Host
if ($LASTEXITCODE -ne 0) { throw "ADB-Verbindung fehlgeschlagen." }
$model = ((& $adb -s $Device shell getprop ro.product.model) -join "").Trim()
$maker = ((& $adb -s $Device shell getprop ro.product.manufacturer) -join "").Trim()
if ($LASTEXITCODE -ne 0 -or $maker -ne "Amazon" -or $model -ne "AFTR") {
    throw "Falsches Gerät: $maker $model (erwartet Amazon Fire TV Cube AFTR)."
}
Write-Host "Installiere zusammen signierte 0.0.12 Update-Testbasis ..." -ForegroundColor Cyan
& $adb -s $Device install -r $apk | Out-Host
if ($LASTEXITCODE -ne 0) { throw "APK-Installation fehlgeschlagen. Keine alte MarsTube-App wurde gelöscht." }
& $adb -s $Device shell monkey -p de.equiummc.marstube2.updatelab.debug -c android.intent.category.LAUNCHER 1 | Out-Host
if ($LASTEXITCODE -ne 0) { throw "App-Start nicht bestätigt." }
Write-Host "Lab-Basis installiert. In MarsTube: Einstellungen > Info > Auf Updates prüfen." -ForegroundColor Green
