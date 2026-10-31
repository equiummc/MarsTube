package com.liskovsoft.smartyoutubetv2.common.app.models.playback.controllers;

/**
 * MT-011B4 experimental preloading policy, inspired by MeeTube's foreground-first
 * one-candidate design. Deliberately independent of Android so it can be tested.
 *
 * Downloaded bytes only qualify a candidate for an attempted handoff: a true
 * sample hit is confirmed separately, after the new video's first decoded frame.
 */
public final class ShortsMediaHandoffPolicy {
    public static final long MIN_FOREGROUND_BUFFER_MS = 3_000L;
    public static final long MIN_BYTES_FOR_HANDOFF = 64L * 1024L;
    public static final long MAX_AGE_MS = 15_000L;

    private ShortsMediaHandoffPolicy() { }

    public static boolean shouldOffer(boolean enabled, boolean knownCandidate,
            boolean foregroundReady, boolean foregroundPlaying, long bufferedMs,
            boolean alreadyAttempted) {
        return enabled && knownCandidate && foregroundReady && foregroundPlaying
                && bufferedMs >= MIN_FOREGROUND_BUFFER_MS && !alreadyAttempted;
    }

    public static boolean canAdopt(boolean enabled, boolean matchingId,
            boolean sameSourceInForegroundQueue, long downloadedBytes,
            long elapsedMs) {
        return enabled && matchingId && sameSourceInForegroundQueue
                && downloadedBytes >= MIN_BYTES_FOR_HANDOFF
                && downloadedBytes <= ShortsSamplePreloadGate.MAX_PRELOAD_BYTES
                && elapsedMs >= 0 && elapsedMs < MAX_AGE_MS;
    }
}
