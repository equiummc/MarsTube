# MT2-000 - Media3/SABR Referenztest (getrennt von MarsTube 1.x)
[CmdletBinding()]
param(
    [ValidateSet('Build','Install','Logs')][string]$Mode = 'Build',
    [string]$Root = 'E:\Mars-TV\Projekte\marstube2\player-lab',
    [string]$Device = '192.168.41.53:5555'
)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$RepoUrl = 'https://github.com/aleixrodriala/newtube.git'
$PinnedCommit = '6f96a1b42bb9272935360d7e5aec34d35f2753f0'
$Checkout = Join-Path $Root 'newtube-reference'
$LogDir = Join-Path $Root 'logs'
$ExpectedPackage = 'io.github.aleixrodriala.arc.debug'
function Require-Command([string]$Name) {
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Fehlendes Werkzeug: $Name"
    }
}
function Assert-Ok([string]$Description) {
    if ($LASTEXITCODE -ne 0) { throw "$Description fehlgeschlagen (Exitcode $LASTEXITCODE)." }
}
function Connect-Device {
    Require-Command 'adb'
    if ($Device -match '^\d{1,3}(\.\d{1,3}){3}:\d+$') {
        & adb connect $Device | Out-Host
        Assert-Ok 'ADB verbinden'
    }
    $devices = (& adb devices | Out-String)
    if ($devices -notmatch ([regex]::Escape($Device) + '\s+device')) {
        throw "ADB-Geraet $Device nicht als 'device' verbunden: $devices"
    }
}
function Get-LabApk {
    $apkRoot = Join-Path $Checkout 'smarttubetv\build\outputs\apk'
    if (-not (Test-Path $apkRoot)) { throw "Keine APK. Zuerst -Mode Build ausfuehren: $apkRoot" }
    $apks = @(Get-ChildItem -LiteralPath $apkRoot -Recurse -Filter '*.apk' -File |
        Where-Object { $_.FullName -match '(?i)stmobile' -and $_.FullName -match '(?i)debug' } |
        Sort-Object LastWriteTime -Descending)
    if ($apks.Count -eq 0) { throw 'Keine stmobile-Debug-APK gefunden.' }
    return $apks[0]
}
switch ($Mode) {
    'Build' {
        Require-Command 'git'
        Require-Command 'java'
        $sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME }
               elseif ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT }
               elseif ($env:LOCALAPPDATA) { Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
               else { $null }
        if (-not $sdk -or -not (Test-Path $sdk)) {
            throw 'Android SDK fehlt. ANDROID_HOME setzen oder SDK unter %LOCALAPPDATA%\Android\Sdk installieren.'
        }
        $env:ANDROID_HOME = $sdk
        New-Item -ItemType Directory -Path $Root -Force | Out-Null
        if (-not (Test-Path (Join-Path $Checkout '.git'))) {
            if (Test-Path $Checkout) { throw "Ziel existiert bereits ohne Git: $Checkout" }
            Write-Host 'Klonen der isolierten NewTube-Referenz ...' -ForegroundColor Cyan
            & git -c core.longpaths=true clone --no-checkout $RepoUrl $Checkout
            Assert-Ok 'Git clone'
            & git -C $Checkout -c core.longpaths=true checkout --detach $PinnedCommit
            Assert-Ok 'Git checkout'
        } else {
            $head = (& git -C $Checkout rev-parse HEAD).Trim()
            Assert-Ok 'Git HEAD'
            if ($head -ne $PinnedCommit) {
                throw "Referenz hat HEAD $head statt $PinnedCommit. Kein automatischer Reset!"
            }
        }
        & git -C $Checkout -c core.longpaths=true submodule update --init --recursive
        Assert-Ok 'Git submodule update'
        # Pinned Linux-Protoc fuer Windows ausschliesslich in der separaten Testkopie anpassen.
        $patched = 0
        foreach ($file in @((Join-Path $Checkout 'sabr-media3\build.gradle'),
                            (Join-Path $Checkout 'smarttubetv\build.gradle'))) {
            if (-not (Test-Path $file)) { throw "Gradle-Datei fehlt: $file" }
            $original = [System.IO.File]::ReadAllText($file)
            if ($original.Contains('linux-x86_64@exe')) {
                $replaced = $original.Replace('linux-x86_64@exe','windows-x86_64@exe')
                [System.IO.File]::WriteAllText($file,$replaced,[System.Text.UTF8Encoding]::new($false))
                $patched++
            }
        }
        # Reference is pinned to API 37; use available API 36 only in the isolated lab copy.
        $constants = Join-Path $Checkout 'SharedModules\constants.gradle'
        if (-not (Test-Path $constants)) { throw "SharedModules constants fehlen: $constants" }
        $source = [System.IO.File]::ReadAllText($constants)
        $compat = $source.Replace('compileSdkVersion = 37','compileSdkVersion = 36').Replace('targetSdkVersion = 37','targetSdkVersion = 36')
        if ($compat -ne $source) {
            [System.IO.File]::WriteAllText($constants,$compat,[System.Text.UTF8Encoding]::new($false))
            Write-Host 'API 36 compatibility patch: nur NewTube-Testkopie.' -ForegroundColor Yellow
        }
        $moduleSource = Join-Path $Checkout 'sabr-media3\src\main\java\com\newtube\sabr\SabrMediaSource.java'
        $appBridge = Join-Path $Checkout 'smarttubetv\src\stmobile\java\com\newtube\mobile\player\Media3PlayerController.java'
        if (-not (Test-Path $moduleSource) -or -not (Test-Path $appBridge)) {
            throw 'SABR-Modul oder Media3-Player-Bridge fehlt.'
        }
        Write-Host "Media3/SABR-Quellen vorhanden, Protoc-Anpassungen: $patched" -ForegroundColor Green
        $gradlew = Join-Path $Checkout 'gradlew.bat'
        Push-Location $Checkout
        try {
            Write-Host 'Unit-Tests + Debug-APK bauen ...' -ForegroundColor Cyan
            & $gradlew --no-daemon :sabr-media3:testDebugUnitTest :smarttubetv:assembleStmobileDebug
            Assert-Ok 'Gradle SABR-Test/Debug-Build'
        } finally { Pop-Location }
        $apk = Get-LabApk
        Write-Host "FERTIG: $($apk.FullName)" -ForegroundColor Green
        Write-Host "SHA256: $((Get-FileHash $apk.FullName -Algorithm SHA256).Hash)"
        Write-Host "Getrennte Test-App: $ExpectedPackage"
        Write-Host ('Installationsaufruf: & "' + $PSCommandPath + '" -Mode Install -Device ' + $Device)
    }
    'Install' {
        $apk = Get-LabApk
        Connect-Device
        Write-Host "Installiere nur die Test-App: $($apk.FullName)" -ForegroundColor Cyan
        & adb -s $Device install -r $apk.FullName | Out-Host
        Assert-Ok 'ADB install'
        Write-Host "Test-App installiert. MarsTube 1.x bleibt unveraendert." -ForegroundColor Green
        Write-Host "Start (falls kein TV-Icon): adb -s $Device shell monkey -p $ExpectedPackage 1"
        Write-Host 'In NewTube SABR explizit einschalten (Standard: AUS).'
        Write-Host 'Test: Video mindestens 120 s, 3x zurueck/vor, dann -Mode Logs.'
    }
    'Logs' {
        Connect-Device
        New-Item -ItemType Directory -Path $LogDir -Force | Out-Null
        $stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
        $logFile = Join-Path $LogDir "MT2-SABR-$stamp.txt"
        $model = (& adb -s $Device shell getprop ro.product.model | Out-String).Trim()
        $android = (& adb -s $Device shell getprop ro.build.version.release | Out-String).Trim()
        $raw = & adb -s $Device logcat -d -v threadtime
        Assert-Ok 'ADB logcat'
        $pattern = '(?i)SABR|Media3|ExoPlayer|first.frame|ATTESTATION_REQUIRED|AndroidRuntime|PlaybackException|onPlayerError'
        $filtered = @($raw | Where-Object { $_ -match $pattern } | ForEach-Object {
            $line = $_ -replace 'https?://[^\s]+','[URL-ENTFERNT]'
            $line -replace '(?i)(Authorization|Cookie|poToken|access_token)[=:]\S+','$1=[ENTFERNT]'
        })
        $header = @(
            'MT2-000 Media3/SABR Referenztest',
            "Zeit: $(Get-Date -Format o)",
            "Geraet: $model; Android: $android; ADB: $Device",
            "Referenz: $PinnedCommit",
            "Gefilterte Logtreffer: $($filtered.Count)",
            'Kein vollstaendiger Logcat-Export.',
            ''
        )
        @($header) + @($filtered) | Set-Content -LiteralPath $logFile -Encoding utf8
        Write-Host "Lokales Protokoll: $logFile" -ForegroundColor Green
        Write-Host 'Keine automatische Uebertragung. Vor dem Teilen auf persoenliche Daten pruefen.'
    }
}