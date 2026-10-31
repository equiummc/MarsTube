package com.liskovsoft.smartyoutubetv2.common.app.models.playback.controllers;

import org.junit.Test;
import static org.junit.Assert.*;

public class ShortsMediaHandoffPolicyTest {
    @Test public void foregroundAlwaysHasPriority() {
        assertFalse(ShortsMediaHandoffPolicy.shouldOffer(false, true, true, true, 5000, false));
        assertFalse(ShortsMediaHandoffPolicy.shouldOffer(true, false, true, true, 5000, false));
        assertFalse(ShortsMediaHandoffPolicy.shouldOffer(true, true, false, true, 5000, false));
        assertFalse(ShortsMediaHandoffPolicy.shouldOffer(true, true, true, false, 5000, false));
        assertFalse(ShortsMediaHandoffPolicy.shouldOffer(true, true, true, true, 2999, false));
        assertFalse(ShortsMediaHandoffPolicy.shouldOffer(true, true, true, true, 5000, true));
        assertTrue(ShortsMediaHandoffPolicy.shouldOffer(true, true, true, true, 3000, false));
    }

    @Test public void metadataOrAnEmptyQueueIsNeverAHit() {
        assertFalse(ShortsMediaHandoffPolicy.canAdopt(true, true, true, 0, 1000));
        assertFalse(ShortsMediaHandoffPolicy.canAdopt(true, true, true, 65535, 1000));
        assertFalse(ShortsMediaHandoffPolicy.canAdopt(true, true, false, 65536, 1000));
        assertFalse(ShortsMediaHandoffPolicy.canAdopt(true, false, true, 65536, 1000));
        assertFalse(ShortsMediaHandoffPolicy.canAdopt(false, true, true, 65536, 1000));
    }

    @Test public void limitedValidHandoffAndExpiry() {
        assertTrue(ShortsMediaHandoffPolicy.canAdopt(true, true, true, 65536, 0));
        assertTrue(ShortsMediaHandoffPolicy.canAdopt(true, true, true, 4 * 1024 * 1024, 14999));
        assertFalse(ShortsMediaHandoffPolicy.canAdopt(true, true, true, 65536, 15000));
        assertFalse(ShortsMediaHandoffPolicy.canAdopt(true, true, true, 65536, -1));
        assertFalse(ShortsMediaHandoffPolicy.canAdopt(true, true, true, 4 * 1024 * 1024L + 1, 10));
    }
}
