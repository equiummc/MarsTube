#!/usr/bin/env python3
"""MT2-025: the approved minimalist TV rail and actual single-player Shorts layout.

Post-024; preserve unchanged owner M vector, package, signature, updater and
the playback BACK machine. Patch matches fail-closed to prevent silent drift.
"""
from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit("Usage: patch_025_tv_ui.py <pinned-checkout>")
root = Path(sys.argv[1]).resolve()
if not (root / "sabr-media3").is_dir():
    raise SystemExit("MT2-025: missing pinned reference")
here = Path(__file__).resolve().parent
mobile = root / "smarttubetv/src/stmobile"
tv = mobile / "java/com/newtube/mobile/ui"

def once(path, old, new):
    text = path.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"MT2-025 unsafe anchor count={count} {path.name}: {old[:86]!r}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")

def copy(src, dst):
    if not src.is_file() or not dst.parent.is_dir():
        raise SystemExit(f"MT2-025 source or destination missing {src} => {dst}")
    dst.write_bytes(src.read_bytes())

# Keep exactly the historic flat white Mars M in ALL places. Never redraw it.
original_m = (here / "assets/drawable/mars_m_flat.xml").read_bytes()
original_installed = (mobile / "res/drawable/mars_m_flat.xml").read_bytes()
if original_installed != original_m:
    raise SystemExit("MT2-025: original Mars-M identity was modified")
for src, dst in (
    ("MarsTvRail025.java", tv / "common/MarsTvRail.java"),
    ("MarsTvShortsStage025.java", tv / "playback/MarsTvShortsStage.java"),
    ("MarsTvSettingsPages025.java", tv / "settings/MarsTvSettingsPages.java"),
):
    copy(here / src, dst)
copy(here / "MarsTvNavigationModel.java", tv / "common/MarsTvNavigationModel.java")
for name in ("search", "abos", "history", "shorts", "profile", "settings"):
    filename = "mars_nav_" + name + "_025.xml"
    copy(here / "assets/drawable" / filename, mobile / "res/drawable" / filename)

theme = tv / "common/MarsTvNavigationTheme.java"
once(theme, "public static final int WIDTH_DP = 64;",
     "public static final int WIDTH_DP = 72;")
once(theme, "public static final int ITEM_HEIGHT_DP = 44;",
     "public static final int ITEM_HEIGHT_DP = 46;")
once(theme, "drawable.setColor(selected ? SELECTED : 0xFF0D1525);",
     "drawable.setColor(selected ? 0xEE1D5987 : 0x590B2B4B);")
once(theme, "    public static GradientDrawable homeActivatedBackground(float density) {",
     """    /** One small floating, translucent capsule, not a full-height sidebar. */
    public static GradientDrawable floatingBackground(float density) {
        GradientDrawable result = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{0xEB1A3A59, 0xD80B1E38, 0xEB102D4A});
        result.setCornerRadius(19f * density);
        result.setStroke(Math.max(1, Math.round(density)), 0x886B9DBF);
        return result;
    }

    public static GradientDrawable homeActivatedBackground(float density) {""")
for dest, source, target in (
    (1,"mars_tv_ic_nav_search","mars_nav_search_025"),
    (2,"mars_tv_ic_nav_subscriptions","mars_nav_abos_025"),
    (3,"mars_tv_ic_nav_history","mars_nav_history_025"),
    (8,"mars_tv_ic_nav_shorts","mars_nav_shorts_025"),
    (6,"mars_tv_ic_nav_profile","mars_nav_profile_025"),
    (5,"mars_tv_ic_nav_settings","mars_nav_settings_025"),
):
    once(theme, f"case {dest}: return R.drawable.{source};",
         f"case {dest}: return R.drawable.{target};")

# Fix the real 024 flyout-offset bug: topMargin and translationY were both set
# to the same screen coordinate. ONE positioning mechanism, screen coordinates
# transformed into the host parent only once.
flyout = tv / "common/MarsTvRailLabel.java"
once(flyout,
     "lp.topMargin = Math.max(0, Math.min(top, Math.max(0, host.getHeight() - lp.height)));",
     "int alignedTop = Math.max(0, Math.min(top, Math.max(0, host.getHeight() - lp.height)));")
once(flyout, "label.setTranslationY(top);", "label.setTranslationY(alignedTop);")

shell = tv / "playback/MarsTvShell.java"
once(shell,
     """        rail.setGravity(Gravity.CENTER_HORIZONTAL);
        rail.setBackground(com.newtube.mobile.ui.common.MarsTvNavigationTheme.railBackground());
        rail.setPadding(dp(4), dp(9), dp(4), dp(6));""",
     """        rail.setGravity(Gravity.CENTER);
        rail.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        rail.setPadding(dp(3), dp(8), dp(3), dp(8));""")
once(shell,
     """        railItems.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        rail.addView(railItems, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));""",
     """        railItems.setGravity(Gravity.CENTER_HORIZONTAL);
        railItems.setPadding(dp(4), dp(10), dp(4), dp(10));
        railItems.setBackground(com.newtube.mobile.ui.common.MarsTvNavigationTheme
                .floatingBackground(activity.getResources().getDisplayMetrics().density));
        rail.addView(railItems, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));""")
once(shell,
     "railPosition = Math.max(0, Math.min(railPosition, railButtons.size() - 1));",
     "railPosition = com.newtube.mobile.ui.common.MarsTvNavigationModel.HOME_INDEX;")
once(shell, """            railButtons.get(i).setTextColor(selected ? CYAN : WHITE);""",
     """            railButtons.get(i).setTextColor(selected ? CYAN : WHITE);
            railButtons.get(i).setCompoundDrawableTintList(
                    android.content.res.ColorStateList.valueOf(
                            selected ? 0xFF68C7FF : WHITE));""")

# Remote focus follows visual order: previews LEFT, 9:16 video CENTER, actions
# RIGHT, progress below. No inaccessible hidden reaction index.
prefix = "        if (actions.isShortsPlayback() && focus != Focus.RAIL && !infoOpen) {"
data = shell.read_text(encoding="utf-8")
start = data.find(prefix)
inner = data.find("            switch (key) {", start + len(prefix))
end = data.find("        switch (key) {", inner + len("            switch (key) {"))
if start < 0 or end < 0 or data[start:end].count("KEYCODE_DPAD_UP") != 1:
    raise SystemExit("MT2-025: cannot isolate Shorts input branch")
shorts_control = """        if (actions.isShortsPlayback() && focus != Focus.RAIL && !infoOpen) {
            switch (key) {
                case KeyEvent.KEYCODE_DPAD_UP:
                    if (focus == Focus.SHORTS_PREVIEWS) shortsStage.movePreview(-1);
                    else if (focus == Focus.SHORTS_REACTIONS)
                        shortsReactionPosition = (shortsReactionPosition + 1) % 2;
                    else if (focus == Focus.SHORTS_PROGRESS) {
                        focus = Focus.SHORTS_REACTIONS;
                        shortsReactionPosition = 1;
                    } else actions.previous();
                    render();
                    return true;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    if (focus == Focus.SHORTS_PREVIEWS) shortsStage.movePreview(1);
                    else if (focus == Focus.SHORTS_REACTIONS)
                        shortsReactionPosition = (shortsReactionPosition + 1) % 2;
                    else if (focus == Focus.SHORTS_PROGRESS) focus = Focus.VIDEO;
                    else actions.next();
                    render();
                    return true;
                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                    actions.previous();
                    render();
                    return true;
                case KeyEvent.KEYCODE_MEDIA_NEXT:
                    actions.next();
                    render();
                    return true;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    if (focus == Focus.SHORTS_PROGRESS) moveSeekPreview(-1);
                    else if (focus == Focus.SHORTS_PREVIEWS) openRail();
                    else if (focus == Focus.VIDEO) focus = Focus.SHORTS_PREVIEWS;
                    else focus = Focus.VIDEO;
                    render();
                    return true;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    if (focus == Focus.SHORTS_PREVIEWS) focus = Focus.VIDEO;
                    else if (focus == Focus.VIDEO) {
                        focus = Focus.SHORTS_REACTIONS;
                        shortsReactionPosition = 0;
                    } else if (focus == Focus.SHORTS_REACTIONS)
                        focus = Focus.SHORTS_PROGRESS;
                    else if (focus == Focus.SHORTS_PROGRESS) moveSeekPreview(1);
                    render();
                    return true;
                default:
                    break; // Playback BACK and MENU remain untouched.
            }
        }
"""
# Preserve the rest of the regular player key handling exactly.
shell.write_text(data[:start] + shorts_control + data[end:], encoding="utf-8")

once(shell, """                else if (shortsReactionPosition == 1) actions.dislike();
                else if (shortsReactionPosition == 2) actions.save();
                else if (shortsReactionPosition == 3) seek(-5_000L);
                else if (shortsReactionPosition == 4) seek(5_000L);
            } else if (focus == Focus.SHORTS_AUTO) {
                shortsStage.toggleAutoNext();""",
     """                else if (shortsReactionPosition == 1) actions.dislike();""")

# Match the same PlayerView size and position as MarsTvShortsStage.videoWidth():
# central 9:16, 22dp top/bottom breathing room; no stretch or second decoder.
playback = tv / "playback/MobilePlaybackActivity.java"
once(playback,
     """                            int bar = Math.max(1, Math.round(
                                    7f * getResources().getDisplayMetrics().density));
                            int videoHeight = Math.max(1, fullHeight - bar);
                            lp.width = Math.min(fullWidth,
                                    Math.round(videoHeight * 9f / 16f));
                            lp.height = videoHeight;
                            lp.gravity = android.view.Gravity.TOP | android.view.Gravity.RIGHT;
                            mPlayerView.setLayoutParams(lp);
                            mPlayerView.setResizeMode(
                                    androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM);""",
     """                            float density = getResources().getDisplayMetrics().density;
                            int margin = Math.round(22f * density);
                            int footer = Math.round(10f * density);
                            int videoHeight = Math.max(1, fullHeight - 2 * margin - footer);
                            lp.width = Math.min(fullWidth,
                                    Math.round(videoHeight * 9f / 16f));
                            lp.height = videoHeight;
                            lp.gravity = android.view.Gravity.TOP
                                    | android.view.Gravity.CENTER_HORIZONTAL;
                            lp.topMargin = margin;
                            mPlayerView.setLayoutParams(lp);
                            mPlayerView.setResizeMode(
                                    androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT);
                            mPlayerView.setOutlineProvider(new android.view.ViewOutlineProvider() {
                                @Override public void getOutline(android.view.View v,
                                                                  android.graphics.Outline outline) {
                                    outline.setRoundRect(0, 0, v.getWidth(), v.getHeight(),
                                            18f * v.getResources().getDisplayMetrics().density);
                                }
                            });
                            mPlayerView.setClipToOutline(true);""")
once(playback,
     """                            lp.gravity = android.view.Gravity.CENTER;
                            mPlayerView.setLayoutParams(lp);""",
     """                            lp.gravity = android.view.Gravity.CENTER;
                            lp.topMargin = 0;
                            mPlayerView.setLayoutParams(lp);
                            mPlayerView.setClipToOutline(false);
                            mPlayerView.setOutlineProvider(
                                    android.view.ViewOutlineProvider.BACKGROUND);""")

# Brand and release invariants; source resource stays exactly byte-identical.
assert original_m == (mobile / "res/drawable/mars_m_flat.xml").read_bytes()
assert 'applicationId "de.equiummc.marstube2"' in (
    root / "smarttubetv/build.gradle").read_text(encoding="utf-8")
assert "highestCode" in (
    root / "SharedModules/appupdatechecker2/src/main/java/com/liskovsoft/appupdatechecker2/core/AppVersionChecker.java").read_text(encoding="utf-8")
assert "MarsTvNavigationModel.HOME_INDEX" in shell.read_text(encoding="utf-8")
assert "new MarsTvShortsStage" in shell.read_text(encoding="utf-8")
assert "return handleBack();" in shell.read_text(encoding="utf-8")
assert "if (focus == Focus.SHORTS_PROGRESS) moveSeekPreview(-1);" in shell.read_text(encoding="utf-8")
assert "else if (focus == Focus.SHORTS_PROGRESS) moveSeekPreview(1);" in shell.read_text(encoding="utf-8")
assert "mPlayerView.setClipToOutline(true)" in playback.read_text(encoding="utf-8")
assert "getSharedPreferences(\"mars_tv_shorts\"" in (
    tv / "settings/MarsTvSettingsPages.java").read_text(encoding="utf-8")
print("MT2-025: exact historic M, centered capsule, real single centered 9:16 Shorts PlayerView, two previews and two reactions.")
