#!/usr/bin/env python3
"""MT2-009: TV-only Shorts section, Mars-blue submenus, compact floating player controls.

Applied AFTER 008 on a disposable, pinned Media3 reference checkout. Nothing
replaces previously sideloaded 0.0.5/6/7/8 or the 1.x release.
"""
from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit("Usage: patch_009_tv_shorts_ui.py <pinned checkout>")
root = Path(sys.argv[1]).resolve()
mobile = root / "smarttubetv/src/stmobile"
tv = mobile / "java/com/newtube/mobile/ui"
if not (root / "sabr-media3").is_dir():
    raise SystemExit("MT2-009 requires pinned SABR Media3 checkout")

def replace_once(path, before, after):
    if not path.is_file():
        raise SystemExit(f"Missing {path}")
    data = path.read_text(encoding="utf-8")
    n = data.count(before)
    if n != 1:
        raise SystemExit(f"MT2-009 fail-closed: {path.name}: expected 1 anchor, saw {n}: {before[:100]!r}")
    path.write_text(data.replace(before, after, 1), encoding="utf-8")

gradle = root / "smarttubetv/build.gradle"
manifest = mobile / "AndroidManifest.xml"
replace_once(gradle, 'applicationId "de.equiummc.marstube2.firetest008"',
             'applicationId "de.equiummc.marstube2.firetest009"')
replace_once(gradle, 'versionCode 12008', 'versionCode 12009')
replace_once(gradle, 'versionName "0.0.8"', 'versionName "0.0.9"')
replace_once(manifest, 'android:label="MarsTube 0.0.8"',
             'android:label="MarsTube 0.0.9"')

for filename, dest in [
    ("MarsTvShell.java", tv / "playback/MarsTvShell.java"),
    ("MarsTvRail.java", tv / "common/MarsTvRail.java")]:
    source = Path(__file__).resolve().parent / filename
    if not source.is_file() or not dest.is_file():
        raise SystemExit(f"Missing modified TV UI source {filename}")
    dest.write_text(source.read_text(encoding="utf-8"), encoding="utf-8")

# Resource-level theme overlay rather than overwriting global day/night colors
# (a single APK still supports genuine touch phones). A TV stays recognizably blue
# in its Search, Settings, Channel and option-dialog pages.
theme_file = mobile / "res/values/mars_tv_theme_009.xml"
if theme_file.exists():
    raise SystemExit("TV theme file unexpectedly already exists")
theme_file.write_text("""<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="mars_tv_bg_009">#081729</color>
    <color name="mars_tv_surface_009">#122B48</color>
    <color name="mars_tv_card_009">#173556</color>
    <color name="mars_tv_field_009">#102946</color>
    <color name="mars_tv_accent_009">#149EFF</color>
    <style name="ThemeOverlay.MarsTube.Tv009">
        <item name="newtubeColorBackground">@color/mars_tv_bg_009</item>
        <item name="newtubeColorNavigationBar">@color/mars_tv_bg_009</item>
        <item name="newtubeColorBackgroundScrim">@color/mars_tv_bg_009</item>
        <item name="newtubeColorField">@color/mars_tv_field_009</item>
        <item name="newtubeColorSurface">@color/mars_tv_surface_009</item>
        <item name="newtubeColorCard">@color/mars_tv_card_009</item>
        <item name="newtubeColorCommentParent">@color/mars_tv_surface_009</item>
        <item name="colorSurface">@color/mars_tv_surface_009</item>
        <item name="colorPrimary">@color/mars_tv_accent_009</item>
        <item name="colorSecondary">@color/mars_tv_accent_009</item>
        <item name="colorControlActivated">@color/mars_tv_accent_009</item>
        <item name="android:windowBackground">@color/mars_tv_bg_009</item>
        <item name="android:statusBarColor">@color/mars_tv_bg_009</item>
        <item name="android:navigationBarColor">@color/mars_tv_bg_009</item>
    </style>
</resources>
""", encoding="utf-8")
activity = tv / "common/MobileActivity.java"
replace_once(activity,
'''        ThemeMode.applyPalette(this);
        super.onCreate(savedInstanceState);''',
'''        ThemeMode.applyPalette(this);
        if (MarsDeviceMode.isTelevision(this)) {
            getTheme().applyStyle(R.style.ThemeOverlay_MarsTube_Tv009, true);
        }
        super.onCreate(savedInstanceState);''')

# Focus in settings rows must be visible for a remote, not just Android's
# touch-centric pressed ripple. TV only: phones keep original styling.
adapter = tv / "settings/SettingsAdapter.java"
replace_once(adapter,
'''            itemView.setClickable(enabled);
        }
    }''',
'''            itemView.setClickable(enabled);
            if (com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(itemView.getContext())) {
                itemView.setFocusable(enabled);
                itemView.setOnFocusChangeListener((view, hasFocus) -> {
                    if (!hasFocus) {
                        view.setForeground(null);
                        return;
                    }
                    android.graphics.drawable.GradientDrawable border =
                            new android.graphics.drawable.GradientDrawable();
                    border.setColor(0x55305B9E);
                    border.setCornerRadius(9f * view.getResources().getDisplayMetrics().density);
                    border.setStroke(Math.max(2, (int) (2f * view.getResources().getDisplayMetrics().density)),
                            0xFF149EFF);
                    view.setForeground(border);
                });
            }
        }
    }''')

# SidebarService supplies the real Shorts observable from the shared TV
# BrowsePresenter. It was never removed, only omitted from phone UI.
# Enable it once on TVs (new test package), never on touch phones.
app = mobile / "java/com/newtube/mobile/MobileMainApplication.java"
replace_once(app,
'''        // TV RECEIVER ROLES OFF (mobile-only, one-shot): Auto Frame Rate''',
'''        // MarsTube 0.0.9: real Shorts browse source, TV only. Preserve a later
        // user-disabled section by running this migration once, not at every boot.
        if (com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(this)
                && !migrations.getBoolean("mars_tv_shorts_009", false)) {
            com.liskovsoft.smartyoutubetv2.common.app.presenters.service.SidebarService.instance(this)
                    .enableSection(com.liskovsoft.mediaserviceinterfaces.data.MediaGroup.TYPE_SHORTS, true);
            migrations.edit().putBoolean("mars_tv_shorts_009", true).apply();
        }

        // TV RECEIVER ROLES OFF (mobile-only, one-shot): Auto Frame Rate''')

browse = tv / "browse/MobileBrowseActivity.java"
replace_once(browse,
'''                    : destination == 4 ? VideoDownloads.SECTION_ID
                    : MediaGroup.TYPE_HOME;''',
'''                    : destination == 4 ? VideoDownloads.SECTION_ID
                    : destination == 8 ? MediaGroup.TYPE_SHORTS
                    : MediaGroup.TYPE_HOME;''')
# TV's dedicated Shorts feed MUST bypass the blanket mobile no-Shorts filter.
# Retain the filter for ordinary Home, Search, History, etc. until deliberately
# redesigned, so restoring Shorts doesn't quietly alter other categories.
replace_once(browse,
'''    private static List<Video> visibleFeedItems(List<Video> videos) {''',
'''    private List<Video> visibleFeedItems(List<Video> videos) {''')
replace_once(browse,
'''    private static boolean isVisibleFeedItem(Video video) {
        return video != null
                && !ShortsFilter.isShort(video)''',
'''    private boolean isVisibleFeedItem(Video video) {
        if (mCurrentSectionId == MediaGroup.TYPE_SHORTS) {
            // The provider already scopes this category to Shorts; a playable
            // videoId is sufficient. Reject section-navigation tiles.
            return video != null && video.hasVideo() && video.videoId != null;
        }
        return video != null
                && !ShortsFilter.isShort(video)''')
print("MT2-009 verified patch: Shorts provider + TV-only filter bypass, version 0.0.9, blue TV theme, video control order.")
