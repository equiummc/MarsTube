#!/usr/bin/env python3
"""MT-011B3: same-player SABR period queue, guarded one-short media preloading.

Apply as build overlay in detached worktree; never modify upstream gitlinks.
Same actual SabrMediaSource is consumed on swipe (no double request, no second
video decoder). Queued source has an enforced 4MiB read budget and 15s lifetime.
"""
from pathlib import Path
import sys

ROOT=Path(__file__).resolve().parents[2]
CHECK="--check" in sys.argv[1:]

def patch(relative,changes):
    path=ROOT/relative
    s=path.read_text(encoding="utf-8")
    for label,old,new in changes:
        count=s.count(old)
        if count!=1:
            raise SystemExit(f"MT-011B3 STOP {relative}: {label}, anchor count={count}")
        s=s.replace(old,new)
    if not CHECK:
        path.write_text(s,encoding="utf-8",newline="\n")
    print(f"MT-011B3 {'checked' if CHECK else 'patched'} {relative}")

patch("common/src/main/java/com/liskovsoft/smartyoutubetv2/common/app/models/playback/manager/PlayerEngine.java",[
("API",
"    void openSabr(MediaItemFormatInfo formatInfo);",
"""    void openSabr(MediaItemFormatInfo formatInfo);
    // Optional for compatible players. Existing engines remain unaffected.
    default boolean hasPreloadedShort(String videoId) { return false; }
    default void preloadNextSabrShort(String videoId, MediaItemFormatInfo formatInfo) { }
    default void cancelPreloadedShort() { }""")
])

patch("smarttubetv/src/main/java/com/liskovsoft/smartyoutubetv2/tv/ui/playback/PlaybackFragment.java",[
("delegate",
"""    @Override
    public void openSabr(MediaItemFormatInfo formatInfo) {
        mExoPlayerController.openSabr(formatInfo);
    }""",
"""    @Override
    public boolean hasPreloadedShort(String videoId) {
        return mExoPlayerController != null && mExoPlayerController.hasPreloadedShort(videoId);
    }

    @Override
    public void preloadNextSabrShort(String videoId, MediaItemFormatInfo formatInfo) {
        if (mExoPlayerController != null) mExoPlayerController.preloadNextSabrShort(videoId, formatInfo);
    }

    @Override
    public void cancelPreloadedShort() {
        if (mExoPlayerController != null) mExoPlayerController.cancelPreloadedShort();
    }

    @Override
    public void openSabr(MediaItemFormatInfo formatInfo) {
        mExoPlayerController.openSabr(formatInfo);
    }""")
])

patch("common/src/main/java/com/liskovsoft/smartyoutubetv2/common/app/models/playback/controllers/VideoLoaderController.java",[
("avoid discarding preloaded period",
"""            getPlayer().setVideo(item);
            getPlayer().resetPlayerState();
            loadFormatInfo(item);""",
"""            getPlayer().setVideo(item);
            // Same-player SABR queue already owns this media period; stopping
            // and re-preparing would discard its preloaded media samples.
            if (!getPlayer().hasPreloadedShort(item.videoId)) {
                // A prepared queue without transferred bytes is not a hit.
                // Cancel it before resetting so delayed network reads cannot
                // make the old candidate eligible during the new video open.
                getPlayer().cancelPreloadedShort();
                getPlayer().resetPlayerState();
            }
            loadFormatInfo(item);"""),
("discard prepared session on account switch",
"""            account -> {
                clearShortPrefetch();
                mShortWarmCache.clear();
            };""",
"""            account -> {
                clearShortPrefetch();
                mShortWarmCache.clear();
                Utils.post(() -> {
                    if (getPlayer() != null) getPlayer().cancelPreloadedShort();
                });
            };"""),
("enqueue after format info ready",
"""                        mShortPrefetchedInfo = info;
                        Log.i("MarsTubePerf", "MT-008 prefetch_ready elapsed_ms=" +""",
"""                        mShortPrefetchedInfo = info;
                        Video playingShort = getVideo();
                        if (playingShort != null && playingShort.isShorts &&
                                !info.isLive() && info.containsSabrFormats() &&
                                !info.containsDashFormats() && getPlayer() != null) {
                            getPlayer().preloadNextSabrShort(mShortPrefetchId, info);
                        }
                        Log.i("MarsTubePerf", "MT-008 prefetch_ready elapsed_ms=" +""")
])

patch("common/src/main/java/com/liskovsoft/smartyoutubetv2/common/exoplayer/ExoMediaSourceFactory.java",[
("SABR overload",
"""    public MediaSource fromSabrFormatInfo(MediaItemFormatInfo formatInfo) {
        return buildSabrMediaSource(formatInfo);
    }""",
"""    public MediaSource fromSabrFormatInfo(MediaItemFormatInfo formatInfo) {
        return buildSabrMediaSource(formatInfo);
    }

    public MediaSource fromSabrFormatInfo(MediaItemFormatInfo formatInfo,
                                          ShortsSabrPreloadBudget budget) {
        return buildSabrMediaSource(formatInfo,
                budget.wrap(getSabrMediaDataSourceFactory()));
    }"""),
("SABR builder overload",
"""    private MediaSource buildSabrMediaSource(MediaItemFormatInfo formatInfo) {
        // Are you using FrameworkSampleSource or ExtractorSampleSource when you build your player?
        SabrMediaSource sabrSource = new SabrMediaSource.Factory(
                getSabrChunkSourceFactory(),""",
"""    private MediaSource buildSabrMediaSource(MediaItemFormatInfo formatInfo) {
        return buildSabrMediaSource(formatInfo, null);
    }

    private MediaSource buildSabrMediaSource(MediaItemFormatInfo formatInfo,
                                              DataSource.Factory prefetchFactory) {
        SabrMediaSource sabrSource = new SabrMediaSource.Factory(
                prefetchFactory != null
                        ? new DefaultSabrChunkSource.Factory(prefetchFactory, MAX_SEGMENTS_PER_LOAD)
                        : getSabrChunkSourceFactory(),""")
])

patch("common/src/main/java/com/liskovsoft/smartyoutubetv2/common/exoplayer/controller/ExoPlayerController.java",[
("imports",
"""import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MergingMediaSource;""",
"""import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.MergingMediaSource;"""),
("imports2",
"""import com.liskovsoft.smartyoutubetv2.common.exoplayer.ExoMediaSourceFactory;""",
"""import com.liskovsoft.smartyoutubetv2.common.exoplayer.ExoMediaSourceFactory;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.ShortsSabrPreloadBudget;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.controllers.ShortsSamplePreloadGate;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.controllers.ShortsMediaHandoffPolicy;"""),
("field",
"""    private SimpleExoPlayer mPlayer;
    /** Not null only for sabr live streams */""",
"""    private SimpleExoPlayer mPlayer;
    private ConcatenatingMediaSource mShortQueue;
    private String mShortCurrentId;
    private String mShortNextId;
    private String mShortAttemptedId;
    private String mShortAdoptedId;
    private long mShortNextAtMs;
    private ShortsSabrPreloadBudget mShortBudget;
    private final android.os.Handler mShortHandler =
            new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable mExpireShort = this::dropShortCandidate;
    private final Runnable mWatchShort = this::watchShortCandidate;
    /** Not null only for sabr live streams */"""),
("openSabr adoption",
"""    public void openSabr(MediaItemFormatInfo formatInfo) {
        MediaSource mediaSource = mMediaSourceFactory.fromSabrFormatInfo(formatInfo);
        openMediaSource(mediaSource);""",
"""    public void openSabr(MediaItemFormatInfo formatInfo) {
        if (!formatInfo.isLive() && adoptShortIfPrepared()) return;
        MediaSource mediaSource = mMediaSourceFactory.fromSabrFormatInfo(formatInfo);
        openMediaSource(mediaSource);"""),
("insert queue before openMediaSource",
"""    private void openMediaSource(MediaSource mediaSource) {""",
"""    /** The queued source stays owned by the current ExoPlayer until adopted. */
    public boolean hasPreloadedShort(String id) {
        long ageMs = android.os.SystemClock.elapsedRealtime() - mShortNextAtMs;
        return ShortsMediaHandoffPolicy.canAdopt(
                PlayerTweaksData.instance(mContext).isMarsShortMediaPreloadEnabled(),
                id != null && id.equals(mShortNextId),
                mShortQueue != null && mShortQueue.getSize() == 2,
                mShortBudget == null ? 0 : mShortBudget.bytes(), ageMs);
    }

    public void preloadNextSabrShort(String id, MediaItemFormatInfo info) {
        if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
            mShortHandler.post(() -> preloadNextSabrShort(id, info));
            return;
        }
        Video current = getVideo();
        if (id == null || current == null || current.videoId == null ||
                id.equals(current.videoId) || id.equals(mShortAttemptedId) ||
                !current.isShorts || info == null ||
                info.isLive() || !info.containsSabrFormats() ||
                info.containsDashFormats() || mShortQueue == null ||
                mPlayer == null || mPlayer.getPlaybackState() != Player.STATE_READY ||
                mShortQueue.getSize() != 1 || !current.videoId.equals(mShortCurrentId)) return;
        long bufferedMs = mPlayer.getBufferedPosition() - mPlayer.getCurrentPosition();
        if (!ShortsMediaHandoffPolicy.shouldOffer(
                PlayerTweaksData.instance(mContext).isMarsShortMediaPreloadEnabled(),
                true, mPlayer.getPlaybackState() == Player.STATE_READY,
                mPlayer.getPlayWhenReady(), bufferedMs, false)) return;
        boolean enoughBuffer = bufferedMs >= ShortsMediaHandoffPolicy.MIN_FOREGROUND_BUFFER_MS;
        ShortsSamplePreloadGate.Input gate = new ShortsSamplePreloadGate.Input(
                true, true, true, false, true, true, enoughBuffer,
                true, true, true, true);
        if (ShortsSamplePreloadGate.evaluate(gate) != ShortsSamplePreloadGate.Decision.ALLOW) {
            return;
        }
        mShortAttemptedId = id; // No speculative retries after a denial or deadline.
        final String expected = id;
        ShortsSabrPreloadBudget budget = new ShortsSabrPreloadBudget(() -> {
            if (expected.equals(mShortNextId)) dropShortCandidate();
        });
        try {
            MediaSource queued = mMediaSourceFactory.fromSabrFormatInfo(info, budget);
            mShortQueue.addMediaSource(queued);
            mShortNextId = id;
            mShortBudget = budget;
            mShortNextAtMs = android.os.SystemClock.elapsedRealtime();
            mShortHandler.removeCallbacks(mExpireShort);
            mShortHandler.postDelayed(mExpireShort, ShortsSabrPreloadBudget.MAX_AGE_MS);
            mShortHandler.removeCallbacks(mWatchShort);
            mShortHandler.postDelayed(mWatchShort, 500);
            android.util.Log.i("MarsTubePerf", "MT-011B4 preload_candidate_queued=true");
        } catch (RuntimeException ex) {
            budget.cancel();
            dropShortCandidate();
            android.util.Log.i("MarsTubePerf", "MT-011B short_prefetch_queued=false");
        }
    }

    private boolean adoptShortIfPrepared() {
        Video target = getVideo();
        if (mPlayer == null || target == null || !hasPreloadedShort(target.videoId)) return false;
        ShortsSabrPreloadBudget budget = mShortBudget;
        // Promotion happens BEFORE seek to keep reading the same SABR session
        // without the speculative byte limit.
        budget.promote();
        mShortHandler.removeCallbacks(mExpireShort);
        long read = budget.bytes();
        android.util.Log.i("MarsTubePerf", "MT-011B4 preload_candidate_bytes=" + read);
        mShortBudget = null;
        mShortNextId = null;
        mShortNextAtMs = 0;
        try {
            mPlayer.seekTo(1, 0);
            mShortQueue.removeMediaSource(0);
            mShortCurrentId = target.videoId;
            mSabrLiveManifest = null;
            mSabrLiveSeekBlocked = false;
            mSabrLiveStartPending = false;
            mTrackSelectorManager.invalidate();
            mOnSourceChanged = true;
            mEventListener.onSourceChanged(target);
            PlaybackStartupMetrics.sourcePrepared(target.videoId, "QueuedSabrMediaSource");
            mShortAdoptedId = target.videoId;
            android.util.Log.i("MarsTubePerf", "MT-011B4 preload_handoff_bytes=" + read);
            return true;
        } catch (RuntimeException ex) {
            android.util.Log.i("MarsTubePerf", "MT-011B short_prefetch_handoff_failed=true");
            dropShortCandidate();
            return false;
        }
    }

    public void cancelPreloadedShort() {
        if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
            mShortHandler.post(this::dropShortCandidate);
        } else {
            dropShortCandidate();
        }
    }

    private void watchShortCandidate() {
        if (mShortNextId == null) return;
        if (mPlayer == null || mPlayer.getPlaybackState() != Player.STATE_READY ||
                !mPlayer.getPlayWhenReady() ||
                mPlayer.getBufferedPosition() - mPlayer.getCurrentPosition() < 1_500) {
            android.util.Log.i("MarsTubePerf", "MT-011B4 preload_cancel_foreground_priority=true");
            dropShortCandidate();
        } else {
            mShortHandler.postDelayed(mWatchShort, 500);
        }
    }

    private void dropShortCandidate() {
        mShortHandler.removeCallbacks(mExpireShort);
        mShortHandler.removeCallbacks(mWatchShort);
        if (mShortNextId != null) {
            android.util.Log.i("MarsTubePerf", "MT-011B4 preload_discard_bytes=" +
                    (mShortBudget == null ? 0 : mShortBudget.bytes()));
        }
        if (mShortBudget != null) mShortBudget.cancel();
        mShortBudget = null;
        mShortNextId = null;
        mShortNextAtMs = 0;
        if (mShortQueue != null && mShortQueue.getSize() > 1) {
            mShortQueue.removeMediaSource(1);
        }
    }

    private void openMediaSource(MediaSource mediaSource) {"""),
("on regular source cancel",
"""    private void openMediaSource(MediaSource mediaSource) {
        mSabrLiveManifest = null;""",
"""    private void openMediaSource(MediaSource mediaSource) {
        dropShortCandidate();
        mShortQueue = null;
        mShortCurrentId = null;
        mShortAttemptedId = null;
        mShortAdoptedId = null;
        mSabrLiveManifest = null;"""),
("prepare same player queue",
"""        mPlayer.prepare(mediaSource);
    }""",
"""        if (video != null && video.isShorts && mediaSource instanceof SabrMediaSource &&
                PlayerTweaksData.instance(mContext).isMarsShortMediaPreloadEnabled()) {
            mShortQueue = new ConcatenatingMediaSource(mediaSource);
            mShortCurrentId = video.videoId;
            mPlayer.prepare(mShortQueue);
        } else {
            mPlayer.prepare(mediaSource);
        }
    }"""),
("cleanup release",
"""    public void release() {
        mTrackSelectorManager.release();""",
"""    public void release() {
        dropShortCandidate();
        mShortQueue = null;
        mShortCurrentId = null;
        mShortAttemptedId = null;
        mShortAdoptedId = null;
        mTrackSelectorManager.release();"""),
("natural end preserves original next-video flow",
"""        if (reason == Player.DISCONTINUITY_REASON_PERIOD_TRANSITION) {
            mPlayer.stop();
            mEventListener.onPlayEnd();""",
"""        if (reason == Player.DISCONTINUITY_REASON_PERIOD_TRANSITION) {
            // The presenter still decides on next/loop/close at the boundary.
            mPlayer.stop();
            dropShortCandidate();
            mEventListener.onPlayEnd();"""),
("quality change cancellation",
"""    public void selectFormat(FormatItem formatItem) {
        if (formatItem != null && formatItem.getTrack() != null) {""",
"""    public void selectFormat(FormatItem formatItem) {
        // A pending source was prepared under the old track settings.
        if (mShortNextId != null) dropShortCandidate();
        if (formatItem != null && formatItem.getTrack() != null) {""")
])
print("MT-011B3 same-player queue overlay: no sidecar decoder or duplicate SABR POST")


# MT-011B4: only count a sample handoff after the next Short renders a frame.
# An error drops the speculative source; ordinary error recovery remains unchanged.
patch("common/src/main/java/com/liskovsoft/smartyoutubetv2/common/exoplayer/controller/ExoPlayerController.java", [
("first frame confirmation",
"""    public void onRenderedFirstFrame() {
        Video video = getVideo();
        if (video != null) {
            PlaybackStartupMetrics.firstFrame(video.videoId);
        }
    }""",
"""    public void onRenderedFirstFrame() {
        Video video = getVideo();
        if (video != null) {
            PlaybackStartupMetrics.firstFrame(video.videoId);
            if (video.videoId.equals(mShortAdoptedId)) {
                android.util.Log.i("MarsTubePerf", "MT-011B4 preload_handoff_first_frame=true");
                mShortAdoptedId = null;
            }
        }
    }"""),
("error invalidation",
"""    public void onPlayerError(ExoPlaybackException error) {
        Log.e(TAG, "onPlayerError: " + error);""",
"""    public void onPlayerError(ExoPlaybackException error) {
        if (mShortNextId != null) dropShortCandidate();
        if (mShortAdoptedId != null) {
            android.util.Log.i("MarsTubePerf", "MT-011B4 preload_handoff_failed=true");
            mShortAdoptedId = null;
        }
        Log.e(TAG, "onPlayerError: " + error);""")
])

# MT-011B4: enable metadata warmup whenever media preload was deliberately enabled.
# Keep the two switches independent and default-off for older installations.
patch("common/src/main/java/com/liskovsoft/smartyoutubetv2/common/app/models/playback/controllers/VideoLoaderController.java",[
("schedule warmup on loaded",
"""        if (video != null && video.isShorts &&
                getPlayerTweaksData().isMarsShortPrefetchEnabled()) {""",
"""        if (video != null && video.isShorts &&
                (getPlayerTweaksData().isMarsShortPrefetchEnabled() ||
                 getPlayerTweaksData().isMarsShortMediaPreloadEnabled())) {"""),
("allow warmup reuse for media preload",
"""        boolean matchingPrefetch = getPlayerTweaksData().isMarsShortPrefetchEnabled() &&
                video.isShorts""",
"""        boolean matchingPrefetch = (getPlayerTweaksData().isMarsShortPrefetchEnabled() ||
                getPlayerTweaksData().isMarsShortMediaPreloadEnabled()) &&
                video.isShorts"""),
("schedule metadata warmup",
"""        if (getVideo() != null && getVideo().isShorts &&
                getPlayerTweaksData().isMarsShortPrefetchEnabled()) {""",
"""        if (getVideo() != null && getVideo().isShorts &&
                (getPlayerTweaksData().isMarsShortPrefetchEnabled() ||
                 getPlayerTweaksData().isMarsShortMediaPreloadEnabled())) {"""),
("guard warmup",
"""        if (!getPlayerTweaksData().isMarsShortPrefetchEnabled()) {
            clearShortPrefetch();
            return;
        }""",
"""        if (!getPlayerTweaksData().isMarsShortPrefetchEnabled() &&
                !getPlayerTweaksData().isMarsShortMediaPreloadEnabled()) {
            clearShortPrefetch();
            return;
        }""")
])
