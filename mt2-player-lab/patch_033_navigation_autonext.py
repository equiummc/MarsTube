#!/usr/bin/env python3
"""MT2-033 first-stage repairs on top of the pinned, successfully built 0.0.32 tree.
Fail closed if source moves; never patch a local working checkout.
"""
from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit("Usage: patch_033_navigation_autonext.py <isolated-source>")
root = Path(sys.argv[1])
tv = root / "smarttubetv/src/stmobile/java/com/newtube/mobile/ui/playback"
shell = tv / "MarsTvShell.java"
stage = tv / "MarsTvShortsStage.java"

def replace_exact(path, before, after):
    source = path.read_text(encoding="utf-8")
    count = source.count(before)
    if count != 1:
        raise SystemExit(f"MT2-033 FAIL CLOSED: {path.name}: expected 1 occurrence, got {count}: {before[:90]!r}")
    path.write_text(source.replace(before, after, 1), encoding="utf-8")

# Preserve the last focused scope before the rail overlays video.
replace_exact(shell,
    "    private Focus focus = Focus.VIDEO;",
    "    private Focus focus = Focus.VIDEO;\n    private Focus focusBeforeRail = Focus.VIDEO;")
replace_exact(shell,
    "        focus = Focus.RAIL;\n        railEverOpened = true;",
    "        if (focus != Focus.RAIL) focusBeforeRail = focus;\n        focus = Focus.RAIL;\n        railEverOpened = true;")
replace_exact(shell,
    "        openRail();\n        render();\n        return true;\n    }\n\n    public boolean handle(KeyEvent event)",
    "        if (focus == Focus.RAIL) {\n"
    "            focus = restoredRailFocus();\n"
    "        } else {\n"
    "            openRail();\n"
    "        }\n"
    "        render();\n        return true;\n    }\n\n"
    "    private Focus restoredRailFocus() {\n"
    "        if (focusBeforeRail == Focus.RAIL) return Focus.VIDEO;\n"
    "        if (focusBeforeRail == Focus.CARDS && adapter.getItemCount() == 0) return Focus.VIDEO;\n"
    "        if (focusBeforeRail == Focus.SHORTS_PREVIEWS\n"
    "                && !actions.isShortsPlayback()) return Focus.VIDEO;\n"
    "        return focusBeforeRail;\n"
    "    }\n\n    public boolean handle(KeyEvent event)")
replace_exact(shell,
    "                if (focus == Focus.RAIL) {\n                    focus = actions.isShortsPlayback() ? Focus.VIDEO\n"
    "                            : adapter.getItemCount() > 0 ? Focus.CARDS : Focus.CONTROLS;",
    "                if (focus == Focus.RAIL) {\n                    focus = restoredRailFocus();")
# Actual loop mode must correspond to selected UI mode; otherwise ExoPlayer's
# REPEAT_MODE_ONE can recycle the same Short without ever reaching STATE_ENDED.
replace_exact(shell,
    "        ExoPlayer exo = player.current();\n        long duration = exo == null ? 0 : exo.getDuration();",
    "        ExoPlayer exo = player.current();\n"
    "        if (actions.isShortsPlayback() && exo != null) {\n"
    "            int repeat = shortsStage.isAutoNext()\n"
    "                    ? androidx.media3.common.Player.REPEAT_MODE_OFF\n"
    "                    : androidx.media3.common.Player.REPEAT_MODE_ONE;\n"
    "            if (exo.getRepeatMode() != repeat) exo.setRepeatMode(repeat);\n"
    "        }\n"
    "        long duration = exo == null ? 0 : exo.getDuration();")
replace_exact(shell,
    "                if (shortsStage.isAutoNext()\n"
    "                        && com.newtube.mobile.ui.common.MarsShortsSession.hasNext()) {\n"
    "                    actions.next();\n                }",
    "                if (shortsStage.isAutoNext()) {\n"
    "                    // At queue end, next() now requests Discovery (MT2-032).\n"
    "                    // Never let generic video recommendations take over.\n"
    "                    actions.next();\n                }")
# Clock changes affect the player chrome only. Browse headers and visual redesign
# must be separately audited before modifying existing focus/rail components.
replace_exact(shell,
    "        clock = text(18, WHITE);",
    "        clock = text(16, WHITE);")
replace_exact(shell,
    "                dp(120), dp(36), Gravity.TOP | Gravity.RIGHT);",
    "                dp(176), dp(58), Gravity.TOP | Gravity.RIGHT);")
replace_exact(shell,
    'new SimpleDateFormat("HH:mm", Locale.GERMANY).format(new Date())',
    'new SimpleDateFormat("HH:mm\\nEEE, dd.MM.yyyy", Locale.GERMANY).format(new Date())')
replace_exact(stage,
    'new SimpleDateFormat("HH:mm", Locale.GERMANY)',
    'new SimpleDateFormat("HH:mm\\nEEE, dd.MM.yyyy", Locale.GERMANY)')
replace_exact(stage,
    "                ViewGroup.LayoutParams.MATCH_PARENT, dp(33));",
    "                ViewGroup.LayoutParams.MATCH_PARENT, dp(48));")

gradle = root / "smarttubetv/build.gradle"
replace_exact(gradle, "versionCode 12032", "versionCode 12033")
replace_exact(gradle, 'versionName "0.0.32"', 'versionName "0.0.33"')
print("MT2-033: auto-next triggers Discovery, player repeat synced, rail focus restored, player dates added.")
