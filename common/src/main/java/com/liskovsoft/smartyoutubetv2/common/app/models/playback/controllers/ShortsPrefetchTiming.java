package com.liskovsoft.smartyoutubetv2.common.app.models.playback.controllers;

/**
 * Short metadata-preload scheduling policy.
 *
 * The old two-second delay routinely expired after the user had swiped away.
 * Begin immediately after the current Short loads; do not make speculative
 * API requests before the foreground reports actual playback. Retry only a
 * few times, bounded in time, and cancel on a new foreground selection.
 */
public final class ShortsPrefetchTiming {
    public static final long INITIAL_DELAY_MS = 100L;
    public static final long FOLLOWING_METADATA_DELAY_MS = 300L;

    private static final long[] FOREGROUND_RETRY_MS = {200L, 350L, 650L};

    private ShortsPrefetchTiming() { }

    /**
     * @return next delay; -1 if foreground did not become playable in time.
     */
    public static long retryDelayMs(int attempt) {
        return attempt >= 0 && attempt < FOREGROUND_RETRY_MS.length ?
                FOREGROUND_RETRY_MS[attempt] : -1L;
    }
}
