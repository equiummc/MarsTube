package com.liskovsoft.smartyoutubetv2.common.app.models.playback.controllers;

import org.junit.Test;
import static org.junit.Assert.*;

public class ShortsDeviceWarmupPolicyTest {
    @Test public void exactReferenceDeviceIsRecognized() {
        assertTrue(ShortsDeviceWarmupPolicy.isThomsonTarget("nagata", "Thomson_2K_ATV3"));
        assertFalse(ShortsDeviceWarmupPolicy.isThomsonTarget("raven", "AFTR"));
        assertFalse(ShortsDeviceWarmupPolicy.isThomsonTarget("other", "Thomson_2K_ATV3"));
        assertFalse(ShortsDeviceWarmupPolicy.isThomsonTarget("nagata", null));
    }

    @Test public void onlyNextShortMayUseSampleBudget() {
        for (int distance = 2; distance <= 4; distance++) {
            assertFalse(allowed(distance, true, false, false, 256L * 1024 * 1024));
        }
        assertTrue(allowed(1, true, false, false, 256L * 1024 * 1024));
        assertFalse(ShortsDeviceWarmupPolicy.allowSecondSampleCandidate(
                "nagata", "Thomson_2K_ATV3"));
        assertTrue(ShortsDeviceWarmupPolicy.allowSecondSampleCandidate("raven", "AFTR"));
    }

    @Test public void foregroundOrMemoryPressureStopsSamplePreload() {
        long enough = ShortsDeviceWarmupPolicy.MIN_AVAILABLE_MEMORY_BYTES;
        assertTrue(allowed(1, true, false, false, enough));
        assertFalse(allowed(1, true, true, false, enough));
        assertFalse(allowed(1, false, false, false, enough));
        assertFalse(allowed(1, true, false, true, enough));
        assertFalse(allowed(1, true, false, false, enough - 1));
        assertFalse(ShortsDeviceWarmupPolicy.shouldPreloadThomsonSample(
                "raven", "AFTR", 1, true, false, false, enough));
    }

    private boolean allowed(int distance, boolean playing, boolean loading,
                            boolean lowMemory, long bytes) {
        return ShortsDeviceWarmupPolicy.shouldPreloadThomsonSample(
                "nagata", "Thomson_2K_ATV3", distance,
                playing, loading, lowMemory, bytes);
    }
}
