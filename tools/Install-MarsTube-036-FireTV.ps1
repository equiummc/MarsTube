#requires -Version 7.0
# MARS-ZIEL: PS7 / Windows 11 / Fire TV Cube / MarsTube 0.0.36
# MARS-MODUS: Nur Update. Keine Deinstallation, kein Downgrade, keine App-Daten loeschen.
[CmdletBinding()]
param(
    [string]$Zip = "$env:USERPROFILE\Downloads\MarsTube-0.0.36-ABC-ci-only.zip",
    [string]$FireTV = "192.168.41.24:5555",
    [switch]$UpdateBestaetigt
)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
$package = 'de.equiummc.marstube2'
$expectedCode = 12036
$expectedCert = 'f80f712f9dbbd16c692e5d5eaff7598db73a660c82911bfd3ff0d0215c8f84e9'
if (-not $UpdateBestaetigt) { throw 'Kein Update ohne -UpdateBestaetigt.' }
if (-not (Test-Path -LiteralPath $Zip -PathType Leaf)) { throw "ZIP fehlt: $Zip" }
if ($FireTV -notmatch '^(?:\d{1,3}\.){3}\d{1,3}:5555$') { throw "Unerwartetes Geraeteziel: $FireTV" }

$roots = @($env:ANDROID_HOME,$env:ANDROID_SDK_ROOT,
    (Join-Path $env:LOCALAPPDATA 'Android\Sdk')) |
    Where-Object { $_ -and (Test-Path -LiteralPath $_ -PathType Container) }
$adbCommand = Get-Command 'adb.exe' -ErrorAction SilentlyContinue
$adb = if ($adbCommand) { $adbCommand.Source } else { $null }
foreach ($root in $roots) {
    $candidate = Join-Path $root 'platform-tools\adb.exe'
    if (-not $adb -and (Test-Path $candidate)) { $adb = $candidate }
}
if (-not $adb) { throw 'ADB aus Android SDK Platform-Tools fehlt.' }
$tools = @()
foreach ($root in $roots) {
    $folder = Join-Path $root 'build-tools'
    if (Test-Path $folder) { $tools += Get-ChildItem $folder -Directory }
}
$build = $tools |
    Sort-Object { try { [version]$_.Name } catch { [version]'0.0' } } -Descending |
    Where-Object { (Test-Path (Join-Path $_.FullName 'aapt.exe')) -and
                   (Test-Path (Join-Path $_.FullName 'lib\apksigner.jar')) } |
    Select-Object -First 1
if (-not $build) { throw 'Android SDK Build Tools mit aapt/apksigner fehlen.' }
$aapt = Join-Path $build.FullName 'aapt.exe'
$apksigner = Join-Path $build.FullName 'apksigner.bat'
if (-not (Test-Path -LiteralPath $apksigner -PathType Leaf)) {
    throw 'apksigner.bat fehlt in den vorhandenen Android-Build-Tools.'
}

function Invoke-Bounded {
    param([string]$Exe,[string[]]$Arguments,[int]$TimeoutSeconds=30)
    $start = [System.Diagnostics.ProcessStartInfo]::new()
    $start.FileName = $Exe
    $start.UseShellExecute = $false
    $start.CreateNoWindow = $true
    $start.RedirectStandardOutput = $true
    $start.RedirectStandardError = $true
    foreach ($argument in $Arguments) { [void]$start.ArgumentList.Add($argument) }
    $process = [System.Diagnostics.Process]::new()
    $process.StartInfo = $start
    try {
        if (-not $process.Start()) { throw "Programm konnte nicht gestartet werden: $Exe" }
        $stdout = $process.StandardOutput.ReadToEndAsync()
        $stderr = $process.StandardError.ReadToEndAsync()
        if (-not $process.WaitForExit($TimeoutSeconds * 1000)) {
            $process.Kill($true)
            throw "Zeitlimit fuer Programm ueberschritten: $Exe"
        }
        $out = $stdout.GetAwaiter().GetResult() + [Environment]::NewLine +
               $stderr.GetAwaiter().GetResult()
        if ($process.ExitCode -ne 0) { throw "Fehler ($Exe): $out" }
        return $out
    } finally { $process.Dispose() }
}
function Get-Certificate {
    param([string]$Apk)
    # Same apksigner.bat invocation that already worked in the 0.0.33 Fire-TV installer.
    # Java -jar output can differ on a Windows SDK; never accept an unverified APK.
    $output = ((& $apksigner verify --print-certs $Apk 2>&1) | Out-String)
    if ($LASTEXITCODE -ne 0) {
        throw "APK-Signaturpruefung fehlgeschlagen (Exitcode $LASTEXITCODE). Keine Installation."
    }
    $match = [regex]::Match($output,'(?im)certificate\s+SHA-256\s+digest\s*:\s*([a-f0-9]{64})')
    if (-not $match.Success) {
        throw "Zertifikat nicht eindeutig aus apksigner auslesbar. Keine Installation."
    }
    return $match.Groups[1].Value.ToLowerInvariant()
}
function Invoke-FireTV {
    param([string[]]$Arguments,[int]$TimeoutSeconds=30)
    return Invoke-Bounded -Exe $adb -Arguments (@('-s',$FireTV) + $Arguments) -TimeoutSeconds $TimeoutSeconds
}

$tmp = Join-Path ([IO.Path]::GetTempPath()) ('MarsTube-036-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $tmp | Out-Null
try {
    Expand-Archive -LiteralPath $Zip -DestinationPath $tmp
    $apks = @(Get-ChildItem -LiteralPath $tmp -File -Recurse -Filter '*.apk')
    $sums = @(Get-ChildItem -LiteralPath $tmp -File -Recurse -Filter 'SHA256SUMS')
    if ($apks.Count -ne 1 -or $apks[0].Name -ne 'MarsTube-0.0.36.apk' -or $sums.Count -ne 1) {
        throw 'ZIP nicht eindeutig: APK oder SHA256SUMS fehlt.'
    }
    $apk = $apks[0].FullName
    $hashes = @(Get-Content -LiteralPath $sums[0].FullName |
        Where-Object { $_ -match '^[0-9a-fA-F]{64}\s+\*?MarsTube-0\.0\.36\.apk\s*$' })
    if ($hashes.Count -ne 1) { throw 'SHA256SUMS fehlt oder ist mehrdeutig.' }
    if ((Get-FileHash -LiteralPath $apk -Algorithm SHA256).Hash.ToLowerInvariant() -ne
        $hashes[0].Substring(0,64).ToLowerInvariant()) { throw 'SHA-256 falsch.' }
    $badge = Invoke-Bounded -Exe $aapt -Arguments @('dump','badging',$apk)
    $info = [regex]::Match($badge,"package: name='([^']+)' versionCode='(\d+)' versionName='([^']+)'")
    if (-not $info.Success -or $info.Groups[1].Value -ne $package -or
        [int]$info.Groups[2].Value -ne $expectedCode -or
        $info.Groups[3].Value -ne '0.0.36') {
        throw 'Falsches Paket oder falsche Version. Abbruch.'
    }
    $newCert = Get-Certificate $apk
    if ($newCert -ne $expectedCert) {
        throw 'APK ist nicht mit dem bekannten MarsTube-Zertifikat signiert. Keine Installation.'
    }
    Write-Host 'MarsTube 0.0.36: SHA256, Paket, Versionsnummer und Signatur geprueft.'

    [void](Invoke-Bounded -Exe $adb -Arguments @('connect',$FireTV) -TimeoutSeconds 15)
    $maker = (Invoke-FireTV -Arguments @('shell','getprop','ro.product.manufacturer')).Trim()
    $model = (Invoke-FireTV -Arguments @('shell','getprop','ro.product.model')).Trim()
    if ($maker -notmatch '(?i)Amazon' -or $model -notmatch '(?i)AFTR|Fire') {
        throw "Geraet ist nicht der vereinbarte Fire TV: $maker $model"
    }
    $pathOutput = Invoke-FireTV -Arguments @('shell','pm','path',$package)
    $matchPath = [regex]::Match($pathOutput,'(?m)^package:(\S*base\.apk)\s*$')
    if (-not $matchPath.Success) { throw 'MarsTube ist nicht installiert. Kein Neuinstallieren.' }
    $before = Invoke-FireTV -Arguments @('shell','dumpsys','package',$package)
    $current = [regex]::Match($before,'versionCode=(\d+)')
    if (-not $current.Success -or [int]$current.Groups[1].Value -ge $expectedCode) {
        throw 'Bestehende Versionsnummer nicht eindeutig kleiner als 12036.'
    }
    Write-Host "Fire TV $model : Version $($current.Groups[1].Value) -> 12036"
    $oldApk = Join-Path $tmp 'installed-base.apk'
    [void](Invoke-FireTV -Arguments @('pull',$matchPath.Groups[1].Value,$oldApk) -TimeoutSeconds 80)
    if (-not (Test-Path -LiteralPath $oldApk -PathType Leaf)) {
        throw 'Die bestehende APK konnte nicht fuer Signaturvergleich gelesen werden.'
    }
    if ((Get-Certificate $oldApk) -ne $newCert) {
        throw 'SIGNATURKONFLIKT. Die vorhandene App bleibt installiert.'
    }
    Write-Host 'Bestehende Signatur ist identisch; installiere ausschliesslich Update.'
    $installed = Invoke-FireTV -Arguments @('install','-r',$apk) -TimeoutSeconds 150
    if ($installed -notmatch '\bSuccess\b') { throw "Installation nicht bestaetigt: $installed" }
    $after = Invoke-FireTV -Arguments @('shell','dumpsys','package',$package)
    $check = [regex]::Match($after,'versionCode=(\d+)')
    if (-not $check.Success -or [int]$check.Groups[1].Value -ne $expectedCode) {
        throw 'ADB meldete Erfolg, Zielversion ist aber nicht eindeutig 12036.'
    }
    Write-Host 'UPDATE ERFOLGREICH: MarsTube 0.0.36 / Fire TV Cube.' -ForegroundColor Green
} finally {
    Remove-Item -LiteralPath $tmp -Force -Recurse -ErrorAction SilentlyContinue
}
