#!/usr/bin/env bash
# Generate a real Android in-app update: 0.0.14 -> 0.0.15.
# Both APKs are built by one runner, with an identical ephemeral Android debug
# signing key and identical package. Existing firetest APKs are not touched.
set -euo pipefail
cd "${GITHUB_WORKSPACE:?}"
root=newtube-reference
mobile="$root/smarttubetv/src/stmobile"
tv="$mobile/java/com/newtube/mobile/ui"
gradle="$root/smarttubetv/build.gradle"
feed="$root/common/src/main/res/values/update_urls.xml"
updater="$mobile/java/com/newtube/mobile/update/AppUpdates.java"
aapt="$ANDROID_HOME/build-tools/37.0.0/aapt"
apksigner="$ANDROID_HOME/build-tools/37.0.0/apksigner"
tag="mt2-015-updatelab-${GITHUB_SHA:0:12}"
test -x "$aapt" && test -x "$apksigner"
mkdir -p dist

# The 014 baseline is the original 014 UI source, NOT the new 015 UI accidentally
# calling methods which exist only after the 015 patch. Feed is pinned to exactly
# one prerelease with a matching APK: no debug URL override or third-party feed.
python3 - "$root" "$tag" <<'PY'
from pathlib import Path
import sys
root, tag = Path(sys.argv[1]), sys.argv[2]
mobile = root / "smarttubetv/src/stmobile"
tv = mobile / "java/com/newtube/mobile/ui"
gradle = root / "smarttubetv/build.gradle"
feed = root / "common/src/main/res/values/update_urls.xml"
updater = mobile / "java/com/newtube/mobile/update/AppUpdates.java"

def replace(path, old, new):
    src = path.read_text(encoding="utf-8")
    if src.count(old) != 1:
        raise SystemExit(f"UpdateLab unsafe replacement {path}: count {src.count(old)} for {old!r}")
    path.write_text(src.replace(old, new, 1), encoding="utf-8")

replace(gradle, 'applicationId "de.equiummc.marstube2.firetest014"',
        'applicationId "de.equiummc.marstube2.updatelab"')
official = "releases/latest/download/marstube-tv.json"
test = f"releases/download/{tag}/marstube-tv.json"
replace(feed, official, test)
replace(updater, official, test)
replace(updater, 'return "https://github.com/equiummc/MarsTube/releases/tag/v" + versionName;',
        f'return "https://github.com/equiummc/MarsTube/releases/tag/{tag}";')

# The source tree already has the patch_014 inputs. Copy the committed, frozen
# 014 files before compiling the actual baseline. Preserve the same installed
# Android package for the subsequent 015 build.
paths = {
    "MarsTvRail.java": tv / "common/MarsTvRail.java",
    "MarsTvShell.java": tv / "playback/MarsTvShell.java",
    "MarsTvSettingsPages.java": tv / "settings/MarsTvSettingsPages.java",
    "MarsTvNavigationTheme.java": tv / "common/MarsTvNavigationTheme.java",
    "MarsShortsSession.java": tv / "common/MarsShortsSession.java",
}
for name, target in paths.items():
    src = Path("mt2-player-lab/legacy") / name.replace(".java", "_014.txt")
    if not src.is_file() or not target.is_file():
        raise SystemExit(f"Missing 014 source snapshot {src} / target {target}")
    target.write_text(src.read_text(encoding="utf-8"), encoding="utf-8")

badging = gradle.read_text()
assert 'versionCode 12014' in badging and 'versionName "0.0.14"' in badging
print(f"UpdateLab: real 014 baseline, pinned manifest for prerelease {tag}")
PY

apk_output() {
    mapfile -t found < <(find "$root/smarttubetv/build/outputs/apk" \
      -type f -name '*.apk' -path '*stmobile*/debug/*' | sort)
    if (( ${#found[@]} != 1 )); then
      printf "::error::Expected exactly one stmobileDebug APK, found %d\n" "${#found[@]}" >&2
      printf '%s\n' "${found[@]}" >&2
      exit 1
    fi
    printf '%s' "${found[0]}"
}

check_apk() {
    local file="$1" name="$2" code="$3" label
    label=$("$aapt" dump badging "$file" | grep -m1 '^package:')
    printf 'Validated package: %s\n' "$label"
    [[ "$label" == *"name='de.equiummc.marstube2.updatelab.debug'"* ]]
    [[ "$label" == *"versionCode='$code'"* ]]
    [[ "$label" == *"versionName='$name'"* ]]
    "$apksigner" verify --print-certs "$file" > "dist/signing-$name.txt"
    grep -q 'Signer #1 certificate SHA-256 digest:' "dist/signing-$name.txt"
}

(cd "$root" && ./gradlew --no-daemon --max-workers=1 \
  -Dorg.gradle.jvmargs=-Xmx4096m :smarttubetv:assembleStmobileDebug --stacktrace)
baseline=$(apk_output)
cp "$baseline" dist/MarsTube-0.0.14-updatebase.apk
check_apk dist/MarsTube-0.0.14-updatebase.apk 0.0.14 12014

# Install only version 015's additions after baseline is completely built.
# The original shared source is still present in the checked-out repository.
python3 - "$tv" <<'PY'
from pathlib import Path
import sys
tv = Path(sys.argv[1])
src = Path("mt2-player-lab")
paths = {
    "MarsTvRail.java": tv / "common/MarsTvRail.java",
    "MarsTvShell.java": tv / "playback/MarsTvShell.java",
    "MarsTvSettingsPages.java": tv / "settings/MarsTvSettingsPages.java",
    "MarsTvNavigationTheme.java": tv / "common/MarsTvNavigationTheme.java",
    "MarsShortsSession.java": tv / "common/MarsShortsSession.java",
}
for name, target in paths.items():
    target.write_text((src / name).read_text(encoding="utf-8"), encoding="utf-8")
PY
MT2_UPDATE_LAB=1 python3 mt2-player-lab/patch_015_universal_back.py "$root"
(cd "$root" && ./gradlew --no-daemon --max-workers=1 \
  -Dorg.gradle.jvmargs=-Xmx4096m :sabr-media3:testDebugUnitTest \
  :smarttubetv:assembleStmobileDebug --stacktrace)
(cd "$root" && ./gradlew --no-daemon --max-workers=1 \
  -Dorg.gradle.jvmargs=-Xmx4096m :smarttubetv:testStmobileDebugUnitTest \
  --tests com.newtube.mobile.player.MarsTvPlayerPrefsTest \
  --tests com.newtube.mobile.ui.common.MarsShortsSessionTest \
  --tests com.newtube.mobile.update.MarsUpdateApkGuardTest --stacktrace)
updated=$(apk_output)
cp "$updated" dist/MarsTube-0.0.15-update.apk
check_apk dist/MarsTube-0.0.15-update.apk 0.0.15 12015

# This is an actual same-package Android update, not merely similarly named
# APKs. Android's package manager verifies the certificate again at install.
cert14=$(grep -m1 'Signer #1 certificate SHA-256 digest:' dist/signing-0.0.14.txt)
cert15=$(grep -m1 'Signer #1 certificate SHA-256 digest:' dist/signing-0.0.15.txt)
if [[ -z "$cert14" || "$cert14" != "$cert15" ]]; then
    echo "::error::Different signing certificates. Do not release." >&2
    exit 1
fi
printf 'Same package, higher version and same signing certificate:\n%s\n' "$cert14"

# The current upstream UpdateManifest parses this exact format. No extra
# top-level fields (other than package and version) are allowed.
size=$(stat -c %s dist/MarsTube-0.0.15-update.apk)
python3 - "$tag" "$size" <<'PY'
from pathlib import Path
import json, sys
tag, size = sys.argv[1], int(sys.argv[2])
url = f"https://github.com/equiummc/MarsTube/releases/download/{tag}/MarsTube-0.0.15-update.apk"
manifest = {
    "package": {"downloadUrlList": [url], "downloadSize": size},
    "0.0.15": {
        "versionCode": 12015,
        "changelog": [
            "BACK öffnet auf allen TV-Seiten die linke Navigation",
            "Einstellungen: rechts direkt in die auswählbaren Optionen",
            "Identisches Menüdesign, passende Symbolgrößen"
        ],
        "changelog_de": [
            "BACK öffnet auf allen TV-Seiten die linke Navigation",
            "Einstellungen: rechts direkt in die auswählbaren Optionen",
            "Identisches Menüdesign, passende Symbolgrößen"
        ]
    }
}
Path("dist/marstube-tv.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
PY
(cd dist && sha256sum MarsTube-0.0.14-updatebase.apk MarsTube-0.0.15-update.apk > SHA256SUMS)
printf '%s\n' "$tag" > dist/release-tag.txt
printf 'Test pair: same package + certificate; ephemeral GitHub Actions debug key.\n%s\n' "$cert14" > dist/proof.txt
echo "Paired update test finished: $tag"
