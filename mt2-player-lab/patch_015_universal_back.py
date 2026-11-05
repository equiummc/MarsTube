#!/usr/bin/env python3
"""MT2-015: TV BACK always selects the rail, even from settings/Shorts/miniplayer.

Run only AFTER patch_014_tv_shorts_mode.py on the pinned disposable checkout.
The permanent installed versions are never overwritten (isolated APK identity).
"""
from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit("Usage: patch_015_universal_back.py <pinned-checkout>")
root = Path(sys.argv[1]).resolve()
mobile = root / "smarttubetv/src/stmobile"
tv = mobile / "java/com/newtube/mobile/ui"
if not (root / "sabr-media3").is_dir():
    raise SystemExit("MT2-015 requires pinned Media3 reference")

def once(path, before, after):
    if not path.is_file():
        raise SystemExit(f"MT2-015 missing path: {path}")
    content = path.read_text(encoding="utf-8")
    n = content.count(before)
    if n != 1:
        raise SystemExit(f"MT2-015 unsafe anchor {path}: count={n}: {before[:90]!r}")
    path.write_text(content.replace(before, after, 1), encoding="utf-8")

gradle = root / "smarttubetv/build.gradle"
manifest = mobile / "AndroidManifest.xml"
once(gradle, 'applicationId "de.equiummc.marstube2.firetest014"',
             'applicationId "de.equiummc.marstube2.firetest015"')
once(gradle, 'versionCode 12014', 'versionCode 12015')
once(gradle, 'versionName "0.0.14"', 'versionName "0.0.15"')
once(manifest, 'android:label="MarsTube 0.0.14"',
               'android:label="MarsTube 0.0.15"')

# The AndroidX Back dispatcher can consume the hardware BACK without ever
# passing a KeyEvent to dispatchKeyEvent(). Install a TV-only callback AFTER
# activity/fragment onCreate callbacks so it is the most recent callback for
# Browse, Settings, Search and Channel. The player has its own shell callback
# and no MarsTvRail installed, so it remains unaffected.
activity = tv / "common/MobileActivity.java"
once(activity,
'''    /** NEWTUBE(theme): the layout {@link #recolourInPlace} inflates again. */''',
'''    // TV-only: AndroidX Back fallback even if dispatchKeyEvent receives no BACK.
    // Registered after page handlers in onCreate; keep exactly one per Activity.
    private androidx.activity.OnBackPressedCallback mMarsTvUniversalBack;

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if (MarsDeviceMode.isTelevision(this)
                && MarsTvRail.isInstalled(this)
                && mMarsTvUniversalBack == null) {
            mMarsTvUniversalBack = new androidx.activity.OnBackPressedCallback(true) {
                @Override public void handleOnBackPressed() {
                    MarsTvRail.focus(MobileActivity.this);
                }
            };
            getOnBackPressedDispatcher().addCallback(this, mMarsTvUniversalBack);
        }
    }

    /** NEWTUBE(theme): the layout {@link #recolourInPlace} inflates again. */''')

browse = tv / "browse/MobileBrowseActivity.java"
once(browse,
'''        if (key == KeyEvent.KEYCODE_BACK) {
            if (mCurrentSectionId != MediaGroup.TYPE_HOME) marsTvRoute(0);
            else com.newtube.mobile.ui.common.MarsTvRail.focus(this);
            return true;
        }''',
'''        if (key == KeyEvent.KEYCODE_BACK) {
            com.newtube.mobile.ui.common.MarsTvRail.focus(this);
            return true;
        }''')

# Earlier mini-player routing swallowed BACK before the rail got to see it.
once(browse,
'''                    || key == KeyEvent.KEYCODE_DPAD_DOWN
                    || key == KeyEvent.KEYCODE_BACK;''',
'''                    || key == KeyEvent.KEYCODE_DPAD_DOWN;''')

# SETTINGS is now fixed-width 132dp while other rails are 64dp.
# The icon sizes and selected-state colors are supplied by the shared theme.
rail = (tv / "common/MarsTvRail.java").read_text(encoding="utf-8")
shell = (tv / "playback/MarsTvShell.java").read_text(encoding="utf-8")
theme = (tv / "common/MarsTvNavigationTheme.java").read_text(encoding="utf-8")
checks = {
    "global BACK before focus test": "if (code == KeyEvent.KEYCODE_BACK || code == KeyEvent.KEYCODE_MENU)" in rail,
    "Settings focus right": "private void focusSettingsContent()" in rail,
    "Settings wide design": "MarsTvNavigationTheme.SETTINGS_WIDTH_DP" in rail,
    "shared wide width": "SETTINGS_WIDTH_DP = 132" in theme,
    "player BACK opens menu": "openRail();\n        render();\n        return true;" in shell,
    "player keeps existing session": "actions.minimize();" not in shell.split("public boolean handleBack()", 1)[1].split("public boolean handle(", 1)[0],
    "player narrow": 'version.setText("0.0.15")' in shell,
    "browse narrow": 'version.setText("0.0.15")' in rail,
}
for label, passed in checks.items():
    if not passed:
        raise SystemExit(f"MT2-015 invariant failed: {label}")

print("MT2-015 verified: one BACK everywhere targets rail; settings RIGHT targets real list.")
