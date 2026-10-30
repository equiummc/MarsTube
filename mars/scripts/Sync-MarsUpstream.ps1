$ErrorActionPreference = 'Stop'
$root = (git rev-parse --show-toplevel).Trim()
if (-not $root) { throw 'Nicht im Repository.' }
Set-Location $root
if ((git status --porcelain)) { throw 'Lokale Aenderungen, zuerst pruefen.' }
if ((git branch --show-current) -ne 'mars/stable') { throw 'Bitte Branch mars/stable auschecken.' }
$upstream = git remote get-url upstream 2>$null
if (-not $upstream) {
    git remote add upstream 'https://github.com/yuliskov/SmartTube.git'
}
git fetch upstream master --tags
if ($LASTEXITCODE -ne 0) { throw 'Upstream nicht erreichbar.' }
$branch = 'review/smarttube-' + (Get-Date -Format 'yyyyMMdd-HHmm')
git switch -c $branch
if ($LASTEXITCODE -ne 0) { throw 'Branch fehlgeschlagen.' }
git merge --no-commit --no-ff upstream/master
if ($LASTEXITCODE -ne 0) {
    Write-Warning 'Merge-Konflikt! Kein Auto-Fix. Bitte git status pruefen. Diese Review-Branch nicht verteilen.'
    exit 1
}
Write-Host 'Upstream-Merge vorbereitet. Android-Dateien und unsere UI-Aenderungen pruefen.'
Write-Host 'Danach: git commit, git push origin BRANCH, gh pr create --base mars/stable.'
