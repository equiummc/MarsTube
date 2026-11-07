#!/usr/bin/env python3
"""MT2-028: preserve original thumbnail aspect while TV Shorts transitions.

NewTube's loading ImageView intentionally used FIT_XY for its phone fullscreen
handoff: a cached 16:9 JPEG is stretched vertically into the TV's 9:16 Shorts
PlayerView for roughly one second while the decoder starts. Change only the
Shorts loading still to FIT_CENTER (letterbox, never deform); keep normal video
and real Media3 output paths unmodified. No new surface/decoder/loading step.
"""
from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit("Usage: patch_028_shorts_undistorted_still.py <pinned-checkout>")
root = Path(sys.argv[1]).resolve()
tv = root / "smarttubetv/src/stmobile/java/com/newtube/mobile/ui"
playback = tv / "playback/MobilePlaybackActivity.java"
if not (root / "sabr-media3").is_dir() or not playback.is_file():
    raise SystemExit("MT2-028: pinned Media3 checkout missing")

def once(path, old, new):
    text = path.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"MT2-028 unsafe anchor count={count}: {path.name}: {old[:100]!r}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")

once(playback,
"""        mStillVideoId = item.videoId;
        mNewVideoStill = true;
        armStillForReady(); // the OLD stream is still READY; wait for the new one""",
"""        mStillVideoId = item.videoId;
        // A TV Short occupies a tall 9:16 PlayerView, but cached YouTube
        // thumbnails often arrive as landscape 16:9 stills. FIT_XY forced
        // that wide picture into the tall view until the first decoded frame,
        // stretching faces vertically. Keep the original ratio instead.
        // This changes ONLY the temporary ImageView, not the TextureView,
        // player content geometry, decoder, start time or stable mini handoff.
        boolean shortsStill = com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(this)
                && com.newtube.mobile.ui.common.MarsShortsSession.isActiveFor(item);
        mVideoStill.setScaleType(shortsStill
                ? android.widget.ImageView.ScaleType.FIT_CENTER
                : android.widget.ImageView.ScaleType.FIT_XY);
        mNewVideoStill = true;
        armStillForReady(); // the OLD stream is still READY; wait for the new one""")

patched = playback.read_text(encoding="utf-8")
assert "ImageView.ScaleType.FIT_CENTER" in patched
assert "mVideoStill.setImageDrawable(seed)" in patched
assert "mVideoStill.setVisibility(View.VISIBLE)" in patched
assert "private boolean maybeLiftStillAtFrame()" in patched
assert "mPlayerView.setResizeMode(" in patched
assert "mPlayerView.setClipToOutline(true)" in patched
shell = (tv / "playback/MarsTvShell.java").read_text(encoding="utf-8")
stage = (tv / "playback/MarsTvShortsStage.java").read_text(encoding="utf-8")
assert "Player.REPEAT_MODE_ONE" in shell
assert "preview.number" not in stage
assert "progressLp.leftMargin = videoLeft + dp(12)" in stage
assert "onShare" not in stage
assert "applicationId \"de.equiummc.marstube2\"" in (
        root / "smarttubetv/build.gradle").read_text(encoding="utf-8")
assert "versionCode 12029" in (root / "smarttubetv/build.gradle").read_text(encoding="utf-8")
print("MT2-028: television Shorts loading still now FIT_CENTER, original 16:9 ratio intact.")
