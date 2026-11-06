#!/usr/bin/env python3
"""Static MT2-019 contract checks before the real Java compile and on-device tests."""
from pathlib import Path
import json
import re

root = Path(__file__).resolve().parent.parent
meta = json.loads((root / "mt2-player-lab/production-version.json").read_text(encoding="utf-8"))
assert (meta["versionName"], meta["versionCode"], meta["packageName"]) == (
    "0.0.19", 12019, "de.equiummc.marstube2")
stage = (root / "mt2-player-lab/MarsTvShortsStage.java").read_text(encoding="utf-8")
shell = (root / "mt2-player-lab/MarsTvShell.java").read_text(encoding="utf-8")
patch = (root / "mt2-player-lab/patch_019_shorts_three_column.py").read_text(encoding="utf-8")
recipe = (root / "tools/build-marstube-production.sh").read_text(encoding="utf-8")

assert len(re.findall(r"for \(int i = 0; i < 3; i\+\+\)", stage)) == 1
assert "Math.round(videoHeight * 9f / 16f)" in patch
assert "RESIZE_MODE_ZOOM" in patch
assert "Gravity.RIGHT | Gravity.BOTTOM" in stage
assert "dp(7), Gravity.RIGHT | Gravity.BOTTOM" in stage
assert "ViewGroup.LayoutParams.MATCH_PARENT" in patch
assert "mMarsShortsOriginalResizeMode" in patch
assert 'openPlayerOption(R.id.action_playlist_add, false)' in patch
assert 'actions::like, actions::dislike, actions::save' in shell
assert "SHORTS_AUTO" in shell and "setAutoFocused" in shell
assert "KEYCODE_MEDIA_REWIND" in shell and "KEYCODE_MEDIA_FAST_FORWARD" in shell
assert "KEYCODE_MEDIA_PLAY_PAUSE" in shell
assert "shortsReactionPosition < 2" in shell
assert "public boolean handleBack()" in shell and "openRail();" in shell
assert "patch_019_shorts_three_column.py" in recipe
assert "↻  EIN" in stage and '"★"' in stage
assert "→ Video" not in stage  # No bottom navigation hint
print("MT2-019 static contract OK (does not replace Android compile or TV tests).")
