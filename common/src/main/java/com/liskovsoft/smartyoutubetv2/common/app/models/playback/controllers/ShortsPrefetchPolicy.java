package com.liskovsoft.smartyoutubetv2.common.app.models.playback.controllers;

/** MT-007: reject expired or clock-invalid memory-only stream information. */
public final class ShortsPrefetchPolicy {
    private ShortsPrefetchPolicy() {
    }

    public static boolean fresh(long readyAtElapsedMs, long nowElapsedMs, long maxAgeMs) {
        return readyAtElapsedMs > 0 && maxAgeMs > 0 && nowElapsedMs >= readyAtElapsedMs
                && nowElapsedMs - readyAtElapsedMs < maxAgeMs;
    }
}
