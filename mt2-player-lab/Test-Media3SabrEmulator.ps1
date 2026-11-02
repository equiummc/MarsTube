# MT2-001: reproducible native Media3 SABR vs DASH test on Android TV emulator.
# Targets only emulator-serials; does not touch real TVs.
[CmdletBinding()]
param(
    [ValidateSet('Sabr','Dash','Report','Mouse')]
    [string]$Mode = 'Sabr',
    [string]$Device = 'emulator-5554',
    [string]$Package = 'io.github.aleixrodriala.arc.debug'
)

$ErrorActionPreference = 'Stop'
if ($Device -notmatch '^emulator-\d+$') {
    throw 'Dieses Testskript akzeptiert nur Emulatoren (emulator-5554 usw.).'
}

$adb = Join-Path $env:USERPROFILE 'Android-Tools\platform-tools\adb.exe'
if (-not (Test-Path -LiteralPath $adb)) {
    $fallback = Get-Command 'adb.exe' -ErrorAction SilentlyContinue
    if (-not $fallback) { throw 'ADB nicht gefunden. Android Platform Tools pruefen.' }
    $adb = $fallback.Source
}

function Invoke-EmulatorAdb {
    param([Parameter(Mandatory = $true)][string[]]$Arguments)
    & $adb -s $Device @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "ADB-Aufruf fehlgeschlagen: $($Arguments -join ' ')"
    }
}

$state = (& $adb -s $Device get-state 2>&1 | Out-String).Trim()
if ($LASTEXITCODE -ne 0 -or $state -ne 'device') {
    throw "Emulator $Device nicht verbunden. Status: $state"
}

switch ($Mode) {
    'Sabr' {
        # Keep the app's native client selection. Do not override identities or tokens.
        # Never enable the SABR-only fallback that is known to hit attestation limits.
        Invoke-EmulatorAdb -Arguments @('shell','setprop','debug.arc.sabr_vod','1')
        Invoke-EmulatorAdb -Arguments @('shell','setprop','debug.arc.sabr_fallback','0')
        Invoke-EmulatorAdb -Arguments @('shell','setprop','debug.arc.player_client','none')
        Write-Host 'SABR-Versuch aktiviert (debug.arc.sabr_vod=1, normale Clientwahl).' -ForegroundColor Cyan
    }
    'Dash' {
        Invoke-EmulatorAdb -Arguments @('shell','setprop','debug.arc.sabr_vod','0')
        Invoke-EmulatorAdb -Arguments @('shell','setprop','debug.arc.sabr_fallback','0')
        Invoke-EmulatorAdb -Arguments @('shell','setprop','debug.arc.player_client','none')
        Write-Host 'DASH-Vergleich aktiviert (normale Clientwahl).' -ForegroundColor Cyan
    }
    'Report' {
        $raw = @(& $adb -s $Device logcat -d -v time -s 'NetPath:D' 'AndroidRuntime:E' '*:S')
        if ($LASTEXITCODE -ne 0) { throw 'Logcat fehlgeschlagen.' }
        $interesting = @($raw | Where-Object {
            $_ -match 'NetPath.*(prepare |first-frame|picture-visible|error |info |player-ring|sabr|Sabr|rebuffer|reload|end)' -and
            $_ -notmatch 'https?://|[?&]pot=|[?&]sig=|Authorization|Bearer |Cookie:|visitorData='
        } | Select-Object -Last 100)
        $dir = Join-Path ([Environment]::GetFolderPath('MyDocuments')) 'MarsTube2\Logs'
        New-Item -ItemType Directory -Force -Path $dir | Out-Null
        $path = Join-Path $dir ("Media3-SABR-{0}.txt" -f (Get-Date -Format 'yyyyMMdd-HHmmss'))
        $interesting | Set-Content -LiteralPath $path -Encoding UTF8

        Write-Host 'Gefiltertes Wiedergabeprotokoll:' -ForegroundColor Cyan
        $interesting | Select-Object -Last 35 | ForEach-Object { Write-Host $_ }
        Write-Host "Gespeichert: $path"
        if (@($interesting | Where-Object { $_ -match 'prepare .*type=sabr-vod' }).Count -gt 0) {
            Write-Host 'SABR-Quelle im Protokoll nachgewiesen.' -ForegroundColor Green
        } elseif (@($interesting | Where-Object { $_ -match 'prepare .*type=dash' }).Count -gt 0) {
            Write-Warning 'Nur DASH nachgewiesen. SABR-Option allein ist KEIN SABR-Beweis.'
        } else {
            Write-Warning 'Kein eindeutiger Quellen-Nachweis. Protokoll/Testvideo pruefen.'
        }
        break
    }
    'Mouse' {
        $scrcpy = Get-Command 'scrcpy.exe' -ErrorAction SilentlyContinue
        if (-not $scrcpy) {
            throw 'scrcpy nicht gefunden. Installieren: winget install --exact --id Genymobile.scrcpy'
        }
        Write-Host 'scrcpy startet. Linksklick = Touch; Rechtsklick = Zurueck.' -ForegroundColor Cyan
        & $scrcpy.Source --serial $Device --mouse=sdk --no-audio --no-mouse-hover
        break
    }
}

if ($Mode -in @('Sabr','Dash')) {
    $pref = (& $adb -s $Device shell getprop debug.arc.sabr_vod | Out-String).Trim()
    $client = (& $adb -s $Device shell getprop debug.arc.player_client | Out-String).Trim()
    if (($Mode -eq 'Sabr' -and $pref -ne '1') -or
        ($Mode -eq 'Dash' -and $pref -ne '0') -or
        $client -ne 'none') {
        throw "Einstellungen nicht gesetzt: sabr=$pref client=$client"
    }
    Invoke-EmulatorAdb -Arguments @('shell','am','force-stop',$Package)
    Invoke-EmulatorAdb -Arguments @('logcat','-c')
    Invoke-EmulatorAdb -Arguments @('shell','monkey','-p',$Package,'1')
    Start-Sleep -Seconds 6
    # Existing NewTube phone layout: select first thumbnail with a real touch event.
    $size = (& $adb -s $Device shell wm size | Out-String).Trim()
    $matchSize = [regex]::Match($size, '(\d+)x(\d+)')
    if ($matchSize.Success) {
        $x = [int]([int]$matchSize.Groups[1].Value * 0.25)
        $y = [int]([int]$matchSize.Groups[2].Value * 0.35)
        Invoke-EmulatorAdb -Arguments @('shell','input','touchscreen','tap',"$x","$y")
        Write-Host "Touch-Eingabe auf erstes Video bei x=$x y=$y gesendet."
    } else {
        Write-Warning 'Bildschirmgroesse nicht erkannt; Video bitte manuell starten.'
    }
    Write-Host 'NewTube gestartet. Pruefen, ob das Video spielt; mindestens 120 Sekunden abspielen.' -ForegroundColor Green
    Write-Host 'Danach Test-Media3SabrEmulator.ps1 -Mode Report ausfuehren.'
    Write-Host 'Mit -Mode Dash denselben Test als DASH-Vergleich wiederholen.'
}
