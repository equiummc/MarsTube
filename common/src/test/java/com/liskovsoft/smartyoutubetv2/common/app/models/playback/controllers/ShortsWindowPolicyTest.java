package com.liskovsoft.smartyoutubetv2.common.app.models.playback.controllers;

import org.junit.Test;
import static org.junit.Assert.*;

public class ShortsWindowPolicyTest {
    @Test public void exactlyThreeNextMetadataSlots() {
        ShortsWindowPolicy policy = new ShortsWindowPolicy();
        assertEquals(1, policy.nextDistance());
        assertTrue(policy.advance());
        assertEquals(2, policy.nextDistance());
        assertTrue(policy.advance());
        assertEquals(3, policy.nextDistance());
        assertFalse(policy.advance());
        assertEquals(-1, policy.nextDistance());
    }

    @Test public void exactlyThreeRecentHistorySlots() {
        ShortsWindowPolicy policy = new ShortsWindowPolicy();
        for (int n = 1; n <= 3; n++) {
            assertTrue(policy.withinPreviousWindow(n));
            assertTrue(policy.withinNextWindow(n));
        }
        assertFalse(policy.withinPreviousWindow(0));
        assertFalse(policy.withinPreviousWindow(4));
        assertFalse(policy.withinNextWindow(4));
        assertEquals(3, ShortsWindowPolicy.PREVIOUS_COUNT);
        assertEquals(3, ShortsWindowPolicy.NEXT_COUNT);
    }

    @Test public void eachVideoStartsNewThreeAheadSequence() {
        ShortsWindowPolicy policy = new ShortsWindowPolicy();
        policy.advance();
        policy.advance();
        policy.advance();
        assertEquals(-1, policy.nextDistance());
        policy.reset();
        assertEquals(1, policy.nextDistance());
    }

    @Test public void requestsAreBoundedInTime() {
        assertTrue(ShortsWindowPolicy.FIRST_WARM_DELAY_MS >= 1000);
        assertTrue(ShortsWindowPolicy.REQUEST_TIMEOUT_MS <= 3500);
        assertTrue(ShortsWindowPolicy.NEXT_WARM_DELAY_MS > 0);
    }
}
