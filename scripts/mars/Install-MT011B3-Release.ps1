# MT-011B31: signed release-mode TCL stage diagnostic install for Thomson only.
# Protects existing app data, does not modify primary Git checkout.
$ErrorActionPreference = 'Stop'
$adb = Join-Path $env:USERPROFILE 'Android-Tools\platform-tools\adb.exe'
$repo = Join-Path $env:USERPROFILE 'MarsTV\mars-smarttube'
$tv = '192.168.41.53:5555'
$device = 'nagata'
$package = 'de.equiummc.mars.smarttube'
$branch = 'feature/mt-011b3-sabr-queue-preloader-20261031'
$backupRoot = Join-Path $env:USERPROFILE 'MarsTV\backups\MT011B3'
$testRoot = Join-Path $env:TEMP ("MTB3-" + $PID)
$createdWorktree = $false
$completed = $false

function Checked([string]$name) {
    if ($LASTEXITCODE -ne 0) { throw "$name fehlgeschlagen (Exit $LASTEXITCODE)." }
}
function SignerDigest([string]$apk, [string]$signer) {
    $result = (& $signer verify --verbose --print-certs $apk 2>&1 | Out-String)
    Checked 'APK-Signaturpruefung'
    $match = [regex]::Match($result, '(?im)Signer #1 certificate SHA-256 digest:\s*([0-9a-f:]+)')
    if (-not $match.Success) { throw "Zertifikatsfingerabdruck nicht auslesbar: $apk" }
    return ($match.Groups[1].Value -replace ':','').ToLowerInvariant()
}

if (-not (Test-Path $adb)) { throw "ADB fehlt: $adb" }
if (-not (Test-Path (Join-Path $repo '.git'))) { throw "Repo fehlt: $repo" }
if (Test-Path $testRoot) { throw "Temporärer Arbeitsordner bereits vorhanden: $testRoot" }

Write-Host '[1] Ausschliesslich Thomson identifizieren' -ForegroundColor Cyan
& $adb connect $tv | Out-Host
Checked 'ADB-Verbindung'
$foundDevice = (& $adb -s $tv shell getprop ro.product.device | Out-String).Trim()
Checked 'Geraetekennung'
if ($foundDevice -ne $device) { throw "STOPP: TV ist '$foundDevice', erwartet '$device'." }

Write-Host '[2] Installierte MarsTube-APK sichern' -ForegroundColor Cyan
New-Item -ItemType Directory -Force -Path $backupRoot | Out-Null
$locations = @(& $adb -s $tv shell pm path $package | Where-Object { $_ -match '^package:' })
Checked 'Paketpfad'
if ($locations.Count -ne 1) { throw "STOPP: erwarte genau eine installierte Base-APK; gefunden $($locations.Count)." }
$remote = ($locations[0] -replace '^package:','').Trim()
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$oldApk = Join-Path $backupRoot "MarsTube-Before-MT011B3-$stamp.apk"
& $adb -s $tv pull $remote $oldApk | Out-Host
Checked 'APK-Backup'
if (-not (Test-Path $oldApk) -or (Get-Item $oldApk).Length -lt 1000000) {
    throw "Backup fehlgeschlagen."
}
Write-Host "Backup: $oldApk" -ForegroundColor Green

Write-Host '[3] Separaten Git-Worktree vorbereiten' -ForegroundColor Cyan
& git -C $repo fetch origin "refs/heads/${branch}:refs/remotes/origin/${branch}" | Out-Host
Checked 'Git fetch'
$commit = (& git -C $repo rev-parse "refs/remotes/origin/$branch" | Out-String).Trim()
Checked 'Branch SHA'
& git -C $repo worktree add --detach $testRoot $commit | Out-Host
Checked 'Git-Worktree'
$createdWorktree = $true

try {
    & git -c core.longpaths=true -C $testRoot submodule update --init --recursive | Out-Host
    Checked 'Submodule'
    $sub = Join-Path $testRoot 'MediaServiceCore'
    $pinned = (& git -C $sub rev-parse HEAD | Out-String).Trim()
    Checked 'MediaServiceCore SHA'
    if ($pinned -ne '40fec843eb24bc264e9c8b429fcee25068e230e2') {
        throw 'STOPP: Unerwartete MediaServiceCore-Version.'
    }

    # Git worktrees do not copy ignored local.properties. Copy only the
    # working Android SDK configuration, never reset or modify the main checkout.
    Write-Host '[3a] Android-SDK fuer den isolierten Worktree konfigurieren' -ForegroundColor Cyan
    $sourceLocal = Join-Path $repo 'local.properties'
    $targetLocal = Join-Path $testRoot 'local.properties'
    $sdkPath = $null
    if (Test-Path $sourceLocal) {
        $sdkLine = Get-Content -Path $sourceLocal |
            Where-Object { $_ -match '^\s*sdk\.dir\s*=' } |
            Select-Object -First 1
        if ($sdkLine) {
            # Java properties often escape Windows colons and backslashes.
            $rawSdk = ($sdkLine -split '=', 2)[1].Trim()
            $candidate = $rawSdk.Replace('\:', ':').Replace('\\', '\')
            if ((Test-Path -LiteralPath $candidate -PathType Container) -and
                    (Test-Path -LiteralPath (Join-Path $candidate 'build-tools'))) {
                $sdkPath = (Resolve-Path -LiteralPath $candidate).Path
                Copy-Item -LiteralPath $sourceLocal -Destination $targetLocal -Force
                Write-Host 'Vorhandene local.properties in den Worktree uebernommen.'
            } else {
                Write-Warning 'SDK-Pfad in local.properties ist nicht verfuegbar; pruefe bekannte SDK-Pfade.'
            }
        }
    }
    if (-not $sdkPath) {
        $sdkCandidates = @(
            $env:ANDROID_HOME
            $env:ANDROID_SDK_ROOT
            (Join-Path $env:LOCALAPPDATA 'Android\Sdk')
            (Join-Path $env:USERPROFILE 'AppData\Local\Android\Sdk')
        ) | Where-Object { $_ } | Select-Object -Unique
        foreach ($candidate in $sdkCandidates) {
            if ((Test-Path -LiteralPath $candidate -PathType Container) -and
                    (Test-Path -LiteralPath (Join-Path $candidate 'build-tools'))) {
                $sdkPath = (Resolve-Path -LiteralPath $candidate).Path
                break
            }
        }
        if (-not $sdkPath) {
            throw 'STOPP: Android-SDK fehlt. Pruefe sdk.dir in der bestehenden lokalen Projektdatei local.properties.'
        }
        # Forward slashes avoid Java properties escaping rules on Windows.
        $sdkForGradle = $sdkPath.Replace('\', '/')
        Set-Content -LiteralPath $targetLocal -Value "sdk.dir=$sdkForGradle" -Encoding ASCII
        Write-Host 'local.properties fuer diesen Worktree neu erstellt.'
    }
    if (-not (Test-Path -LiteralPath (Join-Path $sdkPath 'build-tools') -PathType Container)) {
        throw 'STOPP: SDK build-tools nicht vorhanden.'
    }
    $env:ANDROID_HOME = $sdkPath
    $env:ANDROID_SDK_ROOT = $sdkPath
    Write-Host "Android-SDK erkannt: $sdkPath" -ForegroundColor Green

    Write-Host '[4] MT-008 und MT-011B3 Diagnose in isolierten Quellen aktivieren' -ForegroundColor Cyan
    & python (Join-Path $testRoot 'scripts\mars\mt008_probe_media_core.py') | Out-Host
    Checked 'MT-008 Probe'
    & python (Join-Path $testRoot 'scripts\mars\mt009_probe_signature_stages.py') --check | Out-Host
    Checked 'MT-011B3 Probe-Pruefung'
    & python (Join-Path $testRoot 'scripts\mars\mt009_probe_signature_stages.py') | Out-Host
    Checked 'MT-011B3 Probe'

    Write-Host '[4a] MT-011B3 Cache-Experiment aktivieren' -ForegroundColor Cyan
    & python (Join-Path $testRoot 'scripts\mars\mt010a_decoder_cache.py') --check | Out-Host
    Checked 'MT-011B3 Source-Pruefung'
    & python (Join-Path $testRoot 'scripts\mars\mt010a_decoder_cache.py') | Out-Host
    Checked 'MT-011B3 Cache-Overlay'

    Write-Host '[4b] MT-011B3 V8-Worker-Experiment aktivieren' -ForegroundColor Cyan
    & python (Join-Path $testRoot 'scripts\mars\mt010a2_warm_runtime.py') --check | Out-Host
    Checked 'MT-011B3 V8-Source-Pruefung'
    & python (Join-Path $testRoot 'scripts\mars\mt010a2_warm_runtime.py') | Out-Host
    Checked 'MT-011B3 V8-Worker-Overlay'

    Write-Host '[4c] MT-011B3 TCL-Phasenmessung aktivieren' -ForegroundColor Cyan
    & python (Join-Path $testRoot 'scripts\mars\mt011a_tcl_stage_probes.py') --check | Out-Host
    Checked 'MT-011B3 Timing-Anker'
    & python (Join-Path $testRoot 'scripts\mars\mt011a_tcl_stage_probes.py') | Out-Host
    Checked 'MT-011B3 Timing-Overlay'

    Write-Host '[4d] MT-011B3 kurze V8-Aufrufe aktivieren' -ForegroundColor Cyan
    & python (Join-Path $testRoot 'scripts\mars\mt011ab_lowcopy_tcl.py') --check | Out-Host
    Checked 'MT-011B3 Lowcopy-Anker'
    & python (Join-Path $testRoot 'scripts\mars\mt011ab_lowcopy_tcl.py') | Out-Host
    Checked 'MT-011B3 Lowcopy-Overlay'

    Write-Host '[4e] MT-011B3 RAM-Cache (max. ein Eintrag) aktivieren' -ForegroundColor Cyan
    & python (Join-Path $testRoot 'scripts\mars\mt011ab_memory_source_cache.py') --check | Out-Host
    Checked 'MT-011B3 RAM-Cache-Anker'
    & python (Join-Path $testRoot 'scripts\mars\mt011ab_memory_source_cache.py') | Out-Host
    Checked 'MT-011B3 RAM-Cache-Overlay'

    Write-Host '[4f] MT-011B3 same-player SABR queue aktivieren' -ForegroundColor Cyan
    & python (Join-Path $testRoot 'scripts\mars\mt011b3_queue_preloader.py') --check | Out-Host
    Checked 'MT-011B3 Queue-Pruefung'
    & python (Join-Path $testRoot 'scripts\mars\mt011b3_queue_preloader.py') | Out-Host
    Checked 'MT-011B3 Queue-Overlay'

    Write-Host '[5] MT-011B3 RELEASE (nicht Debug) bauen' -ForegroundColor Cyan
    Push-Location $testRoot
    try {
        & .\gradlew.bat --no-daemon :smarttubetv:assembleStstableRelease | Out-Host
        Checked 'Release Build'
    } finally {
        Pop-Location
    }
    $apk = Get-ChildItem (Join-Path $testRoot 'smarttubetv\build\outputs\apk\ststable\release') -Recurse -Filter '*.apk' |
        Where-Object { $_.Name -match 'universal' } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
    if (-not $apk) { throw 'STOPP: Keine Universal-Release-APK gefunden.' }

    $newApk = Join-Path $backupRoot 'MarsTube-MT011B3-Release-Test.apk'
    Copy-Item $apk.FullName $newApk -Force

    Write-Host '[6] Signatur gegen bestehende App pruefen' -ForegroundColor Cyan
    $sdk = $sdkPath
    $signer = Get-ChildItem (Join-Path $sdk 'build-tools') -Recurse -Filter 'apksigner.bat' -ErrorAction Stop |
        Sort-Object FullName -Descending | Select-Object -First 1 -ExpandProperty FullName
    if (-not $signer) { throw 'Kein apksigner.bat gefunden; Installation abgebrochen.' }
    $before = SignerDigest $oldApk $signer
    $after = SignerDigest $newApk $signer
    if ($before -ne $after) {
        throw "STOPP: Signatur abweichend! Bisherige App bleibt installiert. Backup $oldApk"
    }
    Write-Host "Signatur identisch ($($before.Substring(0, 12))...)." -ForegroundColor Green

    Write-Host '[7] Thomson aktualisieren, ohne App-Daten zu loeschen' -ForegroundColor Cyan
    & $adb -s $tv install -r $newApk | Out-Host
    Checked 'MT-011B3 Installation'
    & $adb -s $tv shell monkey -p $package -c android.intent.category.LAUNCHER 1 | Out-Host
    Checked 'MarsTube Start'
    $completed = $true
    Write-Host ("MT-011B3 RELEASE INSTALLIERT" + [Environment]::NewLine + "Backup: $oldApk" + [Environment]::NewLine + "Test-APK: $newApk") -ForegroundColor Green
} finally {
    if ($createdWorktree) {
        & git -c core.longpaths=true -C $repo worktree remove --force --force $testRoot | Out-Host
        if ($LASTEXITCODE -ne 0) {
            Write-Warning "Arbeitsordner konnte nicht entfernt werden: $testRoot"
        }
    }
    if (-not $completed) {
        Write-Warning "MT-011B3 wurde nicht als erfolgreich installiert bestaetigt. Aktuelle App und Backup kontrollieren."
    }
}

# Cleanup warnings must not mask successful, signature-verified installation.
if ($completed) { exit 0 }
throw 'MT-011B3 installation was not confirmed.'
