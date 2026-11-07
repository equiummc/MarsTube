#!/usr/bin/env python3
"""MT2-027: accurate TV Shorts preview layout, in-place search, and EOS advance.
Applies after MT2-026, only in the pinned reference build tree.
No install/uninstall, app-ID, signing, feed, back-navigation or decoder changes.
"""
from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit("Usage: patch_027_shorts_workspace.py <pinned-checkout>")
root = Path(sys.argv[1]).resolve()
if not (root / "sabr-media3").is_dir():
    raise SystemExit("MT2-027: pinned checkout missing")
here = Path(__file__).resolve().parent
tv = root / "smarttubetv/src/stmobile/java/com/newtube/mobile/ui"
playback = tv / "playback/MobilePlaybackActivity.java"
shell = tv / "playback/MarsTvShell.java"

def once(path, before, after):
    source = path.read_text(encoding="utf-8")
    count = source.count(before)
    if count != 1:
        raise SystemExit("MT2-027 unsafe anchor count=%s in %s: %r"
                         % (count, path.name, before[:100]))
    path.write_text(source.replace(before, after, 1), encoding="utf-8")

for src, dest in (
    ("MarsTvShortsStage027.java", tv / "playback/MarsTvShortsStage.java"),
    ("MarsTvShortsSearchPanel027.java", tv / "playback/MarsTvShortsSearchPanel.java"),
):
    source_file = here / src
    if not source_file.is_file():
        raise SystemExit("MT2-027 missing source: " + src)
    dest.parent.mkdir(parents=True, exist_ok=True)
    dest.write_bytes(source_file.read_bytes())

once(shell,
    """                actions::speed, actions::search, actions::share,
                actions::noInterest);""",
    """                actions::speed, actions::search,
                actions::noInterest);""")

# Visually reordered indices must be identical for D-pad OK.
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
    """                if (shortsReactionPosition == 0) shortsStage.toggleAutoNext();
                else if (shortsReactionPosition == 1) actions.like();
                else if (shortsReactionPosition == 2) actions.dislike();
                else if (shortsReactionPosition == 3) actions.comments();
                else if (shortsReactionPosition == 4) actions.channel();
                else if (shortsReactionPosition == 5) actions.save();
                else if (shortsReactionPosition == 6) actions.captions();
                else if (shortsReactionPosition == 7) actions.speed();
                else if (shortsReactionPosition == 8) actions.search();
                else if (shortsReactionPosition == 9) actions.noInterest();""")

once(shell,
    """        default void search() { }
        default void share() { }""",
    """        default void search() { }
        default boolean searchOpen() { return false; }
        default boolean closeSearch() { return false; }""")

# A search overlay must have first chance to close on BACK, before the existing
# comments and universal player BACK handlers.
once(shell, """        if (actions.closeComments()) return true;
        cancelHold();""",
     """        if (actions.closeSearch()) return true;
        if (actions.closeComments()) return true;
        cancelHold();""")

once(shell,
    """        // When the right comments pane is open, its RecyclerView owns the D-pad.
        if (actions.commentsOpen()) {""",
    """        // Search input / results own all D-pad keys until the overlay closes.
        if (actions.searchOpen()) {
            if (key == KeyEvent.KEYCODE_BACK || key == KeyEvent.KEYCODE_MENU) {
                if (action == KeyEvent.ACTION_DOWN) actions.closeSearch();
                return true;
            }
            return false;
        }
        // When the right comments pane is open, its RecyclerView owns the D-pad.
        if (actions.commentsOpen()) {""")

# Replace ticker-only end handling. OnPlaybackStateChanged END is deterministic,
# and armed READY->END can fire exactly one action. Manual Next is unchanged.
once(shell,
    """        if (shorts && exo != null
                && exo.getPlaybackState() == androidx.media3.common.Player.STATE_ENDED) {
            if (shortsAutoAdvanceArmed) {
                shortsAutoAdvanceArmed = false;
                if (!shortsStage.isAutoNext()) {
                    // Explicit 'Wdh.': repeat the CURRENT Short, not stop at its end.
                    exo.seekTo(0L);
                    exo.play();
                } else if (com.newtube.mobile.ui.common.MarsShortsSession.hasNext()) {
                    actions.next();
                }
            }
        } else if (exo != null
                && exo.getPlaybackState() == androidx.media3.common.Player.STATE_READY) {
            shortsAutoAdvanceArmed = true;
        }""",
    """        // Media3 state events handle EOS directly; the timer only paints UI.
        if (shorts && exo != null
                && exo.getPlaybackState() == androidx.media3.common.Player.STATE_READY) {
            shortsAutoAdvanceArmed = true;
        }""")

once(shell,
    """    private void updateInfo() {""",
    """    /** The host Activity must defer D-pad input to the in-place search overlay. */
    public boolean searchOpen() {
        return actions.searchOpen();
    }

    public boolean isShortsPlayback() {
        return !released && !pip && actions.isShortsPlayback();
    }

    /** Apply the same FIT/9:16 frame as soon as a new source has dimensions. */
    public void ensureShortsFrame() {
        if (isShortsPlayback()) actions.positionShortsVideo(true);
    }

    public void onShortsPlaybackReady() {
        if (isShortsPlayback()) shortsAutoAdvanceArmed = true;
    }

    /** Once per READY->ENDED transition. No generic recommendation fallback. */
    public void onShortsPlaybackEnded() {
        if (!isShortsPlayback() || !shortsAutoAdvanceArmed) return;
        ExoPlayer exo = player.current();
        if (exo == null || exo.getPlaybackState() !=
                androidx.media3.common.Player.STATE_ENDED) return;
        shortsAutoAdvanceArmed = false;
        if (shortsStage.isAutoNext()
                && com.newtube.mobile.ui.common.MarsShortsSession.hasNext()) {
            actions.next(); // marsTvStepShort: only the same scoped Shorts feed
        } else {
            // "Wdh." repeats the current item; an exhausted feed also stays safe.
            exo.seekTo(0L);
            exo.play();
        }
    }

    private void updateInfo() {""")

# The native activity contains exactly one real Media3 listener (normal and
# Shorts share it). Reapply FIT when video size becomes known, not a second later.
once(playback,
    """                mVideoAspect = videoSize.width * videoSize.pixelWidthHeightRatio / videoSize.height;
                applyControlsInsets();""",
    """                mVideoAspect = videoSize.width * videoSize.pixelWidthHeightRatio / videoSize.height;
                applyControlsInsets();
                if (mMarsTvShell != null) mMarsTvShell.ensureShortsFrame();""")

once(playback,
    """        public void onPlaybackStateChanged(int playbackState) {
            handleUiStateChange(mPlayer != null && mPlayer.getPlayWhenReady(), playbackState);
        }""",
    """        public void onPlaybackStateChanged(int playbackState) {
            handleUiStateChange(mPlayer != null && mPlayer.getPlayWhenReady(), playbackState);
            if (mMarsTvShell != null) {
                if (playbackState == Player.STATE_READY) mMarsTvShell.onShortsPlaybackReady();
                else if (playbackState == Player.STATE_ENDED) mMarsTvShell.onShortsPlaybackEnded();
            }
        }""")

once(playback,
    """    @androidx.annotation.Nullable private MarsTvShell mMarsTvShell;""",
    """    @androidx.annotation.Nullable private MarsTvShell mMarsTvShell;
    @androidx.annotation.Nullable private MarsTvShortsSearchPanel mMarsShortsSearch;""")

once(playback,
    """                    @Override public void search() {
                        startActivity(new android.content.Intent(
                                MobilePlaybackActivity.this,
                                com.newtube.mobile.ui.search.MobileSearchActivity.class));
                    }
                    @Override public void share() { shareCurrentVideo(); }""",
    """                    @Override public void search() { marsTvOpenShortsSearch(); }
                    @Override public boolean searchOpen() {
                        return mMarsShortsSearch != null && mMarsShortsSearch.isOpen();
                    }
                    @Override public boolean closeSearch() {
                        if (mMarsShortsSearch == null || !mMarsShortsSearch.isOpen())
                            return false;
                        mMarsShortsSearch.hide();
                        return true;
                    }""")

once(playback,
    """    private void marsTvOpenShortsComments() {""",
    """    private void marsTvOpenShortsSearch() {
        if (mVideoArea == null) return;
        if (mCommentsPanel != null && mCommentsPanel.isOpen()) {
            mCommentsPanel.close();
        }
        if (mMarsShortsSearch == null) {
            mMarsShortsSearch = new MarsTvShortsSearchPanel(
                    this, mVideoArea, this::onRelatedClicked);
        }
        mMarsShortsSearch.show();
    }

    private void marsTvOpenShortsComments() {""")

once(playback,
    """        if (mMarsTvShell != null && mMarsTvShell.commentsOpen())
            return super.dispatchKeyEvent(event);""",
    """        if (mMarsTvShell != null
                && (mMarsTvShell.commentsOpen() || mMarsTvShell.searchOpen()))
            return super.dispatchKeyEvent(event);""")

once(playback,
    """    @Override
    protected void onDestroy() {""",
    """    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    @androidx.annotation.Nullable android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (mMarsShortsSearch != null)
            mMarsShortsSearch.onVoiceResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        if (mMarsShortsSearch != null) {
            mMarsShortsSearch.release();
            mMarsShortsSearch = null;
        }""")

# Preserve identity, update mechanism, decoder count and the existing universal BACK.
assert 'applicationId "de.equiummc.marstube2"' in (
    root / "smarttubetv/build.gradle").read_text(encoding="utf-8")
assert 'versionCode 12027' in (root / "smarttubetv/build.gradle").read_text(encoding="utf-8")
assert 'return handleBack();' in shell.read_text(encoding="utf-8")
assert 'mPlayerView.setResizeMode(' in playback.read_text(encoding="utf-8")
assert 'marsTvOpenShortsSearch()' in playback.read_text(encoding="utf-8")
assert (root / "smarttubetv/src/stmobile/res/drawable/mars_m_flat.xml").read_bytes() == (
    here / "assets/drawable/mars_m_flat.xml").read_bytes()
print("MT2-027: in-place search, audio input, right action rail, EOS transition, original M preserved.")
