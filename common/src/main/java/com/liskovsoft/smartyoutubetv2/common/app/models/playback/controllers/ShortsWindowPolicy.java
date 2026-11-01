package com.liskovsoft.smartyoutubetv2.common.app.models.playback.controllers;

/** MT-018 Fire: bounded recent-history / future metadata window, never three media decoders. */
public final class ShortsWindowPolicy {
    // MT-022 A/B control: disable ALL speculative forward metadata/sample
    // warmup; preserve warm metadata reuse for already watched Shorts.
    public static final boolean ENABLE_SPECULATIVE_FORWARD_WARMUP = false;
    public static final int PREVIOUS_COUNT = 3;
    public static final int NEXT_COUNT = 3;
    public static final long FIRST_WARM_DELAY_MS = 1_500L;
    public static final long NEXT_WARM_DELAY_MS = 550L;
    public static final long REQUEST_TIMEOUT_MS = 3_500L;
    private int distance = 1;

    public void reset() { distance = 1; }
    public int nextDistance() { return distance <= NEXT_COUNT ? distance : -1; }
    public boolean advance() { return ++distance <= NEXT_COUNT; }
    public boolean withinPreviousWindow(int steps) {
        return steps >= 1 && steps <= PREVIOUS_COUNT;
    }
    public boolean withinNextWindow(int steps) {
        return steps >= 1 && steps <= NEXT_COUNT;
    }
}
