#Requires -Version 7.0
<#
  MarsTube 0.0.31 ARM32: TV-Test ohne Deinstallation oder Datenloeschung.
  Sony -> Fire TV -> Thomson. Native original signing certificate pinned.
  APK artifact: https://github.com/equiummc/MarsTube/actions/runs/42470643264
#>
[CmdletBinding()]
param([string]$Root='E:\MarsTube')
Set-StrictMode -Version Latest
$ErrorActionPreference='Stop'
$Pkg='de.equiummc.marstube2'
$Version=12031
$Cert='f80f712f9dbbd16c692e5d5eaff7598db73a660c82911bfd3ff0d0215c8f84e9'
$ArtifactRun=42470643264
$ArtifactName='MarsTube-0.0.31-ARM32-ARM64-verified'
$FileName='MarsTube-0.0.31-ARM32.apk'
$Stage=Join-Path $Root '.mt2-build\TV-Test-031'
$ApkFolder=Join-Path $Root 'APK'
$ReportFolder=Join-Path $Root 'diagnostics'
if (-not $IsWindows) {throw 'PowerShell 7 unter Windows erforderlich.'}
function Assert-Exit([string]$Name) {
    if ($LASTEXITCODE -ne 0) {throw "$Name fehlgeschlagen (Code $LASTEXITCODE)."}
}
function Get-DeviceStrings([string]$Serial) {
    $raw=@(& $script:Adb -s $Serial shell getprop ro.product.model 2>&1)
    Assert-Exit 'Gerätemodell auslesen'
    $manufacturer=((& $script:Adb -s $Serial shell getprop ro.product.manufacturer 2>&1) | Out-String).Trim()
    Assert-Exit 'Hersteller auslesen'
    $abi=((& $script:Adb -s $Serial shell getprop ro.product.cpu.abilist 2>&1) | Out-String).Trim()
    Assert-Exit 'ABI auslesen'
    $sdk=((& $script:Adb -s $Serial shell getprop ro.build.version.sdk 2>&1) | Out-String).Trim()
    Assert-Exit 'SDK auslesen'
    return [pscustomobject]@{Serial=$Serial;Model=($raw | Out-String).Trim();Manufacturer=$manufacturer;ABI=$abi;SDK=$sdk}
}
function Get-Devices {
    $lines=@(& $script:Adb devices -l)
    Assert-Exit 'ADB-Geräteliste'
    $serials=@($lines | Where-Object {$_ -match '^\S+\s+device(\s|$)'} |
        ForEach-Object {($_ -split '\s+')[0]})
    $list=@()
    foreach ($serial in $serials) {
        try {$list+=Get-DeviceStrings $serial}
        catch {Write-Warning "ADB-Geraet $serial nicht auslesbar: $_"}
    }
    return $list
}
function Target-Matches([string]$Kind,$Info) {
    $id="$($Info.Model) $($Info.Manufacturer)"
    switch ($Kind) {
        'Sony' {return ($id -match 'Sony|BRAVIA')}
        'Fire TV' {return ($id -match 'Amazon|AFT|Fire TV')}
        'Thomson' {return ($id -match 'Thomson|Expressluck|nagata')}
    }
    return $false
}
function Get-MemoryKB([string]$Serial) {
    $raw=((& $script:Adb -s $Serial shell cat /proc/meminfo 2>&1) | Out-String)
    if ($LASTEXITCODE -ne 0) {return $null}
    if ($raw -match '(?m)^MemAvailable:\s*(\d+)\s*kB') {return [long]$Matches[1]}
    return $null
}
function Format-Memory($KB) {
    if ($null -eq $KB) {return 'nicht messbar'}
    return ('{0:N0} MB' -f ($KB/1024))
}
function Get-InstalledVersion([string]$Serial) {
    $dump=((& $script:Adb -s $Serial shell dumpsys package $script:Pkg 2>&1) | Out-String)
    if ($LASTEXITCODE -ne 0) {return $null}
    if ($dump -match 'versionCode=(\d+)') {return [int64]$Matches[1]}
    return $null
}
Write-Host 'MarsTube 0.0.31 ARM32 – Sony, Fire TV, Thomson' -ForegroundColor Cyan
Write-Host 'Es werden keine Apps deinstalliert, keine Daten geloescht und keine Einstellungen veraendert.'
$sd=Join-Path $env:LOCALAPPDATA 'Android\Sdk'
if ($env:ANDROID_HOME -and (Test-Path -LiteralPath $env:ANDROID_HOME)) {$sd=$env:ANDROID_HOME}
$adb=Join-Path $sd 'platform-tools\adb.exe'
$bt=Join-Path $sd 'build-tools\37.0.0'
$aapt=Join-Path $bt 'aapt.exe'
$signer=Join-Path $bt 'apksigner.bat'
$align=Join-Path $bt 'zipalign.exe'
foreach ($f in @($adb,$aapt,$signer,$align)) {
    if (-not (Test-Path -LiteralPath $f -PathType Leaf)) {throw "Android-Werkzeug fehlt: $f"}
}
New-Item -ItemType Directory -Force -Path $Stage,$ApkFolder,$ReportFolder | Out-Null
$apk=Join-Path $ApkFolder $FileName
if (-not (Test-Path -LiteralPath $apk)) {
    $zipDir=Join-Path $Stage 'verified-artifact'
    $stageApk=Join-Path $zipDir $FileName
    if (-not (Test-Path -LiteralPath $stageApk)) {
        $downloaded=$false
        $gh=Get-Command gh.exe -ErrorAction SilentlyContinue
        if ($null -ne $gh) {
            Write-Host 'Versuche verifiziertes GitHub-Artefakt automatisch abzurufen ...'
            & $gh.Source run download $ArtifactRun -R 'equiummc/MarsTube' -n $ArtifactName -D $zipDir
            if ($LASTEXITCODE -eq 0 -and (Test-Path -LiteralPath $stageApk)) {$downloaded=$true}
        }
        if (-not $downloaded) {
            # Accept the browser download exactly where the user downloaded it.
            $userDownloads=Join-Path $HOME 'Downloads'
            $archives=@()
            if (Test-Path -LiteralPath $userDownloads) {
                $archives=@(Get-ChildItem -LiteralPath $userDownloads -File -Filter "$ArtifactName*.zip" |
                    Sort-Object LastWriteTime -Descending)
            }
            if ($archives.Count -eq 0) {
                throw "APK-Archiv fehlt. Bitte GitHub-Actions-Artefakt '$ArtifactName' unter https://github.com/equiummc/MarsTube/actions/runs/$ArtifactRun herunterladen. Die ZIP im Downloads-Ordner liegen lassen und dieses Skript erneut starten."
            }
            Write-Host "Entpacke heruntergeladenes Artefakt: $($archives[0].Name)"
            Expand-Archive -LiteralPath $archives[0].FullName -DestinationPath $zipDir -Force
        }
    }
    if (-not (Test-Path -LiteralPath $stageApk)) {throw "ARM32-APK fehlt nach dem Entpacken: $stageApk"}
    $shaFile=Join-Path $zipDir 'SHA256SUMS'
    if (-not (Test-Path -LiteralPath $shaFile)) {throw 'SHA256SUMS im Artefakt fehlt.'}
    $line=@(Get-Content -LiteralPath $shaFile | Where-Object {$_ -match '\s+MarsTube-0\.0\.31-ARM32\.apk$'})
    if ($line.Count -ne 1 -or $line[0] -notmatch '^([a-fA-F0-9]{64})\s+') {throw 'ARM32-SHA256 fehlt oder ist nicht eindeutig.'}
    $expected=$Matches[1]
    if ((Get-FileHash -LiteralPath $stageApk -Algorithm SHA256).Hash -ne $expected) {throw 'ARM32-Datei stimmt nicht mit SHA256SUMS ueberein.'}
    Copy-Item -LiteralPath $stageApk -Destination $apk -ErrorAction Stop
}
# The signature fingerprint was extracted from the trusted signed universal APK.
$badge=((& $aapt dump badging $apk 2>&1) | Out-String)
Assert-Exit 'Paket-/Versionspruefung'
if ($badge -notmatch "package: name='de\.equiummc\.marstube2' versionCode='12031' versionName='0\.0\.31'") {
    throw 'Falsches Paket oder falsche Version.'
}
$certOutput=((& $signer verify --print-certs $apk 2>&1) | Out-String)
Assert-Exit 'Signaturpruefung'
if ($certOutput -notmatch 'certificate SHA-256 digest:\s*([a-fA-F0-9]{64})' -or $Matches[1].ToLowerInvariant() -ne $Cert) {
    throw 'Release-Zertifikat stimmt nicht mit MarsTube Original ueberein.'
}
& $align -c -P 16 4 $apk 1>$null
Assert-Exit 'Native-Alignment-Pruefung'
Add-Type -AssemblyName System.IO.Compression.ZipFile
$z=[IO.Compression.ZipFile]::OpenRead($apk)
try {
    $libs=@($z.Entries | Where-Object {$_.FullName -match '^lib/[^/]+/.+\.so$'} |
        ForEach-Object {($_.FullName -split '/')[1]} | Sort-Object -Unique)
    if ($libs.Count -ne 1 -or $libs[0] -ne 'armeabi-v7a') {
        throw "Falsche APK-Architekturen: $($libs -join ',')"
    }
} finally {$z.Dispose()}
Write-Host ("ARM32-APK geprueft: {0:N2} MB" -f ((Get-Item -LiteralPath $apk).Length/1MB)) -ForegroundColor Green
$report=[collections.generic.list[string]]::new()
$report.Add('MarsTube 0.0.31 TV-Pruefung am '+(Get-Date -Format 'yyyy-MM-dd HH:mm:ss'))
$report.Add('APK: '+$FileName)
$report.Add('APK SHA256: '+(Get-FileHash -LiteralPath $apk -Algorithm SHA256).Hash)
$devices=@(Get-Devices)
Write-Host 'Aktuell verbundene Geräte:'
foreach ($d in $devices) {Write-Host ("  {0} | {1} | {2} | {3}" -f $d.Model,$d.Manufacturer,$d.ABI,$d.Serial)}
if ($devices.Count -eq 0) {Write-Host 'Noch kein autorisiertes ADB-Geraet gefunden.'}
$testOrder=@('Sony','Fire TV','Thomson')
foreach ($kind in $testOrder) {
    Write-Host ''
    Write-Host "=== $kind ===" -ForegroundColor Cyan
    $match=@($devices | Where-Object {Target-Matches $kind $_})
    $info=$null
    if ($match.Count -eq 1) {
        $info=$match[0]
    } elseif ($match.Count -gt 1) {
        Write-Warning "Mehrere passende $kind-Geraete. Bitte IP/Seriennummer manuell eingeben."
    }
    if ($null -eq $info) {
        $hint=if ($kind -eq 'Fire TV') {' (Enter = 192.168.41.24:5555 versuchen)'} else {' (Enter = ueberspringen)'}
        $address=(Read-Host "$kind-IP[:Port] oder Seriennummer$hint").Trim()
        if (-not $address -and $kind -eq 'Fire TV') {$address='192.168.41.24:5555'}
        if (-not $address) {
            $report.Add("$kind | uebersprungen – keine Verbindung")
            continue
        }
        if ($address -match '^\d{1,3}(\.\d{1,3}){3}$') {$address+=':5555'}
        $serial=$address
        if ($address -match '^\d{1,3}(\.\d{1,3}){3}:\d+$') {
            Write-Host "ADB verbinde $address ..."
            $connect=((& $adb connect $address 2>&1) | Out-String)
            if ($LASTEXITCODE -ne 0) {
                Write-Warning "ADB-Verbindung fehlgeschlagen: $connect"
                $report.Add("$kind | ADB-Verbindung fehlgeschlagen")
                continue
            }
        }
        try {$info=Get-DeviceStrings $serial}
        catch {
            Write-Warning "Geraet nicht erreichbar oder ADB nicht autorisiert: $_"
            $report.Add("$kind | ADB nicht autorisiert / nicht erreichbar")
            continue
        }
        if (-not (Target-Matches $kind $info)) {
            Write-Warning "Geräteidentitaet passt nicht sicher zu '$kind': $($info.Model) / $($info.Manufacturer). Keine Installation."
            $report.Add("$kind | Gerateidentitaet ungeprueft")
            continue
        }
    }
    Write-Host "Gerät: $($info.Model), Android-SDK $($info.SDK), ABI $($info.ABI)"
    if (($info.ABI -split ',') -notcontains 'armeabi-v7a') {
        Write-Warning 'Kein ARM32-Geraet. Installation unterbleibt.'
        $report.Add("$kind | falsche Architektur: $($info.ABI)")
        continue
    }
    $current=Get-InstalledVersion $info.Serial
    if ($null -ne $current -and $current -gt $Version) {
        Write-Warning "Aktuell installierte Version ist neuer (versionCode $current). Kein Downgrade."
        $report.Add("$kind | neueres App-Build vorhanden: $current")
        continue
    }
    $ramBefore=Get-MemoryKB $info.Serial
    Write-Host "Freier/verfuegbarer RAM laut Android vor Installation: $(Format-Memory $ramBefore)"
    if ($kind -eq 'Thomson' -and $null -ne $ramBefore -and $ramBefore -lt 153600) {
        Write-Warning 'Thomson meldet weniger als 150 MB verfuegbaren RAM. Installation vorsichtshalber uebersprungen.'
        $report.Add("$kind | wegen RAM-Druck uebersprungen: $(Format-Memory $ramBefore)")
        continue
    }
    $answer=(Read-Host "$kind jetzt als Update/Erstinstallation einspielen? (J = ja, sonst ueberspringen)").Trim()
    if ($answer -notmatch '^(j|ja)$') {
        $report.Add("$kind | vom Nutzer uebersprungen")
        continue
    }
    Write-Host "Installiere ARM32 ohne Deinstallation ..."
    $result=((& $adb -s $info.Serial install -r $apk 2>&1) | Out-String)
    if ($LASTEXITCODE -ne 0 -or $result -notmatch 'Success') {
        Write-Warning "Installation fehlgeschlagen; keine Daten geloescht: $result"
        $report.Add("$kind | Installation fehlgeschlagen: $result")
        continue
    }
    $installed=Get-InstalledVersion $info.Serial
    if ($installed -ne $Version) {
        Write-Warning "Die installierte Versionsnummer konnte nicht bestaetigt werden ($installed)."
        $report.Add("$kind | Installation meldet Erfolg, Versionscheck unklar")
        continue
    }
    Write-Host "$kind: MarsTube 0.0.31 installiert." -ForegroundColor Green
    Write-Host 'Teste jetzt mit der Fernbedienung: App starten, ein normales Video, ein Short, ZURUECK-Taste.'
    [void](Read-Host 'Nach dem kurzen Test Enter druecken, um den RAM erneut auszulesen')
    $ramAfter=Get-MemoryKB $info.Serial
    Write-Host "Android MemAvailable nach dem Test: $(Format-Memory $ramAfter)"
    $report.Add("$kind | installiert 0.0.31 ARM32 | ABI $($info.ABI) | RAM vorher: $(Format-Memory $ramBefore) | RAM nachher: $(Format-Memory $ramAfter)")
    if ($kind -eq 'Thomson' -and $null -ne $ramAfter -and $ramAfter -lt 153600) {
        Write-Warning 'Thomson hat nach dem Test weniger als 150 MB verfuegbaren RAM. Test jetzt beenden; keine Belastungstests.'
    }
}
$reportPath=Join-Path $ReportFolder ('MT2-031-TV-Architektur-Test-'+(Get-Date -Format 'yyyyMMdd-HHmmss')+'.txt')
$report | Set-Content -LiteralPath $reportPath -Encoding utf8
Write-Host ''
Write-Host "Fertig. Bericht: $reportPath" -ForegroundColor Green
