package com.liskovsoft.smartyoutubetv2.common.app.models.playback.controllers;

import org.junit.Test;
import static org.junit.Assert.*;

public class ShortsFormatWarmCacheTest {
    @Test public void repeatedShortAvoidsFetch() {
        ShortsFormatWarmCache<String> cache = new ShortsFormatWarmCache<>(2, 15000);
        cache.put("one", "known-format", 100);
        assertEquals("known-format", cache.get("one", 200));
    }

    @Test public void expiresAtTtlBoundary() {
        ShortsFormatWarmCache<String> cache = new ShortsFormatWarmCache<>(2, 15000);
        cache.put("one", "format", 100);
        assertEquals("format", cache.get("one", 15099));
        assertNull(cache.get("one", 15100));
        assertEquals(0, cache.size());
    }

    @Test public void neverReusesFromPreviousClockEpoch() {
        ShortsFormatWarmCache<String> cache = new ShortsFormatWarmCache<>(2, 15000);
        cache.put("one", "format", 15000);
        assertNull(cache.get("one", 14999));
    }

    @Test public void recentShortSurvivesLruEviction() {
        ShortsFormatWarmCache<String> cache = new ShortsFormatWarmCache<>(2, 15000);
        cache.put("one", "A", 100);
        cache.put("two", "B", 200);
        assertEquals("A", cache.get("one", 300));
        cache.put("three", "C", 400);
        assertNull(cache.get("two", 500));
        assertEquals("A", cache.get("one", 500));
        assertEquals("C", cache.get("three", 500));
    }

    @Test public void clearedAtAccountChangeAndExit() {
        ShortsFormatWarmCache<String> cache = new ShortsFormatWarmCache<>(2, 15000);
        cache.put("one", "A", 100);
        cache.clear();
        assertNull(cache.get("one", 200));
        assertEquals(0, cache.size());
    }

    @Test public void invalidDataNeverStored() {
        ShortsFormatWarmCache<String> cache = new ShortsFormatWarmCache<>(2, 15000);
        cache.put(null, "A", 100);
        cache.put("", "A", 100);
        cache.put("one", null, 100);
        assertEquals(0, cache.size());
        assertNull(cache.get(null, 200));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnboundedCapacity() {
        new ShortsFormatWarmCache<String>(0, 15000);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnboundedTtl() {
        new ShortsFormatWarmCache<String>(2, 0);
    }
}
