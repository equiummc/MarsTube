# MT-017 CI: parse + execute Fire installer startup/diagnostics with simulated ADB.
$ErrorActionPreference='Stop'
$env:TEMP=$env:RUNNER_TEMP
$scriptPath=Join-Path $PSScriptRoot 'Install-MT017-Fire-OK-DoubleSpeed.ps1'
$tokens=$null
$errors=$null
[System.Management.Automation.Language.Parser]::ParseFile($scriptPath,[ref]$tokens,[ref]$errors) | Out-Null
if($errors.Count -gt 0){$errors | Format-List | Out-Host;throw 'MT-017 parser errors'}
$contents=Get-Content -LiteralPath $scriptPath -Raw
$prefix=($contents -split '(?m)^try \{',2)[0]
if($prefix -eq $contents){throw 'MT-017 main entrypoint missing'}
. ([scriptblock]::Create($prefix))
if($fireSerial -ne '192.168.41.24:5555' -or $codeSha -ne 'db9c40697280d4268acd5527ec327e2abc551bc4'){
    throw 'Wrong Fire target or source pin'
}
if($contents -match '(?m)^\s*\$home\s*='){throw 'Read-only HOME assignment not allowed'}
if($contents -match '(?i)cmd package set-home-activity|pm uninstall|pm clear|settings put'){throw 'Unexpected destructive device operation'}
$mock=Join-Path $env:RUNNER_TEMP 'mt017-test-adb'
$source=@'
#!/usr/bin/env bash
set -eu
if [[ "$1" != "-s" ]]; then exit 1; fi
shift 2
if [[ "$1" != "shell" ]]; then exit 2; fi
shift
if [[ "$1" == "am" && "$2" == "force-stop" ]]; then exit 0; fi
if [[ "$1" == "am" && "$2" == "start" ]]; then
    echo "Status: ok"
    echo "TotalTime: 415"
    exit 0
fi
if [[ "$1" == "dumpsys" && "$2" == "meminfo" ]]; then
    echo "TOTAL PSS: 24000"
    exit 0
fi
if [[ "$1" == "logcat" ]]; then
    echo "MarsTubePerf: MT-017 shorts_continue_start retry=0"
    exit 0
fi
exit 4
'@
Set-Content -LiteralPath $mock -Value $source -Encoding utf8 -NoNewline
& chmod +x $mock
if($LASTEXITCODE -ne 0){throw 'Cannot run adb mock'}
$script:adbExe=$mock
$script:fireSerial='mock-fire:5555'
$script:save=Join-Path $env:RUNNER_TEMP 'mt017-test-results'
New-Item -Path $script:save -ItemType Directory -Force | Out-Null
Test-MarsTubeStartup
Read-Performance
$rows=@(Import-Csv (Join-Path $save 'MarsTube-Startzeiten.csv'))
if($rows.Count -ne 3){throw "Expected three cold starts, got $($rows.Count)"}
foreach($row in $rows){
    if($row.StartMs -ne '415' -or $row.Ergebnis -ne 'OK'){throw "Invalid benchmark row: $($row|Out-String)"}
}
if(-not (Test-Path (Join-Path $save 'MarsTube-RAM.txt'))){throw 'No RAM log'}
if(-not (Test-Path (Join-Path $save 'MarsTubePerf-Shorts.txt'))){throw 'No playback log'}
Write-Host 'MT-017: preflight, 3 simulated app starts and log extraction GREEN.' -ForegroundColor Green
