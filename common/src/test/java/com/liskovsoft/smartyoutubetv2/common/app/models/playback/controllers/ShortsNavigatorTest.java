package com.liskovsoft.smartyoutubetv2.common.app.models.playback.controllers;

import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

/** MT-007: remote swipe must never jump from a Short to a regular video. */
public class ShortsNavigatorTest {
    private static Video item(String id, boolean shortVideo) {
        Video v = new Video();
        v.videoId = id;
        v.isShorts = shortVideo;
        return v;
    }

    @Test public void nextSkipsOrdinaryVideos() {
        Video one = item("short-1", true);
        Video regular = item("normal", false);
        Video two = item("short-2", true);
        VideoGroup group = VideoGroup.from(Arrays.asList(one, regular, two));
        assertNotNull(group);
        assertSame(two, ShortsNavigator.adjacent(one, true));
        assertSame(one, ShortsNavigator.adjacent(two, false));
    }

    @Test public void endDoesNotFallThroughToGeneralVideo() {
        Video last = item("last", true);
        Video normal = item("unrelated", false);
        VideoGroup group = VideoGroup.from(Arrays.asList(last, normal));
        assertNotNull(group);
        assertNull(ShortsNavigator.adjacent(last, true));
    }

    @Test public void noGroupOrUnplayableCandidatesAreIgnored() {
        Video shortVideo = item("short", true);
        assertNull(ShortsNavigator.adjacent(shortVideo, true));
        Video upcoming = item("upcoming", true);
        upcoming.isUpcoming = true;
        VideoGroup group = VideoGroup.from(Arrays.asList(shortVideo, upcoming));
        assertNotNull(group);
        assertNull(ShortsNavigator.adjacent(shortVideo, true));
        assertFalse(ShortsNavigator.isPlayableShort(upcoming));
    }

    @Test public void regularVideosCannotEnterShortsNavigation() {
        Video regular = item("regular", false);
        Video candidate = item("short", true);
        VideoGroup group = VideoGroup.from(Arrays.asList(regular, candidate));
        assertNotNull(group);
        assertNull(ShortsNavigator.adjacent(regular, true));
    }

    @Test public void skipsDuplicateAndInvalidEntries() {
        Video a = item("a", true);
        Video duplicate = item("a", true);
        Video invalid = item(null, true);
        Video b = item("b", true);
        VideoGroup group = VideoGroup.from(Arrays.asList(a, duplicate, invalid, b));
        assertNotNull(group);
        assertSame(b, ShortsNavigator.adjacent(a, true));
    }
}
