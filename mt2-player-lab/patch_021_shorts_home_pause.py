#!/usr/bin/env python3
"""MT2-021: user-visible TV navigation to Home pauses, two Shorts previews.

After ALL 020 patches, replace only TV overlay sources, keep the one Media3
player/window, signed application ID, update feed, URL and signatures unchanged.
"""
from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit("Usage: patch_021_shorts_home_pause.py <pinned-newtube-root>")
root = Path(sys.argv[1]).resolve()
if not (root / "sabr-media3").is_dir():
    raise SystemExit("MT2-021: pinned Media3 source missing")
src = Path(__file__).resolve().parent
tv = root / "smarttubetv/src/stmobile/java/com/newtube/mobile/ui/playback"
playback = tv / "MobilePlaybackActivity.java"
if not playback.is_file():
    raise SystemExit("MT2-021: original playback class missing")

for file in ("MarsTvShortsStage.java", "MarsTvShell.java"):
    source = src / file
    target = tv / file
    if not source.is_file() or not target.is_file():
        raise SystemExit(f"MT2-021 missing TV class: {file}")
    target.write_text(source.read_text(encoding="utf-8"), encoding="utf-8")

code = playback.read_text(encoding="utf-8")
# Earlier patch_005_navigation.py deliberately replaces CLEAR_TOP with
# mars_tv_return_to_player. Match only the callback header so that the
# existing Android task/back-stack behavior is preserved byte-for-byte.
before = '''                    @Override public void navigate(int destination) {'''
after = '''                    @Override public void navigate(int destination) {
                        // Leaving the playback screen is different from merely
                        // opening the rail: pause audio/video before navigation.
                        // Keep the Media3 session and Android return-to-player.
                        if (mPlayer != null) mPlayer.pause();'''
if "mars_tv_return_to_player" not in code or "mars_tv_section" not in code:
    raise SystemExit("MT2-021: expected prior stable Browse navigation missing")
if code.count(before) != 1:
    raise SystemExit(f"MT2-021 unsafe navigation anchor: {code.count(before)}")
playback.write_text(code.replace(before, after, 1), encoding="utf-8")

stage = (tv / "MarsTvShortsStage.java").read_text(encoding="utf-8")
shell = (tv / "MarsTvShell.java").read_text(encoding="utf-8")
assert "for (int i = 0; i < 2; i++)" in stage
assert "for (int i = 0; i < 3; i++)" not in stage
assert "previewImageWidth" in stage and ".fitCenter()" in stage
assert "Math.round(menuWidth * 0.75f)" in stage
assert '"⏪"' in stage and '"⏩"' in stage
assert "shortsReactionPosition == 3) seek(-5_000L)" in shell
assert "shortsReactionPosition == 4) seek(5_000L)" in shell
assert "shortVideo ? 5_000L : 10_000L" in shell
assert "public boolean handleBack()" in shell
assert "openRail();" in shell
assert "android.view.Gravity.RIGHT" in playback.read_text(encoding="utf-8")
assert "RESIZE_MODE_ZOOM" in playback.read_text(encoding="utf-8")
assert "if (mPlayer != null) mPlayer.pause();" in playback.read_text(encoding="utf-8")
assert 'applicationId "de.equiummc.marstube2"' in (root / "smarttubetv/build.gradle").read_text(encoding="utf-8")
assert "mars_check" in (root / "SharedModules/appupdatechecker2/src/main/java/"
    "com/liskovsoft/appupdatechecker2/core/AppVersionChecker.java").read_text(encoding="utf-8")
print("MT2-021: playback pause on navigation only, BACK retained; two 9:16 previews,"
      " compact center and 5s seek controls. One permanent signed app.")
