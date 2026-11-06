# MarsTube update check diagnostic. Read-only except clearing old adb log buffer.
# Run in Windows Terminal -> PowerShell 7 after connecting Android TV/Raspberry Pi via ADB.
param([string]$Device = "")
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

if (-not (Get-Command adb -ErrorAction SilentlyContinue)) {
    throw 'adb ist nicht im PATH. Bitte Android Platform-Tools starten.'
}
$connected = @(& adb devices | ForEach-Object {
    if ($_ -match '^(\S+)\s+device) { $Matches[1] }
})
if ($Device -eq '') {
    if ($connected.Count -eq 0) { throw 'Kein freigegebenes Android-ADB-Gerät verbunden.' }
    if ($connected.Count -gt 1) {
        throw ('Mehrere Geräte verbunden: ' + ($connected -join ', ') + '. Bitte -Device ANGABE setzen.')
    }
    $Device = $connected[0]
}
$adbArgs = @('-s', $Device)
$package = 'de.equiummc.marstube2'
$details = @(& adb @adbArgs shell dumpsys package $package | Select-String -Pattern 'versionCode=|versionName=' | Select-Object -First 5 | ForEach-Object { $_.Line })
if ($LASTEXITCODE -ne 0) { throw 'Android-Paketinformationen konnten nicht gelesen werden.' }
& adb @adbArgs logcat -c | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'Android-Log konnte nicht vorbereitet werden.' }

Write-Host 'Jetzt auf dem Android-TV in MarsTube -> Einstellungen -> Auf Updates prüfen.' -ForegroundColor Cyan
Read-Host 'Nach der Meldung auf dem Fernseher hier ENTER drücken'
$logs = @(& adb @adbArgs logcat -d -v time -s 'AppUpdates:V' 'AppVersionChecker:V' 'AppUpdateChecker:V' 'DownloadManager:V' 'OkHttpManager:V' '*:S')
if ($LASTEXITCODE -ne 0) { throw 'ADB-Log konnte nicht ausgelesen werden.' }

$out = Join-Path (Get-Location).Path 'MarsTube-Update-Diagnose.txt'
@('MarsTube TV Update Diagnose', ('ADB: ' + $Device), 'Paketinformationen:') + $details + @('', 'Gefilterte Update-Logs:') + $logs |
    Set-Content -LiteralPath $out -Encoding utf8
Write-Host ('Diagnose erstellt: ' + $out) -ForegroundColor Green
Write-Host 'Bitte diese Datei an ChatGPT senden. Keine APK und keine App-Daten wurden verändert.'
) { $Matches[1] }
})
if ($Device -eq '') {
    if ($connected.Count -eq 0) { throw 'Kein freigegebenes Android-ADB-Gerät verbunden.' }
    if ($connected.Count -gt 1) {
        throw ('Mehrere Geräte verbunden: ' + ($connected -join ', ') + '. Bitte -Device ANGABE setzen.')
    }
    $Device = $connected[0]
}
$adbArgs = @('-s', $Device)
$package = 'de.equiummc.marstube2'
$details = @(& adb @adbArgs shell dumpsys package $package | Select-String -Pattern 'versionCode=|versionName=' | Select-Object -First 5 | ForEach-Object { $_.Line })
if ($LASTEXITCODE -ne 0) { throw 'Android-Paketinformationen konnten nicht gelesen werden.' }
& adb @adbArgs logcat -c | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'Android-Log konnte nicht vorbereitet werden.' }

Write-Host 'Jetzt auf dem Android-TV in MarsTube -> Einstellungen -> Auf Updates prüfen.' -ForegroundColor Cyan
Read-Host 'Nach der Meldung auf dem Fernseher hier ENTER drücken'
$logs = @(& adb @adbArgs logcat -d -v time -s 'AppUpdates:V' 'AppVersionChecker:V' 'AppUpdateChecker:V' 'DownloadManager:V' 'OkHttpManager:V' '*:S')
if ($LASTEXITCODE -ne 0) { throw 'ADB-Log konnte nicht ausgelesen werden.' }

$out = Join-Path (Get-Location).Path 'MarsTube-Update-Diagnose.txt'
@('MarsTube TV Update Diagnose', ('ADB: ' + $Device), 'Paketinformationen:') + $details + @('', 'Gefilterte Update-Logs:') + $logs |
    Set-Content -LiteralPath $out -Encoding utf8
Write-Host ('Diagnose erstellt: ' + $out) -ForegroundColor Green
Write-Host 'Bitte diese Datei an ChatGPT senden. Keine APK und keine App-Daten wurden verändert.'
