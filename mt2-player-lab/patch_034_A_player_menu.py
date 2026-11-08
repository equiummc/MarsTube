#!/usr/bin/env python3
"""MT2-034 Punkt A: TV player sidebar, real in-panel track selectors and comments.

Apply only after the signed/tested 0.0.33 patch chain in an isolated checkout.
Fail closed on every source contract mismatch. Never change package or signing.
"""
from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit("Usage: patch_034_A_player_menu.py <isolated-checkout>")
root = Path(sys.argv[1])
here = Path(__file__).resolve().parent
tv = root / "smarttubetv/src/stmobile/java/com/newtube/mobile/ui/playback"
shell = tv / "MarsTvShell.java"
activity = tv / "MobilePlaybackActivity.java"
panel_src = here / "MarsTvPlayerPanels034.java"
panel_dst = tv / panel_src.name

def one(path, old, new):
    src = path.read_text(encoding="utf-8")
    count = src.count(old)
    if count != 1:
        raise SystemExit("MT2-034 FAIL CLOSED %s: %d matches for %r" %
                         (path.name, count, old[:120]))
    path.write_text(src.replace(old, new, 1), encoding="utf-8")

def between(path, first, last, new):
    src = path.read_text(encoding="utf-8")
    if src.count(first) != 1 or src.count(last) != 1:
        raise SystemExit("MT2-034 FAIL CLOSED section %s" % path.name)
    a = src.index(first)
    b = src.index(last, a + len(first))
    if b <= a:
        raise SystemExit("MT2-034 reversed section in " + path.name)
    path.write_text(src[:a] + new + src[b:], encoding="utf-8")

if not panel_src.is_file() or not shell.is_file() or not activity.is_file():
    raise SystemExit("MT2-034: missing versioned TV sources")
panel_dst.write_bytes(panel_src.read_bytes())

one(shell, "    private final MarsTvPlayerPanels033 playerPanels;",
    "    private final MarsTvPlayerPanels034 playerPanels;")
one(shell, "        default void positionPanelVideo(boolean docked) { }",
    """        default void positionPanelVideo(boolean docked) { }
        default java.util.List<String> playerMenuChoices(String page) {
            return java.util.Collections.emptyList();
        }
        default void playerMenuChoose(String page, int choice, String videoId) { }
        default void captionStyle() { }
        default void watchLater() { }""")
one(shell,
    """        playerPanels = new MarsTvPlayerPanels033(activity, root,
                this::render, this::performPanelAction);""",
    """        playerPanels = new MarsTvPlayerPanels034(activity, root, this::render,
                new MarsTvPlayerPanels034.Action() {
                    @Override public void invoke(String id) {
                        performPanelAction(id);
                    }
                    @Override public java.util.List<String> choices(String page) {
                        return actions.playerMenuChoices(page);
                    }
                    @Override public void choose(String page, int index, String videoId) {
                        actions.playerMenuChoose(page, index, videoId);
                    }
                    @Override public boolean subscribed() {
                        return actions.subscribed();
                    }
                });""")
one(shell,
    """        if (playerPanels.isOpen()) {
            playerPanels.close();
            return true;
        }""",
    """        if (playerPanels.isOpen()) {
            playerPanels.back();
            return true;
        }""")
one(shell, '            case "audio": actions.audio(); break;',
    '            case "audio": actions.audio(); break;\n'
    '            case "caption_style": actions.captionStyle(); break;\n'
    '            case "watch_later": actions.watchLater(); break;')
one(shell,
    """        if (shortsMode && playerPanels.isOpen()) playerPanels.close();
        boolean dock = !pip && !shortsMode && playerPanels.isOpen();""",
    """        com.liskovsoft.smartyoutubetv2.common.app.models.data.Video currentPanelVideo =
                actions.currentVideo();
        String panelId = currentPanelVideo == null ? null : currentPanelVideo.videoId;
        if (playerPanels.isOpen() && (shortsMode || !android.text.TextUtils.equals(
                panelId, playerPanels.videoId()))) playerPanels.close();
        boolean dock = !pip && !shortsMode
                && (playerPanels.isOpen() || actions.commentsOpen());""")
# The player uses nine actions; D-pad/hardware seeks remain available but the
# redundant rewind/forward buttons around Play/Pause are removed.
between(shell,
    "    private static final int[] CONTROL_ICONS = {",
    "    private final Activity activity;",
    """    private static final int[] CONTROL_ICONS = {
            R.drawable.mars_tv_ic_like,
            R.drawable.mars_tv_ic_dislike,
            R.drawable.mars_tv_ic_nav_profile,
            com.liskovsoft.smartyoutubetv2.tv.R.drawable.ic_watch_comment,
            R.drawable.mars_tv_ic_play,
            com.liskovsoft.smartyoutubetv2.tv.R.drawable.ic_player_playlist_add,
            R.drawable.mars_tv_ic_cc,
            R.drawable.mars_tv_ic_hd_settings,
            R.drawable.mars_tv_ic_info
    };

    private static final String[] CONTROL_LABELS = {
            "Gefällt mir", "Gefällt mir nicht", "Kanal", "Kommentare",
            "Pause oder Wiedergabe", "Merken", "Untertitel",
            "Einstellungen", "Weitere Optionen"
    };

""")
between(shell,
    "    private void executeControl() {",
    "    private void performPanelAction(String id) {",
    """    private void executeControl() {
        switch (controlPosition) {
            case CONTROL_LIKE: actions.like(); break;
            case CONTROL_DISLIKE: actions.dislike(); break;
            case CONTROL_PREVIOUS: openPlayerPanel("Kanal"); break;
            case CONTROL_REWIND: openPlayerPanel("Kommentare"); break;
            case CONTROL_PLAY: actions.toggle(); break;
            case CONTROL_FORWARD: openPlayerPanel("Merken"); break;
            case CONTROL_NEXT: openPlayerPanel("Untertitel"); break;
            case CONTROL_CC: openPlayerPanel("Einstellungen"); break;
            case CONTROL_INFO: openPlayerPanel("Mehr"); break;
            default: break;
        }
    }

""")
# A proper right-hand, lazily loaded comments pane for ordinary videos:
# reuse the already existing CommentsPanel and one existing Media3 PlayerView.
one(activity,
    "                    @Override public void comments() { marsTvOpenShortsComments(); }",
    """                    @Override public void comments() { marsTvOpenPlayerComments(); }
                    @Override public java.util.List<String> playerMenuChoices(String page) {
                        return marsTvMenuChoices(page);
                    }
                    @Override public void playerMenuChoose(String page, int index, String videoId) {
                        marsTvChooseMenuOption(page, index, videoId);
                    }
                    @Override public void captionStyle() {
                        SubtitleSettingsPresenter.instance(MobilePlaybackActivity.this).show();
                    }
                    @Override public void watchLater() { marsTvAddToWatchLater(); }""")
one(activity,
    "    private void marsTvOpenShortsComments() {",
    """    private void marsTvOpenPlayerComments() {
        if (com.newtube.mobile.ui.common.MarsShortsSession.isActiveFor(getVideo())) {
            marsTvOpenShortsComments();
            return;
        }
        if (mCommentsPanel == null || mCommentsKey == null || mVideoArea == null) {
            android.widget.Toast.makeText(this, "Keine Kommentare verfügbar",
                    android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        View pane = findViewById(R.id.mobile_comments_panel);
        if (pane != null && pane.getParent() instanceof ViewGroup) {
            ((ViewGroup) pane.getParent()).removeView(pane);
            int screen = getResources().getDisplayMetrics().widthPixels;
            int sideWidth = Math.round(screen * 0.43f);
            android.widget.FrameLayout.LayoutParams lp =
                    new android.widget.FrameLayout.LayoutParams(sideWidth,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.Gravity.RIGHT | android.view.Gravity.TOP);
            mVideoArea.addView(pane, lp);
            if (pane instanceof CommentsPanelLayout) {
                ((CommentsPanelLayout) pane).setMarsTvSideWidth(sideWidth);
            }
            pane.setElevation(12f * getResources().getDisplayMetrics().density);
            pane.bringToFront();
        }
        mCommentsPanel.marsTvKeepOpen(true);
        onCommentsEntryClicked();
        if (pane != null) {
            View list = pane.findViewById(R.id.comments_list);
            if (list != null) list.post(() -> {
                list.setFocusable(true);
                list.requestFocus();
            });
        }
    }

    private void marsTvOpenShortsComments() {""")
# The picker model is generated from the genuine track data already exposed by
# NewTube/Media3, not a fake fixed selection or independent playback service.
one(activity, "    private void showQualitySheet() {",
    r'''    private java.util.List<com.liskovsoft.smartyoutubetv2.common.app.models.data.Video> marsUnused() {
        return java.util.Collections.emptyList();
    }

    /** The same official-service Watch Later path used by the existing video menu. */
    private void marsTvAddToWatchLater() {
        Video target = getVideo();
        if (target == null || !target.hasVideo()) return;
        if (WatchActionFeedback.blockIfSignedOut(this, R.string.msg_sign_in_to_save)) return;
        com.liskovsoft.smartyoutubetv2.common.misc.MediaServiceManager
                .instance().addToWatchLaterPlaylist(target);
        com.liskovsoft.smartyoutubetv2.common.app.presenters.BrowsePresenter
                .instance(this).onPlaylistEdited();
    }

    private java.util.List<FormatItem> marsTvCaptionTracks() {
        java.util.List<FormatItem> manual = new ArrayList<>();
        java.util.List<FormatItem> automatic = new ArrayList<>();
        java.util.List<FormatItem> all = getSubtitleFormats();
        if (all != null) for (FormatItem item : all) {
            if (!isCaptionTrack(item)) continue;
            if (SubtitleTrack.isAuto(item.getLanguage())) automatic.add(item);
            else manual.add(item);
        }
        moveLastUsedCaptionsFirst(manual);
        moveLastUsedCaptionsFirst(automatic);
        manual.addAll(automatic);
        return manual;
    }

    private java.util.LinkedHashMap<String, FormatItem> marsTvQualityRungs() {
        java.util.LinkedHashMap<String, FormatItem> map = new java.util.LinkedHashMap<>();
        List<FormatItem> formats = getVideoFormats();
        if (formats != null) for (FormatItem item : formats) {
            if (item == null || item.getHeight() <= 0) continue;
            String label = qualityLabel(item);
            if (!map.containsKey(label)) map.put(label, item);
        }
        return map;
    }

    private java.util.List<String> marsTvMenuChoices(String page) {
        java.util.List<String> result = new ArrayList<>();
        switch (page) {
            case "Qualität": {
                PlayerData data = PlayerData.instance(this);
                FormatItem temp = data.getTempVideoFormat();
                FormatItem current = temp != null ? temp : data.getFormat(FormatItem.TYPE_VIDEO);
                boolean auto = isAutoFormat(current);
                result.add((auto ? "✓  " : "") + "Automatisch");
                for (java.util.Map.Entry<String, FormatItem> rung :
                        marsTvQualityRungs().entrySet()) {
                    result.add((!auto && rung.getValue().isSelected() ? "✓  " : "")
                            + rung.getKey());
                }
                break;
            }
            case "Benachrichtigungen": {
                Video current = getVideo();
                if (current != null && current.notificationStates != null) {
                    for (com.liskovsoft.mediaserviceinterfaces.data.NotificationState state :
                            current.notificationStates) {
                        result.add((state.isSelected() ? "✓  " : "") + state.getTitle());
                    }
                }
                break;
            }
            case "Audiotrack":
                for (AudioTrackChoices.Choice choice : audioTrackChoices()) {
                    result.add((choice.selected ? "✓  " : "") + choice.label);
                }
                break;
            case "Untertitel":
                result.add((!areCaptionsOn() ? "✓  " : "") + "Aus");
                for (FormatItem item : marsTvCaptionTracks()) {
                    result.add((item.isSelected() ? "✓  " : "") + captionLabel(item));
                }
                break;
            case "Tempo": {
                float current = mExoPlayerController != null
                        ? mExoPlayerController.getSpeed() : 1f;
                for (float speed : SPEED_PRESETS) {
                    result.add((Helpers.floatEquals(speed, current) ? "✓  " : "")
                            + speedLabel(speed));
                }
                break;
            }
            default: break;
        }
        return result;
    }

    private void marsTvChooseMenuOption(String page, int index, String videoId) {
        Video video = getVideo();
        if (video == null || !TextUtils.equals(videoId, video.videoId)
                || index < 0) return;
        switch (page) {
            case "Qualität": {
                PlayerData data = PlayerData.instance(this);
                if (index == 0) {
                    data.setTempVideoFormat(null);
                    FormatItem auto = data.getDefaultVideoFormat();
                    setFormat(auto);
                    data.setFormat(auto);
                } else {
                    java.util.List<FormatItem> rungs =
                            new ArrayList<>(marsTvQualityRungs().values());
                    if (index > rungs.size()) return;
                    FormatItem picked = rungs.get(index - 1);
                    setFormat(picked);
                    if (isAutoFormat(data.getFormat(FormatItem.TYPE_VIDEO)))
                        data.setTempVideoFormat(picked);
                    else data.setFormat(picked);
                }
                break;
            }
            case "Benachrichtigungen": {
                if (video.notificationStates == null
                        || index >= video.notificationStates.size()) return;
                com.liskovsoft.mediaserviceinterfaces.data.NotificationState state =
                        video.notificationStates.get(index);
                com.liskovsoft.smartyoutubetv2.common.misc.MediaServiceManager
                        .instance().setNotificationState(state, error ->
                                runOnUiThread(() -> android.widget.Toast.makeText(
                                        this, "Benachrichtigungen: " + error.getLocalizedMessage(),
                                        android.widget.Toast.LENGTH_LONG).show()));
                video.isSubscribed = true;
                setButtonState(R.id.action_subscribe, BUTTON_ON);
                break;
            }
            case "Audiotrack": {
                List<AudioTrackChoices.Choice> choices = audioTrackChoices();
                if (index >= choices.size()) return;
                FormatItem picked = choices.get(index).item;
                setFormat(picked);
                PlayerData data = PlayerData.instance(this);
                data.setVideoAudioFormat(videoId, picked);
                data.setTempAudioFormat(null);
                break;
            }
            case "Untertitel": {
                if (index == 0) applyCaptionFormat(FormatItem.SUBTITLE_NONE);
                else {
                    List<FormatItem> choices = marsTvCaptionTracks();
                    if (index > choices.size()) return;
                    applyCaptionFormat(choices.get(index - 1));
                }
                break;
            }
            case "Tempo":
                if (index < SPEED_PRESETS.length) applySpeed(SPEED_PRESETS[index]);
                break;
            default: break;
        }
    }

    private void showQualitySheet() {''')
# Remove redundant helper if present after inserting exact type-independent code.
one(activity,
    '''    private java.util.List<com.liskovsoft.smartyoutubetv2.common.app.models.data.Video> marsUnused() {
        return java.util.Collections.emptyList();
    }

''', "")
gradle = root / "smarttubetv/build.gradle"
one(gradle, "versionCode 12033", "versionCode 12034")
one(gradle, 'versionName "0.0.33"', 'versionName "0.0.34"')
for check in ("new MarsTvPlayerPanels034", "playerPanels.back()",
              "marsTvMenuChoices(page)", "marsTvChooseMenuOption(page, index, videoId)"):
    if check not in (shell.read_text(encoding="utf-8")
                     + activity.read_text(encoding="utf-8")):
        raise SystemExit("MT2-034 missing invariant: " + check)
print("MT2-034 A: 9 floating actions, true nested menus, real quality/audio/caption/speed choices, comments dock.")
