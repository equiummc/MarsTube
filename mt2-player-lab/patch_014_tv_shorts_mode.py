#!/usr/bin/env python3
"""MT2-014: real dedicated Shorts D-Pad mode + one shared TV rail + visible updates.
Apply after patch_013_tv_focus_ui.py to disposable pinned checkout only.
"""
from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit("Usage: patch_014_tv_shorts_mode.py <pinned checkout>")
root = Path(sys.argv[1]).resolve()
mobile = root / "smarttubetv/src/stmobile"
tv = mobile / "java/com/newtube/mobile/ui"
if not (root / "sabr-media3").is_dir():
    raise SystemExit("MT2-014 expects pinned Media3/SABR reference")

def once(path, before, after):
    if not path.is_file():
        raise SystemExit(f"MT2-014 file missing: {path}")
    original = path.read_text(encoding="utf-8")
    count = original.count(before)
    if count != 1:
        raise SystemExit(f"MT2-014 unsafe patch: {path}: {count} anchors {before[:90]!r}")
    path.write_text(original.replace(before, after, 1), encoding="utf-8")

gradle = root / "smarttubetv/build.gradle"
manifest = mobile / "AndroidManifest.xml"
once(gradle, 'applicationId "de.equiummc.marstube2.firetest013"',
             'applicationId "de.equiummc.marstube2.firetest014"')
once(gradle, 'versionCode 12013', 'versionCode 12014')
once(gradle, 'versionName "0.0.13"', 'versionName "0.0.14"')
once(manifest, 'android:label="MarsTube 0.0.13"',
               'android:label="MarsTube 0.0.14"')

src = Path(__file__).resolve().parent
theme = src / "MarsTvNavigationTheme.java"
target = tv / "common/MarsTvNavigationTheme.java"
if not theme.is_file() or target.exists():
    raise SystemExit("MT2-014 theme missing or already patched")
target.write_text(theme.read_text(encoding="utf-8"), encoding="utf-8")

# Input mode, settings and both TV rail sources were copied by 010/013
# from this branch's versioned sources. Assert the result, do not duplicate.
shell = (tv / "playback/MarsTvShell.java").read_text(encoding="utf-8")
rail = (tv / "common/MarsTvRail.java").read_text(encoding="utf-8")
settings = (tv / "settings/MarsTvSettingsPages.java").read_text(encoding="utf-8")
shorts = (tv / "common/MarsShortsSession.java").read_text(encoding="utf-8")
checks = {
    "single rail width in player": "MarsTvNavigationTheme.WIDTH_DP" in shell,
    "same rail style on Home & Settings": "MarsTvNavigationTheme.WIDTH_DP" in rail,
    "Shorts-only input block": "if (actions.isShortsPlayback() && focus != Focus.RAIL && !infoOpen)" in shell,
    "Shorts regular overlay hidden": "boolean panelVisible = !pip && !shortsMode" in shell,
    "Shorts single OK action": "if (actions.isShortsPlayback() && focus != Focus.RAIL)" in shell,
    "Shorts hold to speed": "if (actions.isShortsPlayback() && focus != Focus.RAIL) return true" in shell,
    "Settings root direct update entry": settings.count('SettingsRow.action("Auf Updates prüfen"') >= 2,
    "Shorts queue during prepare": "return video == null || contains(video)" in shorts,
    "player v0.0.14": 'version.setText("0.0.14")' in shell,
    "rail v0.0.14": 'version.setText("0.0.14")' in rail,
}
for item, condition in checks.items():
    if not condition:
        raise SystemExit(f"MT2-014 failed invariant: {item}")

print("MT2-014 applied: dedicated Short D-Pad actions, common 64dp UI, discoverable updates.")
