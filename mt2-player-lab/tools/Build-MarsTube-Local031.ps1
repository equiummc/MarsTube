#Requires -Version 7.0
<#
  Lokaler MarsTube-0.0.31-Release-Build, PowerShell 7, Windows.
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
$BaseSource=Join-Path $Cache 'source-030'
$Work=Join-Path $Cache 'source-031'
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
# Keep the successfully signed/compiled 0.0.30 source and active Git checkout
# completely untouched. In a new build sandbox use only 0.0.31's patch and
# unchanged pinned Media3/third-party dependencies.
$present=(& $git -C $Root cat-file -t $Tested 2>$null | Out-String).Trim()
if ($LASTEXITCODE -ne 0 -or $present -ne 'commit') {
    if ($Offline) { throw 'Getesteter 0.0.31-Patch-Commit fehlt im lokalen Git-Cache.' }
    Write-Host 'Hole nur den 0.0.31-Quellpatch per Git (keine APK).'
    & $git -C $Root fetch --no-tags origin $Branch
    Checked '0.0.31-Quellpatch laden'
}
$present=(& $git -C $Root cat-file -t $Tested | Out-String).Trim()
Checked 'MT2-031 Patch-Commit pruefen'
if ($present -ne 'commit') { throw 'Getesteter 0.0.31-Quellpatch fehlt.' }
if (-not (Test-Path -LiteralPath $Overlay)) {
    & $git -C $Root worktree add --detach $Overlay $Tested
    Checked 'Getrennten Patch-Worktree fuer 0.0.31 anlegen'
} else {
    Must (Join-Path $Overlay '.git')
    $existing=(& $git -C $Overlay rev-parse HEAD | Out-String).Trim()
    Checked '0.0.31-Worktree-Version pruefen'
    if ($existing -ne $Tested) {
        throw "Vorhandener Patch-Worktree hat eine andere Version ($existing). Kein Reset."
    }
}
$previousMarker=Join-Path $BaseSource '.mars-source-version'
Must $previousMarker
$baseStamp=[IO.File]::ReadAllText($previousMarker)
if (-not $baseStamp.Contains('8a2b0c4121e3e2efac93126258c8cc2fb50c3363')) {
    throw 'Vorhandener 0.0.30-Quellstand ist nicht der erwartete getestete Build.'
}
Must (Join-Path $BaseSource 'smarttubetv\build.gradle')
Must (Join-Path $Overlay 'mt2-player-lab\patch_031_compact_shorts_feed.py')
$marker=Join-Path $Work '.mars-source-version-031'
if (-not (Test-Path -LiteralPath $marker)) {
    if (Test-Path -LiteralPath $Work) {
        throw "Vorhandener 0.0.31-Build-Ordner ohne Versionierungsmarker: $Work. Nichts geloescht."
    }
    New-Item -ItemType Directory -Path $Work -Force | Out-Null
    Write-Host 'Kopiere unveraenderten 0.0.30-Quellstand in einen separaten 0.0.31-Build-Ordner ...'
    & robocopy.exe $BaseSource $Work /E /XD .git .gradle build .cxx /XF .mars-source-version .mars-source-version-031 keystore.properties /R:0 /W:0 /NFL /NDL /NJH /NJS /NP | Out-Null
    if ($LASTEXITCODE -ge 8) { throw "Quellcode-Kopie fehlgeschlagen ($LASTEXITCODE)." }
    & $python (Join-Path $Overlay 'mt2-player-lab\patch_031_compact_shorts_feed.py') $Work
    Checked 'Kompakte Shorts-Karten einrichten'
    $gradleData=[IO.File]::ReadAllText((Join-Path $Work 'smarttubetv\build.gradle'))
    if (-not $gradleData.Contains('applicationId "de.equiummc.marstube2"') -or
        -not $gradleData.Contains('versionCode 12031') -or
        -not $gradleData.Contains('versionName "0.0.31"')) {
        throw '0.0.31-Identitaet kann nicht verifiziert werden.'
    }
    [IO.File]::WriteAllText($marker,$Tested,[Text.UTF8Encoding]::new($false))
} else {
    $content=[IO.File]::ReadAllText($marker).Trim()
    if ($content -ne $Tested) {
        throw '0.0.31-Build-Ordner enthaelt einen anderen Quellstand.'
    }
}
# Windows-only SABR compiler already comes from 0.0.30; verify again rather
# than attempting to execute the Linux ELF executable on Windows.
$protoGradle=Join-Path $Work 'sabr-media3\build.gradle'
Must $protoGradle
$protocText=[IO.File]::ReadAllText($protoGradle)
if (-not $protocText.Contains("'com.google.protobuf:protoc:3.22.3:windows-x86_64@exe'")) {
    throw 'Windows-Protobuf-Compiler fehlt im 0.0.31-Quellstand.'
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
        Write-Host 'Baue und signiere MarsTube lokal ...'
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
New-Item -ItemType Directory -Path $ApkFolder -Force | Out-Null
$target=Join-Path $ApkFolder 'MarsTube-0.0.31.apk'
if (Test-Path -LiteralPath $target) {
    if ((Get-FileHash -LiteralPath $target -Algorithm SHA256).Hash -ne
        (Get-FileHash -LiteralPath $built -Algorithm SHA256).Hash) {
        $archive=Join-Path $ApkFolder 'Archiv'
        New-Item -ItemType Directory -Path $archive -Force | Out-Null
        $backup=Join-Path $archive ('MarsTube-0.0.30-vorher-'+(Get-Date -Format 'yyyyMMdd-HHmmss')+'.apk')
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
Write-Host 'Paket de.equiummc.marstube2, Version 0.0.31, unveraenderte Release-Signatur.'
Write-Host 'Keine App wurde deinstalliert. Der bestehende Projekt-Branch blieb unveraendert.'
