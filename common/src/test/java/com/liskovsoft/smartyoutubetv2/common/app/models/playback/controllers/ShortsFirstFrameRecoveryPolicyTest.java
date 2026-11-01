package com.liskovsoft.smartyoutubetv2.common.app.models.playback.controllers;

import org.junit.Test;
import static org.junit.Assert.*;

public class ShortsFirstFrameRecoveryPolicyTest {
    @Test public void missingFirstFrameTriggersExactlyOneFreshRetry() {
        ShortsFirstFrameRecoveryPolicy p = new ShortsFirstFrameRecoveryPolicy();
        p.selected("short1");
        assertTrue(p.waitingForFirstFrame("short1"));
        assertTrue(p.claimFreshRetry("short1"));
        assertFalse(p.claimFreshRetry("short1"));
        assertTrue(p.hasRetried());
    }

    @Test public void successfulFirstFrameCancelsRetry() {
        ShortsFirstFrameRecoveryPolicy p = new ShortsFirstFrameRecoveryPolicy();
        p.selected("short1");
        p.firstFrame("short1");
        assertFalse(p.waitingForFirstFrame("short1"));
        assertFalse(p.claimFreshRetry("short1"));
    }

    @Test public void lateFrameFromPreviousShortCannotCompleteNewOne() {
        ShortsFirstFrameRecoveryPolicy p = new ShortsFirstFrameRecoveryPolicy();
        p.selected("short1");
        p.selected("short2");
        p.firstFrame("short1");
        assertTrue(p.waitingForFirstFrame("short2"));
        assertFalse(p.claimFreshRetry("short1"));
        assertTrue(p.claimFreshRetry("short2"));
    }

    @Test public void changingVideoRestoresRetryBudget() {
        ShortsFirstFrameRecoveryPolicy p = new ShortsFirstFrameRecoveryPolicy();
        p.selected("old");
        assertTrue(p.claimFreshRetry("old"));
        p.selected("new");
        assertFalse(p.hasRetried());
        assertTrue(p.claimFreshRetry("new"));
    }

    @Test public void neverRetryAfterEngineIsCleared() {
        ShortsFirstFrameRecoveryPolicy p = new ShortsFirstFrameRecoveryPolicy();
        p.selected("short");
        p.clear();
        assertFalse(p.claimFreshRetry("short"));
    }

    @Test public void boundedTimeouts() {
        assertTrue(ShortsFirstFrameRecoveryPolicy.INITIAL_TIMEOUT_MS >= 6_000);
        assertTrue(ShortsFirstFrameRecoveryPolicy.RETRY_TIMEOUT_MS >=
                ShortsFirstFrameRecoveryPolicy.INITIAL_TIMEOUT_MS);
    }
}
