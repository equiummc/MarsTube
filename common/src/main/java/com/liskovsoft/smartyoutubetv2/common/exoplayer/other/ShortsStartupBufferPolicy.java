package com.liskovsoft.smartyoutubetv2.common.exoplayer.other;

/**
 * Per-video startup gating for Shorts; not an adjustment to the allocator,
 * steady-state buffering, source signatures or the rebuffer threshold.
 */
public final class ShortsStartupBufferPolicy {
    public static final long STARTUP_US = 1_000_000L;
    public static final long EARLY_REBUFFER_GUARD_MS = 8_000L;

    private ShortsStartupBufferPolicy() { }

    public static boolean permitsEarlyStart(boolean isShort, boolean rebuffering,
            boolean disabledForSession, long bufferedDurationUs, float speed) {
        return isShort && !rebuffering && !disabledForSession &&
                bufferedDurationUs > 0 && speed > 0f && speed <= 8f &&
                bufferedDurationUs >= (long) (STARTUP_US * speed);
    }

    public static boolean shouldDisableAfterRebuffer(boolean fastStartUsed,
            boolean rebuffering, long elapsedSinceFastStartMs) {
        return fastStartUsed && rebuffering &&
                elapsedSinceFastStartMs >= 0 &&
                elapsedSinceFastStartMs <= EARLY_REBUFFER_GUARD_MS;
    }
}
