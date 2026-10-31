package com.liskovsoft.smartyoutubetv2.common.app.models.playback.controllers;

import org.junit.Test;
import static org.junit.Assert.*;

public class ShortsPrefetchPolicyTest {
    @Test public void freshWithinBoundedTime() {
        assertTrue(ShortsPrefetchPolicy.fresh(1000, 5000, 15000));
        assertTrue(ShortsPrefetchPolicy.fresh(1000, 15999, 15000));
        assertFalse(ShortsPrefetchPolicy.fresh(1000, 16000, 15000));
    }

    @Test public void rejectsUnknownAndBackwardsClock() {
        assertFalse(ShortsPrefetchPolicy.fresh(0, 1000, 15000));
        assertFalse(ShortsPrefetchPolicy.fresh(5000, 4000, 15000));
        assertFalse(ShortsPrefetchPolicy.fresh(1000, 2000, 0));
    }
}
