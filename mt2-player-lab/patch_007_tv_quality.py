#!/usr/bin/env python3
"""MT2-007 TV visual continuity and quality, applied after MT2-006 isolate.

All replacements are fail-closed and operate only on the pinned throwaway
NewTube checkout. Mobile phones keep touch UI and the 1080p mobile default.
"""
from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit("Usage: patch_007_tv_quality.py <pinned reference root>")
root = Path(sys.argv[1]).resolve()
mobile = root / "smarttubetv/src/stmobile"
tv = mobile / "java/com/newtube/mobile/ui"
if not (root / "sabr-media3").is_dir():
    raise SystemExit("Fail closed: expected pinned SABR-Media3 reference")

def one(path, before, after):
    if not path.is_file():
        raise SystemExit(f"Missing file: {path}")
    s = path.read_text(encoding="utf-8")
    n = s.count(before)
    if n != 1:
        raise SystemExit(f"MT2-007 fail-closed: {path.name}: anchor count {n}: {before[:90]!r}")
    path.write_text(s.replace(before, after, 1), encoding="utf-8")

# Collision-free one-off TV candidate. Reinstalling unsigned CI debug builds
# over the 0.0.6 isolated package is intentionally NOT attempted.
gradle = root / "smarttubetv/build.gradle"
manifest = mobile / "AndroidManifest.xml"
one(gradle, 'applicationId "de.equiummc.marstube2.firetest006"',
    'applicationId "de.equiummc.marstube2.firetest007"')
one(gradle, 'versionCode 12006', 'versionCode 12007')
one(gradle, 'versionName "0.0.6"', 'versionName "0.0.7"')
one(manifest, 'android:label="MarsTube TV-Test 006"',
    'android:label="MarsTube TV-Test 007"')

# ThemeMode persists an explicit user preference; only brand-new real TV
# installations default to dark. A phone remains governed by system theme.
theme = tv / "common/ThemeMode.java"
one(theme, 'int initial = existingInstall ? DARK : SYSTEM;',
    'int initial = (existingInstall || MarsDeviceMode.isTelevision(app)) ? DARK : SYSTEM;')

# Reuse one TV rail on Browse, Search, Settings and channel screens.
# It does not affect the player Activity (already has its dedicated TV HUD),
# sign-in dialogs or any phone screen.
rail_source = Path(__file__).resolve().parent / "MarsTvRail.java"
if not rail_source.is_file():
    raise SystemExit("MarsTvRail.java missing next to patch script")
rail_dest = tv / "common/MarsTvRail.java"
if rail_dest.exists():
    raise SystemExit("Fail closed: MarsTvRail already exists")
rail_dest.write_text(rail_source.read_text(encoding="utf-8"), encoding="utf-8")

activity = tv / "common/MobileActivity.java"
one(activity,
    '        mContentLayoutId = layoutResID;\n        installContentInsets();',
    '        mContentLayoutId = layoutResID;\n        installContentInsets();\n'
    '        MarsTvRail.install(this);')
one(activity,
    '    /** NEWTUBE(theme): the layout {@link #recolourInPlace} inflates again. */',
    '    @Override\n'
    '    public boolean dispatchKeyEvent(android.view.KeyEvent event) {\n'
    '        if (MarsTvRail.handleKey(this, event)) return true;\n'
    '        return super.dispatchKeyEvent(event);\n'
    '    }\n\n'
    '    /** NEWTUBE(theme): the layout {@link #recolourInPlace} inflates again. */')

browse = tv / "browse/MobileBrowseActivity.java"
one(browse,
    '        if (marsTvNavigationKey(event)) return true;',
    '        // The visible left rail replaces the old invisible bottom-navigation key map.\n'
    '        if (com.newtube.mobile.ui.common.MarsTvRail.handleKey(this, event)) return true;\n'
    '        if (com.newtube.mobile.ui.common.MarsTvRail.isInstalled(this)\n'
    '                && !mYouShowing && mMarsTvSelection <= 0\n'
    '                && event.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT) {\n'
    '            if (event.getAction() == KeyEvent.ACTION_DOWN)\n'
    '                com.newtube.mobile.ui.common.MarsTvRail.focus(this);\n'
    '            return true;\n'
    '        }\n'
    '        if (!com.newtube.mobile.ui.common.MarsTvRail.isInstalled(this)\n'
    '                && marsTvNavigationKey(event)) return true;')
one(browse,
    '    private void marsTvHandleDestination(android.content.Intent intent) {',
    '    public void marsTvRoute(int destination) {\n'
    '        android.content.Intent intent = new android.content.Intent();\n'
    '        intent.putExtra("mars_tv_section", destination);\n'
    '        marsTvHandleDestination(intent);\n'
    '    }\n\n'
    '    private void marsTvHandleDestination(android.content.Intent intent) {')
one(browse,
    '        } else if (destination == 5) {\n            openSettings();',
    '        } else if (destination == 5) {\n'
    '            openSettings();\n'
    '        } else if (destination == 6) {\n'
    '            mBottomNav.post(() -> mBottomNav.setSelectedItemId(YOU_ITEM_ID));')

# A phone caps initial Auto to 1080p for startup. A TV should instead use
# the pre-existing device codec capability check in PlayerData (4K where
# supported), without blindly pinning a fixed 4K track.
app = mobile / "java/com/newtube/mobile/MobileMainApplication.java"
one(app,
    'PlayerData.setDefaultVideoFormatMax1080(true);',
    'PlayerData.setDefaultVideoFormatMax1080(!com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(this));')
initializer = mobile / "java/com/newtube/mobile/player/Media3PlayerInitializer.java"
one(initializer,
    'int ceiling = VideoRung.ceilingSide(1080);',
    'int autoHeight = com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(mContext)\n'
    '                ? PlayerData.instance(mContext).getDefaultVideoFormat().getHeight() : 1080;\n'
    '        int ceiling = VideoRung.ceilingSide(autoHeight);')

# 10 Mbps is a CAP ON A RECENT, ACTUALLY MEASURED startup seed. It is not
# a forced stream bitrate or a claim that the current link can sustain it.
meter = mobile / "java/com/newtube/mobile/player/StartupBandwidthMeter.java"
one(meter, 'static final long MAX_RECENT_SEED_BPS = 4_000_000;',
    'static final long MAX_RECENT_SEED_BPS = 10_000_000;')

# The precise preset list already exists under Advanced in NewTube.
# Expose it at the primary Quality screen on TV, with its real persistence,
# hardware support filter and optional higher-bitrate metadata request.
pages = tv / "settings/PlayerPages.java"
one(pages,
    '        rows.add(defaultQuality(context));\n\n        rows.add(SettingsRow.header(context.getString(R.string.mobile_settings_audio)));',
    '        rows.add(defaultQuality(context));\n'
    '        if (com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(context)) {\n'
    '            rows.add(SettingsRow.header("Detaillierte TV-Bildqualität"));\n'
    '            rows.add(SettingsRow.note("Auflösung, Bildrate (fps), Codec und HDR '
    'getrennt wählen. Das Video muss das Format anbieten und der Fernseher es '
    'dekodieren können. Automatisch passt sich weiter an die Verbindung an."));\n'
    '            rows.add(videoFormat(context, playerData, tweaks));\n'
    '            rows.add(SettingsRow.toggle("1080p mit höherer Bitrate",\n'
    '                    "Zusätzliche hochwertige Videoformate anfragen, '
    'sofern der Dienst sie bereitstellt.",\n'
    '                    tweaks::isHighBitrateFormatsEnabled, tweaks::setHighBitrateFormatsEnabled));\n'
    '        }\n\n'
    '        rows.add(SettingsRow.header(context.getString(R.string.mobile_settings_audio)));')

print("MT2-007: TV dark default, visible left rail, detailed quality, capability-based Auto, recent 10 Mbps seed.")
