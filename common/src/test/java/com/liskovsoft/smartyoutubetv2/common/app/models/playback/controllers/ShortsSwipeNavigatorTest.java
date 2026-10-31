package com.liskovsoft.smartyoutubetv2.common.app.models.playback.controllers;

import org.junit.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;

/** MT-007: pure JVM regression tests for the isolated Shorts swipe path. */
public class ShortsSwipeNavigatorTest {
    private static final class Clip {
        final String id;
        final boolean shortVideo;
        final boolean available;
        Clip(String id, boolean shortVideo, boolean available) {
            this.id = id;
            this.shortVideo = shortVideo;
            this.available = available;
        }
    }
    private static Clip shortClip(int n) { return new Clip("short-" + n, true, true); }
    private static final ShortsSwipeNavigator.Eligible<Clip> SHORT_ONLY =
            item -> item.shortVideo && item.available;

    @Test public void downMovesOneShort() {
        Clip one = shortClip(1), two = shortClip(2);
        assertSame(two, ShortsSwipeNavigator.adjacent(Arrays.asList(one, two), one, 1, SHORT_ONLY));
    }

    @Test public void upMovesBackToThePreviousShort() {
        Clip one = shortClip(1), two = shortClip(2);
        assertSame(one, ShortsSwipeNavigator.adjacent(Arrays.asList(one, two), two, -1, SHORT_ONLY));
    }

    @Test public void neverSelectsNormalVideosOrUnavailableShorts() {
        Clip first = shortClip(1), regular = new Clip("normal", false, true);
        Clip unavailable = new Clip("upcoming", true, false), last = shortClip(2);
        assertSame(last, ShortsSwipeNavigator.adjacent(
                Arrays.asList(first, regular, unavailable, last), first, 1, SHORT_ONLY));
    }

    @Test public void noNextShortDoesNotWrapOrEscapeToQueue() {
        Clip last = shortClip(3);
        assertNull(ShortsSwipeNavigator.adjacent(Arrays.asList(shortClip(1), last),
                last, 1, SHORT_ONLY));
    }

    @Test public void noPreviousShortDoesNotWrap() {
        Clip first = shortClip(1);
        assertNull(ShortsSwipeNavigator.adjacent(Arrays.asList(first, shortClip(2)),
                first, -1, SHORT_ONLY));
    }

    @Test public void unknownCurrentAndInvalidDirectionFailClosed() {
        Clip one = shortClip(1);
        List<Clip> group = Arrays.asList(one, shortClip(2));
        assertNull(ShortsSwipeNavigator.adjacent(group, shortClip(99), 1, SHORT_ONLY));
        assertNull(ShortsSwipeNavigator.adjacent(group, one, 0, SHORT_ONLY));
        assertNull(ShortsSwipeNavigator.adjacent(group, one, 2, SHORT_ONLY));
        assertNull(ShortsSwipeNavigator.adjacent(null, one, 1, SHORT_ONLY));
    }

    @Test public void twentyConsecutiveShortSwipesStayInGroup() {
        List<Clip> clips = new ArrayList<>();
        for (int i = 0; i < 21; i++) clips.add(shortClip(i));
        Clip current = clips.get(0);
        for (int i = 1; i <= 20; i++) {
            current = ShortsSwipeNavigator.adjacent(clips, current, 1, SHORT_ONLY);
            assertSame(clips.get(i), current);
        }
        assertNull(ShortsSwipeNavigator.adjacent(clips, current, 1, SHORT_ONLY));
        for (int i = 19; i >= 0; i--) {
            current = ShortsSwipeNavigator.adjacent(clips, current, -1, SHORT_ONLY);
            assertSame(clips.get(i), current);
        }
    }

    @Test public void nullCandidatesAndEmptyGroupsAreSafe() {
        Clip a = shortClip(1), b = shortClip(2);
        assertSame(b, ShortsSwipeNavigator.adjacent(Arrays.asList(a, null, b), a, 1, SHORT_ONLY));
        assertNull(ShortsSwipeNavigator.adjacent(new ArrayList<Clip>(), a, 1, SHORT_ONLY));
    }
}
