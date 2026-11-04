#!/usr/bin/env python3
"""MT2-011: reproducible TV BACK, scoped Shorts and reachable mini-player.

Apply AFTER patch_010_player_settings_ui.py to the pinned, disposable checkout.
All edits fail closed on missing/ambiguous source anchors.
"""
from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit("Usage: patch_011_firetv_navigation.py <pinned-checkout>")
root = Path(sys.argv[1]).resolve()
mobile = root / "smarttubetv/src/stmobile"
tv = mobile / "java/com/newtube/mobile/ui"
if not (root / "sabr-media3").is_dir():
    raise SystemExit("MT2-011: expected SABR Media3 checkout")

def once(path, old, new):
    if not path.is_file():
        raise SystemExit("MT2-011 missing: " + str(path))
    data = path.read_text(encoding="utf-8")
    count = data.count(old)
    if count != 1:
        raise SystemExit("MT2-011: ambiguous anchor: " + str(path) +
                         ": " + str(count) + ": " + repr(old[:90]))
    path.write_text(data.replace(old, new, 1), encoding="utf-8")

gradle = root / "smarttubetv/build.gradle"
manifest = mobile / "AndroidManifest.xml"
once(gradle, 'applicationId "de.equiummc.marstube2.firetest010"',
     'applicationId "de.equiummc.marstube2.firetest011"')
once(gradle, 'versionCode 12010', 'versionCode 12011')
once(gradle, 'versionName "0.0.10"', 'versionName "0.0.11"')
once(manifest, 'android:label="MarsTube 0.0.10"',
     'android:label="MarsTube 0.0.11"')

# 010 copies these from this branch after 009; 011 adds Shorts feed handoff.
shorts = Path(__file__).resolve().parent / "MarsShortsSession.java"
dst = tv / "common/MarsShortsSession.java"
if not shorts.is_file():
    raise SystemExit("MT2-011: MarsShortsSession.java missing")
dst.write_text(shorts.read_text(encoding="utf-8"), encoding="utf-8")

playback = tv / "playback/MobilePlaybackActivity.java"
once(playback,
'''                    @Override public void previous() {
                        if (mPrevButton != null) mPrevButton.performClick();
                    }
                    @Override public void next() {
                        if (mNextButton != null) mNextButton.performClick();
                    }''',
'''                    @Override public void previous() {
                        if (!marsTvStepShort(-1) && mPrevButton != null)
                            mPrevButton.performClick();
                    }
                    @Override public void next() {
                        if (!marsTvStepShort(1) && mNextButton != null)
                            mNextButton.performClick();
                    }
                    @Override public boolean isShortsPlayback() {
                        return com.newtube.mobile.ui.common.MarsShortsSession
                                .isActiveFor(getVideo());
                    }
                    @Override public void minimize() {
                        // Share exactly the same Media3 player/session with the TV mini host.
                        minimizeByDrag();
                    }''')

once(playback,
'''    private void onRelatedClicked(Video video) {
        onRelatedClicked(video, null);
    }''',
'''    /**
     * Shorts can only advance along the same dedicated Shorts browse feed.
     * Never delegate Shorts UP/DOWN to the generic video recommendations.
     */
    private boolean marsTvStepShort(int direction) {
        com.newtube.mobile.ui.common.MarsShortsSession session = null; // namespace only
        if (!com.newtube.mobile.ui.common.MarsShortsSession.isActiveFor(getVideo())) {
            return false;
        }
        Video target = com.newtube.mobile.ui.common.MarsShortsSession.step(
                getVideo(), direction);
        if (target == null) {
            android.widget.Toast.makeText(this, "Kein weiterer Short in dieser Liste",
                    android.widget.Toast.LENGTH_SHORT).show();
        } else {
            onRelatedClicked(target);
        }
        return true;
    }

    private void onRelatedClicked(Video video) {
        onRelatedClicked(video, null);
    }''')

once(playback,
'''    private void onRelatedClicked(Video video, @Nullable ImageView thumbnail) {
        seedTappedStill(video, thumbnail);''',
'''    private void onRelatedClicked(Video video, @Nullable ImageView thumbnail) {
        // A regular suggestion must never keep a stale Shorts queue active.
        if (!com.newtube.mobile.ui.common.MarsShortsSession.contains(video)) {
            com.newtube.mobile.ui.common.MarsShortsSession.clear();
        }
        seedTappedStill(video, thumbnail);''')

browse = tv / "browse/MobileBrowseActivity.java"
once(browse,
'''        mPresenter.onVideoItemSelected(video);

        // Cards without a videoId''',
'''        // Explicit TV Shorts feed handoff: index and identity, not frame aspect ratio.
        if (com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(this)
                && mCurrentSectionId == MediaGroup.TYPE_SHORTS && video.isShorts) {
            com.newtube.mobile.ui.common.MarsShortsSession.begin(mCurrentVideos, video);
        } else {
            com.newtube.mobile.ui.common.MarsShortsSession.clear();
        }
        mPresenter.onVideoItemSelected(video);

        // Cards without a videoId''')

# Refresh the dynamic running-video entry whenever the player docks/undocks.
once(browse,
'''        mMiniPlayerBar.setVisibility(View.VISIBLE);
        updateMiniPlayPauseIcon(player);''',
'''        mMiniPlayerBar.setVisibility(View.VISIBLE);
        marsTvUpdateMiniDock();
        updateMiniPlayPauseIcon(player);''')
once(browse,
'''        mMiniPlayerBar.setVisibility(View.GONE);
        mMiniPlayerBar.setAlpha(1f);''',
'''        mMiniPlayerBar.setVisibility(View.GONE);
        marsTvUpdateMiniDock();
        mMiniPlayerBar.setAlpha(1f);''')

once(browse,
'''    private void syncMiniPlayer() {
        if (com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(this)''',
'''    /**
     * Keep the mini-card clear of real video tiles and refresh the rail entry.
     * Restore the original grid padding when disabled or no session exists.
     */
    private int marsTvGridBaseRight = -1;

    private void marsTvUpdateMiniDock() {
        if (!com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(this)) return;
        boolean active = mMiniPlayerBar != null
                && mMiniPlayerBar.getVisibility() == View.VISIBLE
                && MiniPlayerBridge.isActive()
                && com.newtube.mobile.player.MarsTvPlayerPrefs.isMiniPlayerEnabled(this);
        if (mContentGrid != null) {
            if (marsTvGridBaseRight < 0) {
                marsTvGridBaseRight = mContentGrid.getPaddingRight();
            }
            int reserve = active ? Math.round(208f * getResources().getDisplayMetrics().density) : 0;
            int right = marsTvGridBaseRight + reserve;
            if (mContentGrid.getPaddingRight() != right) {
                mContentGrid.setPadding(mContentGrid.getPaddingLeft(),
                        mContentGrid.getPaddingTop(), right, mContentGrid.getPaddingBottom());
                mContentGrid.setClipToPadding(true);
            }
        }
        com.newtube.mobile.ui.common.MarsTvRail.updateMini(this);
    }

    private void syncMiniPlayer() {
        if (com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(this)''')

# Re-sync mini navigation even after returning from another TV section.
once(browse,
'''    private boolean marsTvMiniPlayerKey(KeyEvent event) {
        if (!com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(this)''',
'''    private boolean marsTvMiniPlayerKey(KeyEvent event) {
        if (com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(this)) {
            com.newtube.mobile.ui.common.MarsTvRail.updateMini(this);
        }
        if (!com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(this)''')
print("MT2-011 applied: 0.0.11 isolated package, scoped Shorts queue, BACK and mini navigation.")
