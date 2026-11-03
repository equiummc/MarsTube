#!/usr/bin/env python3
"""MT2-003: one-time SABR-to-DASH rescue at the same playhead, for reference lab only.

Applies exclusively to a disposable pinned NewTube checkout. No modifications to MarsTube 1.x.
"""
from pathlib import Path
import sys
if len(sys.argv) != 2:
    raise SystemExit("usage: python3 patch_sabr_recovery.py <newtube-reference-root>")
root = Path(sys.argv[1]).resolve()
path = root / "smarttubetv/src/stmobile/java/com/newtube/mobile/player/Media3PlayerController.java"
if not (root / "sabr-media3").is_dir() or not path.is_file():
    raise SystemExit("not the independent NewTube reference checkout")
s = path.read_text(encoding="utf-8")
if "MT2-003 BEGIN" in s:
    raise SystemExit("MT2-003 already applied")
def replace_once(before, after):
    global s
    count = s.count(before)
    if count != 1:
        raise SystemExit(f"anchor changed: {before[:75]!r}, count={count}")
    s = s.replace(before, after, 1)

replace_once(
    "    private boolean mSabrWasFallback;\n",
    """    private boolean mSabrWasFallback;
    // MT2-003 BEGIN: bounded, per-title rescue from experimental preferred SABR to DASH.
    @Nullable private MediaItemFormatInfo mMarsSabrDashAnswer;
    @Nullable private String mMarsBlockedSabrVideoId;
    @Nullable private String mMarsResumeVideoId;
    private long mMarsResumePositionMs = -1;
    private boolean mMarsResumePlayWhenReady;
    // MT2-003 END
""")

replace_once(
    """    public void openSabr(MediaItemFormatInfo formatInfo) {
        mReadinessAnswer = null; // not a gated route""",
    """    public void openSabr(MediaItemFormatInfo formatInfo) {
        // A failed preferred SABR source must not be reopened for the same title.
        if (formatInfo.containsUrlFormats()
                && java.util.Objects.equals(mMarsBlockedSabrVideoId, formatInfo.getVideoId())) {
            openDash(formatInfo);
            return;
        }
        mReadinessAnswer = null; // not a gated route""")

replace_once(
    """            mSabrWasFallback = !SabrSourcePreference.isPreferred(mContext);
            mOpenGeneration.next();""",
    """            mSabrWasFallback = !SabrSourcePreference.isPreferred(mContext);
            mMarsSabrDashAnswer = !mSabrWasFallback && formatInfo.containsUrlFormats()
                    ? formatInfo : null;
            mOpenGeneration.next();""")

replace_once(
    """        if (SabrSourcePreference.isPreferred(mContext) && SabrFormatAdapter.eligible(formatInfo)) {
            openSabr(formatInfo);
            return;
        }""",
    """        if (SabrSourcePreference.isPreferred(mContext) && SabrFormatAdapter.eligible(formatInfo)
                && !java.util.Objects.equals(mMarsBlockedSabrVideoId, formatInfo.getVideoId())) {
            openSabr(formatInfo);
            return;
        }""")

replace_once(
    """    public void setVideo(Video video) {
        mVideo = new WeakReference<>(video);
    }""",
    """    public void setVideo(Video video) {
        String nextId = video != null ? video.videoId : null;
        String previousId = getVideoId();
        if (!java.util.Objects.equals(previousId, nextId)) {
            mMarsSabrDashAnswer = null;
            mMarsBlockedSabrVideoId = null;
            mMarsResumeVideoId = null;
            mMarsResumePositionMs = -1;
        }
        mVideo = new WeakReference<>(video);
    }""")

replace_once(
    """        PlayerInfrastructureWarmup.onPlaybackPreparing();
        mPlayer.setMediaSource(mediaSource);
        mPlayer.prepare();""",
    """        PlayerInfrastructureWarmup.onPlaybackPreparing();
        // Restore the exact playhead BEFORE preparing the replacement DASH timeline.
        // A new video invalidates the marker in setVideo(); a stale async build cannot
        // steal it, since the existing open generation already guards delivery.
        boolean marsResume = netPathType.startsWith("dash-mpd")
                && mMarsResumePositionMs >= 0L
                && java.util.Objects.equals(mMarsResumeVideoId, getVideoId());
        long marsResumePosition = mMarsResumePositionMs;
        boolean marsResumePlay = mMarsResumePlayWhenReady;
        if (marsResume) {
            mMarsResumeVideoId = null;
            mMarsResumePositionMs = -1L;
        }
        mPlayer.setMediaSource(mediaSource);
        if (marsResume) mPlayer.seekTo(marsResumePosition);
        mPlayer.prepare();
        if (marsResume) {
            mPlayer.setPlayWhenReady(marsResumePlay);
            NetPath.log(NetPath.context() + " mt2-003 sabr-to-dash resumed position-ms="
                    + marsResumePosition + " play=" + marsResumePlay);
        }""")

replace_once(
    """        if (mSabrSourceActive) {
            if (mSabrWasFallback) {
                noteRecoveryResume(); // its recovery may land on DASH
            }""",
    """        if (mSabrSourceActive) {
            // MT2-003: SABR can fail immediately when restoring a saved position.
            // If the SAME /player answer offered DASH URLs, switch once to DASH at
            // the actual playhead. Never downgrade a SABR-only response or loop.
            String failingId = getVideoId();
            MediaItemFormatInfo dashAnswer = mMarsSabrDashAnswer;
            if (!mSabrWasFallback && dashAnswer != null && mPlayer != null
                    && failingId != null
                    && java.util.Objects.equals(dashAnswer.getVideoId(), failingId)
                    && !java.util.Objects.equals(mMarsBlockedSabrVideoId, failingId)) {
                mMarsBlockedSabrVideoId = failingId;
                mMarsResumeVideoId = failingId;
                mMarsResumePositionMs = Math.max(0L, mPlayer.getCurrentPosition());
                mMarsResumePlayWhenReady = mPlayer.getPlayWhenReady();
                NetPath.log(NetPath.context() + " mt2-003 sabr-error -> dash-once"
                        + " position-ms=" + mMarsResumePositionMs);
                openDash(dashAnswer);
                return;
            }
            if (mSabrWasFallback) {
                noteRecoveryResume(); // its recovery may land on DASH
            }""")
path.write_text(s, encoding="utf-8")
print("MT2-003 SABR->DASH recovery patch applied safely.")
