package com.liskovsoft.smartyoutubetv2.common.app.models.playback.controllers;

import org.junit.Test;
import static org.junit.Assert.*;

/** Fire-TV regression: missing continuation callbacks must never block indefinitely. */
public class ShortsContinuationRecoveryPolicyTest {
    @Test public void oneAutomaticRetryThenStop() {
        ShortsContinuationRecoveryPolicy p = new ShortsContinuationRecoveryPolicy();
        assertEquals(0, p.retries());
        assertTrue(p.claimTimeoutRetry());
        assertEquals(1, p.retries());
        assertFalse(p.claimTimeoutRetry());
        assertFalse(p.claimTimeoutRetry());
    }

    @Test public void newShortRestoresOneRetryBudget() {
        ShortsContinuationRecoveryPolicy p = new ShortsContinuationRecoveryPolicy();
        assertTrue(p.claimTimeoutRetry());
        assertFalse(p.claimTimeoutRetry());
        p.reset();
        assertEquals(0, p.retries());
        assertTrue(p.claimTimeoutRetry());
    }

    @Test public void eachControllerOwnsItsOwnBudget() {
        ShortsContinuationRecoveryPolicy a = new ShortsContinuationRecoveryPolicy();
        ShortsContinuationRecoveryPolicy b = new ShortsContinuationRecoveryPolicy();
        assertTrue(a.claimTimeoutRetry());
        assertFalse(a.claimTimeoutRetry());
        assertEquals(0, b.retries());
        assertTrue(b.claimTimeoutRetry());
    }
}
