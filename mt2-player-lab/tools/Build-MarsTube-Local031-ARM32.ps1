#Requires -Version 7.0
<#
  Lokaler MarsTube-0.0.31-ARM32-Release-Build, PowerShell 7, Windows.
  Keine Aenderung am aktiven Git-Branch; keine Deinstallation; kein Git-Reset.
  Signatur nur mit dem vorhandenen DPAPI-geschuetzten Originalschluessel.
#>
[CmdletBinding()]
param([string]$Root='E:\MarsTube', [switch]$Offline)
Set-StrictMode -Version Latest
$ErrorActionPreference='Stop'
if (-not $IsWindows) { throw 'Windows und PowerShell 7 erforderlich.' }
$Tested='b6ccde14c5f54343c376d075e95f36a57acaec1d'
$Branch='feature/mt2-031-compact-shorts-apk-audit-20261108'
$Base='6f96a1b42bb9272935360d7e5aec34d35f2753f0'
$Cache=Join-Path $Root '.mt2-build'
$Overlay=Join-Path $Cache 'mars-patches-031'
$BaseSource=Join-Path $Cache 'source-031'
$Work=Join-Path $Cache 'source-031-arm32'
$Jks=Join-Path $Root 'signing\release.jks'
$Dpapi=Join-Path $Root 'signing\release-credentials.dpapi'
$ApkFolder=Join-Path $Root 'APK'
function Must([string]$file) {
    if (-not (Test-Path -LiteralPath $file -PathType Leaf)) { throw "Datei fehlt: $file" }
}
function Checked([string]$label) {
    if ($LASTEXITCODE -ne 0) { throw "$label fehlgeschlagen; Exitcode $LASTEXITCODE" }
}
function PropEscape([string]$s) {
    return $s.Replace([string][char]92,'\\').Replace([string][char]13,'\r').Replace([string][char]10,'\n')
}
Must $Jks
Must $Dpapi
Must (Join-Path $Root 'gradlew.bat')
$git=(Get-Command git.exe -ErrorAction Stop).Source
$java=(Get-Command java.exe -ErrorAction Stop).Source
$keytool=(Get-Command keytool.exe -ErrorAction Stop).Source
$python='C:\Users\equiu\AppData\Local\Python\bin\python.exe'
if (-not (Test-Path -LiteralPath $python)) {
    $python=(Get-Command python.exe -ErrorAction Stop).Source
}
$javaVersion=(& $java -version 2>&1 | Out-String)
if ($javaVersion -notmatch 'version "17\.|openjdk version "17\.') { throw 'Benötigt wird Java 17.' }
$env:JAVA_HOME=Split-Path -Parent (Split-Path -Parent $java)
$repo=(& $git -C $Root rev-parse --show-toplevel | Out-String).Trim()
Checked 'Projektpruefung'
if ([IO.Path]::GetFullPath($repo).TrimEnd('\') -ne [IO.Path]::GetFullPath($Root).TrimEnd('\')) {
    throw 'Unbekanntes Git-Projekt; der vorhandene Arbeitsstand bleibt unangetastet.'
}
$activeBranch=(& $git -C $Root branch --show-current | Out-String).Trim()
Checked 'Aktiven Git-Branch lesen'
Write-Host "Git-Branch bleibt: $activeBranch" -ForegroundColor Cyan

$sdk=if ($env:ANDROID_HOME) {$env:ANDROID_HOME} else {Join-Path $env:LOCALAPPDATA 'Android\Sdk'}
if (-not (Test-Path -LiteralPath $sdk)) { throw "Android SDK fehlt: $sdk" }
$env:ANDROID_HOME=$sdk
$env:ANDROID_SDK_ROOT=$sdk
$platform=Join-Path $sdk 'platforms\android-37.0\android.jar'
$buildtools=Join-Path $sdk 'build-tools\37.0.0'
$ndk=Join-Path $sdk 'ndk\21.0.6113669'
$aapt=Join-Path $buildtools 'aapt.exe'
$signer=Join-Path $buildtools 'apksigner.bat'
if (-not (Test-Path -LiteralPath $platform) -or -not (Test-Path -LiteralPath $aapt) -or
    -not (Test-Path -LiteralPath $signer) -or -not (Test-Path -LiteralPath $ndk)) {
    if ($Offline) { throw 'Android SDK 37.0, Build-Tools 37.0.0 oder NDK fehlen (Offline-Modus).' }
    $manager=Join-Path $sdk 'cmdline-tools\latest\bin\sdkmanager.bat'
    Must $manager
    Write-Host 'Installiere fehlende Android-Buildwerkzeuge ...'
    & $manager "--sdk_root=$sdk" '--channel=3' 'platforms;android-37.0' 'build-tools;37.0.0' 'ndk;21.0.6113669'
    Checked 'Android SDK einrichten'
}
Must $platform
Must $signer
$platformAlias=Join-Path $sdk 'platforms\android-37'
if (-not (Test-Path -LiteralPath $platformAlias)) {
    New-Item -ItemType Junction -Path $platformAlias -Target (Split-Path -Parent $platform) | Out-Null
}
New-Item -ItemType Directory -Path $Cache -Force | Out-Null
# The universal 0.0.31 remains untouched; this is a separate ARM32 build tree.
# No Git reset, worktree swap, or private-key export.
$sourceMarker=Join-Path $BaseSource '.mars-source-version-031'
Must $sourceMarker
$sourceStamp=[IO.File]::ReadAllText($sourceMarker).Trim()
if ($sourceStamp -ne 'b6ccde14c5f54343c376d075e95f36a57acaec1d') {
    throw 'Die Universal-0.0.31-Quellen stammen nicht aus dem getesteten Build.'
}
Must (Join-Path $BaseSource 'smarttubetv\build.gradle')
$busy=Join-Path $BaseSource 'keystore.properties'
if (Test-Path -LiteralPath $busy) {
    throw 'Der Universal-Build ist noch aktiv oder nicht sauber beendet. Bitte nicht parallel starten.'
}
$marker=Join-Path $Work '.mars-source-version-031-arm32'
if (-not (Test-Path -LiteralPath $marker)) {
    if (Test-Path -LiteralPath $Work) {
        throw 'Ein unvollstaendiger ARM32-Build-Ordner existiert. Keine Daten werden geloescht.'
    }
    New-Item -ItemType Directory -Path $Work -Force | Out-Null
    Write-Host 'Kopiere 0.0.31 in eine getrennte ARM32-Build-Umgebung ...'
    & robocopy.exe $BaseSource $Work /E /XD .git .gradle build .cxx /XF keystore.properties .mars-source-version-031 .mars-source-version-031-arm32 /R:0 /W:0 /NFL /NDL /NJH /NJS /NP | Out-Null
    if ($LASTEXITCODE -ge 8) { throw "Sichere Quellkopie fehlgeschlagen: $LASTEXITCODE" }
    $gradleFile=Join-Path $Work 'smarttubetv\build.gradle'
    Must $gradleFile
    $gradleData=[IO.File]::ReadAllText($gradleFile)
    $oldFilter="abiFilters 'armeabi-v7a', 'arm64-v8a'"
    $newFilter="abiFilters 'armeabi-v7a'"
    $oldSplit="include 'armeabi-v7a', 'arm64-v8a', 'x86'"
    $newSplit="include 'armeabi-v7a'"
    if ($gradleData.IndexOf($oldFilter) -lt 0 -or
        $gradleData.IndexOf($oldFilter) -ne $gradleData.LastIndexOf($oldFilter) -or
        $gradleData.IndexOf($oldSplit) -lt 0 -or
        $gradleData.IndexOf($oldSplit) -ne $gradleData.LastIndexOf($oldSplit)) {
        throw 'Unerwartete ABI-Konfiguration; keine riskante Aenderung.'
    }
    $gradleData=$gradleData.Replace($oldFilter,$newFilter).Replace($oldSplit,$newSplit)
    [IO.File]::WriteAllText($gradleFile,$gradleData,[Text.UTF8Encoding]::new($false))
    if (-not $gradleData.Contains('applicationId "de.equiummc.marstube2"') -or
        -not $gradleData.Contains('versionCode 12031') -or
        -not $gradleData.Contains('versionName "0.0.31"')) {
        throw 'App-ID oder Version weichen vom Original ab.'
    }
    [IO.File]::WriteAllText($marker,'arm32-'+$sourceStamp,[Text.UTF8Encoding]::new($false))
} else {
    if ([IO.File]::ReadAllText($marker).Trim() -ne ('arm32-'+$sourceStamp)) {
        throw 'Unbekannte ARM32-Build-Version; keine Aenderung.'
    }
}
$gradleVerify=[IO.File]::ReadAllText((Join-Path $Work 'smarttubetv\build.gradle'))
if (-not $gradleVerify.Contains("abiFilters 'armeabi-v7a'") -or
    $gradleVerify.Contains("abiFilters 'armeabi-v7a', 'arm64-v8a'") -or
    -not $gradleVerify.Contains("include 'armeabi-v7a'") -or
    -not $gradleVerify.Contains('versionCode 12031')) {
    throw 'ARM32-Build-Konfiguration ist nicht korrekt.'
}
$protoGradle=Join-Path $Work 'sabr-media3\build.gradle'
Must $protoGradle
if (-not ([IO.File]::ReadAllText($protoGradle)).Contains("'com.google.protobuf:protoc:3.22.3:windows-x86_64@exe'")) {
    throw 'Falscher Protobuf-Compiler fuer Windows.'
}
$props=Join-Path $Work 'keystore.properties'
if (Test-Path -LiteralPath $props) { throw "Vorhandene Geheimnisdatei wird nicht ueberschrieben: $props" }
try {
    $cipher=[IO.File]::ReadAllText($Dpapi).Trim()
    $secret=ConvertTo-SecureString -String $cipher
    $credentialJson=ConvertFrom-SecureString -SecureString $secret -AsPlainText
    $cred=$credentialJson | ConvertFrom-Json
    if ([string]::IsNullOrWhiteSpace($cred.storePassword) -or
        [string]::IsNullOrWhiteSpace($cred.keyPassword) -or
        [string]::IsNullOrWhiteSpace($cred.keyAlias)) {
        throw 'DPAPI-Zugangsdaten sind unvollstaendig.'
    }
    $env:MARS_LOCAL_STORE_PASS=[string]$cred.storePassword
    try {
        & $keytool -list -keystore $Jks '-storepass:env' 'MARS_LOCAL_STORE_PASS' -alias $cred.keyAlias 1>$null 2>$null
        Checked 'Originalschluessel pruefen'
    } finally {
        Remove-Item Env:\MARS_LOCAL_STORE_PASS -ErrorAction SilentlyContinue
    }
    $values=@(
        'storeFile='+(PropEscape $Jks.Replace('\','/'))
        'storePassword='+(PropEscape ([string]$cred.storePassword))
        'keyAlias='+(PropEscape ([string]$cred.keyAlias))
        'keyPassword='+(PropEscape ([string]$cred.keyPassword))
    )
    [IO.File]::WriteAllLines($props,$values,[Text.UTF8Encoding]::new($false))
    $owner=[Security.Principal.WindowsIdentity]::GetCurrent().Name
    & icacls.exe $props /inheritance:r /grant:r ($owner+':F') '*S-1-5-18:F' '*S-1-5-32-544:F' 1>$null
    Checked 'Temporaere Signierdatei absichern'
    $gradlew=Join-Path $Work 'gradlew.bat'
    Must $gradlew
    $argsGradle=@('--no-daemon','--max-workers=1','-Dorg.gradle.jvmargs=-Xmx4096m')
    if ($Offline) { $argsGradle+='--offline' }
    $argsGradle+=@(':sabr-media3:testDebugUnitTest',':smarttubetv:assembleStmobileRelease','--stacktrace')
    Push-Location $Work
    try {
        Write-Host 'Baue und signiere MarsTube 0.0.31 nur fuer ARM32 ...'
        & $gradlew @argsGradle
        Checked 'Release-Build'
    } finally { Pop-Location }
} finally {
    if (Test-Path -LiteralPath $props) { Remove-Item -LiteralPath $props -Force }
    Remove-Item Env:\MARS_LOCAL_STORE_PASS -ErrorAction SilentlyContinue
    Remove-Variable cipher,secret,credentialJson,cred,values -ErrorAction SilentlyContinue
}
$apks=@(Get-ChildItem -LiteralPath (Join-Path $Work 'smarttubetv\build\outputs\apk') -Recurse -File -Filter '*.apk' |
    Where-Object {$_.FullName -match '[\\/]release[\\/]'})
if ($apks.Count -ne 1) { throw "Erwartete eine Release-APK, gefunden: $($apks.Count)" }
$built=$apks[0].FullName
$badge=(& $aapt dump badging $built | Out-String)
Checked 'App-Paket pruefen'
if ($badge -notmatch "package: name='de.equiummc.marstube2' versionCode='12031' versionName='0.0.31'") {
    throw 'Unerwartete App-ID oder Versionsnummer.'
}
$cert=(& $signer verify --print-certs $built | Out-String)
Checked 'Signatur pruefen'
if ($cert -notmatch 'certificate SHA-256 digest:\s*([a-fA-F0-9]+)') {
    throw 'APK hat keine gueltige Release-Signatur.'
}
# Inspect the actual signed APK. A stray 64-bit native library makes the
# variant unsuitable for an ARM32-only television; do not publish it.
Add-Type -AssemblyName System.IO.Compression
$zip=[IO.Compression.ZipFile]::OpenRead($built)
try {
    $abis=@($zip.Entries | Where-Object {$_.FullName -match '^lib/[^/]+/[^/]+\.so$'} |
        ForEach-Object {($_.FullName -split '/')[1]} | Sort-Object -Unique)
    if ($abis.Count -ne 1 -or $abis[0] -ne 'armeabi-v7a') {
        throw "ARM32-APK enthaelt falsche Architektur-Bibliotheken: $($abis -join ', ')"
    }
} finally { $zip.Dispose() }
New-Item -ItemType Directory -Path $ApkFolder -Force | Out-Null
$target=Join-Path $ApkFolder 'MarsTube-0.0.31-ARM32.apk'
if (Test-Path -LiteralPath $target) {
    if ((Get-FileHash -LiteralPath $target -Algorithm SHA256).Hash -ne
        (Get-FileHash -LiteralPath $built -Algorithm SHA256).Hash) {
        $archive=Join-Path $ApkFolder 'Archiv'
        New-Item -ItemType Directory -Path $archive -Force | Out-Null
        $backup=Join-Path $archive ('MarsTube-0.0.31-ARM32-vorher-'+(Get-Date -Format 'yyyyMMdd-HHmmss')+'.apk')
        Copy-Item -LiteralPath $target -Destination $backup -ErrorAction Stop
        Write-Host "Bisherige APK gesichert: $backup"
    }
}
Copy-Item -LiteralPath $built -Destination $target -Force
if ((Get-FileHash -LiteralPath $target -Algorithm SHA256).Hash -ne
    (Get-FileHash -LiteralPath $built -Algorithm SHA256).Hash) {
    throw 'Die kopierte APK stimmt nicht mit der gebauten Datei ueberein.'
}
$now=(& $git -C $Root branch --show-current | Out-String).Trim()
Checked 'Aktiven Branch nach Build pruefen'
if ($now -ne $activeBranch) { throw 'Git-Branch hat sich unerwartet geaendert.' }
Write-Host "ERFOLG: $target" -ForegroundColor Green
Write-Host 'Paket de.equiummc.marstube2, 0.0.31, nur ARM32, Original-Release-Signatur.'
Write-Host 'Keine App wurde deinstalliert. Der bestehende Projekt-Branch blieb unveraendert.'
$universal=Join-Path $ApkFolder 'MarsTube-0.0.31.apk'
if (Test-Path -LiteralPath $universal) {
    $saved=(Get-Item -LiteralPath $universal).Length - (Get-Item -LiteralPath $target).Length
    Write-Host ("Einsparung gegenueber 0.0.31 Universal: {0:N2} MB" -f ($saved/1MB))
}
