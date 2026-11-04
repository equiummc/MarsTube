#!/usr/bin/env python3
"""MT2-008: TV usability, minimize/expand, condensed controls, rating and format.

Applied after MT2-007 in the pinned ephemeral reference checkout. Never changes
the installed 0.0.5/6/7 binaries or production MarsTube 1.x package.
"""
from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit("Usage: patch_008_tv_player.py <pinned-test-checkout>")
root = Path(sys.argv[1]).resolve()
m = root / "smarttubetv/src/stmobile"
tv = m / "java/com/newtube/mobile/ui"
if not (root/"sabr-media3").is_dir():
    raise SystemExit("Expected pinned disposable Media3 checkout")

def replace_once(path, before, after):
    if not path.is_file():
        raise SystemExit("Missing " + str(path))
    s = path.read_text(encoding="utf-8")
    n = s.count(before)
    if n != 1:
        raise SystemExit(f"MT2-008 fail-closed {path.name}: {n} matches: {before[:80]!r}")
    path.write_text(s.replace(before, after, 1), encoding="utf-8")

gradle = root/"smarttubetv/build.gradle"
manifest = m/"AndroidManifest.xml"
replace_once(gradle, 'applicationId "de.equiummc.marstube2.firetest007"',
             'applicationId "de.equiummc.marstube2.firetest008"')
replace_once(gradle, 'versionCode 12007', 'versionCode 12008')
replace_once(gradle, 'versionName "0.0.7"', 'versionName "0.0.8"')
replace_once(manifest, 'android:label="MarsTube TV-Test 007"',
             'android:label="MarsTube 0.0.8"')
# 0.0.8 player shell source is now a slimmer panel with real rating actions.
shell = Path(__file__).resolve().parent / "MarsTvShell.java"
dest = tv/"playback/MarsTvShell.java"
if not shell.is_file() or not dest.is_file():
    raise SystemExit("TV shell missing")
dest.write_text(shell.read_text(encoding="utf-8"), encoding="utf-8")

# The callbacks invoke the EXISTING rating, sign-in feedback, and quality code.
# They do not bypass account checks or change server calls.
player = tv/"playback/MobilePlaybackActivity.java"
replace_once(player,
'''                    @Override public void fullscreen() {
                        if (mMarsTvShell != null) mMarsTvShell.hideUi();
                    }
                });''',
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
                });''')
# Android TV is ALWAYS landscape. The inherited phone handler's first BACK
# just toggled orientation. Instead dock to the real in-app mini-host.
replace_once(player,
'''    private void handleBack() {
        if (mClosing) {
            return;
        }''',
'''    private void handleBack() {
        if (mClosing) {
            return;
        }
        if (mMarsTvShell != null
                && com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(this)
                && !mIsInPip && !mPipEnterPending) {
            // Return to the TV browse page with the *same* playing Media3 session.
            // No portrait-only morph or artificial orientation change.
            minimizeByDrag();
            return;
        }''')

# Let the TV remote select the currently playing mini tile. In v0.0.7 the
# global D-pad bridge swallowed UP/OK even while a mini card was focused.
browse = tv/"browse/MobileBrowseActivity.java"
replace_once(browse,
'''        if (com.newtube.mobile.ui.common.MarsTvRail.handleKey(this, event)) return true;''',
'''        if (marsTvMiniPlayerKey(event)) return true;
        if (com.newtube.mobile.ui.common.MarsTvRail.handleKey(this, event)) return true;''')
replace_once(browse,
'''    public void marsTvRoute(int destination) {''',
'''    // TV mini player: D-pad UP from the first row, then OK to expand.
    // Also reachable via the persistent "Miniplayer" sidebar item.
    private boolean marsTvMiniPlayerKey(KeyEvent event) {
        if (!com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(this)
                || mMiniPlayerBar == null
                || mMiniPlayerBar.getVisibility() != View.VISIBLE
                || !MiniPlayerBridge.isActive()) return false;
        int key = event.getKeyCode();
        if (mMiniPlayerBar.hasFocus()) {
            boolean handled = key == KeyEvent.KEYCODE_DPAD_CENTER
                    || key == KeyEvent.KEYCODE_ENTER
                    || key == KeyEvent.KEYCODE_NUMPAD_ENTER
                    || key == KeyEvent.KEYCODE_DPAD_LEFT
                    || key == KeyEvent.KEYCODE_DPAD_DOWN
                    || key == KeyEvent.KEYCODE_BACK;
            if (!handled) return false;
            if (event.getAction() != KeyEvent.ACTION_DOWN) return true;
            if (key == KeyEvent.KEYCODE_DPAD_CENTER
                    || key == KeyEvent.KEYCODE_ENTER
                    || key == KeyEvent.KEYCODE_NUMPAD_ENTER) {
                mMiniPlayerBar.performClick();
            } else if (key == KeyEvent.KEYCODE_DPAD_LEFT) {
                com.newtube.mobile.ui.common.MarsTvRail.focus(this);
            } else {
                marsTvFocusCard(mMarsTvSelection);
            }
            return true;
        }
        // The first-row UP arrow makes a visible, focusable jump to mini.
        if (key == KeyEvent.KEYCODE_DPAD_UP && !mYouShowing
                && mMarsTvSelection < (mLayoutManager == null ? 1
                        : Math.max(1, mLayoutManager.getSpanCount()))) {
            if (event.getAction() == KeyEvent.ACTION_DOWN)
                mMiniPlayerBar.requestFocus();
            return true;
        }
        return false;
    }

    public void marsTvRoute(int destination) {''')
replace_once(browse,
'''        mMiniPlayerBar = findViewById(R.id.mobile_mini_player);''',
'''        mMiniPlayerBar = findViewById(R.id.mobile_mini_player);
        if (com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(this)) {
            mMiniPlayerBar.setFocusable(true);
            mMiniPlayerBar.setFocusableInTouchMode(false);
            mMiniPlayerBar.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus) {
                    v.setForeground(null);
                    return;
                }
                android.graphics.drawable.GradientDrawable indicator =
                        new android.graphics.drawable.GradientDrawable();
                indicator.setColor(android.graphics.Color.TRANSPARENT);
                indicator.setCornerRadius(10f * getResources().getDisplayMetrics().density);
                indicator.setStroke(Math.max(2, (int)(3 * getResources().getDisplayMetrics().density)),
                        0xFF0B9BFF);
                v.setForeground(indicator);
            });
        }''')
# All pages use the existing true M branding and version on the TV rail.
rail = Path(__file__).resolve().parent/"MarsTvRail.java"
rail_dest = tv/"common/MarsTvRail.java"
if not rail.is_file() or not rail_dest.is_file():
    raise SystemExit("TV rail missing")
rail_dest.write_text(rail.read_text(encoding="utf-8"), encoding="utf-8")
print("MT2-008: separate app, true M and version, TV BACK -> mini, remote mini expand, ratings, quality HUD.")
