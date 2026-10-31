package com.liskovsoft.smartyoutubetv2.common.app.models.playback.controllers;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ShortsPrefetchTimingTest {
    @Test public void firstAttemptStartsBeforeTypicalRapidSwipe() {
        assertTrue(ShortsPrefetchTiming.INITIAL_DELAY_MS <= 150);
        assertTrue(ShortsPrefetchTiming.FOLLOWING_METADATA_DELAY_MS < 500);
    }

    @Test public void foregroundRetriesAreBounded() {
        assertEquals(200L, ShortsPrefetchTiming.retryDelayMs(0));
        assertEquals(350L, ShortsPrefetchTiming.retryDelayMs(1));
        assertEquals(650L, ShortsPrefetchTiming.retryDelayMs(2));
        assertEquals(-1L, ShortsPrefetchTiming.retryDelayMs(3));
        assertEquals(-1L, ShortsPrefetchTiming.retryDelayMs(-1));
    }
}
