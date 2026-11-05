#!/usr/bin/env python3
"""MT2-016: one permanent MarsTube Android app, never disposable firetest packages.

Apply AFTER patch_015_universal_back.py to the pinned NewTube checkout.
Release builds are signed with one private, permanent owner-controlled keystore.
This script contains NO private key or signing password.
"""
from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit("Usage: patch_016_production.py <pinned-checkout>")
root = Path(sys.argv[1]).resolve()
if not (root / "sabr-media3").is_dir():
    raise SystemExit("MT2 production requires pinned SABR Media3 checkout")
mobile = root / "smarttubetv/src/stmobile"
gradle = root / "smarttubetv/build.gradle"
manifest = mobile / "AndroidManifest.xml"
feed = root / "common/src/main/res/values/update_urls.xml"
updater = mobile / "java/com/newtube/mobile/update/AppUpdates.java"
tv = mobile / "java/com/newtube/mobile/ui"

def once(path, old, new):
    data = path.read_text(encoding="utf-8")
    count = data.count(old)
    if count != 1:
        raise SystemExit(f"UNSAFE production patch {path}: {count} occurrences of {old!r}")
    path.write_text(data.replace(old, new, 1), encoding="utf-8")

# The production applicationId is NEVER versioned. It stays identical forever.
once(gradle, 'applicationId "de.equiummc.marstube2.firetest015"',
             'applicationId "de.equiummc.marstube2"')
once(gradle, 'versionCode 12015', 'versionCode 12016')
once(gradle, 'versionName "0.0.15"', 'versionName "0.0.16"')
once(manifest, 'android:label="MarsTube 0.0.15"', 'android:label="MarsTube"')

# Do not display a stale literal version on the left rail or player overlay.
for target in (tv / "common/MarsTvRail.java",
               tv / "playback/MarsTvShell.java"):
    once(target, 'version.setText("0.0.15");',
                 'version.setText(com.liskovsoft.smartyoutubetv2.tv.BuildConfig.VERSION_NAME);')

url = "https://github.com/equiummc/MarsTube/releases/latest/download/marstube-tv.json"
if feed.read_text(encoding="utf-8").count(url) != 1:
    raise SystemExit("Production feed must be the ONE real stable/latest GitHub release")
if updater.read_text(encoding="utf-8").count(url) != 0:
    # AppUpdates splits the URL across string literals; it is guarded separately.
    raise SystemExit("Unexpected updater feed representation")
updater_text = updater.read_text(encoding="utf-8")
if ("Reject untrusted MarsTube update feed" not in updater_text
        or 'MarsUpdateApkGuard.isCompatible' not in updater_text):
    raise SystemExit("Production updater security guards missing")
if 'android:icon="@drawable/mars_m_logo"' not in manifest.read_text(encoding="utf-8"):
    raise SystemExit("MarsTube app identity icon missing from manifest")

final = gradle.read_text(encoding="utf-8")
if 'applicationId "de.equiummc.marstube2"' not in final:
    raise SystemExit("Production app identity was not set")
print("MarsTube production 0.0.16: permanent ID, stable update channel, release signing required.")
