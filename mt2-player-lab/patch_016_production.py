#!/usr/bin/env python3
"""MT2-016: one permanent MarsTube Android app, never disposable firetest packages.

Apply AFTER patch_015_universal_back.py to the pinned NewTube checkout.
Release builds are signed with one private, permanent owner-controlled keystore.
This script contains NO private key or signing password.
"""
from pathlib import Path
import json
import sys

if len(sys.argv) != 2:
    raise SystemExit("Usage: patch_016_production.py <pinned-checkout>")
root = Path(sys.argv[1]).resolve()
if not (root / "sabr-media3").is_dir():
    raise SystemExit("MT2 production requires pinned SABR Media3 checkout")
meta = json.loads((Path(__file__).parent / "production-version.json").read_text(encoding="utf-8"))
version_name = meta["versionName"]
version_code = int(meta["versionCode"])
app_id = "de.equiummc.marstube2"
if meta["packageName"] != app_id or version_code < 12016:
    raise SystemExit("Invalid immutable production package or version")
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
             f'applicationId "{app_id}"')
once(gradle, 'versionCode 12015', f'versionCode {version_code}')
once(gradle, 'versionName "0.0.15"', f'versionName "{version_name}"')
once(manifest, 'android:label="MarsTube 0.0.15"', 'android:label="MarsTube"')

# Set the app-level icon only after older migration patches have completed.
# Earlier MT2-006 matches the original manifest header exactly.
# Legacy migration may append other manifest tools:replace fields. Update only
# the <application> header so earlier steps are free to keep their own values.
import re
xml = manifest.read_text(encoding="utf-8")
app_start = xml.find("<application")
app_end = xml.find(">", app_start)
if app_start < 0 or app_end < 0:
    raise SystemExit("Missing application manifest header")
header = xml[app_start:app_end]
# Other patches can already set an application icon. Replace it, do not duplicate it.
icons = re.findall(r'android:icon="[^"]+"', header)
if len(icons) > 1:
    raise SystemExit("Duplicate application icons before production patch")
if icons:
    header = header.replace(icons[0], 'android:icon="@drawable/mars_m_logo"', 1)
else:
    header += '\n        android:icon="@drawable/mars_m_logo"'
match = re.search(r'tools:replace="([^"]+)"', header)
if match:
    attributes = match.group(1).split(",")
    if "android:icon" not in attributes:
        attributes.append("android:icon")
    header = header[:match.start()] + 'tools:replace="' + ",".join(attributes) + '"' + header[match.end():]
else:
    header += '\n        tools:replace="android:icon"'
manifest.write_text(xml[:app_start] + header + xml[app_end:], encoding="utf-8")

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
if f'applicationId "{app_id}"' not in final:
    raise SystemExit("Production app identity was not set")
print(f"MarsTube production {version_name}: permanent ID, stable update channel, release signing required.")
