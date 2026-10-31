package com.liskovsoft.smartyoutubetv2.common.exoplayer.other;

import android.os.SystemClock;
import android.util.Log;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.Allocator;

/**
 * Same ExoPlayer and allocator, dynamic Shorts-only initial buffer threshold.
 *
 * The original DefaultLoadControl still owns all download/alloc/rebuffer
 * decisions. On the first early-start rebuffer within eight seconds we disable
 * the experiment for the remainder of this player session (fail safely).
 */
public final class ShortsAwareLoadControl implements LoadControl {
    private final DefaultLoadControl delegate;
    private volatile boolean shorts;
    private boolean disabledForSession;
    private boolean fastStartUsed;
    private long fastStartAtMs;

    public ShortsAwareLoadControl(DefaultLoadControl delegate) {
        if (delegate == null) throw new IllegalArgumentException("delegate");
        this.delegate = delegate;
    }

    public void setShorts(boolean shorts) {
        this.shorts = shorts;
    }

    @Override
    public void onPrepared() {
        fastStartUsed = false;
        fastStartAtMs = 0L;
        delegate.onPrepared();
    }

    @Override
    public void onTracksSelected(Renderer[] renderers, TrackGroupArray groups,
            TrackSelectionArray selections) {
        delegate.onTracksSelected(renderers, groups, selections);
    }

    @Override
    public void onStopped() {
        fastStartUsed = false;
        fastStartAtMs = 0L;
        delegate.onStopped();
    }

    @Override
    public void onReleased() {
        delegate.onReleased();
    }

    @Override
    public Allocator getAllocator() {
        return delegate.getAllocator();
    }

    @Override
    public long getBackBufferDurationUs() {
        return delegate.getBackBufferDurationUs();
    }

    @Override
    public boolean retainBackBufferFromKeyframe() {
        return delegate.retainBackBufferFromKeyframe();
    }

    @Override
    public boolean shouldContinueLoading(long durationUs, float speed) {
        return delegate.shouldContinueLoading(durationUs, speed);
    }

    @Override
    public boolean shouldStartPlayback(long durationUs, float speed, boolean rebuffering) {
        boolean normal = delegate.shouldStartPlayback(durationUs, speed, rebuffering);
        long now = SystemClock.elapsedRealtime();
        if (ShortsStartupBufferPolicy.shouldDisableAfterRebuffer(
                fastStartUsed, rebuffering, now - fastStartAtMs)) {
            disabledForSession = true;
            fastStartUsed = false;
            Log.i("MarsTubePerf", "MT-011B9 shorts_fast_start_auto_disabled=true");
        }
        if (normal) return true;
        boolean early = ShortsStartupBufferPolicy.permitsEarlyStart(
                shorts, rebuffering, disabledForSession, durationUs, speed);
        if (early && !fastStartUsed) {
            fastStartUsed = true;
            fastStartAtMs = now;
            Log.i("MarsTubePerf", "MT-011B9 shorts_fast_start buffered_ms=" +
                    (durationUs / 1_000L));
        }
        return early;
    }
}
