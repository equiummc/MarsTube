#!/usr/bin/env python3
"""MT2-005: attach a separate, lightweight TV shell to pinned NewTube stmobile.

Apply AFTER patch_tv_input.py, patch_sabr_recovery.py, patch_lab_identity.py.
The entire operation modifies only the disposable NewTube CI checkout.
Never touch the production MarsTube 1.x modules.
"""
from pathlib import Path
import base64
import sys

if len(sys.argv) != 2:
    raise SystemExit("usage: patch_tv_shell.py <pinned-newtube-root>")
root = Path(sys.argv[1]).resolve()
repo = Path(__file__).resolve().parents[1]
mobile = root / "smarttubetv/src/stmobile"
player = mobile / "java/com/newtube/mobile/ui/playback/MobilePlaybackActivity.java"
gradle = root / "smarttubetv/build.gradle"
manifest = mobile / "AndroidManifest.xml"
if not all([player.is_file(), gradle.is_file(), manifest.is_file(),
            (root / "sabr-media3").is_dir()]):
    raise SystemExit("Expected pinned NewTube checkout not found")

def replace_once(path, old, new):
    s = path.read_text(encoding="utf-8")
    if s.count(old) != 1:
        raise SystemExit(f"UNSAFE MT2-005 anchor {path.name}: occurrences={s.count(old)}: {old[:70]!r}")
    path.write_text(s.replace(old, new, 1), encoding="utf-8")

# New package deliberately DIFFERENT from MT2-003R and stable 1.x.
# Debug variant applies suffix .debug => de.equiummc.marstube2.tvui.debug
replace_once(gradle, 'applicationId "de.equiummc.marstube2.lab"',
             'applicationId "de.equiummc.marstube2.tvui"')
replace_once(manifest, 'android:label="MarsTube 2.0 Recovery Lab"',
             'android:label="MarsTube"')

# Android's own installer and the application list use the merged manifest icon,
# not our in-app rail image. Override upstream NewTube's @mipmap/app_icon
# exclusively in the stmobile flavor. The original TV fork is unchanged.
replace_once(manifest,
    '        android:theme="@style/Theme.NewTube"\\n'
    '        tools:replace="android:name,android:theme">',
    '        android:theme="@style/Theme.NewTube"\\n'
    '        android:icon="@drawable/mars_m_logo"\\n'
    '        tools:replace="android:name,android:theme,android:icon">')


# Remove old upstream wordmark from the real Home UI and system title.
strings = root / "smarttubetv/src/main/res/values/strings.xml"
replace_once(strings, '<![CDATA[NewTube]]>', '<![CDATA[MarsTube]]>')

java = repo / "mt2-player-lab/MarsTvShell.java"
dest = mobile / "java/com/newtube/mobile/ui/playback/MarsTvShell.java"
dest.write_text(java.read_text(encoding="utf-8"), encoding="utf-8")

asset = repo / "mt2-player-lab/assets/mars_m_logo.b64"
png = mobile / "res/drawable-nodpi/mars_m_logo.png"
png.parent.mkdir(parents=True, exist_ok=True)
png.write_bytes(base64.b64decode(asset.read_text(encoding="ascii").strip()))
if not png.read_bytes().startswith(b"\x89PNG\r\n\x1a\n"):
    raise SystemExit("Logo resource not a PNG")

replace_once(player,
    '    private Media3PlayerController mExoPlayerController;',
    '    private Media3PlayerController mExoPlayerController;\n'
    '    @androidx.annotation.Nullable private MarsTvShell mMarsTvShell;')

init = r"""
        // MT2-005: only in this isolated test APK. Existing Media3 player and surface stay intact.
        mMarsTvShell = new MarsTvShell(this, mVideoArea, () -> mPlayer,
                new MarsTvShell.Actions() {
                    @Override public void openVideo(Video video) {
                        onRelatedClicked(video);
                    }
                    @Override public void navigate(int destination) {
                        android.content.Intent intent = new android.content.Intent(
                                MobilePlaybackActivity.this,
                                com.newtube.mobile.ui.browse.MobileBrowseActivity.class);
                        intent.putExtra("mars_tv_section", destination);
                        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                                | android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                    }
                    @Override public void previous() {
                        if (mPrevButton != null) mPrevButton.performClick();
                    }
                    @Override public void next() {
                        if (mNextButton != null) mNextButton.performClick();
                    }
                    @Override public void toggle() {
                        togglePlayPause();
                    }
                    @Override public void captions() {
                        if (mSubtitlesButton != null) mSubtitlesButton.performClick();
                    }
                    @Override public void fullscreen() {
                        if (mMarsTvShell != null) mMarsTvShell.hideUi();
                    }
                });
        // The phone chrome is not the control surface on TV. It must not consume remote focus.
        mControlsRoot.setVisibility(View.GONE);
        mControlsRoot.setFocusable(false);
        mTimeBar.setVisibility(View.GONE);
"""
replace_once(player,
             '        createPlayerObjects();\n\n        registerPipReceiver();',
             '        createPlayerObjects();\n' + init + '\n        registerPipReceiver();')
# MT2-002 patch has already installed this dispatch hook; TV shell runs FIRST.
replace_once(player,
             '        if (marsTvPlaybackKey(event)) return true;',
             '        if (mMarsTvShell != null && mMarsTvShell.handle(event)) return true;\n'
             '        if (marsTvPlaybackKey(event)) return true;')
replace_once(player,
             '        mControlsRoot.setVisibility(View.VISIBLE);\n        mControlsRoot.animate().cancel();',
             '        mControlsRoot.setVisibility(mMarsTvShell == null ? View.VISIBLE : View.GONE);\n'
             '        mControlsRoot.animate().cancel();')
replace_once(player,
             '        mTimeBar.setShown(true, animate);',
             '        mTimeBar.setShown(true, animate);\n'
             '        if (mMarsTvShell != null) mTimeBar.setVisibility(View.GONE);')
replace_once(player,
             'if (!mIsInPip && mTimeBar != null && mTimeBar.getVisibility() != View.VISIBLE)',
             'if (!mIsInPip && mMarsTvShell == null && mTimeBar != null && mTimeBar.getVisibility() != View.VISIBLE)')
replace_once(player,
             '        mIsInPip = isInPictureInPictureMode;',
             '        mIsInPip = isInPictureInPictureMode;\n'
             '        if (mMarsTvShell != null) mMarsTvShell.setPictureInPicture(isInPictureInPictureMode);')
replace_once(player,
             '    protected void onDestroy() {\n',
             '    protected void onDestroy() {\n'
             '        if (mMarsTvShell != null) { mMarsTvShell.release(); mMarsTvShell = null; }\n')
replace_once(player,
             '        submitRelatedWindow(() -> {\n',
             '        if (mMarsTvShell != null) mMarsTvShell.setRecommendations(mRelatedVideos);\n'
             '        submitRelatedWindow(() -> {\n')

# The browse screen still uses its real data/presenter. Route the selected rail
# destination to its EXISTING Home/Subscription/History/Search/Settings actions.
browse = mobile / "java/com/newtube/mobile/ui/browse/MobileBrowseActivity.java"
browse_code = r"""
    // MT2-005: one-shot routing from the TV icon rail (no hard-coded metadata/feed).
    private void marsTvHandleDestination(android.content.Intent intent) {
        if (intent == null || !intent.hasExtra("mars_tv_section")) return;
        int destination = intent.getIntExtra("mars_tv_section", 0);
        intent.removeExtra("mars_tv_section");
        if (destination == 1) {
            SearchPresenter.instance(this).startSearch(null);
        } else if (destination == 5) {
            openSettings();
        } else {
            int section = destination == 2 ? MediaGroup.TYPE_SUBSCRIPTIONS
                    : destination == 3 ? MediaGroup.TYPE_HISTORY
                    : destination == 4 ? VideoDownloads.SECTION_ID
                    : MediaGroup.TYPE_HOME;
            mBottomNav.post(() -> {
                int itemId = toMenuItemId(section);
                if (mBottomNav.getMenu().findItem(itemId) != null) {
                    mBottomNav.setSelectedItemId(itemId);
                } else {
                    hideYouPanel();
                    onSectionChosen(section);
                }
            });
        }
    }

"""
# Insert where setupBottomNav begins; openSettings and toMenuItemId are in same class.
replace_once(browse,
             '    private void setupBottomNav() {',
             browse_code + '\n    private void setupBottomNav() {')
# Setup page then consume the destination extra after the navigation is created.
replace_once(browse,
             '        setupBottomNav();',
             '        setupBottomNav();\n'
             '        mBottomNav.post(() -> marsTvHandleDestination(getIntent()));')

# An existing onNewIntent() handles download notifications: extend it, never duplicate it.
replace_once(browse,
             '        handleOpenDownloads(intent);\n    }\n\n    @Override\n    protected void onDestroy()',
             '        handleOpenDownloads(intent);\n'
             '        marsTvHandleDestination(intent);\n    }\n\n    @Override\n    protected void onDestroy()')

print("MT2-005 TV UI integrated; six-card rail, true M logo, independent package.")
