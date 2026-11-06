#!/usr/bin/env python3
"""MT2-017: integrated TV Shorts stage, not a second player or a separate app.

Apply AFTER patch_016_production.py to the pinned disposable checkout.
The production app ID and signing chain must remain unchanged.
"""
from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit("Usage: patch_017_shorts_stage.py <pinned-checkout>")
root = Path(sys.argv[1]).resolve()
src = Path(__file__).resolve().parent
tv = root / "smarttubetv/src/stmobile/java/com/newtube/mobile/ui"
if not (root / "sabr-media3").is_dir():
    raise SystemExit("MT2-017: no pinned Media3/SABR reference found")


def once(path, old, new):
    if not path.is_file():
        raise SystemExit("MT2-017 missing file: " + str(path))
    data = path.read_text(encoding="utf-8")
    count = data.count(old)
    if count != 1:
        raise SystemExit(f"MT2-017 unsafe anchor count {count}: {path}: {old[:75]!r}")
    path.write_text(data.replace(old, new, 1), encoding="utf-8")


stage = src / "MarsTvShortsStage.java"
if not stage.is_file():
    raise SystemExit("MT2-017: missing versioned Shorts stage")
(tv / "playback/MarsTvShortsStage.java").write_text(stage.read_text(encoding="utf-8"), encoding="utf-8")

# The 010/013/014 build steps copy these versioned sources. Never modify
# untracked external app sources instead of our own reproducible overlays.
shell = (tv / "playback/MarsTvShell.java").read_text(encoding="utf-8")
session = (tv / "common/MarsShortsSession.java").read_text(encoding="utf-8")
theme = (tv / "common/MarsTvNavigationTheme.java").read_text(encoding="utf-8")
if not all([
    "new MarsTvShortsStage(activity, root" in shell,
    "stageSeekHold(key)" in shell,
    "MarsShortsSession.hasNext()" in shell,
    "List<Video> upcoming(int limit)" in session,
    "mars_tv_ic_nav_home" in theme
]):
    raise SystemExit("MT2-017: one of the versioned UI sources was not copied")

# New icons are brought into res/drawable by patch_010's mars_tv_ic_*.xml glob.
drawable = root / "smarttubetv/src/stmobile/res/drawable"
for name in ("home", "shorts", "search", "subscriptions", "history", "settings", "profile", "autoplay"):
    if not (drawable / ("mars_tv_ic_nav_" + name + ".xml")).is_file():
        raise SystemExit("MT2-017: missing modern navigation icon " + name)

playback = tv / "playback/MobilePlaybackActivity.java"
once(playback,
'''                    @Override public boolean isShortsPlayback() {
                        return com.newtube.mobile.ui.common.MarsShortsSession
                                .isActiveFor(getVideo());
                    }
                    @Override public void minimize()''',
'''                    @Override public boolean isShortsPlayback() {
                        return com.newtube.mobile.ui.common.MarsShortsSession
                                .isActiveFor(getVideo());
                    }
                    @Override public void positionShortsVideo(boolean enabled) {
                        // The *same* Media3 PlayerView is shifted into the 9:16 right column.
                        // No second decoder, no new source/seek/pause. Immediately reversible.
                        if (mPlayerView != null) {
                            mPlayerView.animate().cancel();
                            mPlayerView.setTranslationX(enabled
                                    ? getResources().getDisplayMetrics().widthPixels * 0.27f
                                    : 0f);
                        }
                    }
                    @Override public void minimize()''')

# Avoid a spinner flash on a normal sub-5-second Short load. Still report a
# genuinely stalled video after five seconds, never hide an infinite buffer.
once(playback,
'''    /** NEWTUBE(motion): the buffering spinner's state, see showProgressBar. */
    private boolean mSpinnerShown;

    @Override
    public void showProgressBar(boolean show) {''',
'''    /** NEWTUBE(motion): the buffering spinner's state, see showProgressBar. */
    private boolean mSpinnerShown;
    private final android.os.Handler mMarsShortsSpinnerHandler =
            new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable mMarsShortsSpinnerPending;
    private boolean mMarsShortsSpinnerGraceExpired;

    @Override
    public void showProgressBar(boolean show) {
        if (!show) {
            if (mMarsShortsSpinnerPending != null) {
                mMarsShortsSpinnerHandler.removeCallbacks(mMarsShortsSpinnerPending);
                mMarsShortsSpinnerPending = null;
            }
            mMarsShortsSpinnerGraceExpired = false;
        } else if (com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(this)
                && com.newtube.mobile.ui.common.MarsShortsSession.isActiveFor(getVideo())
                && !mMarsShortsSpinnerGraceExpired) {
            if (mMarsShortsSpinnerPending == null) {
                final Video expected = getVideo();
                mMarsShortsSpinnerPending = () -> {
                    mMarsShortsSpinnerPending = null;
                    Video current = getVideo();
                    if (expected != null && current != null
                            && expected.videoId != null
                            && !expected.videoId.equals(current.videoId)) return;
                    if (mPlayer == null || mPlayer.getPlaybackState()
                            != Player.STATE_BUFFERING) return;
                    mMarsShortsSpinnerGraceExpired = true;
                    showProgressBar(true);
                };
                mMarsShortsSpinnerHandler.postDelayed(mMarsShortsSpinnerPending, 5000L);
            }
            return;
        }''')

once(playback,
'''    private void onRelatedClicked(Video video, @Nullable ImageView thumbnail) {
        // A regular suggestion must never keep a stale Shorts queue active.''',
'''    private void onRelatedClicked(Video video, @Nullable ImageView thumbnail) {
        // Reset the 5-second loading grace for each newly opened Short.
        if (com.newtube.mobile.ui.common.MarsShortsSession.contains(video)) {
            showProgressBar(false);
        }
        // A regular suggestion must never keep a stale Shorts queue active.''')

# Opening the Shorts section immediately launches its first real Short,
# entering the new two-column stage. It is a one-shot, never a loop on refresh.
browse = tv / "browse/MobileBrowseActivity.java"
once(browse,
'''    private int mMarsTvExpectedFocusSection = -1;''',
'''    private int mMarsTvExpectedFocusSection = -1;
    private boolean mMarsTvAutoOpenShorts;''')
once(browse,
'''                    marsTvFocusCard(0);
                    mMarsTvExpectedFocusSection = -1;''',
'''                    marsTvFocusCard(0);
                    mMarsTvExpectedFocusSection = -1;
                    if (mMarsTvAutoOpenShorts
                            && mCurrentSectionId == MediaGroup.TYPE_SHORTS) {
                        mMarsTvAutoOpenShorts = false;
                        for (Video first : new ArrayList<>(mCurrentVideos)) {
                            if (first != null && first.videoId != null && first.hasVideo()) {
                                onVideoClicked(first);
                                break;
                            }
                        }
                    }''')
once(browse,
'''            mMarsTvExpectedFocusSection = destination == 8 ? MediaGroup.TYPE_SHORTS''',
'''            mMarsTvAutoOpenShorts = destination == 8;
            mMarsTvExpectedFocusSection = destination == 8 ? MediaGroup.TYPE_SHORTS''')

print("MT2-017: inline Shorts stage, 4 previews, Auto toggle, TV shift, hold seek, 5s spinner grace.")
