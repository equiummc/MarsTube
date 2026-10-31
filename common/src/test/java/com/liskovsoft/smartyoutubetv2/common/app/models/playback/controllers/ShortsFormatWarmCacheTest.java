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

    @Test public void invalidSignedFormatCanBeEvicted() {
        ShortsFormatWarmCache<String> cache = new ShortsFormatWarmCache<>(3, 15000);
        cache.put("one", "expired-url", 100);
        cache.remove("one");
        assertNull(cache.get("one", 110));
        cache.remove(null);
    }

    @Test public void eightShortsSurviveBackAndForthThenOldestEvicts() {
        ShortsFormatWarmCache<String> cache = new ShortsFormatWarmCache<>(8, 60000);
        for (int i = 0; i < 8; i++) {
            cache.put("short-" + i, "fmt-" + i, 100 + i);
        }
        assertEquals(8, cache.size());
        for (int i = 0; i < 8; i++) {
            assertEquals("fmt-" + i, cache.get("short-" + i, 900));
        }
        // Access to 0 made 0 recent; adding a ninth evicts 1.
        cache.put("short-8", "fmt-8", 1000);
        assertNull(cache.get("short-0", 1001)); // 0 was oldest after walking 0..7
        assertEquals("fmt-8", cache.get("short-8", 1001));
        assertEquals(8, cache.size());
    }

    @Test public void extendedCacheStillExpiresAndNeverCrossesClockEpoch() {
        ShortsFormatWarmCache<String> cache = new ShortsFormatWarmCache<>(8, 60000);
        cache.put("one", "fmt", 250);
        assertEquals("fmt", cache.get("one", 60249));
        assertNull(cache.get("one", 60250));
        cache.put("two", "fmt2", 1500);
        assertNull(cache.get("two", 1400));
    }

    @Test public void longWatchKeepsSignedMetadataForThirtyMinutes() {
        ShortsFormatWarmCache<String> cache = new ShortsFormatWarmCache<>(12, 30 * 60000);
        cache.put("one", "format", 100);
        assertEquals("format", cache.get("one", 25 * 60000));
        assertNull(cache.get("one", 30 * 60000 + 100));
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
