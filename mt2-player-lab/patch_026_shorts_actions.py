#!/usr/bin/env python3
"""MT2-026: TV-only Shorts action rail, genuinely centered icons, lazy comments.
Applied AFTER patch_025_tv_ui.py. Fail-closed on upstream/source drift.
No new player/decoder, package ID, signing key, auto-updater or BACK regressions.
"""
from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit("Usage: patch_026_shorts_actions.py <pinned-checkout>")
root = Path(sys.argv[1]).resolve()
if not (root / "sabr-media3").is_dir():
    raise SystemExit("MT2-026: pinned checkout missing")
here = Path(__file__).resolve().parent
tv = root / "smarttubetv/src/stmobile/java/com/newtube/mobile/ui"
mobile = root / "smarttubetv/src/stmobile"


def once(path, old, new):
    source = path.read_text(encoding="utf-8")
    count = source.count(old)
    if count != 1:
        raise SystemExit("MT2-026 unsafe anchor count=%s %s: %r"
                         % (count, path.name, old[:90]))
    path.write_text(source.replace(old, new, 1), encoding="utf-8")


def block(path, first, after, replacement):
    source = path.read_text(encoding="utf-8")
    begin = source.find(first)
    end = source.find(after, begin + len(first))
    if begin < 0 or end <= begin or source.count(first) != 1:
        raise SystemExit("MT2-026 cannot isolate %s in %s" % (first, path))
    path.write_text(source[:begin] + replacement + source[end:], encoding="utf-8")


# One static artifact per version. Do not mutate 025 source or the original Mars M.
for source, dest in (
        ("MarsTvRail026.java", tv / "common/MarsTvRail.java"),
        ("MarsTvShortsStage026.java", tv / "playback/MarsTvShortsStage.java")):
    path = here / source
    if not path.is_file() or not dest.is_file():
        raise SystemExit("MT2-026 missing %s" % source)
    dest.write_bytes(path.read_bytes())

shell = tv / "playback/MarsTvShell.java"
once(shell, "    private final List<TextView> railButtons = new ArrayList<>();",
     "    private final List<FrameLayout> railButtons = new ArrayList<>();")
block(shell, "    private void addRailItem(String label, int destination) {",
      "    public void setRecommendations(List<Video> videos) {",
"""    private void addRailItem(String label, int destination) {
        final int index = railButtons.size();
        FrameLayout button = new FrameLayout(activity);
        ImageView glyph = new ImageView(activity);
        int iconDp = destination == 0 ? 32 : 20;
        glyph.setImageResource(com.newtube.mobile.ui.common.MarsTvNavigationTheme.iconFor(destination));
        glyph.setScaleType(ImageView.ScaleType.FIT_CENTER);
        glyph.setColorFilter(WHITE);
        button.addView(glyph, new FrameLayout.LayoutParams(dp(iconDp), dp(iconDp), Gravity.CENTER));
        button.setContentDescription(label);
        button.setBackground(com.newtube.mobile.ui.common.MarsTvNavigationTheme.itemBackground(
                false, activity.getResources().getDisplayMetrics().density));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(com.newtube.mobile.ui.common.MarsTvNavigationTheme.ITEM_HEIGHT_DP));
        lp.topMargin = dp(3);
        railItems.addView(button, lp);
        button.setOnClickListener(v -> { railPosition = index; activateRail(); });
        railButtons.add(button);
        railDestinations.add(new RailItem(label, destination));
    }

""")
once(shell, "TextView home = railButtons.get(railPosition);",
     "FrameLayout home = railButtons.get(railPosition);")
once(shell,
"""            railButtons.get(i).setTextColor(selected ? CYAN : WHITE);
            railButtons.get(i).setCompoundDrawableTintList(
                    android.content.res.ColorStateList.valueOf(
                            selected ? 0xFF68C7FF : WHITE));""",
"""            ((ImageView) railButtons.get(i).getChildAt(0))
                    .setColorFilter(selected ? 0xFF68C7FF : WHITE);""")

once(shell, """        void quality();

        /** Is this playback from the dedicated Shorts feed""",
"""        void quality();
        default void comments() { }
        default boolean commentsOpen() { return false; }
        default boolean closeComments() { return false; }
        default void channel() { }
        default void speed() { }
        default void search() { }
        default void share() { }
        default void noInterest() { }
        default String commentsCount() { return null; }
        default Video currentVideo() { return null; }

        /** Is this playback from the dedicated Shorts feed""")
once(shell,
"""        shortsStage = new MarsTvShortsStage(activity, root, this::render,
                actions::like, actions::dislike, actions::save,
                () -> seek(-5_000L), () -> seek(5_000L));""",
"""        shortsStage = new MarsTvShortsStage(activity, root, this::render,
                actions::like, actions::dislike, actions::save,
                () -> seek(-5_000L), () -> seek(5_000L),
                actions::comments, actions::channel, actions::captions,
                actions::speed, actions::search, actions::share,
                actions::noInterest);""")
once(shell, """        cancelHold();
        cancelSeekHold();
        openRail();
        render();
        return true;
    }

    public boolean handle(KeyEvent event) {""",
"""        if (actions.closeComments()) return true;
        cancelHold();
        cancelSeekHold();
        openRail();
        render();
        return true;
    }

    public boolean handle(KeyEvent event) {""")
once(shell,
"""        int key = event.getKeyCode();
        int action = event.getAction();""",
"""        int key = event.getKeyCode();
        int action = event.getAction();
        // When the right comments pane is open, its RecyclerView owns the D-pad.
        if (actions.commentsOpen()) {
            if (key == KeyEvent.KEYCODE_BACK || key == KeyEvent.KEYCODE_MENU) {
                if (action == KeyEvent.ACTION_DOWN) actions.closeComments();
                return true;
            }
            return false;
        }""")
# 025 restored the proper progress seek; preserve LEFT/RIGHT on the slim bar.
once(shell, "shortsReactionPosition = (shortsReactionPosition + 1) % 2;",
     "shortsReactionPosition = Math.max(0, shortsReactionPosition - 1);")
once(shell, "shortsReactionPosition = (shortsReactionPosition + 1) % 2;",
     "shortsReactionPosition = Math.min(shortsStage.reactionCount() - 1, shortsReactionPosition + 1);")
once(shell, """                        shortsReactionPosition = 1;
                    } else actions.previous();""",
"""                        shortsReactionPosition = shortsStage.reactionCount() - 1;
                    } else actions.previous();""")
once(shell,
"""                    else if (focus == Focus.SHORTS_REACTIONS)
                        focus = Focus.SHORTS_PROGRESS;""",
"""                    else if (focus == Focus.SHORTS_REACTIONS)
                        focus = Focus.SHORTS_PROGRESS;""") if False else None
once(shell,
"""                else if (shortsReactionPosition == 1) actions.dislike();
            } else if (focus == Focus.SHORTS_PROGRESS) {""",
"""                else if (shortsReactionPosition == 1) actions.dislike();
                else if (shortsReactionPosition == 2) actions.comments();
                else if (shortsReactionPosition == 3) actions.channel();
                else if (shortsReactionPosition == 4) actions.save();
                else if (shortsReactionPosition == 5) actions.captions();
                else if (shortsReactionPosition == 6) actions.speed();
                else if (shortsReactionPosition == 7) actions.search();
                else if (shortsReactionPosition == 8) actions.share();
                else if (shortsReactionPosition == 9) actions.noInterest();
                else if (shortsReactionPosition == 10) shortsStage.toggleAutoNext();
            } else if (focus == Focus.SHORTS_PROGRESS) {""")
# The previous 025 branch keeps the obsolete SHORTS_AUTO branch in the OK logic.
# It is not reachable from the new right rail, but is safe to retain.
once(shell,
"""        shortsStage.show(shortsMode && !pip, player.current(), seekPreviewMs);""",
"""        shortsStage.show(shortsMode && !pip, player.current(), seekPreviewMs);
        if (shortsMode) shortsStage.updateCounts(actions.currentVideo(), actions.commentsCount());""")
once(shell,
"""        shortsStage.show(shorts && !pip, exo, seekPreviewMs);""",
"""        shortsStage.show(shorts && !pip, exo, seekPreviewMs);
        if (shorts) shortsStage.updateCounts(actions.currentVideo(), actions.commentsCount());""")
once(shell,
"""                if (shortsStage.isAutoNext()
                        && com.newtube.mobile.ui.common.MarsShortsSession.hasNext()) {
                    actions.next();
                }""",
"""                if (!shortsStage.isAutoNext()) {
                    // Explicit 'Wdh.': repeat the CURRENT Short, not stop at its end.
                    exo.seekTo(0L);
                    exo.play();
                } else if (com.newtube.mobile.ui.common.MarsShortsSession.hasNext()) {
                    actions.next();
                }""")

playback = tv / "playback/MobilePlaybackActivity.java"
once(playback,
"""                    @Override public void quality() {
                        showQualitySheet();
                    }
                    @Override public void beginHold() {""",
"""                    @Override public void quality() {
                        showQualitySheet();
                    }
                    @Override public Video currentVideo() { return getVideo(); }
                    @Override public String commentsCount() {
                        return mWatchCommentsCount == null ? null
                                : mWatchCommentsCount.getText().toString();
                    }
                    @Override public void comments() { marsTvOpenShortsComments(); }
                    @Override public boolean commentsOpen() {
                        return mCommentsPanel != null && mCommentsPanel.isOpen();
                    }
                    @Override public boolean closeComments() {
                        if (mCommentsPanel == null || !mCommentsPanel.isOpen()) return false;
                        mCommentsPanel.close();
                        return true;
                    }
                    @Override public void channel() { openCurrentChannel(); }
                    @Override public void speed() { showSpeedSheet(); }
                    @Override public void search() {
                        startActivity(new android.content.Intent(
                                MobilePlaybackActivity.this,
                                com.newtube.mobile.ui.search.MobileSearchActivity.class));
                    }
                    @Override public void share() { shareCurrentVideo(); }
                    @Override public void noInterest() {
                        com.liskovsoft.smartyoutubetv2.common.app.presenters.dialogs.menu.VideoMenuPresenter
                                .instance(MobilePlaybackActivity.this).showMenu(getVideo());
                    }
                    @Override public void beginHold() {""")

# TV layout: panel is physically moved beside the Shorts action icons; the
# one existing CommentsPanel retains replies, sorting, pagination and Likes.
# No competing comments service or background loading is added.
once(playback,
"""    private void onCommentsEntryClicked() {""",
"""    private void marsTvOpenShortsComments() {
        if (mCommentsPanel == null || mCommentsKey == null || mVideoArea == null) {
            android.widget.Toast.makeText(this, "Keine Kommentare verfügbar",
                    android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        View pane = findViewById(R.id.mobile_comments_panel);
        if (pane != null && pane.getParent() instanceof ViewGroup) {
            ((ViewGroup) pane.getParent()).removeView(pane);
            int width = getResources().getDisplayMetrics().widthPixels;
            int height = getResources().getDisplayMetrics().heightPixels;
            int videoWidth = MarsTvShortsStage.videoWidth(this);
            int controlStart = (width + videoWidth) / 2 + (int) (86f * getResources().getDisplayMetrics().density);
            int sideWidth = Math.max((int) (160f * getResources().getDisplayMetrics().density),
                    width - controlStart - (int) (16f * getResources().getDisplayMetrics().density));
            android.widget.FrameLayout.LayoutParams lp =
                    new android.widget.FrameLayout.LayoutParams(sideWidth,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.Gravity.RIGHT | android.view.Gravity.CENTER_VERTICAL);
            lp.rightMargin = (int) (10f * getResources().getDisplayMetrics().density);
            mVideoArea.addView(pane, lp);
            pane.setElevation(12f * getResources().getDisplayMetrics().density);
            pane.bringToFront();
        }
        onCommentsEntryClicked();
        if (pane != null) {
            View list = pane.findViewById(R.id.comments_list);
            if (list != null) list.post(() -> {
                list.setFocusable(true);
                list.requestFocus();
            });
        }
    }

    private void onCommentsEntryClicked() {""")
once(playback,
"""    private void prefetchComments() {
        if (mCommentsPanel == null || mIsInPip || mIsStopped || isFinishing()) {""",
"""    private void prefetchComments() {
        // Shorts on television: zero comment-list requests before explicit OK.
        if (mMarsTvShell != null
                && com.newtube.mobile.ui.common.MarsShortsSession.isActiveFor(getVideo()))
            return;
        if (mCommentsPanel == null || mIsInPip || mIsStopped || isFinishing()) {""")

# Preserve the app identity, one Media3 surface, old update feeds and original logo.
assert 'applicationId "de.equiummc.marstube2"' in (
        root / "smarttubetv/build.gradle").read_text(encoding="utf-8")
assert 'versionCode 12026' in (root / "smarttubetv/build.gradle").read_text(encoding="utf-8")
assert 'return handleBack();' in shell.read_text(encoding="utf-8")
assert 'if (focus == Focus.SHORTS_PROGRESS) moveSeekPreview(-1);' in shell.read_text(encoding="utf-8")
assert 'mPlayerView.setClipToOutline(true)' in playback.read_text(encoding="utf-8")
assert (mobile / "res/drawable/mars_m_flat.xml").read_bytes() == (
        here / "assets/drawable/mars_m_flat.xml").read_bytes()
print("MT2-026: icon centers, small Shorts controls, lazy side comments and real repeat mode.")
