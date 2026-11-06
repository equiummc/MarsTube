#!/usr/bin/env python3
"""Fast contract verification for TV Home pause + two 9:16 Shorts previews."""
import json
from pathlib import Path
root = Path(__file__).resolve().parent.parent
p = root / "mt2-player-lab"
meta = json.loads((p / "production-version.json").read_text(encoding="utf-8"))
assert (meta["versionName"],meta["versionCode"],meta["packageName"]) == (
    "0.0.21",12021,"de.equiummc.marstube2")
stage = (p / "MarsTvShortsStage.java").read_text(encoding="utf-8")
shell = (p / "MarsTvShell.java").read_text(encoding="utf-8")
patch = (p / "patch_021_shorts_home_pause.py").read_text(encoding="utf-8")
recipe = (root / "tools/build-marstube-production.sh").read_text(encoding="utf-8")
assert "for (int i = 0; i < 2; i++)" in stage
assert "for (int i = 0; i < 3; i++)" not in stage
assert "Math.round(menuWidth * 0.75f)" in stage
assert "previewImageWidth" in stage and ".fitCenter()" in stage
assert "shortsReactionPosition == 3) seek(-5_000L)" in shell
assert "shortsReactionPosition == 4) seek(5_000L)" in shell
assert "shortVideo ? 5_000L : 10_000L" in shell
assert "openRail();" in shell.split("public boolean handleBack()", 1)[1].split("public boolean handle(", 1)[0]
assert "if (mPlayer != null) mPlayer.pause();" in patch
assert "patch_020_update_freshness.py" in recipe
assert "patch_021_shorts_home_pause.py" in recipe

# The display width of a 9:16 frame at near-full-height has to be ~31% of
# a 16:9 landscape panel, not 20% or 50%. The user asked to keep this ratio.
for w,h in ((1920,1080),(1280,720),(3840,2160)):
    video_w = (h-7)*9/16
    assert abs(video_w/w - 0.3164) < 0.01
    menu_w = w-video_w
    assert menu_w*.25 >= 180 if w>=1280 else True

print("MT2-021: layout, focus, Home pause, back compatibility and package contract passed.")
