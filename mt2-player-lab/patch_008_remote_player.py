#!/usr/bin/env python3
"""MT2-008: TV Back, focusable in-app mini-player, launcher version and player actions.

Must run after patch_007_tv_quality.py in the disposable pinned reference only.
No existing MarsTube app is modified or removed. Every replacement fails closed.
"""
from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit("Usage: patch_008_remote_player.py <pinned NewTube checkout>")
root = Path(sys.argv[1]).resolve()
mobile = root / "smarttubetv/src/stmobile"
ui = mobile / "java/com/newtube/mobile/ui"
if not (root/"sabr-media3").is_dir():
    raise SystemExit("Expected pinned, temporary Media3 reference checkout")

def one(path: Path, old: str, new: str):
    if not path.is_file():
        raise SystemExit("Missing expected file: " + str(path))
    s = path.read_text(encoding="utf-8")
    if s.count(old) != 1:
        raise SystemExit(f"Unsafe MT2-008 anchor {path.name}: {s.count(old)}: {old[:90]}")
    path.write_text(s.replace(old, new, 1), encoding="utf-8")

gradle = root/"smarttubetv/build.gradle"
manifest = mobile/"AndroidManifest.xml"
one(gradle, 'applicationId "de.equiummc.marstube2.firetest007"',
            'applicationId "de.equiummc.marstube2.firetest008"')
one(gradle, 'versionCode 12007', 'versionCode 12008')
one(gradle, 'versionName "0.0.7"', 'versionName "0.0.8"')
one(manifest, 'android:label="MarsTube TV-Test 007"',
    'android:label="MarsTube 0.0.8"')
# Explicit launcher Activity label, rather than relying on inherited main flavor label.
one(manifest,
    'android:icon="@drawable/mars_m_logo"\n            android:launchMode="singleTop"',
    'android:icon="@drawable/mars_m_logo"\n            android:label="MarsTube 0.0.8"\n            android:launchMode="singleTop"')

# The TV's BACK should dock the player directly. The reference phone handler
# checks for portrait and toggles fullscreen when in landscape; a TV always
# is landscape, so its "minimize by back" path could never run.
player = ui/"playback/MobilePlaybackActivity.java"
one(player, '    private void handleBack() {\n',
    '''    // TV remote BACK: return to the invoking Browse/Search/Channel activity,
    // retaining the same playback session in the existing in-app mini-player.
    // The phone still uses the original predictive-back gesture implementation.
    private void marsTvBackToBrowse() {
        if (mClosing || mIsInPip || mPipEnterPending) return;
        if (mPlayer != null) {
            minimizeByDrag();
        } else {
            getViewManager().startView(BrowseView.class);
        }
    }

    private void handleBack() {
''')
one(player, '''                    @Override public void fullscreen() {
                        if (mMarsTvShell != null) mMarsTvShell.hideUi();
                    }''',
    '''                    @Override public void fullscreen() {
                        if (mMarsTvShell != null) mMarsTvShell.hideUi();
                    }
                    @Override public void like() {
                        if (mWatchLike != null) mWatchLike.performClick();
                    }
                    @Override public void dislike() {
                        if (mWatchDislike != null) mWatchDislike.performClick();
                    }
                    @Override public void quality() {
                        showQualitySheet();
                    }
                    @Override public void backToBrowse() {
                        marsTvBackToBrowse();
                    }''')

# Intercept mini-card keys BEFORE the old Browse keyboard grid bridge,
# otherwise its D-pad handler consumes ENTER as "open the selected video card".
browse = ui/"browse/MobileBrowseActivity.java"
one(browse, '''        // The visible left rail replaces the old invisible bottom-navigation key map.''',
    '''        if (com.newtube.mobile.ui.common.MarsTvRail.handleMiniKey(this, event)) return true;
        // The visible left rail replaces the old invisible bottom-navigation key map.''')
one(browse, '''        mMiniPlayerBar.setVisibility(View.VISIBLE);
        updateMiniPlayPauseIcon(player);''',
    '''        mMiniPlayerBar.setVisibility(View.VISIBLE);
        if (com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(this)) {
            mMiniPlayerBar.post(() ->
                    com.newtube.mobile.ui.common.MarsTvRail.focusMiniPlayer(this));
        }
        updateMiniPlayPauseIcon(player);''')

# Search/Channel also use MobileMiniPlayerController. Every TV mini-host
# receives focus on the card as it becomes visible.
controller = ui/"playback/MobileMiniPlayerController.java"
one(controller, '''        mBar.setVisibility(View.VISIBLE);
        updatePlayPause(player);''',
    '''        mBar.setVisibility(View.VISIBLE);
        if (com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(mActivity)) {
            mBar.post(() ->
                    com.newtube.mobile.ui.common.MarsTvRail.focusMiniPlayer(mActivity));
        }
        updatePlayPause(player);''')

# Search, settings, channels go through MobileActivity.dispatchKeyEvent.
# Browse above has a subclass bridge, hence its separate early mini check.
activity = ui/"common/MobileActivity.java"
one(activity,
    '''        if (MarsTvRail.handleKey(this, event)) return true;''',
    '''        if (MarsTvRail.handleMiniKey(this, event)) return true;
        if (MarsTvRail.handleKey(this, event)) return true;''')

print("MT2-008 applied: launcher 0.0.8; TV BACK docks mini; OK on mini expands; ratings/quality accessible.")
