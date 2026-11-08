#!/usr/bin/env python3
"""MT2-033: detach regular-TV player overlay chrome from the seek bar.
Visual only: no playback, Media3, D-pad, button action or shared decoder changes.
Runs after MT2-033 submenus in a disposable signed-build workspace.
"""
from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit("Usage: patch_034_player_overlay.py <isolated-source>")
root = Path(sys.argv[1])
shell = root / "smarttubetv/src/stmobile/java/com/newtube/mobile/ui/playback/MarsTvShell.java"

def one(before, after):
    text = shell.read_text(encoding="utf-8")
    count = text.count(before)
    if count != 1:
        raise SystemExit(f"MT2-034 unsafe shell anchor {count}: {before[:80]!r}")
    shell.write_text(text.replace(before, after, 1), encoding="utf-8")

one(
    "    private final FrameLayout panel;",
    "    private final FrameLayout panel;\n    private final View bottomShade;")
one(
    "        cardWidthPx = Math.max(dp(96), (width - dp(88) - gapPx * 5) / 6);",
    "        cardWidthPx = Math.max(dp(112), (width - dp(220) - gapPx * 5) / 6);")
one(
    """        host.addView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));""",
    """        host.addView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        // A cheap GPU-only wash behind floating controls and carousel.
        bottomShade = new View(activity);
        bottomShade.setBackground(new GradientDrawable(
                GradientDrawable.Orientation.BOTTOM_TOP,
                new int[]{0xA90773B8, 0x78062B55, 0x00000000}));
        root.addView(bottomShade, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(292), Gravity.BOTTOM));
        bottomShade.setVisibility(View.GONE);""")
one(
    "        panel.setBackground(chrome);",
    "        // Transparent surface: progress gets its own narrow blue capsule.\n"
    "        panel.setBackgroundColor(android.graphics.Color.TRANSPARENT);")
one(
    "        progressRow.setGravity(Gravity.CENTER_VERTICAL);",
    """        progressRow.setGravity(Gravity.CENTER_VERTICAL);
        GradientDrawable seekChrome = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xC808598F, 0xD8092448});
        seekChrome.setCornerRadius(dp(11));
        seekChrome.setStroke(dp(1), 0x993FC6F0);
        progressRow.setBackground(seekChrome);
        progressRow.setPadding(dp(9), 0, dp(9), 0);""")
one(
    """                        : new int[]{0xCC1473A4, 0xDC0A3C67, 0xE3072445});""",
    """                        : new int[]{0x330A5B93, 0x16092545});""")
one(
    """        bg.setStroke(dp(selected || primary ? 2 : 1),
                selected ? 0xFF9FFFFF : primary ? 0xFF27E8FD : 0xAA2CB1EC);""",
    """        bg.setStroke(dp(selected || primary ? 2 : 0),
                selected ? 0xFF9FFFFF : primary ? 0xFF27E8FD : 0x00000000);""")
one(
    "        panel.setVisibility(panelVisible ? View.VISIBLE : View.GONE);",
    "        panel.setVisibility(panelVisible ? View.VISIBLE : View.GONE);\n"
    "        bottomShade.setVisibility(panelVisible ? View.VISIBLE : View.GONE);")

# Version and production identity are already pinned by patch_033_navigation_autonext.py
gradle = root / "smarttubetv/build.gradle"
assert "versionCode 12033" in gradle.read_text(encoding="utf-8")
assert 'versionName "0.0.33"' in gradle.read_text(encoding="utf-8")
print("MT2-033 CHROME: narrow seek capsule, floating blue glow, compact six-card carousel.")
