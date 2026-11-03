#!/usr/bin/env python3
"""MT2-006: one MarsTube 2 application for touch phones and TV remotes.

Run after MT2-005 overlays, on the disposable pinned Media3 reference checkout.
Preserves its fast Media3 playback while choosing the 0.0.5 TV chrome ONLY
when Android advertises a real TV. Touch phones retain full mobile playback,
home, search, controls and mini-player (never a D-pad overlay).
"""
from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit("Usage: patch_006_unified.py <newtube-reference>")
root = Path(sys.argv[1]).resolve()
mobile = root / "smarttubetv/src/stmobile"
player = mobile / "java/com/newtube/mobile/ui/playback/MobilePlaybackActivity.java"
app = mobile / "java/com/newtube/mobile/MobileMainApplication.java"
settings = mobile / "java/com/newtube/mobile/ui/settings/SettingsPages.java"
manifest = mobile / "AndroidManifest.xml"
gradle = root / "smarttubetv/build.gradle"
for required in (player,app,settings,manifest,gradle,root/"sabr-media3"):
    if not required.exists():
        raise SystemExit(f"Expected pinned reference plus overlays: missing {required}")

def once(path, needle, replacement):
    s = path.read_text(encoding="utf-8")
    count = s.count(needle)
    if count != 1:
        raise SystemExit(f"MT2-006 fail-closed: {path.name}: expected one anchor, found {count}: {needle[:80]!r}")
    path.write_text(s.replace(needle,replacement,1), encoding="utf-8")

# Available system TV signals are preferable to display size or orientation.
# A phone rotated into landscape must NEVER turn into the D-pad-only shell.
mode = mobile/"java/com/newtube/mobile/ui/common/MarsDeviceMode.java"
mode.parent.mkdir(parents=True,exist_ok=True)
mode.write_text("""package com.newtube.mobile.ui.common;

import android.app.UiModeManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;

/**
 * One APK, two interface modes. Not screen-width detection: tablets and
 * landscape phones remain touch-operated, Fire TV/Android TV/Google TV use D-pad.
 */
public final class MarsDeviceMode {
    private MarsDeviceMode() {}

    public static boolean isTelevision(Context context) {
        UiModeManager manager = (UiModeManager)
                context.getSystemService(Context.UI_MODE_SERVICE);
        if (manager != null && manager.getCurrentModeType()
                == Configuration.UI_MODE_TYPE_TELEVISION) {
            return true;
        }
        PackageManager pm = context.getPackageManager();
        return pm != null && (pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
                || pm.hasSystemFeature("android.software.leanback_only"));
    }
}
""",encoding="utf-8")

# The latest 0.0.5 TV shell is retained unchanged. It is optional on a phone.
once(player,
     '        mMarsTvShell = new MarsTvShell(this, mVideoArea, () -> mPlayer,',
     '        if (com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(this)) {\n'
     '            mMarsTvShell = new MarsTvShell(this, mVideoArea, () -> mPlayer,')
once(player,
     '                });\n'
     '        // The phone chrome is not the control surface on TV. It must not consume remote focus.\n'
     '        mControlsRoot.setVisibility(View.GONE);\n'
     '        mControlsRoot.setFocusable(false);\n'
     '        mTimeBar.setVisibility(View.GONE);',
     '                });\n'
     '            // TV: native remote chrome owns focus. Phone retains its own controls.\n'
     '            mControlsRoot.setVisibility(View.GONE);\n'
     '            mControlsRoot.setFocusable(false);\n'
     '            mTimeBar.setVisibility(View.GONE);\n'
     '        }')

# MT2-005 already owns a complete German values-de translation. Initialize its
# existing LocaleUpdater on NEW installs only, never overwrite user selection.
once(app,'        super.onCreate();',
'''        super.onCreate();
        // MarsTube 0.0.6 starts in understandable German even on English Fire OS.
        // This happens only ONCE per installation, and changing language in
        // Settings is honored on all later launches.
        android.content.SharedPreferences marsPrefs =
                getSharedPreferences("mars_v2_application", MODE_PRIVATE);
        if (!marsPrefs.getBoolean("initialized_default_language", false)) {
            com.liskovsoft.sharedutils.locale.LocaleUpdater lang =
                    new com.liskovsoft.sharedutils.locale.LocaleUpdater(this);
            if (lang.getPreferredLanguage().isEmpty()) {
                lang.setPreferredLanguage("de");
                com.liskovsoft.sharedutils.locale.LocaleUpdater.clearCache();
            }
            marsPrefs.edit().putBoolean("initialized_default_language", true).apply();
        }''')

# About is OUR product. Keep the real installed version, diagnostic-log export
# and legally-required Open Source license attribution, but never offer
# NewTube's share badges, URLs or update installer as if it were ours.
s = settings.read_text(encoding="utf-8")
start = s.index('    private static Page about(Context context) {')
end_marker = '        return new Page(context.getString(R.string.about_app, context.getString(R.string.app_name)), rows);\n    }'
end=s.index(end_marker,start)+len(end_marker)
body = '''    private static Page about(Context context) {
        List<SettingsRow> rows = new ArrayList<>();
        rows.add(SettingsRow.action(context.getString(R.string.mobile_settings_version),
                versionLine(context), page -> { }));
        rows.add(SettingsRow.action("MarsTube-Updates",
                "Diese Testversion wird als APK lokal installiert.",
                page -> { }));
        rows.add(SettingsRow.divider());
        rows.add(SettingsRow.action(context.getString(R.string.diagnostic_log_send),
                context.getString(R.string.diagnostic_log_send_desc),
                page -> DiagnosticLog.share(page.requireContext())));
        rows.add(SettingsRow.action("MarsTube auf GitHub",
                "github.com/equiummc/mars-smarttube",
                page -> Utils.openLinkExt(page.requireContext(),
                        "https://github.com/equiummc/mars-smarttube")));
        // Attribution is distinct from the product name. Don't falsely claim
        // that open-source dependencies were authored by MarsTube.
        rows.add(SettingsRow.action("Open-Source-Lizenzen",
                context.getString(R.string.about_license_name),
                page -> Utils.openLinkExt(page.requireContext(),
                        context.getString(R.string.about_license_url))));
        return new Page("Über MarsTube", rows);
    }'''
if s.count(end_marker)!=1:
    raise SystemExit("Settings About source changed")
settings.write_text(s[:start]+body+s[end:],encoding="utf-8")

# Ensure the launcher Activity has the user's original blue M icon, even if a
# parent Activity ever declares a different icon from the main manifest.
once(manifest,
     'android:name="com.liskovsoft.smartyoutubetv2.tv.ui.main.SplashActivity"\n',
     'android:name="com.liskovsoft.smartyoutubetv2.tv.ui.main.SplashActivity"\n'
     '            android:icon="@drawable/mars_m_logo"\n')

# The 0.0.5 source only produced 32-bit Fire TV builds. 0.0.6 builds ONE
# sideloadable universal APK with 32-bit and 64-bit libraries, rather than a
# phone APK and a TV APK that could drift apart.
once(gradle,'applicationId "de.equiummc.marstube2.v005"',
             'applicationId "de.equiummc.marstube2.beta"')
once(gradle,'versionCode 12005','versionCode 12006')
once(gradle,'versionName "0.0.5"','versionName "0.0.6"')
once(gradle,'// ABI selection exclusively via splits: armeabi-v7a',
             "abiFilters 'armeabi-v7a', 'arm64-v8a' // single universal APK: Fire TV + arm64 phones")
once(gradle,'            enable = true\n            reset()',
             '            enable = false // single sideloadable universal APK, no ABI splits\n            reset()')
# The existing "include" is intentionally ignored when the split is disabled.
# A permanent beta package id is fixed, BUT DEBUG GitHub signatures need not
# match across runs. Release upgrade requires a controlled signing key first.

print("MT2-006 unified MarsTube phone/TV overlay, own About, M launcher, one universal APK.")
