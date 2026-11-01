package com.liskovsoft.smartyoutubetv2.tv.ui.playback;

import org.junit.Test;
import static org.junit.Assert.*;

public class MarsOkHoldSpeedStateTest {
    @Test public void shortPressDoesNotBoostAndDoesNotStealOtherKeys() {
        MarsOkHoldSpeedState state = new MarsOkHoldSpeedState();
        assertTrue(state.begin(23, "short-a"));
        assertTrue(state.owns(23));
        assertFalse(state.owns(66));
        assertFalse(state.boosted());
        state.reset();
        assertFalse(state.pending());
    }

    @Test public void holdBoostPreservesOriginalRate() {
        MarsOkHoldSpeedState state = new MarsOkHoldSpeedState();
        assertTrue(state.begin(23, "video-a"));
        assertTrue(state.beginBoost(1.5f));
        assertTrue(state.boosted());
        assertEquals(1.5f, state.previousSpeed(), 0.001f);
        assertFalse(state.beginBoost(1.0f));
        state.reset();
        assertEquals(1.0f, state.previousSpeed(), 0.001f);
        assertFalse(state.boosted());
    }

    @Test public void changingVideoInvalidatesOriginalSpeedOverride() {
        MarsOkHoldSpeedState state = new MarsOkHoldSpeedState();
        assertTrue(state.begin(66, "short-a"));
        assertTrue(state.beginBoost(1.0f));
        assertTrue(state.isSameVideo("short-a"));
        assertFalse(state.isSameVideo("short-b"));
        assertFalse(state.isSameVideo(null));
        state.reset();
        assertFalse(state.isSameVideo("short-a"));
    }

    @Test public void invalidOrMissingPlaybackRateDoesNotBoost() {
        MarsOkHoldSpeedState state = new MarsOkHoldSpeedState();
        assertFalse(state.begin(23, null));
        assertTrue(state.begin(23, "v"));
        assertFalse(state.begin(23, "v")); // repeat can't start a second hold
        assertFalse(state.beginBoost(-1f));
        assertFalse(state.beginBoost(Float.NaN));
        assertTrue(state.beginBoost(1f));
    }

    @Test public void secondHoldAfterReleaseCanBoostAgain() {
        MarsOkHoldSpeedState state = new MarsOkHoldSpeedState();
        assertTrue(state.begin(23, "a"));
        assertTrue(state.beginBoost(1.0f));
        state.reset();
        assertTrue(state.begin(23, "b"));
        assertTrue(state.beginBoost(1.25f));
        assertEquals(1.25f, state.previousSpeed(), 0.001f);
    }
}
