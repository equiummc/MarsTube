param([string]$Owner = 'equiummc', [string]$ReleaseNotes = 'MarsTube Stabilitaets- und Designupdate')
$ErrorActionPreference = 'Stop'
$root = (git rev-parse --show-toplevel).Trim()
if (-not $root) { throw 'Nicht im Mars-Repo.' }
Set-Location $root
if ((git branch --show-current) -ne 'mars/stable') { throw 'Nur auf mars/stable freigeben.' }
if (git status --porcelain) { throw 'Arbeitsbaum nicht sauber.' }
if (-not (Test-Path 'keystore.properties')) { throw 'Keystore fehlt. Zuerst eigene Signatur einrichten.' }
& .\gradlew.bat :smarttubetv:assembleStstableRelease --no-daemon
if ($LASTEXITCODE -ne 0) { throw 'Build fehlgeschlagen.' }
$gradle = Get-Content 'smarttubetv\build.gradle' -Raw
$code = [regex]::Match($gradle,'(?m)^\s*versionCode\s+(\d+)').Groups[1].Value
$version = [regex]::Match($gradle,'(?m)^\s*versionName\s+"([^"]+)"').Groups[1].Value
if (-not $code -or -not $version) { throw 'Version nicht lesbar.' }
$apks = @(Get-ChildItem 'smarttubetv\build\outputs\apk' -Recurse -Filter '*universal*.apk' | Where-Object { $_.FullName -match 'release' })
if ($apks.Count -ne 1) { throw "Erwartet genau eine Universal-Release-APK, gefunden: $($apks.Count)" }
$apk = $apks[0].FullName
$apksigner = Get-ChildItem "$env:LOCALAPPDATA\Android\Sdk\build-tools" -Recurse -Filter 'apksigner.bat' -ErrorAction SilentlyContinue | Sort-Object FullName -Descending | Select-Object -First 1
if (-not $apksigner) { throw 'apksigner nicht gefunden: Android SDK Build Tools installieren.' }
& $apksigner.FullName verify --verbose $apk
if ($LASTEXITCODE -ne 0) { throw 'APK-Signatur ungültig.' }
$tag = 'mars-v' + $code
$releaseAsset = Join-Path $env:TEMP 'marstube-stable-universal.apk'
Copy-Item $apk $releaseAsset -Force
$full = "$Owner/mars-smarttube"
Write-Host "Veroeffentliche signierte APK als $tag"
& gh release create $tag $releaseAsset --repo $full --title "MarsTube $version" --notes $ReleaseNotes
if ($LASTEXITCODE -ne 0) { throw 'GitHub Release fehlgeschlagen.' }
# AppVersionChecker expects "package" and versionName -> versionCode changelog.
$stable = 'mars/updates/stable.json'
$feed = Get-Content $stable -Raw | ConvertFrom-Json
$feed.package.downloadUrl = "https://github.com/$full/releases/download/$tag/marstube-stable-universal.apk"
$entry = [pscustomobject]@{versionCode=[int]$code;changelog=@($ReleaseNotes)}
$feed | Add-Member -NotePropertyName $version -NotePropertyValue $entry -Force
$feedJson = $feed | ConvertTo-Json -Depth 8
[System.IO.File]::WriteAllText((Join-Path $root $stable), $feedJson, (New-Object System.Text.UTF8Encoding($false)))
& git add $stable
& git commit -m "release: MarsTube $version update feed"
if ($LASTEXITCODE -ne 0) { throw 'Feed Commit fehlgeschlagen.' }
& git push origin mars/stable
if ($LASTEXITCODE -ne 0) { throw 'Feed Push fehlgeschlagen.' }

# Updater reads raw.githubusercontent.com/OWNER/mars-smarttube/master/...;
# therefore publish the reviewed manifest on the PUBLIC default branch too.
& git switch master
if ($LASTEXITCODE -ne 0) { throw 'Master-Branch fehlt.' }
& git pull --ff-only origin master
if ($LASTEXITCODE -ne 0) { throw 'Master ist nicht aktuell.' }
& git checkout 'mars/stable' -- 'mars/updates/stable.json'
if ($LASTEXITCODE -ne 0) { throw 'Feed konnte nicht auf master gespiegelt werden.' }
& git add 'mars/updates/stable.json'
& git diff --cached --quiet
if ($LASTEXITCODE -ne 0) {
    & git commit -m "release: public MarsTube update feed $version"
    if ($LASTEXITCODE -ne 0) { throw 'Feed Commit auf master fehlgeschlagen.' }
    & git push origin master
    if ($LASTEXITCODE -ne 0) { throw 'Feed Push auf master fehlgeschlagen.' }
}
& git switch mars/stable
Write-Host "MarsTube $version signiert; Release und oeffentlicher Update-Feed veroeffentlicht."
