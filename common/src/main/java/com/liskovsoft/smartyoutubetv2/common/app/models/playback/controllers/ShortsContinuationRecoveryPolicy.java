package com.liskovsoft.smartyoutubetv2.common.app.models.playback.controllers;

/**
 * MT-016: bounded retry budget for a Shorts page request that never calls back.
 * The caller must still cancel the timed-out Rx subscription and invalidate its
 * generation before trying again.
 */
public final class ShortsContinuationRecoveryPolicy {
    private static final int MAX_TIMEOUT_RETRIES = 1;
    private int retries;

    public void reset() {
        retries = 0;
    }

    public int retries() {
        return retries;
    }

    /** True exactly once per selected Short. Never loops indefinitely. */
    public boolean claimTimeoutRetry() {
        if (retries >= MAX_TIMEOUT_RETRIES) return false;
        retries++;
        return true;
    }
}
