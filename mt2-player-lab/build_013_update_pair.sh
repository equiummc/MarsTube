#!/usr/bin/env bash
# One CI runner, one debug keystore, two APKs, one immutable prerelease.
# The pair is intentionally temporary, separate from the installed 0.0.12.
set -euo pipefail
cd "${GITHUB_WORKSPACE:?}"
root="newtube-reference"
gradle="$root/smarttubetv/build.gradle"
xml="$root/common/src/main/res/values/update_urls.xml"
updater="$root/smarttubetv/src/stmobile/java/com/newtube/mobile/update/AppUpdates.java"
rail="$root/smarttubetv/src/stmobile/java/com/newtube/mobile/ui/common/MarsTvRail.java"
shell="$root/smarttubetv/src/stmobile/java/com/newtube/mobile/ui/playback/MarsTvShell.java"
tag="mt2-updatelab-${GITHUB_SHA:0:12}"

# Baseline: 0.0.12 with a NEW package name; original Fire TV 0.0.12 is safe.
python3 - "$gradle" "$xml" "$updater" "$rail" "$shell" "$tag" <<'PY'
from pathlib import Path
import sys
gradle, feed, updater, rail, shell = map(Path, sys.argv[1:6])
tag = sys.argv[6]
def edit(path, before, after):
    text = path.read_text(encoding='utf-8')
    if text.count(before) != 1:
        raise SystemExit(f"Update lab: expected one {before!r} in {path}, got {text.count(before)}")
    path.write_text(text.replace(before, after), encoding='utf-8')
edit(gradle, 'applicationId "de.equiummc.marstube2.firetest012"',
     'applicationId "de.equiummc.marstube2.updatelab"')
official = 'releases/latest/download/marstube-tv.json'
test = f'releases/download/{tag}/marstube-tv.json'
edit(feed, official, test)
edit(updater, official, test)
# Exact pre-v13 layouts belong to the baseline. The new rail calls a
# Browse focus API added only by patch_013, so using it in v12 would not compile.
legacy = Path("mt2-player-lab/legacy")
for source, destination in ((legacy / "MarsTvRail_012.txt", rail),
                            (legacy / "MarsTvShell_012.txt", shell)):
    if not source.is_file():
        raise SystemExit(f"Missing tested baseline overlay: {source}")
    destination.write_text(source.read_text(encoding="utf-8"), encoding="utf-8")
edit(updater,
     'return "https://github.com/equiummc/MarsTube/releases/tag/v" + versionName;',
     f'return "https://github.com/equiummc/MarsTube/releases/tag/{tag}";')
PY

(cd "$root" && ./gradlew --no-daemon --max-workers=1 -Dorg.gradle.jvmargs=-Xmx4096m \
  :smarttubetv:assembleStmobileDebug --stacktrace)
mkdir -p dist
apk=$(find "$root/smarttubetv/build/outputs/apk" -type f -name '*.apk' -path '*stmobile*' | head -1)
test -s "$apk"
cp "$apk" dist/MarsTube-0.0.12-updatelab.apk

# The ONLY modifications between the paired builds are the reviewed 0.0.13
# focus fixes and versionCode increment. Do not clear ~/.android/debug.keystore.
MT2_UPDATE_LAB=1 python3 mt2-player-lab/patch_013_tv_focus_ui.py "$root"
(cd "$root" && ./gradlew --no-daemon --max-workers=1 -Dorg.gradle.jvmargs=-Xmx4096m \
  :sabr-media3:testDebugUnitTest :smarttubetv:assembleStmobileDebug --stacktrace)
(cd "$root" && ./gradlew --no-daemon --max-workers=1 -Dorg.gradle.jvmargs=-Xmx4096m \
  :smarttubetv:testStmobileDebugUnitTest \
  --tests com.newtube.mobile.player.MarsTvPlayerPrefsTest \
  --tests com.newtube.mobile.ui.common.MarsShortsSessionTest \
  --tests com.newtube.mobile.update.MarsUpdateApkGuardTest --stacktrace)
apk=$(find "$root/smarttubetv/build/outputs/apk" -type f -name '*.apk' -path '*stmobile*' | head -1)
test -s "$apk"
cp "$apk" dist/MarsTube-0.0.13-updatelab.apk

aapt="$ANDROID_HOME/build-tools/37.0.0/aapt"
signer="$ANDROID_HOME/build-tools/37.0.0/apksigner"
test -x "$aapt"
test -x "$signer"
for version in 12 13; do
  apk="dist/MarsTube-0.0.$version-updatelab.apk"
  "$aapt" dump badging "$apk" > "dist/badging-$version.txt"
  grep -Fq "name='de.equiummc.marstube2.updatelab.debug'" "dist/badging-$version.txt"
  grep -Fq "versionName='0.0.$version'" "dist/badging-$version.txt"
  "$signer" verify --print-certs "$apk" > "dist/signing-$version.txt"
done
cert12=$(grep -m1 'Signer #1 certificate SHA-256 digest:' dist/signing-12.txt)
cert13=$(grep -m1 'Signer #1 certificate SHA-256 digest:' dist/signing-13.txt)
test -n "$cert12" && test "$cert12" = "$cert13"
echo "Apt signing matches for update pair."

# Publish only the actual 13 APK as in-app update. Baseline 12 is distributed
# through a workflow artifact; the release includes only version 13 and JSON.
size=$(stat -c %s dist/MarsTube-0.0.13-updatelab.apk)
python3 - "$tag" "$size" <<'PY'
import json,sys
from pathlib import Path
tag,size = sys.argv[1],int(sys.argv[2])
url=f"https://github.com/equiummc/MarsTube/releases/download/{tag}/MarsTube-0.0.13-updatelab.apk"
manifest={"package":{"downloadUrlList":[url],"downloadSize":size},
          "0.0.13":{"versionCode":12013,"changelog":[
             "Schmalere, einheitliche TV-Navigation",
             "Kleinere Bedienelemente und zuverlässiger BACK",
             "Fokuswechsel bei Shorts und Menüs verbessert"]}}
Path("dist/marstube-tv.json").write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+"\n")
PY
sha256sum dist/*.apk > dist/SHA256SUMS
printf "%s\n" "$tag" > dist/release-tag.txt
printf "Update lab pair, one CI signing certificate: %s\n" "$cert12" > dist/proof.txt
echo "Built paired update test for $tag"
