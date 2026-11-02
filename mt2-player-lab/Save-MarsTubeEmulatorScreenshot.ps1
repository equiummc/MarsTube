# MT2-002 Save an Android TV emulator screenshot to Windows (PowerShell 7.4+).
# No contacts to real TVs; works with emulator-5554 only.
param([string]$Device = 'emulator-5554')

$ErrorActionPreference = 'Stop'
if ($Device -notmatch '^emulator-\d+$') { throw 'Nur Emulator-Geräte erlaubt.' }
if ($PSVersionTable.PSVersion -lt [version]'7.4') {
    throw 'Screenshot-Binärumleitung erfordert PowerShell 7.4+ (pwsh).'
}
$adb = Join-Path $env:USERPROFILE 'Android-Tools\platform-tools\adb.exe'
if (-not (Test-Path $adb)) { throw "ADB fehlt: $adb" }
if (-not ((& $adb devices | Out-String) -match ("(?m)^" + [regex]::Escape($Device) + "\s+device\s*$"))) {
    throw "Emulator $Device nicht angeschlossen."
}
$folder = Join-Path ([Environment]::GetFolderPath('MyPictures')) 'MarsTube2'
New-Item -Path $folder -ItemType Directory -Force | Out-Null
$path = Join-Path $folder ("MarsTube2-Emulator-{0}.png" -f (Get-Date -Format 'yyyyMMdd-HHmmss'))
& $adb -s $Device exec-out screencap -p > $path
if ($LASTEXITCODE -ne 0 -or -not (Test-Path $path) -or (Get-Item $path).Length -lt 100) {
    throw 'Screenshot konnte nicht erstellt werden.'
}
Write-Host "Screenshot gespeichert: $path" -ForegroundColor Green
