package com.liskovsoft.smartyoutubetv2.common.exoplayer.other;

import org.junit.Test;
import static org.junit.Assert.*;

public class ShortsStartupBufferPolicyTest {
    @Test public void shortsOnlyAfterOneSecondOfBufferedPlaytime() {
        assertFalse(ShortsStartupBufferPolicy.permitsEarlyStart(true, false, false, 999_999, 1f));
        assertTrue(ShortsStartupBufferPolicy.permitsEarlyStart(true, false, false, 1_000_000, 1f));
        assertFalse(ShortsStartupBufferPolicy.permitsEarlyStart(false, false, false, 2_000_000, 1f));
    }
    @Test public void neverOverridesRebufferOrSessionAutoDisable() {
        assertFalse(ShortsStartupBufferPolicy.permitsEarlyStart(true, true, false, 9_000_000, 1f));
        assertFalse(ShortsStartupBufferPolicy.permitsEarlyStart(true, false, true, 9_000_000, 1f));
        assertFalse(ShortsStartupBufferPolicy.permitsEarlyStart(true, false, false, 9_000_000, 0f));
    }
    @Test public void speedScalingAndRebufferWatchWindow() {
        assertFalse(ShortsStartupBufferPolicy.permitsEarlyStart(true, false, false, 1_400_000, 1.5f));
        assertTrue(ShortsStartupBufferPolicy.permitsEarlyStart(true, false, false, 1_500_000, 1.5f));
        assertTrue(ShortsStartupBufferPolicy.shouldDisableAfterRebuffer(true, true, 4_000L));
        assertFalse(ShortsStartupBufferPolicy.shouldDisableAfterRebuffer(true, true, 8_001L));
        assertFalse(ShortsStartupBufferPolicy.shouldDisableAfterRebuffer(false, true, 4_000L));
    }
}
