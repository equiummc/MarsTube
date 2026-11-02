package com.liskovsoft.smartyoutubetv2.common.app.models.playback.controllers;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class MarsAutoNextPolicyTest {
    private int decide(boolean shorts, boolean autoShorts, boolean autoVideos,
                       boolean repeat, boolean live, boolean finish, boolean embed) {
        return MarsAutoNextPolicy.decide(shorts, autoShorts, autoVideos, repeat, live, finish, embed);
    }

    @Test public void disabledShortsPauseAtBeginningForManualReplay() {
        assertEquals(MarsAutoNextPolicy.PAUSE_FOR_REPLAY,
                decide(true, false, false, false, false, false, false));
        // Even if the legacy default "Loop Shorts" is enabled,
        // the explicit Mars-Spezial OFF switch wins.
        assertEquals(MarsAutoNextPolicy.PAUSE_FOR_REPLAY,
                decide(true, false, false, true, false, false, false));
    }

    @Test public void enabledShortsGoNextRegardlessOfVideoPreference() {
        assertEquals(MarsAutoNextPolicy.NEXT,
                decide(true, true, false, true, false, false, false));
        assertEquals(MarsAutoNextPolicy.PAUSE_FOR_REPLAY,
                decide(true, false, true, false, false, false, false));
    }

    @Test public void normalVideosAreIndependentFromShorts() {
        assertEquals(MarsAutoNextPolicy.PAUSE_FOR_REPLAY,
                decide(false, true, false, false, false, false, false));
        assertEquals(MarsAutoNextPolicy.NEXT,
                decide(false, false, true, false, false, false, false));
    }

    @Test public void userExplicitRepeatOneIsNeverOverriddenForNormalVideo() {
        assertEquals(MarsAutoNextPolicy.KEEP_ORIGINAL,
                decide(false, true, true, true, false, false, false));
    }

    @Test public void liveEmbedAndFinishOnEndedRemainOriginal() {
        for (boolean shortItem : new boolean[]{true, false}) {
            assertEquals(MarsAutoNextPolicy.KEEP_ORIGINAL,
                    decide(shortItem, true, true, false, true, false, false));
            assertEquals(MarsAutoNextPolicy.KEEP_ORIGINAL,
                    decide(shortItem, true, true, false, false, true, false));
            assertEquals(MarsAutoNextPolicy.KEEP_ORIGINAL,
                    decide(shortItem, true, true, false, false, false, true));
        }
    }
}
