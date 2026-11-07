#Requires -Version 7.0
<#
  Lokaler MarsTube-0.0.30-Release-Build, PowerShell 7, Windows.
  Keine Aenderung am aktiven Git-Branch; keine Deinstallation; kein Git-Reset.
  Signatur nur mit dem vorhandenen DPAPI-geschuetzten Originalschluessel.
#>
[CmdletBinding()]
param([string]$Root='E:\MarsTube', [switch]$Offline)
Set-StrictMode -Version Latest
$ErrorActionPreference='Stop'
if (-not $IsWindows) { throw 'Windows und PowerShell 7 erforderlich.' }
$Tested='8a2b0c4121e3e2efac93126258c8cc2fb50c3363'
$Branch='feature/mt2-030-inapp-search-input-20261107'
$Base='6f96a1b42bb9272935360d7e5aec34d35f2753f0'
$Cache=Join-Path $Root '.mt2-build'
$Overlay=Join-Path $Cache 'mars-patches-030'
$Upstream=Join-Path $Cache 'upstream-pinned'
$Work=Join-Path $Cache 'source-030'
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
$present=(& $git -C $Root cat-file -t $Tested 2>$null | Out-String).Trim()
if ($LASTEXITCODE -ne 0 -or $present -ne 'commit') {
    if ($Offline) { throw 'Getesteter MarsTube-Commit fehlt im lokalen Git-Cache.' }
    Write-Host 'Lade Quellcode-Patches per Git; keine APK wird heruntergeladen.'
    & $git -C $Root fetch --no-tags origin $Branch
    Checked 'Getestete MarsTube-Patches holen'
}
$present=(& $git -C $Root cat-file -t $Tested | Out-String).Trim()
Checked 'Getesteten Commit pruefen'
if ($present -ne 'commit') { throw 'Getesteter Commit nicht verfuegbar.' }
if (-not (Test-Path -LiteralPath $Overlay)) {
    & $git -C $Root worktree add --detach $Overlay $Tested
    Checked 'Separaten Patch-Worktree anlegen'
} else {
    Must (Join-Path $Overlay '.git')
    $head=(& $git -C $Overlay rev-parse HEAD | Out-String).Trim()
    Checked 'Patch-Worktree pruefen'
    if ($head -ne $Tested) { throw 'Abweichender vorhandener Patch-Worktree; keine Aenderung.' }
}
if (-not (Test-Path -LiteralPath $Upstream)) {
    if ($Offline) { throw 'Getesteter Upstream ist noch nicht lokal vorhanden.' }
    New-Item -ItemType Directory -Path $Upstream | Out-Null
    & $git -C $Upstream init
    Checked 'Upstream-Git initialisieren'
    & $git -C $Upstream remote add origin 'https://github.com/aleixrodriala/newtube.git'
    Checked 'Upstream-Quelle setzen'
    & $git -C $Upstream fetch --depth 1 origin $Base
    Checked 'Getesteten Upstream holen'
    & $git -C $Upstream checkout --detach FETCH_HEAD
    Checked 'Upstream-Commit auschecken'
    & $git -C $Upstream submodule update --init --recursive --depth 1
    Checked 'Upstream-Submodule vorbereiten'
}
Must (Join-Path $Upstream '.git\HEAD')
$baseHead=(& $git -C $Upstream rev-parse HEAD | Out-String).Trim()
Checked 'Upstream-Commit pruefen'
if ($baseHead -ne $Base -or -not (Test-Path -LiteralPath (Join-Path $Upstream 'sabr-media3'))) {
    throw 'Upstream-Cache ist unvollstaendig oder hat den falschen Commit.'
}
$marker=Join-Path $Work '.mars-source-version'
if (-not (Test-Path -LiteralPath $marker)) {
    if (Test-Path -LiteralPath $Work) { throw 'Build-Quellordner ist unvollstaendig. Keine Dateien werden geloescht.' }
    New-Item -ItemType Directory -Path $Work | Out-Null
    Write-Host 'Erstelle den getrennten MarsTube-0.0.30-Quellstand.'
    & robocopy.exe $Upstream $Work /E /XD .gradle build /NFL /NDL /NJH /NJS /NP | Out-Null
    if ($LASTEXITCODE -ge 8) { throw "Quellcode-Kopie fehlgeschlagen: $LASTEXITCODE" }
    $settings=Join-Path $Work 'settings.gradle'
    $original=[IO.File]::ReadAllText($settings)
    $updated=$original.Replace("'../SharedModules'","'./SharedModules'").Replace("'../MediaServiceCore'","'./MediaServiceCore'")
    if ($original -eq $updated) { throw 'settings.gradle weicht vom getesteten Stand ab.' }
    [IO.File]::WriteAllText($settings,$updated,[Text.UTF8Encoding]::new($false))
    $patches=@(
      'patch_tv_input.py','patch_sabr_recovery.py','patch_lab_identity.py','patch_tv_shell.py',
      'patch_005_navigation.py','patch_005_localize.py','patch_005_mobile_de.py',
      'patch_006_unified.py','patch_006_isolated_firetv.py','patch_007_tv_quality.py',
      'patch_007_translate_common.py','patch_008_tv_player.py','patch_009_tv_shorts_ui.py',
      'patch_010_player_settings_ui.py','patch_011_firetv_navigation.py','patch_012_tv_updates.py',
      'patch_013_tv_focus_ui.py','patch_014_tv_shorts_mode.py','patch_015_universal_back.py',
      'patch_016_production.py','patch_017_shorts_stage.py','patch_018_tv_ux.py',
      'patch_019_shorts_three_column.py','patch_020_update_freshness.py',
      'patch_021_shorts_home_pause.py','patch_022_update_reliability.py',
      'patch_023_tv_settings.py','patch_024_unified_navigation.py',
      'patch_025_tv_ui.py','patch_026_shorts_actions.py','patch_027_shorts_polish.py',
      'patch_028_shorts_undistorted_still.py','patch_029_shorts_compact.py',
      'patch_030_inapp_search_input.py'
    )
    foreach ($patch in $patches) {
        $patchFile=Join-Path $Overlay ('mt2-player-lab\' + $patch)
        Must $patchFile
        & $python $patchFile $Work
        Checked "MarsTube-Patch $patch"
    }
    $gradleText=[IO.File]::ReadAllText((Join-Path $Work 'smarttubetv\build.gradle'))
    if (-not $gradleText.Contains('applicationId "de.equiummc.marstube2"') -or
        -not $gradleText.Contains('versionCode 12030') -or
        -not $gradleText.Contains('versionName "0.0.30"')) {
        throw 'App-ID und Versionsnummer stimmen nicht; kein APK-Build.'
    }
    [IO.File]::WriteAllText($marker,($Tested+[Environment]::NewLine+$Base),[Text.UTF8Encoding]::new($false))
} else {
    $stamp=[IO.File]::ReadAllText($marker)
    if (-not $stamp.Contains($Tested) -or -not $stamp.Contains($Base)) {
        throw 'Vorhandene Build-Quellen stammen aus einer anderen Version.'
    }
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
if ($badge -notmatch "package: name='de.equiummc.marstube2' versionCode='12030' versionName='0.0.30'") {
    throw 'Unerwartete App-ID oder Versionsnummer.'
}
$cert=(& $signer verify --print-certs $built | Out-String)
Checked 'Signatur pruefen'
if ($cert -notmatch 'certificate SHA-256 digest:\s*([a-fA-F0-9]+)') {
    throw 'APK hat keine gueltige Release-Signatur.'
}
New-Item -ItemType Directory -Path $ApkFolder -Force | Out-Null
$target=Join-Path $ApkFolder 'MarsTube-0.0.30.apk'
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
Write-Host 'Paket de.equiummc.marstube2, Version 0.0.30, unveraenderte Release-Signatur.'
Write-Host 'Keine App wurde deinstalliert. Der bestehende Projekt-Branch blieb unveraendert.'
