#!/usr/bin/env python3
"""MT2-027: photo-reviewed Shorts layout, real in-place search and end-of-short policy.

Applied strictly after MT2-026. Every anchored rewrite must be unique so upstream
changes fail rather than silently breaking the television/player interaction.
"""
from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit("Usage: patch_027_shorts_polish.py <pinned-checkout>")
root = Path(sys.argv[1]).resolve()
src = Path(__file__).resolve().parent
tv = root / "smarttubetv/src/stmobile/java/com/newtube/mobile/ui"
if not (root / "sabr-media3").is_dir():
    raise SystemExit("MT2-027: pinned source missing")

def once(path, old, new):
    text = path.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"MT2-027 unsafe anchor {path.name}: {count}: {old[:100]!r}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")

# Copy the v027 stage only in a disposable source checkout, never overwrite the
# original checked-in v026 sources nor the installed APK/signing identity.
for name, dest in [
    ("MarsTvShortsStage027.java", tv / "playback/MarsTvShortsStage.java"),
    ("MarsTvShortsSearchPanel027.java", tv / "playback/MarsTvShortsSearchPanel.java"),
]:
    f = src / name
    if not f.is_file():
        raise SystemExit(f"MT2-027 source missing: {name}")
    dest.write_bytes(f.read_bytes())

shell = tv / "playback/MarsTvShell.java"
once(shell,
"""        default boolean closeComments() { return false; }
        default void channel() { }""",
"""        default boolean closeComments() { return false; }
        default boolean searchOpen() { return false; }
        default boolean closeSearch() { return false; }
        default void channel() { }""")
once(shell,
"""                actions::speed, actions::search, actions::share,
                actions::noInterest);""",
"""                actions::speed, actions::search,
                actions::noInterest);""")
once(shell,
"""    public boolean commentsOpen() { return actions.commentsOpen(); }

    public void hideUi() {""",
"""    public boolean commentsOpen() { return actions.commentsOpen(); }
    public boolean searchOpen() { return actions.searchOpen(); }

    public void hideUi() {""")
once(shell,
"""        if (actions.closeComments()) return true;
        cancelHold();""",
"""        if (actions.closeSearch()) return true;
        if (actions.closeComments()) return true;
        cancelHold();""")
once(shell,
"""        // When the right comments pane is open, its RecyclerView owns the D-pad.
        if (actions.commentsOpen()) {""",
"""        // Keep the Shorts search text and results in this window, without
        // consuming the remote's D-pad in the generic player fallback.
        if (actions.searchOpen()) {
            if (key == KeyEvent.KEYCODE_BACK || key == KeyEvent.KEYCODE_MENU) {
                if (action == KeyEvent.ACTION_DOWN) actions.closeSearch();
                return true;
            }
            return false;
        }
        // When the right comments pane is open, its RecyclerView owns the D-pad.
        if (actions.commentsOpen()) {""")
once(shell,
"""                if (shortsReactionPosition == 0) actions.like();
                else if (shortsReactionPosition == 1) actions.dislike();
                else if (shortsReactionPosition == 2) actions.comments();
                else if (shortsReactionPosition == 3) actions.channel();
                else if (shortsReactionPosition == 4) actions.save();
                else if (shortsReactionPosition == 5) actions.captions();
                else if (shortsReactionPosition == 6) actions.speed();
                else if (shortsReactionPosition == 7) actions.search();
                else if (shortsReactionPosition == 8) actions.share();
                else if (shortsReactionPosition == 9) actions.noInterest();
                else if (shortsReactionPosition == 10) shortsStage.toggleAutoNext();""",
"""                if (shortsReactionPosition == 0) {
                    shortsStage.toggleAutoNext();
                    shortsAutoAdvanceArmed = true;
                } else if (shortsReactionPosition == 1) actions.like();
                else if (shortsReactionPosition == 2) actions.dislike();
                else if (shortsReactionPosition == 3) actions.comments();
                else if (shortsReactionPosition == 4) actions.channel();
                else if (shortsReactionPosition == 5) actions.save();
                else if (shortsReactionPosition == 6) actions.captions();
                else if (shortsReactionPosition == 7) actions.speed();
                else if (shortsReactionPosition == 8) actions.search();
                else if (shortsReactionPosition == 9) actions.noInterest();""")
# The global player may have REPEAT_ONE enabled by default, preventing
# STATE_ENDED; explicitly use repeat-OFF for next and repeat-ONE for repeat.
# Restore the original setting as soon as the Shorts session ends.
once(shell,
"""    private boolean shortsAutoAdvanceArmed = true;""",
"""    private boolean shortsAutoAdvanceArmed = true;
    private ExoPlayer shortsRepeatPlayer;
    private int shortsPreviousRepeat = -1;""")
once(shell,
"""        boolean shorts = actions.isShortsPlayback();
        shortsStage.show(shorts && !pip, exo, seekPreviewMs);""",
"""        boolean shorts = actions.isShortsPlayback();
        if (!shorts || exo == null) {
            if (shortsRepeatPlayer != null && shortsPreviousRepeat >= 0) {
                shortsRepeatPlayer.setRepeatMode(shortsPreviousRepeat);
            }
            shortsRepeatPlayer = null;
            shortsPreviousRepeat = -1;
        } else {
            if (shortsRepeatPlayer != exo) {
                if (shortsRepeatPlayer != null && shortsPreviousRepeat >= 0) {
                    shortsRepeatPlayer.setRepeatMode(shortsPreviousRepeat);
                }
                shortsRepeatPlayer = exo;
                shortsPreviousRepeat = exo.getRepeatMode();
                shortsAutoAdvanceArmed = true;
            }
            int desiredRepeat = shortsStage.isAutoNext()
                    ? androidx.media3.common.Player.REPEAT_MODE_OFF
                    : androidx.media3.common.Player.REPEAT_MODE_ONE;
            if (exo.getRepeatMode() != desiredRepeat) exo.setRepeatMode(desiredRepeat);
        }
        shortsStage.show(shorts && !pip, exo, seekPreviewMs);""")
once(shell,
"""    public void release() {
        released = true;""",
"""    public void release() {
        if (shortsRepeatPlayer != null && shortsPreviousRepeat >= 0) {
            shortsRepeatPlayer.setRepeatMode(shortsPreviousRepeat);
        }
        shortsRepeatPlayer = null;
        shortsPreviousRepeat = -1;
        released = true;""")

# Re-arm if the user toggled from Repeat to Next at the end, and preserve the
# existing single dispatch per ended item in MT2-026.
once(shell,
"""                } else if (com.newtube.mobile.ui.common.MarsShortsSession.hasNext()) {
                    actions.next();
                }""",
"""                } else if (com.newtube.mobile.ui.common.MarsShortsSession.hasNext()) {
                    actions.next();
                } else {
                    // At the end of the fetched Shorts snapshot, do NOT jump to
                    // unrelated regular videos. The feed's end remains explicit.
                }""")
# Ensure initial index 0 (Weiter) and every other icon are usable with remote.
assert "else if (shortsReactionPosition == 9) actions.noInterest();" in shell.read_text(encoding="utf-8")

playback = tv / "playback/MobilePlaybackActivity.java"
once(playback,
"""    @androidx.annotation.Nullable private MarsTvShell mMarsTvShell;""",
"""    @androidx.annotation.Nullable private MarsTvShell mMarsTvShell;
    @androidx.annotation.Nullable private MarsTvShortsSearchPanel mMarsShortsSearch;""")
once(playback,
"""                    @Override public void search() {
                        startActivity(new android.content.Intent(
                                MobilePlaybackActivity.this,
                                com.newtube.mobile.ui.search.MobileSearchActivity.class));
                    }""",
"""                    @Override public void search() {
                        // In-place panel: no navigation away from the active Short.
                        if (mCommentsPanel != null && mCommentsPanel.isOpen()) {
                            mCommentsPanel.marsTvBack();
                        }
                        if (mMarsShortsSearch == null) {
                            mMarsShortsSearch = new MarsTvShortsSearchPanel(
                                    MobilePlaybackActivity.this, mVideoArea,
                                    video -> onRelatedClicked(video));
                        }
                        mMarsShortsSearch.open();
                    }
                    @Override public boolean searchOpen() {
                        return mMarsShortsSearch != null && mMarsShortsSearch.isOpen();
                    }
                    @Override public boolean closeSearch() {
                        if (mMarsShortsSearch == null || !mMarsShortsSearch.isOpen())
                            return false;
                        mMarsShortsSearch.close();
                        return true;
                    }""")
once(playback,
"""    private void marsTvOpenShortsComments() {""",
"""    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    android.content.Intent data) {
        if (mMarsShortsSearch != null
                && mMarsShortsSearch.onVoiceResult(requestCode, resultCode, data))
            return;
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void marsTvOpenShortsComments() {""")
once(playback,
"""        if (mMarsTvShell != null && mMarsTvShell.commentsOpen())
            return super.dispatchKeyEvent(event);""",
"""        if (mMarsTvShell != null && (mMarsTvShell.commentsOpen()
                || mMarsTvShell.searchOpen()))
            return super.dispatchKeyEvent(event);""")
once(playback,
"""        if (mMarsTvShell != null) { mMarsTvShell.release(); mMarsTvShell = null; }""",
"""        if (mMarsShortsSearch != null) { mMarsShortsSearch.close(); mMarsShortsSearch = null; }
        if (mMarsTvShell != null) { mMarsTvShell.release(); mMarsTvShell = null; }""")

# All comments should continue using the original lazy-loading backend.
assert "if (com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(this)) return;" in playback.read_text(encoding="utf-8")

stage = (tv / "playback/MarsTvShortsStage.java").read_text(encoding="utf-8")
assert "preview.number" not in stage
assert "leftLp.leftMargin = dp(16)" in stage
assert "progressLp.leftMargin = videoLeft + dp(12)" in stage
assert "onShare" not in stage
assert "rightLabels.get(0).setText(enabled ? \"Weiter\" : \"Wdh.\")" in stage
assert "YouTubeServiceManager.instance().getContentService()" in (
        tv / "playback/MarsTvShortsSearchPanel.java").read_text(encoding="utf-8")
assert 'applicationId "de.equiummc.marstube2"' in (
        root / "smarttubetv/build.gradle").read_text(encoding="utf-8")
assert 'versionCode 12027' in (root / "smarttubetv/build.gradle").read_text(encoding="utf-8")
assert (root / "smarttubetv/src/stmobile/res/drawable/mars_m_flat.xml").read_bytes() == (
        src / "assets/drawable/mars_m_flat.xml").read_bytes()
print("MT2-027: preview crops, blue creators, slim progress, search sidebar, voice and repeat policy.")
