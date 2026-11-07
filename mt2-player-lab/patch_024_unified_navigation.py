#!/usr/bin/env python3
"""MT2-024: one narrow, circular TV rail with focusable M and fast label flyouts.

Applied after the 0.0.23 patch. Does not modify update logic, APK signature,
Media3, SABR, player BACK behavior or Android task-stack routes.
"""
from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit("Usage: patch_024_unified_navigation.py <pinned-newtube-root>")
root = Path(sys.argv[1]).resolve()
if not (root / "sabr-media3").is_dir():
    raise SystemExit("MT2-024: pinned Media3 checkout missing")
here = Path(__file__).resolve().parent
tv = root / "smarttubetv/src/stmobile/java/com/newtube/mobile/ui"

def once(path, old, new):
    if not path.is_file():
        raise SystemExit(f"MT2-024: missing {path}")
    source = path.read_text(encoding="utf-8")
    count = source.count(old)
    if count != 1:
        raise SystemExit(f"MT2-024: unexpected anchor count={count}, {path.name}: {old[:90]!r}")
    path.write_text(source.replace(old, new, 1), encoding="utf-8")

def copy_checked(source, target):
    if not source.is_file():
        raise SystemExit(f"MT2-024: missing source {source}")
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_bytes(source.read_bytes())

for name in ("MarsTvNavigationModel.java", "MarsTvRailLabel.java"):
    copy_checked(here / name, tv / "common" / name)
copy_checked(here / "MarsTvNavigationModelTest.java",
             root / "smarttubetv/src/test/java/com/newtube/mobile/ui/common/MarsTvNavigationModelTest.java")
# The 023 staged rail was copied into the final pinned checkout by patch_023.
copy_checked(here / "MarsTvRail024.java", tv / "common/MarsTvRail.java")

theme = tv / "common/MarsTvNavigationTheme.java"
once(theme, "public static final int SETTINGS_WIDTH_DP = 132;",
     "public static final int SETTINGS_WIDTH_DP = WIDTH_DP;")
once(theme, "public static final int ITEM_HEIGHT_DP = 38;",
     "public static final int ITEM_HEIGHT_DP = 44;")
once(theme, "case 0: return R.drawable.mars_tv_ic_nav_home;",
     "case 0: return R.drawable.mars_m_flat;")

shell = tv / "playback/MarsTvShell.java"
once(shell, """        ImageView logo = new ImageView(activity);
        logo.setImageResource(R.drawable.mars_m_flat);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        logo.setContentDescription("MarsTube");
        rail.addView(logo, new LinearLayout.LayoutParams(dp(38), dp(40)));
        TextView version = text(11, MUTED);
        version.setText("0.0.15");
        version.setGravity(Gravity.CENTER);
        rail.addView(version, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(16)));""",
     """        // The first focusable rail action is the single-colour M logo / Startseite.
        // Version is shown in Settings, never as a misleading old overlay label.""")

once(shell, """        addRailItem("Start", 0);
        addRailItem("Shorts", 8);
        addRailItem("Suche", 1);
        addRailItem("Abos", 2);
        addRailItem("Verlauf", 3);
        addRailItem("Profil", 6);
        addRailItem("Einstellungen", 5);
        addRailItem("Shorts Auto", -18);""",
     """        for (com.newtube.mobile.ui.common.MarsTvNavigationModel.Entry entry
                : com.newtube.mobile.ui.common.MarsTvNavigationModel.main()) {
            addRailItem(entry.label, entry.destination);
        }
        // Shorts auto-advance remains in Shorts controls, not the global rail.""")

once(shell, """        railHint = text(12, WHITE);
        railHint.setGravity(Gravity.CENTER_VERTICAL);
        railHint.setPadding(dp(14), 0, dp(14), 0);
        railHint.setBackground(shape(0xF00A1D33, CYAN, 1, 10));
        rail.addView(railHint, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(28)));""",
     """        railHint = com.newtube.mobile.ui.common.MarsTvRailLabel.attach(
                activity, root, com.newtube.mobile.ui.common.MarsTvNavigationTheme.WIDTH_DP);""")

once(shell, """            glyph.setBounds(0, 0, dp(20), dp(20));""",
     """            int iconDp = destination == 0 ? 32 : 20;
            glyph.setBounds(0, 0, dp(iconDp), dp(iconDp));""")

once(shell,
     "railPosition = (railPosition + railButtons.size() - 1) % railButtons.size();",
     "railPosition = com.newtube.mobile.ui.common.MarsTvNavigationModel.wrap("
     "railPosition, -1, com.newtube.mobile.ui.common.MarsTvNavigationModel.playerCount("
     "actions.isShortsPlayback()));")
once(shell, "railPosition = (railPosition + 1) % railButtons.size();",
     "railPosition = com.newtube.mobile.ui.common.MarsTvNavigationModel.wrap("
     "railPosition, 1, com.newtube.mobile.ui.common.MarsTvNavigationModel.playerCount("
     "actions.isShortsPlayback()));")
once(shell, """        railPosition = Math.min(railPosition,
                shortsMode ? railButtons.size() - 1 : railButtons.size() - 2);""",
     """        railPosition = Math.min(railPosition,
                com.newtube.mobile.ui.common.MarsTvNavigationModel.playerCount(shortsMode) - 1);""")
once(shell,
     """        railHint.setVisibility(View.GONE); // no clipped labels on narrow video rail""",
     """        if (!railOpen) {
            com.newtube.mobile.ui.common.MarsTvRailLabel.hide(railHint);
        }""")
once(shell,
     """        if (focus == Focus.RAIL) {
            railHint.setText(railDestinations.get(railPosition).label);
        }""",
     """        if (focus == Focus.RAIL) {
            com.newtube.mobile.ui.common.MarsTvRailLabel.show(
                    railHint, railButtons.get(railPosition),
                    railDestinations.get(railPosition).label);
        }""")
# Input and focus are unchanged except wraparound; player BACK state machine retained.
assert "openRail();\n        render();\n        return true;" in shell.read_text(encoding="utf-8")
assert "MarsTvNavigationModel.playerCount(shortsMode)" in shell.read_text(encoding="utf-8")
assert "MarsTvRailLabel.show" in shell.read_text(encoding="utf-8")
# MT2-021 re-copied a stale player source after MT2-016's dynamic version fix.
# Do not allow any historical version to survive in the final playback rail.
assert 'version.setText("0.0.15")' not in shell.read_text(encoding="utf-8")
rail = (tv / "common/MarsTvRail.java").read_text(encoding="utf-8")
assert "MarsTvNavigationModel.wrap(index, -1" in rail
assert 'addAction("Start", 0)' not in rail
assert "marsTvGoToSettingsRoot()" in rail
assert "SETTINGS_WIDTH_DP = WIDTH_DP" in theme.read_text(encoding="utf-8")
assert 'applicationId "de.equiummc.marstube2"' in (
    root / "smarttubetv/build.gradle").read_text(encoding="utf-8")
assert "highestCode" in (root / "SharedModules/appupdatechecker2/src/main/java/"
                        "com/liskovsoft/appupdatechecker2/core/AppVersionChecker.java").read_text(encoding="utf-8")
print("MT2-024: identical 7-entry TV/Shorts/player rail, wrap, home M and flyout labels.")
