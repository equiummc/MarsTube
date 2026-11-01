# MT-019 Thomson nagata rollout: MarsTube ONLY; preserve Thomson Mars TV/HOME and leave Fire Cube untouched.
# InstallAndTest builds stable B6 player with MT-019 OK long-hold speed and MT-016 Shorts navigation; CollectOnly reads playback logs.
[CmdletBinding()]
param([ValidateSet('InstallAndTest','CollectOnly')][string]$Action='InstallAndTest')
$ErrorActionPreference='Stop'
$ProgressPreference='SilentlyContinue'
$thomsonSerial='192.168.41.53:5555'
$adbExe=Join-Path $HOME 'Android-Tools\platform-tools\adb.exe'
$repo=Join-Path $HOME 'MarsTV\mars-smarttube'
$branch='fix/mt-019-shorts-blackframe-recovery-20261101'
$codeSha='f9596441e5f53f193f84e3cc08bdb625f6feb010'
$package='de.equiummc.mars.smarttube'
$activity='com.liskovsoft.smartyoutubetv2.tv.ui.main.SplashActivity'
$save=Join-Path $HOME ('MarsTV\backups\Thomson-MT019\' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
$work=Join-Path $env:TEMP ('MT019-Thomson-Tube-' + $PID)
$created=$false
$installed=$false
function Assert-Exit([string]$step) {
    if($LASTEXITCODE -ne 0){throw "$($step): Exit $LASTEXITCODE"}
}
function Is-Installed {
    $list=(& $adbExe -s $thomsonSerial shell pm list packages --user 0 $package | Out-String)
    Assert-Exit 'Fire TV Paketinventar'
    return (@($list -split '\r?\n' | Where-Object {$_.Trim() -eq ('package:'+$package)}).Count -gt 0)
}
function Current-Version {
    if(-not (Is-Installed)){return -1}
    $raw=(& $adbExe -s $thomsonSerial shell dumpsys package $package | Out-String)
    Assert-Exit 'Installierter Versionscode'
    $match=[regex]::Match($raw,'(?m)^\s*versionCode=(\d+)\b')
    if(-not $match.Success){throw 'MarsTube installiert, Versionscode nicht lesbar'}
    return [int]$match.Groups[1].Value
}
function Cert([string]$apk,[string]$signer) {
    $raw=(& $signer verify --verbose --print-certs $apk 2>&1 | Out-String)
    Assert-Exit 'Zertifikatspruefung'
    $match=[regex]::Match($raw,'(?im)Signer #1 certificate SHA-256 digest:\s*([0-9a-f:]+)')
    if(-not $match.Success){throw "Zertifikat nicht lesbar: $apk"}
    return ($match.Groups[1].Value -replace ':','').ToLowerInvariant()
}
function Check-Apk([string]$apk,[string]$aapt){
    $raw=(& $aapt dump badging $apk | Out-String)
    Assert-Exit 'APK-Paketdaten'
    $match=[regex]::Match($raw,"(?m)^package:\s+name='([^']+)'\s+versionCode='(\d+)'")
    if(-not $match.Success -or $match.Groups[1].Value -ne $package){
        throw "Falsche MarsTube Paketkennung: $apk"
    }
    return [int]$match.Groups[2].Value
}
function Test-MarsTubeStartup {
    for($i=1;$i -le 3;$i++){
        & $adbExe -s $thomsonSerial shell am force-stop $package | Out-Null
        Assert-Exit 'MarsTube force-stop'
        $component=$package+'/'+$activity
        $raw=(& $adbExe -s $thomsonSerial shell am start -W -n $component 2>&1 | Out-String)
        $status=$LASTEXITCODE
        Add-Content (Join-Path $save 'Appstart-Rohdaten.txt') -Value ("Startversuch $i" + [Environment]::NewLine + $raw)
        $m=[regex]::Match($raw,'(?m)^\s*TotalTime:\s*(\d+)')
        $ms=if($status -eq 0 -and $m.Success){$m.Groups[1].Value}else{''}
        [pscustomobject]@{App='MarsTube';Versuch=$i;StartMs=$ms;Ergebnis=if($ms){'OK'}else{'keine Android-Zeit'}} |
            Export-Csv (Join-Path $save 'MarsTube-Startzeiten.csv') -Append -NoTypeInformation -Encoding utf8
        if($ms){Write-Host "MarsTube Kaltstart $($i): $ms ms" -ForegroundColor Cyan}
        else{Write-Warning "Kaltstart $i unvollstaendig, Rohdaten gespeichert."}
    }
}
function Read-Performance {
    $logs=& $adbExe -s $thomsonSerial shell logcat -d -v time -s 'MarsTubePerf:I' '*:S' 2>&1
    $logs | Select-Object -Last 1200 | Set-Content (Join-Path $save 'MarsTubePerf-Shorts.txt') -Encoding utf8
    $ram=(& $adbExe -s $thomsonSerial shell dumpsys meminfo $package | Out-String)
    $ram | Set-Content (Join-Path $save 'MarsTube-RAM.txt') -Encoding utf8
    Write-Host "Logs und Backups: $save" -ForegroundColor Green
}
try {
    if(-not (Test-Path $adbExe)){throw "ADB fehlt: $adbExe"}
    New-Item -ItemType Directory -Force -Path $save | Out-Null
    & $adbExe connect $thomsonSerial | Out-Host
    Assert-Exit 'Fire TV verbinden'
    $model=(& $adbExe -s $thomsonSerial shell getprop ro.product.model | Out-String).Trim()
    $device=(& $adbExe -s $thomsonSerial shell getprop ro.product.device | Out-String).Trim()
    $sdk=(& $adbExe -s $thomsonSerial shell getprop ro.build.version.sdk | Out-String).Trim()
    Assert-Exit 'Geraetedaten'
    if($device -ne 'nagata' -or $model -notmatch 'Thomson'){
        throw "STOP: Nicht der bekannte Thomson: $model / $device"
    }
    if([int]$sdk -lt 28){throw "Android API $sdk zu alt"}
    Write-Host "Thomson erkannt: $model ($device), API $sdk" -ForegroundColor Green
    if($Action -eq 'CollectOnly'){
        if(-not (Is-Installed)){throw 'MarsTube nicht auf dem Fire TV installiert'}
        Read-Performance
    }else{
        if(Test-Path $work){throw "Temp-Worktree schon vorhanden: $work"}
        if(-not (Test-Path $repo)){throw "MarsTube Repo fehlt: $repo"}
        $versionBefore=Current-Version
        $backup=$null
        if($versionBefore -ge 0){
            $paths=@(& $adbExe -s $thomsonSerial shell pm path $package | Where-Object {$_ -match '^package:'})
            Assert-Exit 'Alte MarsTube APK'
            if($paths.Count -ne 1){throw 'Split APK oder unklarer Paketpfad; keine Installation'}
            $backup=Join-Path $save 'MarsTube-Vor-MT019.apk'
            & $adbExe -s $thomsonSerial pull (($paths[0] -replace '^package:','').Trim()) $backup | Out-Host
            Assert-Exit 'APK-Sicherung'
            if(-not (Test-Path $backup) -or (Get-Item $backup).Length -lt 1000000){throw 'Backup unvollstaendig'}
        }
        Write-Host 'Isolierten MT-019 Quellstand laden (Thomson)' -ForegroundColor Cyan
        & git -C $repo fetch origin "refs/heads/$($branch):refs/remotes/origin/$($branch)" | Out-Host
        Assert-Exit 'Git fetch'
        & git -C $repo merge-base --is-ancestor $codeSha "refs/remotes/origin/$branch"
        Assert-Exit 'Gepruefter Code-Commit fehlt'
        & git -C $repo worktree add --detach $work $codeSha | Out-Host
        Assert-Exit 'Isolierter Worktree'
        $created=$true
        $loader=Get-Content (Join-Path $work 'common\src\main\java\com\liskovsoft\smartyoutubetv2\common\app\models\playback\controllers\VideoLoaderController.java') -Raw
        $suggest=Get-Content (Join-Path $work 'common\src\main\java\com\liskovsoft\smartyoutubetv2\common\app\models\playback\controllers\SuggestionsController.java') -Raw
        $fragment=Get-Content (Join-Path $work 'smarttubetv\src\main\java\com\liskovsoft\smartyoutubetv2\tv\ui\playback\PlaybackFragment.java') -Raw
        $activitySource=Get-Content (Join-Path $work 'smarttubetv\src\main\java\com\liskovsoft\smartyoutubetv2\tv\ui\playback\PlaybackActivity.java') -Raw
        $holdSource=Get-Content (Join-Path $work 'smarttubetv\src\main\java\com\liskovsoft\smartyoutubetv2\tv\ui\playback\MarsOkHoldSpeedState.java') -Raw
        if($loader -notmatch 'SHORT_CONTINUATION_TIMEOUT_MS = 6_000L' -or
           $loader -notmatch 'mShortRecovery.claimTimeoutRetry' -or
           $suggest -notmatch 'cancelShortContinuation' -or
           $fragment -notmatch 'setInitialDelay' -or
           $fragment -notmatch 'resetMarsOkDoubleSpeed' -or
           $activitySource -notmatch 'ViewConfiguration.getLongPressTimeout' -or
           $activitySource -notmatch 'setSpeed\(2\.0f\)' -or
           $activitySource -notmatch 'setSpeed\(mOkSpeed.previousSpeed\(\)\)' -or
           $holdSource -notmatch 'class MarsOkHoldSpeedState' -or
           $loader -notmatch 'MT-019 shorts_back_route' -or
           $loader -notmatch 'ShortsFirstFrameRecoveryPolicy' -or
           $loader -notmatch 'mFirstFrameWatchdog' -or
           $loader -notmatch 'mFreshShortPlaybackId' -or
           $loader -notmatch 'mShortWindowAction' -or
           $loader -notmatch 'warmNextKnownShort' -or
           $loader -notmatch 'isFireShortWindowTarget' -or
           $fragment -notmatch 'if \(!show\)' -or
           $fragment -notmatch 'getProgressBarManager\(\).hide\(\)' -or
           (Get-Content (Join-Path $work 'common\src\main\java\com\liskovsoft\smartyoutubetv2\common\exoplayer\controller\ExoPlayerController.java') -Raw) -notmatch 'mEventListener.onFirstFrame\(video\)'){
            throw 'MT-019 Schutzfunktionen fehlen, keine Installation'
        }
        $sdkCandidates=@($env:ANDROID_HOME,$env:ANDROID_SDK_ROOT,(Join-Path $env:LOCALAPPDATA 'Android\Sdk'))
        $local=Join-Path $repo 'local.properties'
        if(Test-Path $local){
            $line=Get-Content $local | Where-Object {$_ -match '^\s*sdk\.dir\s*='} | Select-Object -First 1
            if($line){$sdkCandidates+=(($line -split '=',2)[1].Trim()).Replace('\:',':').Replace('\\','\')}
        }
        $sdkPath=$null
        foreach($candidate in $sdkCandidates){
            if($candidate -and (Test-Path (Join-Path $candidate 'build-tools') -PathType Container)){
                $sdkPath=(Resolve-Path $candidate).Path
                break
            }
        }
        if(-not $sdkPath){throw 'Android SDK fehlt'}
        $env:ANDROID_HOME=$sdkPath
        $env:ANDROID_SDK_ROOT=$sdkPath
        Set-Content (Join-Path $work 'local.properties') -Value ('sdk.dir='+$sdkPath.Replace('\','/')) -Encoding ascii
        & git -c core.longpaths=true -C $work submodule update --init --recursive | Out-Host
        Assert-Exit 'MediaServiceCore'
        $core=(& git -C (Join-Path $work 'MediaServiceCore') rev-parse HEAD | Out-String).Trim()
        Assert-Exit 'MediaServiceCore SHA'
        if($core -ne '40fec843eb24bc264e9c8b429fcee25068e230e2'){throw 'Falscher MediaServiceCore-Stand'}
        foreach($name in @('mt008_probe_media_core.py','mt009_probe_signature_stages.py','mt010a_decoder_cache.py','mt010a2_warm_runtime.py','mt011a_tcl_stage_probes.py','mt011ab_lowcopy_tcl.py','mt011ab_memory_source_cache.py','mt011b3_queue_preloader.py')){
            $patch=Join-Path $work "scripts\mars\$name"
            if($name -ne 'mt008_probe_media_core.py'){
                & python $patch --check | Out-Host
                Assert-Exit "$name check"
            }
            & python $patch | Out-Host
            Assert-Exit "$name apply"
        }
        $log=Join-Path $save 'MarsTube-Gradle.log'
        Push-Location $work
        try{
            & cmd.exe /d /s /c ('call gradlew.bat --no-daemon :common:testStstableDebugUnitTest :smarttubetv:testStstableDebugUnitTest :smarttubetv:assembleStstableRelease > "'+$log+'" 2>&1')
            if($LASTEXITCODE -ne 0){Get-Content $log -Tail 100 | Out-Host;throw "Gradle fehlgeschlagen: $log"}
        } finally { Pop-Location }
        $apks=@(Get-ChildItem (Join-Path $work 'smarttubetv\build\outputs\apk\ststable\release') -File -Filter '*.apk' | Where-Object {$_.Name -match 'universal'})
        if($apks.Count -ne 1){throw "Keine eindeutige Universal-APK: $($apks.Count)"}
        $newApk=Join-Path $save 'MarsTube-MT019-Thomson-Release.apk'
        Copy-Item $apks[0].FullName $newApk
        $signer=Get-ChildItem (Join-Path $sdkPath 'build-tools') -File -Recurse -Filter 'apksigner.bat' | Sort-Object FullName -Descending | Select-Object -First 1 -ExpandProperty FullName
        $aapt=Get-ChildItem (Join-Path $sdkPath 'build-tools') -File -Recurse -Filter 'aapt.exe' | Sort-Object FullName -Descending | Select-Object -First 1 -ExpandProperty FullName
        if(-not $signer -or -not $aapt){throw 'apksigner/aapt fehlt'}
        $newVersion=Check-Apk $newApk $aapt
        if($newVersion -lt $versionBefore){throw "Versions-Downgrade blockiert: $versionBefore -> $newVersion"}
        if($backup -and (Cert $backup $signer) -ne (Cert $newApk $signer)){
            throw 'Signatur abweichend; keine Installation'
        }
        Write-Host "Paket, Version ($newVersion) und Signatur geprueft." -ForegroundColor Green
        $result=& $adbExe -s $thomsonSerial install -r $newApk 2>&1
        $rc=$LASTEXITCODE
        $result | Out-Host
        if($rc -ne 0 -or ($result | Out-String) -notmatch '(?m)^Success\s*$'){
            throw 'MarsTube Update fehlgeschlagen, keine App-Daten geloescht'
        }
        $installed=$true
        Write-Host 'MarsTube auf Thomson aktualisiert; Mars TV und HOME unveraendert.' -ForegroundColor Green
        Test-MarsTubeStartup
        Read-Performance
    }
} finally {
    if($created -and (Test-Path $work)){
        & git -c core.longpaths=true -C $repo worktree remove --force --force $work | Out-Null
        if($LASTEXITCODE -ne 0){Write-Warning "Temp-Worktree noch da: $work"}
    }
    if($installed){Write-Host 'MT-019 APK auf Thomson installiert.' -ForegroundColor Green}
    Write-Host "Sicherungen und Logs: $save"
}
