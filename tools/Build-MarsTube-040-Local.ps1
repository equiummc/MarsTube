#requires -Version 7.0
[CmdletBinding()]
param([string]$Source = 'E:\MarsTube\.mt2-040-work\.mt2-build\source040')
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$env:ANDROID_HOME = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
$java = (Get-Command java.exe -ErrorAction Stop).Source
$env:JAVA_HOME = Split-Path (Split-Path $java)
$props = Join-Path $Source 'keystore.properties'
if (Test-Path -LiteralPath $props) { throw 'Vorhandene Signierkonfiguration wird nicht überschrieben.' }
function Escape-Property([string]$Value) {
    return $Value.Replace('\','\\').Replace("`r",'\r').Replace("`n",'\n')
}
try {
    $secret = ConvertTo-SecureString ([IO.File]::ReadAllText('E:\MarsTube\signing\release-credentials.dpapi').Trim())
    $credentials = (ConvertFrom-SecureString $secret -AsPlainText) | ConvertFrom-Json
    $values = @(
        'storeFile=E:/MarsTube/signing/release.jks'
        'storePassword=' + (Escape-Property $credentials.storePassword)
        'keyAlias=' + (Escape-Property $credentials.keyAlias)
        'keyPassword=' + (Escape-Property $credentials.keyPassword)
    )
    [IO.File]::WriteAllLines($props,$values,[Text.UTF8Encoding]::new($false))
    $owner = [Security.Principal.WindowsIdentity]::GetCurrent().Name
    & icacls.exe $props /inheritance:r /grant:r ($owner+':F') '*S-1-5-18:F' '*S-1-5-32-544:F' 1>$null
    if ($LASTEXITCODE -ne 0) { throw 'Temporäre Signierkonfiguration konnte nicht abgesichert werden.' }
    Push-Location $Source
    try {
        & $java -classpath (Join-Path $Source 'gradle\wrapper\gradle-wrapper.jar') org.gradle.wrapper.GradleWrapperMain --no-daemon --max-workers=2 '-Dorg.gradle.jvmargs=-Xmx4096m' :sabr-media3:testDebugUnitTest :smarttubetv:testStmobileDebugUnitTest :smarttubetv:assembleStmobileRelease --stacktrace
        if ($LASTEXITCODE -ne 0) { throw 'Gradle-Build oder Unit-Tests fehlgeschlagen.' }
    } finally { Pop-Location }
} finally {
    if (Test-Path -LiteralPath $props) { Remove-Item -LiteralPath $props -Force }
    Remove-Variable secret,credentials,values -ErrorAction SilentlyContinue
}
