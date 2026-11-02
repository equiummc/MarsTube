# MT-023 Sony: two auto-next choices; preserve MT-022 optimized player, data, Google HOME.
# InstallAndTest builds pinned MT-023 source; CollectOnly reads logs.
[CmdletBinding()]
param([ValidateSet('InstallAndTest','CollectOnly')][string]$Action='InstallAndTest')
$ErrorActionPreference='Stop'
$ProgressPreference='SilentlyContinue'
$sonySerial='192.168.41.48:5555'
$adbExe=Join-Path $HOME 'Android-Tools\platform-tools\adb.exe'
$repo=Join-Path $HOME 'MarsTV\mars-smarttube'
$branch='feature/mt-023-mars-special-autoplay-20261102'
$codeSha='83f934ec667a4b5015edbd68910a59abd92367ce'
$package='de.equiummc.mars.smarttube'
$activity='com.liskovsoft.smartyoutubetv2.tv.ui.main.SplashActivity'
$save=Join-Path $HOME ('MarsTV\backups\Sony-MT023\' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
$work=Join-Path $env:TEMP ('MT023-Sony-Tube-' + $PID)
$created=$false
$installed=$false
function Assert-Exit([string]$step) {
    if($LASTEXITCODE -ne 0){throw "$($step): Exit $LASTEXITCODE"}
}
function Is-Installed {
    $list=(& $adbExe -s $sonySerial shell pm list packages --user 0 $package | Out-String)
    Assert-Exit 'Sony Paketinventar'
    return (@($list -split '\r?\n' | Where-Object {$_.Trim() -eq ('package:'+$package)}).Count -gt 0)
}
function Current-Version {
    if(-not (Is-Installed)){return -1}
    $raw=(& $adbExe -s $sonySerial shell dumpsys package $package | Out-String)
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
        & $adbExe -s $sonySerial shell am force-stop $package | Out-Null
        Assert-Exit 'MarsTube force-stop'
        $component=$package+'/'+$activity
        $raw=(& $adbExe -s $sonySerial shell am start -W -n $component 2>&1 | Out-String)
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
    $logs=& $adbExe -s $sonySerial shell logcat -d -v time -s 'MarsTubePerf:I' '*:S' 2>&1
    $logs | Select-Object -Last 1200 | Set-Content (Join-Path $save 'MarsTubePerf-Shorts.txt') -Encoding utf8
    $ram=(& $adbExe -s $sonySerial shell dumpsys meminfo $package | Out-String)
    $ram | Set-Content (Join-Path $save 'MarsTube-RAM.txt') -Encoding utf8
    Write-Host "Logs und Backups: $save" -ForegroundColor Green
}
try {
    if(-not (Test-Path $adbExe)){throw "ADB fehlt: $adbExe"}
    New-Item -ItemType Directory -Force -Path $save | Out-Null
    & $adbExe connect $sonySerial | Out-Host
    Assert-Exit 'Sony verbinden'
    $model=(& $adbExe -s $sonySerial shell getprop ro.product.model | Out-String).Trim()
    $maker=(& $adbExe -s $sonySerial shell getprop ro.product.manufacturer | Out-String).Trim()
    $device=(& $adbExe -s $sonySerial shell getprop ro.product.device | Out-String).Trim()
    $sdk=(& $adbExe -s $sonySerial shell getprop ro.build.version.sdk | Out-String).Trim()
    $abis=(& $adbExe -s $sonySerial shell getprop ro.product.cpu.abilist | Out-String).Trim()
    Assert-Exit 'Geraetedaten'
    if($maker -notmatch '(?i)^sony$' -or $model -ne 'BRAVIA 4K UR1' -or
       $sdk -ne '29' -or $abis -notmatch 'armeabi-v7a'){
        throw "ABBRUCH: kein Sony BRAVIA Android 10: $maker / $model / $device / $sdk / $abis"
    }
    Write-Host "Sony BRAVIA erkannt: $model ($device), API $sdk" -ForegroundColor Green
    if($Action -eq 'CollectOnly'){
        if(-not (Is-Installed)){throw 'MarsTube nicht auf dem Sony installiert'}
        Read-Performance
    }else{
        if(Test-Path $work){throw "Temp-Worktree schon vorhanden: $work"}
        if(-not (Test-Path $repo)){throw "MarsTube Repo fehlt: $repo"}
        $versionBefore=Current-Version
        if($versionBefore -gt 100012){throw "Neuere MarsTube-App v$versionBefore installiert: Kein Downgrade."}
        $backup=$null
        if($versionBefore -ge 0){
            $paths=@(& $adbExe -s $sonySerial shell pm path $package | Where-Object {$_ -match '^package:'})
            Assert-Exit 'Alte MarsTube APK'
            if($paths.Count -ne 1){throw 'Split APK oder unklarer Paketpfad; keine Installation'}
            $backup=Join-Path $save 'MarsTube-Vor-MT023.apk'
            & $adbExe -s $sonySerial pull (($paths[0] -replace '^package:','').Trim()) $backup | Out-Host
            Assert-Exit 'APK-Sicherung'
            if(-not (Test-Path $backup) -or (Get-Item $backup).Length -lt 1000000){throw 'Backup unvollstaendig'}
        }
        Write-Host 'Isolierten MT-023 Quellstand laden' -ForegroundColor Cyan
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
        # MT-022: enforce the current no-speculation policy, not MT-021's
        # superseded 'return isFireShortWindowTarget() || ...' guard.
        # MT-022 safety gate: the A/B install must not enable any
        # speculative Shorts format requests or SABR media queues.
        $windowPolicy=Get-Content (Join-Path $work 'common\src\main\java\com\liskovsoft\smartyoutubetv2\common\app\models\playback\controllers\ShortsWindowPolicy.java') -Raw
        $prefsSource=Get-Content (Join-Path $work 'common\src\main\java\com\liskovsoft\smartyoutubetv2\common\prefs\PlayerTweaksData.java') -Raw
        if($windowPolicy -notmatch 'ENABLE_SPECULATIVE_FORWARD_WARMUP\s*=\s*false' -or
           $loader -match 'maybePreloadThomsonNextShort\(' -or
           $loader -notmatch 'MT-019 black_frame_recovery fresh_retry' -or
           $loader -notmatch 'return ShortsWindowPolicy.ENABLE_SPECULATIVE_FORWARD_WARMUP &&' -or
           $prefsSource -notmatch 'return ShortsWindowPolicy.ENABLE_SPECULATIVE_FORWARD_WARMUP &&' -or
           $loader -notmatch 'MT-022 forward_warmup=false previous_cache=preserved' -or
           $loader -notmatch 'MarsAutoNextPolicy.decide' -or
           $prefsSource -notmatch 'isMarsAutoNextShortsEnabled' -or
           $prefsSource -notmatch 'isMarsAutoNextVideosEnabled' -or
           $prefsSource -notmatch 'Helpers.parseBoolean\(split, 65, false\)' -or
           $prefsSource -notmatch 'Helpers.parseBoolean\(split, 66, false\)' -or
           (Get-Content (Join-Path $work 'smarttubetv\build.gradle') -Raw) -notmatch 'versionCode 100012'){
            throw 'MT-023 auto-next, MT-022 no-prefetch or MT-019 recovery missing; no installation'
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
        $newApk=Join-Path $save 'MarsTube-MT023-Sony-Release.apk'
        Copy-Item $apks[0].FullName $newApk
        $signer=Get-ChildItem (Join-Path $sdkPath 'build-tools') -File -Recurse -Filter 'apksigner.bat' | Sort-Object FullName -Descending | Select-Object -First 1 -ExpandProperty FullName
        $aapt=Get-ChildItem (Join-Path $sdkPath 'build-tools') -File -Recurse -Filter 'aapt.exe' | Sort-Object FullName -Descending | Select-Object -First 1 -ExpandProperty FullName
        if(-not $signer -or -not $aapt){throw 'apksigner/aapt fehlt'}
        $newVersion=Check-Apk $newApk $aapt
        if($newVersion -ne 100012){throw "Unerwarteter MT-023-Versionscode: $newVersion"}
        if($newVersion -lt $versionBefore){throw "Versions-Downgrade blockiert: $versionBefore -> $newVersion"}
        if($backup -and (Cert $backup $signer) -ne (Cert $newApk $signer)){
            throw 'Signatur abweichend; keine Installation'
        }
        Write-Host "Paket, Version ($newVersion) und Signatur geprueft." -ForegroundColor Green
        $result=& $adbExe -s $sonySerial install -r $newApk 2>&1
        $rc=$LASTEXITCODE
        $result | Out-Host
        if($rc -ne 0 -or ($result | Out-String) -notmatch '(?m)^Success\s*$'){
            throw 'MarsTube Update fehlgeschlagen, keine App-Daten geloescht'
        }
        $installed=$true
        Write-Host 'MarsTube auf Sony TV aktualisiert; Mars TV und Sony HOME unveraendert.' -ForegroundColor Green
        Test-MarsTubeStartup
        Read-Performance
    }
} finally {
    if($created -and (Test-Path $work)){
        & git -c core.longpaths=true -C $repo worktree remove --force --force $work | Out-Null
        if($LASTEXITCODE -ne 0){Write-Warning "Temp-Worktree noch da: $work"}
    }
    if($installed){Write-Host 'MT-023 APK auf Sony TV installiert.' -ForegroundColor Green}
    Write-Host "Sicherungen und Logs: $save"
}
