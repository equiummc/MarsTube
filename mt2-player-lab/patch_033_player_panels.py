#!/usr/bin/env python3
"""MT2-033 unified in-player TV submenus. Runs after 033 navigation patch.
Fails closed on unexpected upstream source. Single existing Media3 player only.
"""
from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit("Usage: patch_033_player_panels.py <isolated-source>")
root = Path(sys.argv[1])
here = Path(__file__).resolve().parent
tv = root / "smarttubetv/src/stmobile/java/com/newtube/mobile/ui/playback"
shell = tv / "MarsTvShell.java"
activity = tv / "MobilePlaybackActivity.java"
menu_src = here / "MarsTvPlayerPanels033.java"
menu_dst = tv / "MarsTvPlayerPanels033.java"

def once(path, old, new):
    content = path.read_text(encoding="utf-8")
    n = content.count(old)
    if n != 1:
        raise SystemExit(f"MT2-033 PANEL source mismatch {path.name}: {n} x {old[:85]!r}")
    path.write_text(content.replace(old, new, 1), encoding="utf-8")

def section(path, start, end, content):
    source = path.read_text(encoding="utf-8")
    if source.count(start) != 1 or source.count(end) != 1:
        raise SystemExit(f"MT2-033 PANEL section mismatch: {path.name}")
    a = source.index(start)
    b = source.index(end, a + len(start))
    if b <= a:
        raise SystemExit("MT2-033 PANEL invalid section")
    path.write_text(source[:a] + content + source[b:], encoding="utf-8")

if not menu_src.is_file() or not shell.is_file() or not activity.is_file():
    raise SystemExit("MT2-033 PANEL versioned source missing")
menu_dst.write_bytes(menu_src.read_bytes())

# Video continues to play on the SAME Media3 surface. Adjust PlayerView bounds,
# never instantiate a second player or pre-fetch a media stream.
once(shell,
    "        default void positionShortsVideo(boolean shorts) { }",
    "        default void positionShortsVideo(boolean shorts) { }\n"
    "        default void positionPanelVideo(boolean docked) { }\n"
    "        default boolean subscribed() { return false; }\n"
    "        default void subscribe() { }\n"
    "        default void audio() { }\n"
    "        default boolean hasAudioAlternatives() { return false; }")
once(shell,
    "    private final MarsTvShortsStage shortsStage;",
    "    private final MarsTvShortsStage shortsStage;\n"
    "    private final MarsTvPlayerPanels033 playerPanels;")
once(shell,
    "    private boolean lastShortsPositioned;",
    "    private boolean lastShortsPositioned;\n"
    "    private boolean lastPanelPositioned;")

# Reuse existing actions. The channel page remains the authoritative
# location for account-dependent subscription and bell changes.
once(shell,
    "        root.bringChildToFront(clock);\n        render();",
    """        root.bringChildToFront(clock);
        playerPanels = new MarsTvPlayerPanels033(activity, root,
                this::render, this::performPanelAction);
        render();""")
once(shell,
    """    public boolean handleBack() {
        if (released || pip) return false;""",
    """    public boolean handleBack() {
        if (released || pip) return false;
        if (playerPanels.isOpen()) {
            playerPanels.close();
            return true;
        }""")
once(shell,
    """    public boolean handle(KeyEvent event) {
        if (released || pip) {
            return false;
        }""",
    """    public boolean handle(KeyEvent event) {
        if (released || pip) {
            return false;
        }
        if (playerPanels.isOpen() && playerPanels.handle(event)) return true;""")

# Reuse actual available icon resources; old transport functionality remains
# available using the remote's media keys and the D-Pad seeking paths.
once(shell, "            R.drawable.mars_tv_ic_previous,",
           "            R.drawable.mars_tv_ic_nav_profile,")
once(shell, "            R.drawable.mars_tv_ic_next,",
           "            com.liskovsoft.smartyoutubetv2.tv.R.drawable.ic_player_playlist_add,")
once(shell, "            R.drawable.mars_tv_ic_info,",
           "            com.liskovsoft.smartyoutubetv2.tv.R.drawable.ic_watch_comment,")
once(shell, "            R.drawable.mars_tv_ic_fullscreen",
           "            R.drawable.mars_tv_ic_info")
once(shell,
    """            "Gefällt mir", "Gefällt mir nicht", "Vorheriges", "10 Sekunden zurück",
            "Pause oder Wiedergabe", "10 Sekunden vor", "Nächstes", "Untertitel",
            "Videoinfos", "Videoqualität", "Vollbild" """.rstrip(),
    """            "Gefällt mir", "Gefällt mir nicht", "Kanal", "10 Sekunden zurück",
            "Pause oder Wiedergabe", "10 Sekunden vor", "Merken", "Untertitel",
            "Kommentare", "Einstellungen", "Mehr" """.rstrip())

# A single reusable menu state machine; all actions go through the existing
# Activity callbacks (rating, channel, save, comments, captions, quality, etc.).
section(shell, "    private void executeControl() {",
        "    private void activateRail() {",
"""    private void openPlayerPanel(String kind) {
        playerPanels.setCapabilities(actions.subscribed(), actions.hasAudioAlternatives());
        playerPanels.open(kind, actions.currentVideo());
    }

    private void executeControl() {
        switch (controlPosition) {
            case CONTROL_LIKE:
                actions.like();
                break;
            case CONTROL_DISLIKE:
                actions.dislike();
                break;
            case CONTROL_PREVIOUS:
                openPlayerPanel("Kanal");
                break;
            case CONTROL_REWIND:
                seek(-10_000L);
                break;
            case CONTROL_PLAY:
                actions.toggle();
                break;
            case CONTROL_FORWARD:
                seek(10_000L);
                break;
            case CONTROL_NEXT:
                openPlayerPanel("Merken");
                break;
            case CONTROL_CC:
                openPlayerPanel("Einstellungen");
                break;
            case CONTROL_INFO:
                openPlayerPanel("Kommentare");
                break;
            case CONTROL_HD:
                openPlayerPanel("Einstellungen");
                break;
            case CONTROL_FULLSCREEN:
                openPlayerPanel("Mehr");
                break;
            default:
                break;
        }
    }

    private void performPanelAction(String id) {
        switch (id) {
            case "channel": actions.channel(); break;
            case "subscribe": actions.subscribe(); break;
            case "audio": actions.audio(); break;
            case "comments": actions.comments(); break;
            case "save": actions.save(); break;
            case "like": actions.like(); break;
            case "dislike": actions.dislike(); break;
            case "quality": actions.quality(); break;
            case "captions": actions.captions(); break;
            case "speed": actions.speed(); break;
            case "no_interest": actions.noInterest(); break;
            case "fullscreen": actions.fullscreen(); break;
            case "info":
                infoOpen = true;
                updateInfo();
                break;
            default:
                return;
        }
        render();
    }

""")

once(shell,
    """        boolean shortsMode = actions.isShortsPlayback();
        if (lastShortsPositioned != (shortsMode && !pip)) {""",
    """        boolean shortsMode = actions.isShortsPlayback();
        if (shortsMode && playerPanels.isOpen()) playerPanels.close();
        boolean dock = !pip && !shortsMode && playerPanels.isOpen();
        if (lastPanelPositioned != dock) {
            lastPanelPositioned = dock;
            actions.positionPanelVideo(dock);
        }
        if (lastShortsPositioned != (shortsMode && !pip)) {""")
once(shell,
    """        boolean panelVisible = !pip && !shortsMode
                && focus != Focus.VIDEO && focus != Focus.RAIL;""",
    """        boolean panelVisible = !pip && !shortsMode && !playerPanels.isOpen()
                && focus != Focus.VIDEO && focus != Focus.RAIL;""")
once(shell,
    """        clock.setVisibility(!pip && !shortsMode && (panelVisible || focus == Focus.RAIL || infoOpen)""",
    """        clock.setVisibility(!pip && !shortsMode && !playerPanels.isOpen()
                && (panelVisible || focus == Focus.RAIL || infoOpen)""")
once(shell,
    """    public void hideUi() {
        cancelHold();""",
    """    public void hideUi() {
        if (playerPanels.isOpen()) playerPanels.close();
        cancelHold();""")
once(shell,
    """        released = true;
        cancelSeekHold();""",
    """        released = true;
        if (lastPanelPositioned) actions.positionPanelVideo(false);
        lastPanelPositioned = false;
        playerPanels.dispose();
        cancelSeekHold();""")

# Existing Activity callback is the only holder of the current PlayerView:
# move it on left when the right panel is open; restore full-screen on close.
once(activity,
    "    @androidx.annotation.Nullable private MarsTvShell mMarsTvShell;",
    "    @androidx.annotation.Nullable private MarsTvShell mMarsTvShell;\n"
    "    private int mMarsTvPanelPreviousResizeMode = -1;")
once(activity,
    "                    @Override public void channel() { openCurrentChannel(); }",
    """                    @Override public void channel() { openCurrentChannel(); }
                    @Override public boolean subscribed() {
                        return getButtonState(R.id.action_subscribe) == BUTTON_ON;
                    }
                    @Override public void subscribe() { onSubscribeTapped(); }
                    @Override public void audio() { showAudioTrackSheet(); }
                    @Override public boolean hasAudioAlternatives() {
                        return audioTrackChoices().size() > 1;
                    }""")
once(activity,
    "                    @Override public void positionShortsVideo(boolean enabled) {",
    """                    @Override public void positionPanelVideo(boolean enabled) {
                        if (mPlayerView == null) return;
                        if (com.newtube.mobile.ui.common.MarsShortsSession
                                .isActiveFor(getVideo()) && enabled) return;
                        android.view.ViewGroup.LayoutParams raw = mPlayerView.getLayoutParams();
                        if (!(raw instanceof android.widget.FrameLayout.LayoutParams)) return;
                        android.widget.FrameLayout.LayoutParams lp =
                                (android.widget.FrameLayout.LayoutParams) raw;
                        mPlayerView.animate().cancel();
                        mPlayerView.setTranslationX(0f);
                        if (enabled) {
                            if (mMarsTvPanelPreviousResizeMode < 0)
                                mMarsTvPanelPreviousResizeMode = mPlayerView.getResizeMode();
                            int w = mVideoArea != null && mVideoArea.getWidth() > 0
                                    ? mVideoArea.getWidth()
                                    : getResources().getDisplayMetrics().widthPixels;
                            int h = mVideoArea != null && mVideoArea.getHeight() > 0
                                    ? mVideoArea.getHeight()
                                    : getResources().getDisplayMetrics().heightPixels;
                            float density = getResources().getDisplayMetrics().density;
                            lp.width = Math.max(1, Math.round(w * 0.54f));
                            lp.height = Math.max(1, Math.round(h * 0.54f));
                            lp.gravity = android.view.Gravity.LEFT
                                    | android.view.Gravity.CENTER_VERTICAL;
                            lp.leftMargin = Math.round(24f * density);
                            lp.topMargin = 0;
                            mPlayerView.setLayoutParams(lp);
                            mPlayerView.setResizeMode(
                                    androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT);
                            mPlayerView.setOutlineProvider(new android.view.ViewOutlineProvider() {
                                @Override public void getOutline(android.view.View v,
                                        android.graphics.Outline outline) {
                                    outline.setRoundRect(0, 0, v.getWidth(), v.getHeight(),
                                            14f * v.getResources().getDisplayMetrics().density);
                                }
                            });
                            mPlayerView.setClipToOutline(true);
                            if (mVideoArea != null) mVideoArea.setBackgroundColor(0xFF081729);
                        } else {
                            lp.width = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
                            lp.height = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
                            lp.gravity = android.view.Gravity.CENTER;
                            lp.leftMargin = 0;
                            lp.topMargin = 0;
                            mPlayerView.setLayoutParams(lp);
                            mPlayerView.setClipToOutline(false);
                            mPlayerView.setOutlineProvider(
                                    android.view.ViewOutlineProvider.BACKGROUND);
                            if (mMarsTvPanelPreviousResizeMode >= 0) {
                                mPlayerView.setResizeMode(mMarsTvPanelPreviousResizeMode);
                                mMarsTvPanelPreviousResizeMode = -1;
                            }
                            if (mVideoArea != null) mVideoArea.setBackgroundColor(
                                    android.graphics.Color.BLACK);
                        }
                    }
                    @Override public void positionShortsVideo(boolean enabled) {""")

content = shell.read_text(encoding="utf-8")
assert "new MarsTvPlayerPanels033" in content
assert "actions.positionPanelVideo(dock)" in content
assert "return handleBack();" in content
menu_text = menu_src.read_text(encoding="utf-8")
assert "new ExoPlayer" not in menu_text and "new PlayerView" not in menu_text
assert "getButtonState(R.id.action_subscribe)" in activity.read_text(encoding="utf-8")
assert "showAudioTrackSheet()" in activity.read_text(encoding="utf-8")
print("MT2-033 PANELS: TV right-side menu routes, one PlayerView dock, DPAD/BACK focus restored.")
