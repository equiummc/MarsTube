#!/usr/bin/env python3
"""MT2-023: one TV settings tree, AndroidX Back, TV comment prefetch guard.

Apply after MT2-022 to pinned NewTube checkout. Preserve permanent app ID,
stable update routes, Media3/SABR and the working playback BACK logic.
"""
from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit("Usage: patch_023_tv_settings.py <pinned-newtube-root>")
root = Path(sys.argv[1]).resolve()
if not (root / "sabr-media3").is_dir():
    raise SystemExit("MT2-023: pinned Media3 checkout missing")
tv = root / "smarttubetv/src/stmobile/java/com/newtube/mobile/ui"
here = Path(__file__).resolve().parent

def once(path, old, new):
    if not path.is_file():
        raise SystemExit(f"MT2-023 missing: {path}")
    code = path.read_text(encoding="utf-8")
    count = code.count(old)
    if count != 1:
        raise SystemExit(f"MT2-023 unsafe anchor in {path.name}: {count}, {old[:100]!r}")
    path.write_text(code.replace(old, new, 1), encoding="utf-8")

for name, destination in (
        ("MarsTvSettingsPages.java", tv / "settings/MarsTvSettingsPages.java"),
        ("MarsTvRail.java", tv / "common/MarsTvRail.java")):
    source = here / name
    if not source.is_file() or not destination.is_file():
        raise SystemExit(f"MT2-023 TV overlay missing: {name}")
    new_code = source.read_text(encoding="utf-8")
    if "MarsTube" not in new_code:
        raise SystemExit("MT2-023 TV overlay identity missing")
    destination.write_text(new_code, encoding="utf-8")

settings = tv / "settings/MobileSettingsActivity.java"
once(settings,
    "    private void handleBack() {",
    """    /** Re-select Settings in the side rail without opening a second Activity. */
    public void marsTvGoToSettingsRoot() {
        if (!getSupportFragmentManager().isStateSaved()) {
            getSupportFragmentManager().popBackStackImmediate(null,
                    androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
    }

    private void handleBack() {""")

activity = tv / "common/MobileActivity.java"
once(activity,
    """                @Override public void handleOnBackPressed() {
                    MarsTvRail.focus(MobileActivity.this);
                }""",
    """                @Override public void handleOnBackPressed() {
                    if (MobileActivity.this instanceof com.newtube.mobile.ui.settings.MobileSettingsActivity) {
                        // Never trap a Settings fragment in the rail: delegate to
                        // the registered Settings BackStack callback.
                        mMarsTvUniversalBack.setEnabled(false);
                        try {
                            getOnBackPressedDispatcher().onBackPressed();
                        } finally {
                            mMarsTvUniversalBack.setEnabled(true);
                        }
                        return;
                    }
                    MarsTvRail.focus(MobileActivity.this);
                }""")

player = tv / "playback/MobilePlaybackActivity.java"
once(player,
    """    private void prefetchComments() {
        if (mCommentsPanel == null || mIsInPip || mIsStopped || isFinishing()) {""",
    """    private void prefetchComments() {
        // TV: opening a video must not fetch a full comments page invisibly.
        if (com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(this)) return;
        if (mCommentsPanel == null || mIsInPip || mIsStopped || isFinishing()) {""")
once(player,
    """                if (mCommentsKey != null) {
                    Utils.postDelayed(mPrefetchComments, COMMENTS_PREFETCH_DELAY_MS);
                }""",
    """                if (mCommentsKey != null
                        && !com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(this)) {
                    Utils.postDelayed(mPrefetchComments, COMMENTS_PREFETCH_DELAY_MS);
                }""")

rail_text = (tv / "common/MarsTvRail.java").read_text(encoding="utf-8")
pages_text = (tv / "settings/MarsTvSettingsPages.java").read_text(encoding="utf-8")
assert "if (code == KeyEvent.KEYCODE_BACK && rail.expandedSettings) return false;" in rail_text
assert "marsTvGoToSettingsRoot()" in rail_text
assert "private static SettingsPages.Page advanced(Context context)" in pages_text
assert "Bei Ladeproblemen Qualität reduzieren" in pages_text
assert "Auf Updates prüfen" in pages_text
assert 'versionLine(context)' in pages_text
assert "SABR bevorzugen (experimentell)" in pages_text
assert "mMarsTvUniversalBack.setEnabled(false)" in activity.read_text(encoding="utf-8")
assert "mCommentsPanel.prefetch();" in player.read_text(encoding="utf-8")
assert 'applicationId "de.equiummc.marstube2"' in (
    root / "smarttubetv/build.gradle").read_text(encoding="utf-8")
assert "highestCode" in (
    root / "SharedModules/appupdatechecker2/src/main/java/"
    "com/liskovsoft/appupdatechecker2/core/AppVersionChecker.java").read_text(encoding="utf-8")
print("MT2-023: TV Settings back stack, focus, one-setting-one-place, comments lazy on TV.")
