package com.liskovsoft.smartyoutubetv2.common.app.models.playback.controllers;

import org.junit.Test;
import static org.junit.Assert.*;
import static com.liskovsoft.smartyoutubetv2.common.app.models.playback.controllers.ShortsSamplePreloadGate.*;

public class ShortsSamplePreloadGateTest {
    private Input input(boolean enabled, boolean shortVods, boolean foreground,
            boolean quality, boolean sabr, boolean sabrVerified, boolean samplesVerified) {
        return new Input(enabled, shortVods, shortVods, false, true,
                foreground, foreground, quality, sabr, sabrVerified, samplesVerified);
    }

    @Test public void remainsOffByDefault() {
        assertEquals(Decision.SWITCH_OFF, evaluate(input(false, true, true, true, true, false, false)));
        assertEquals(Decision.SWITCH_OFF, evaluate(null));
    }

    @Test public void noSabrPreloadWithoutVerifiedHandoff() {
        assertEquals(Decision.SABR_HANDOFF_NOT_PROVEN,
                evaluate(input(true, true, true, true, true, false, true)));
    }

    @Test public void noSecondPlayerUnlessSamplesCanBeTransferred() {
        assertEquals(Decision.SOURCE_HANDOFF_NOT_PROVEN,
                evaluate(input(true, true, true, true, false, false, false)));
    }

    @Test public void onlyReadyBufferedForeground() {
        assertEquals(Decision.FOREGROUND_BUSY,
                evaluate(input(true, true, false, true, false, false, true)));
    }

    @Test public void noLiveOrRegularVideos() {
        assertEquals(Decision.NOT_SHORT_VOD,
                evaluate(input(true, false, true, true, true, true, true)));
        Input live = new Input(true, true, true, true, true,
                true, true, true, true, true, true);
        assertEquals(Decision.NOT_SHORT_VOD, evaluate(live));
    }

    @Test public void noUnknownCandidateAndNoQualityOverride() {
        Input noNext = new Input(true, true, true, false, false,
                true, true, true, false, false, true);
        assertEquals(Decision.INVALID_NEXT, evaluate(noNext));
        assertEquals(Decision.QUALITY_MISMATCH,
                evaluate(input(true, true, true, false, false, false, true)));
    }

    @Test public void gateOpensOnlyAfterFullVerifiedHandoff() {
        assertEquals(Decision.ALLOW, evaluate(input(true, true, true, true, true, true, true)));
        assertEquals(Decision.ALLOW, evaluate(input(true, true, true, true, false, false, true)));
    }

    @Test public void fixedMemoryAndRequestBudgets() {
        assertEquals(4 * 1024 * 1024, MAX_PRELOAD_BYTES);
        assertEquals(4_000, MAX_PRELOAD_LEAD_MS);
        assertEquals(15_000, MAX_PRELOAD_DURATION_MS);
        assertEquals(2, MAX_TARGETS);
    }
}
