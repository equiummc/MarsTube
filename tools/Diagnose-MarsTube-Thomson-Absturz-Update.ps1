#requires -Version 7.0
# Read-only collection: crash first, then manual updater check. No installation or reset.
$ErrorActionPreference = 'Stop'
$adb = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
$pkg = 'de.equiummc.marstube2'
if (-not (Test-Path -LiteralPath $adb -PathType Leaf)) { throw 'ADB nicht gefunden.' }
$devices = @(& $adb devices | Where-Object { $_ -match '^\S+\s+device(?:\s|$)' -and $_ -notmatch '^emulator-' } | ForEach-Object { ($_ -split '\s+')[0] })
$thomsons = @($devices | Where-Object {
    $model = (@(& $adb -s $_ shell getprop ro.product.model) -join '').Trim()
    $device = (@(& $adb -s $_ shell getprop ro.product.device) -join '').Trim()
    $model -match 'Thomson' -or $device -eq 'nagata'
})
if ($thomsons.Count -ne 1) {
    & $adb devices -l
    throw 'Genau ein autorisierter Thomson muss per ADB verbunden sein.'
}
$serial = $thomsons[0]
$packageInfo = @(& $adb -s $serial shell dumpsys package $pkg)
$versions = @($packageInfo | Select-String 'versionCode=|versionName=' | Select-Object -First 4)
if ($versions.Count -eq 0) { throw 'Die regulaere MarsTube-App ist auf dem Thomson nicht installiert.' }
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$dir = Join-Path $env:TEMP "MarsTube-Diagnose-$stamp"
New-Item -ItemType Directory -Path $dir -Force | Out-Null
Write-Host 'Lese zuerst die noch vorhandenen Absturzdaten ...'
$crash = @(& $adb -s $serial logcat -b crash -d -v threadtime)
$crash | Set-Content -LiteralPath (Join-Path $dir 'crash-roh.txt') -Encoding utf8
$exitInfo = @(& $adb -s $serial shell dumpsys activity exit-info $pkg)
$exitInfo | Set-Content -LiteralPath (Join-Path $dir 'exit-info-roh.txt') -Encoding utf8
$updateTags = @('AppUpdates:V','AppVersionChecker:V','AppUpdateChecker:V','MarsTubeUpdate:V','AndroidRuntime:E','*:S')
$before = @(& $adb -s $serial logcat -d -v threadtime -s $updateTags)
Write-Host ''
Write-Host 'Jetzt am Thomson: REGULAERE MarsTube-App > Einstellungen > Auf Updates pruefen.'
Write-Host 'Wenn das Ergebnis angezeigt wird, am PC ENTER druecken.'
[void](Read-Host 'Weiter')
$after = @(& $adb -s $serial logcat -d -v threadtime -s $updateTags)
$after | Set-Content -LiteralPath (Join-Path $dir 'updates-roh.txt') -Encoding utf8
$crashes = @($crash | Select-String -Pattern 'Process: de\.equiummc\.marstube2' -Context 5,55 | ForEach-Object {
    $_.Context.PreContext + $_.Line + $_.Context.PostContext
})
$reason = @($exitInfo | Select-String -Pattern 'reason=|REASON_|description=|de\.equiummc\.marstube2|low memory|ANR' | Select-Object -Last 55 | ForEach-Object { $_.Line })
$newUpdates = if ($after.Count -ge $before.Count) { @($after | Select-Object -Skip $before.Count) } else { @($after | Select-Object -Last 90) }
$summary = @('THOMSON / MARSTUBE-DIAGNOSE',$pkg) + @($versions | ForEach-Object { $_.Line.Trim() })
$summary += '--- UPDATE NACH MANUELLER PRUEFUNG ---'
if ($newUpdates.Count -gt 0) { $summary += @($newUpdates | Select-Object -Last 90) }
else { $summary += 'Keine neuen Updater-Logs gefunden.' }
$summary += '--- LETZTER JAVA-ABSTURZ ---'
if ($crashes.Count -gt 0) { $summary += @($crashes | Select-Object -Last 120) }
else { $summary += 'Kein MarsTube-Java-Absturz im Crash-Puffer. Moeglich: Prozessabbruch/ANR/OOM oder Puffer ueberschrieben.' }
$summary += '--- SYSTEM-PROZESSENDEN ---'
if ($reason.Count -gt 0) { $summary += $reason }
else { $summary += 'Kein eindeutiger Prozessgrund in exit-info gefunden.' }
$report = Join-Path $dir 'ERGEBNIS.txt'
$summary | Set-Content -LiteralPath $report -Encoding utf8
Write-Host ''
Write-Host 'Diagnose fertig. Bitte nur diese Datei hochladen:'
Write-Host $report
Write-Host 'Die Rohlogs bleiben lokal. Es wurde nichts deinstalliert, installiert oder geloescht.'
