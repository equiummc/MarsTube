# MarsTube In-App-Update-Diagnose, PowerShell 7 / Windows.
# Liest nur Paket- und Logdaten; loescht lediglich den alten Android-Logcat-Puffer.
param(
    [string]$Device = '',
    [string]$AdbPath = ''
)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

# adb.exe kann installiert sein, ohne im Windows-PATH zu stehen.
$candidates = [System.Collections.Generic.List[string]]::new()
if ($AdbPath) { $candidates.Add($AdbPath) }
$cmd = Get-Command adb.exe -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
if ($cmd) { $candidates.Add($cmd.Source) }
foreach ($sdk in @($env:ANDROID_HOME, $env:ANDROID_SDK_ROOT)) {
    if ($sdk) { $candidates.Add((Join-Path $sdk 'platform-tools\adb.exe')) }
}
if ($env:LOCALAPPDATA) {
    $candidates.Add((Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'))
}
if ($env:USERPROFILE) {
    $candidates.Add((Join-Path $env:USERPROFILE 'platform-tools\adb.exe'))
}
$candidates.Add('C:\platform-tools\adb.exe')
$candidates.Add('C:\Android\Sdk\platform-tools\adb.exe')
$candidates.Add('E:\platform-tools\adb.exe')
$candidates.Add('E:\Android\Sdk\platform-tools\adb.exe')
$candidates.Add('E:\MarsTube\platform-tools\adb.exe')
$adb = $null
foreach ($candidate in $candidates) {
    if ($candidate -and (Test-Path -LiteralPath $candidate -PathType Leaf)) {
        $adb = (Resolve-Path -LiteralPath $candidate).Path
        break
    }
}
if (-not $adb) {
    Write-Host 'adb.exe wurde an den ueblichen Installationsorten nicht gefunden.' -ForegroundColor Yellow
    Write-Host 'Pruefe insbesondere: %LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe'
    throw 'Android Platform-Tools fehlen oder liegen an einem anderen Ort. Bei bekanntem Pfad: Skript mit -AdbPath "C:\...\adb.exe" aufrufen.'
}
Write-Host ("ADB gefunden: " + $adb) -ForegroundColor Green
& $adb version | Select-Object -First 2
if ($LASTEXITCODE -ne 0) { throw 'Gefundene adb.exe konnte nicht ausgefuehrt werden.' }

$connected = @(& $adb devices | ForEach-Object {
    if ($_ -match '^(\S+)\s+device(?:\s|$)') { $Matches[1] }
})
if ($LASTEXITCODE -ne 0) { throw 'ADB-Geraeteliste konnte nicht gelesen werden.' }
if ($connected.Count -eq 0 -and -not $Device) {
    Write-Host 'Noch kein Android-Geraet verbunden. USB-/Netzwerk-Debugging muss am TV eingeschaltet sein.' -ForegroundColor Yellow
    $hostInput = Read-Host 'IP-Adresse vom Android TV / Raspberry Pi eingeben (leer = abbrechen)'
    if (-not $hostInput) { throw 'Keine ADB-Verbindung vorhanden. Nichts am Geraet geaendert.' }
    $endpoint = $hostInput.Trim()
    if ($endpoint -notmatch ':\d+$') { $endpoint += ':5555' }
    & $adb connect $endpoint
    $connected = @(& $adb devices | ForEach-Object {
        if ($_ -match '^(\S+)\s+device(?:\s|$)') { $Matches[1] }
    })
    if ($connected.Count -eq 0) { throw 'ADB-Verbindung nicht freigegeben/erreichbar. Auf TV-Debugging und IP achten.' }
}
if (-not $Device) {
    if ($connected.Count -eq 0) {
        Write-Host 'Kein Android-TV/Raspberry Pi mit freigegebenem USB- oder Netzwerk-Debugging verbunden.' -ForegroundColor Yellow
        throw 'Bitte ADB-Verbindung zum Zielgeraet herstellen. Noch keine Diagnose gestartet.'
    }
    if ($connected.Count -eq 1) {
        $Device = $connected[0]
    } else {
        Write-Host 'Mehrere ADB-Geraete gefunden:' -ForegroundColor Yellow
        for ($i=0; $i -lt $connected.Count; $i++) {
            Write-Host ('  ' + ($i + 1) + ': ' + $connected[$i])
        }
        $choice = Read-Host 'Nummer des Android-TV/Raspberry Pi eingeben'
        $number = 0
        if (-not [int]::TryParse($choice, [ref]$number) -or $number -lt 1 -or $number -gt $connected.Count) {
            throw 'Keine gueltige Geraeteauswahl.'
        }
        $Device = $connected[$number - 1]
    }
}
if ($Device -notin $connected) { throw ('Dieses Geraet ist nicht als verbunden/freigegeben gelistet: ' + $Device) }
$adbArgs = @('-s', $Device)
$package = 'de.equiummc.marstube2'
$details = @(& $adb @adbArgs shell dumpsys package $package |
    Select-String -Pattern 'versionCode=|versionName=' |
    Select-Object -First 5 | ForEach-Object { $_.Line })
if ($LASTEXITCODE -ne 0 -or $details.Count -eq 0) {
    throw ('MarsTube-Paket ' + $package + ' auf ' + $Device + ' nicht gefunden oder nicht lesbar.')
}
Write-Host ('Zielgeraet: ' + $Device) -ForegroundColor Cyan
$details | ForEach-Object { Write-Host $_ }

# Nur das alte Logcat-Protokoll leeren, um diese eine Updatepruefung isoliert zu sehen.
& $adb @adbArgs logcat -c | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'Android-Logcat konnte nicht vorbereitet werden.' }
Write-Host 'Jetzt auf dem Zielgeraet in MarsTube > Einstellungen > Auf Updates pruefen.' -ForegroundColor Cyan
Read-Host 'Wenn die Meldung angezeigt wird, hier ENTER druecken'
$logs = @(& $adb @adbArgs logcat -d -v time -s 'AppUpdates:V' 'AppVersionChecker:V' 'AppUpdateChecker:V' 'DownloadManager:V' 'OkHttpManager:V' 'NetPath:V' '*:S')
if ($LASTEXITCODE -ne 0) { throw 'Android-Logcat konnte nicht ausgelesen werden.' }

$out = Join-Path $PSScriptRoot 'MarsTube-Update-Diagnose.txt'
@('MarsTube Update-Diagnose', ('ADB-Geraet: ' + $Device), 'Paketinformationen:') + $details +
@('', 'Update-Logs:') + $logs |
    Set-Content -LiteralPath $out -Encoding utf8
Write-Host ('Fertig: ' + $out) -ForegroundColor Green
Write-Host 'Bitte diese Textdatei hier hochladen. MarsTube und seine Einstellungen wurden nicht veraendert.'
