#!/usr/bin/env python3
"""MT2-019: real right-edge portrait video and existing YouTube save action.

Runs AFTER patch_018_tv_ux.py on the reproducible, pinned disposable checkout.
Never changes the permanent package ID, updater or signing configuration.
"""
from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit("Usage: patch_019_shorts_three_column.py <pinned-checkout>")
root = Path(sys.argv[1]).resolve()
tv = root / "smarttubetv/src/stmobile/java/com/newtube/mobile/ui"
if not (root / "sabr-media3").is_dir():
    raise SystemExit("MT2-019: pinned Media3 checkout missing")


def once(path, before, after):
    if not path.is_file():
        raise SystemExit(f"MT2-019 missing: {path}")
    data = path.read_text(encoding="utf-8")
    count = data.count(before)
    if count != 1:
        raise SystemExit(
            f"MT2-019 unsafe anchor {path}: {count} matches: {before[:90]!r}"
        )
    path.write_text(data.replace(before, after, 1), encoding="utf-8")


playback = tv / "playback/MobilePlaybackActivity.java"

# The full-screen PinchZoomLayout is also the full-screen shell host. Never
# resize that host: resize the existing child PlayerView instead.
# Restore the prior user-selected fit/zoom mode when leaving Shorts.
once(playback,
     "    private Media3PlayerController mExoPlayerController;",
     """    private Media3PlayerController mExoPlayerController;
    private int mMarsShortsOriginalResizeMode = -1;""")

once(playback,
     """                    @Override public void dislike() {
                        if (mWatchDislike != null) mWatchDislike.performClick();
                    }
                    @Override public void quality() {""",
     """                    @Override public void dislike() {
                        if (mWatchDislike != null) mWatchDislike.performClick();
                    }
                    @Override public void save() {
                        // The existing YouTube Add-to-Playlist dialog; never pretend
                        // to save only in a local fake favourite list.
                        openPlayerOption(R.id.action_playlist_add, false);
                    }
                    @Override public void quality() {""")

once(playback,
     """                    @Override public void positionShortsVideo(boolean enabled) {
                        // The *same* Media3 PlayerView is shifted into the 9:16 right column.
                        // No second decoder, no new source/seek/pause. Immediately reversible.
                        if (mVideoArea != null) {
                            // Letterbox space is MarsTube blue, not hard black.
                            mVideoArea.setBackgroundColor(enabled
                                    ? 0xFF081A33 : android.graphics.Color.BLACK);
                        }
                        if (mPlayerView != null) {
                            mPlayerView.animate().cancel();
                            mPlayerView.setTranslationX(enabled
                                    ? getResources().getDisplayMetrics().widthPixels * 0.27f
                                    : 0f);
                        }
                    }""",
     """                    @Override public void positionShortsVideo(boolean enabled) {
                        // Keep the parent video area and MarsTvShell full-screen.
                        // Crop only the existing Media3 PlayerView to a fixed 9:16
                        // window, flush right; progress occupies the bottom 7dp.
                        if (mVideoArea != null) {
                            mVideoArea.setBackgroundColor(enabled
                                    ? 0xFF081A33 : android.graphics.Color.BLACK);
                        }
                        if (mPlayerView == null) return;
                        mPlayerView.animate().cancel();
                        mPlayerView.setTranslationX(0f);
                        android.view.ViewGroup.LayoutParams raw = mPlayerView.getLayoutParams();
                        if (!(raw instanceof android.widget.FrameLayout.LayoutParams)) {
                            throw new IllegalStateException("MarsTube Shorts requires FrameLayout player");
                        }
                        android.widget.FrameLayout.LayoutParams lp =
                                (android.widget.FrameLayout.LayoutParams) raw;
                        if (enabled) {
                            if (mMarsShortsOriginalResizeMode < 0) {
                                mMarsShortsOriginalResizeMode = mPlayerView.getResizeMode();
                            }
                            int fullHeight = mVideoArea != null && mVideoArea.getHeight() > 0
                                    ? mVideoArea.getHeight()
                                    : getResources().getDisplayMetrics().heightPixels;
                            int fullWidth = mVideoArea != null && mVideoArea.getWidth() > 0
                                    ? mVideoArea.getWidth()
                                    : getResources().getDisplayMetrics().widthPixels;
                            int bar = Math.max(1, Math.round(
                                    7f * getResources().getDisplayMetrics().density));
                            int videoHeight = Math.max(1, fullHeight - bar);
                            lp.width = Math.min(fullWidth,
                                    Math.round(videoHeight * 9f / 16f));
                            lp.height = videoHeight;
                            lp.gravity = android.view.Gravity.TOP | android.view.Gravity.RIGHT;
                            mPlayerView.setLayoutParams(lp);
                            mPlayerView.setResizeMode(
                                    androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM);
                        } else {
                            lp.width = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
                            lp.height = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
                            lp.gravity = android.view.Gravity.CENTER;
                            mPlayerView.setLayoutParams(lp);
                            if (mMarsShortsOriginalResizeMode >= 0) {
                                mPlayerView.setResizeMode(mMarsShortsOriginalResizeMode);
                                mMarsShortsOriginalResizeMode = -1;
                            }
                        }
                    }""")

stage = (tv / "playback/MarsTvShortsStage.java").read_text(encoding="utf-8")
shell = (tv / "playback/MarsTvShell.java").read_text(encoding="utf-8")
activity = playback.read_text(encoding="utf-8")
for name, ok in {
    "one regular signed app": 'applicationId "de.equiummc.marstube2"'
        in (root / "smarttubetv/build.gradle").read_text(encoding="utf-8"),
    "3 only": "for (int i = 0; i < 3; i++)" in stage,
    "portrait crop": "RESIZE_MODE_ZOOM" in activity,
    "flush-right player": "android.view.Gravity.RIGHT" in activity,
    "true playlist save": "openPlayerOption(R.id.action_playlist_add, false)" in activity,
    "center Save focus": "shortsReactionPosition == 1) actions.dislike()" in shell,
    "center Auto focus": "SHORTS_AUTO" in shell,
    "existing BACK": "public boolean handleBack()" in shell,
    "media hold": "KEYCODE_MEDIA_FAST_FORWARD" in shell,
    "bar below video": "dp(7), Gravity.RIGHT | Gravity.BOTTOM" in stage,
}.items():
    if not ok:
        raise SystemExit(f"MT2-019 missing invariant: {name}")

print("MT2-019: real 9:16 right-edge crop, below-video progress, three previews,"
      " center Auto/Like/Dislike/Save, BACK unchanged, genuine update identity.")
