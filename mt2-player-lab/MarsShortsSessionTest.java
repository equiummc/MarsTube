package com.newtube.mobile.ui.common;

import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import java.util.Arrays;
import java.util.Collections;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

/** Scoped Shorts feed must never silently move into generic recommendations. */
public class MarsShortsSessionTest {
    private static Video video(String id, boolean shorts) {
        Video result = Video.from(id);
        result.isShorts = shorts;
        return result;
    }

    @After
    public void reset() {
        MarsShortsSession.clear();
    }

    @Test
    public void nextAndPreviousStayWithinActualShortsFeed() {
        Video first = video("aaaaaaaaaaa", true);
        Video second = video("bbbbbbbbbbb", true);
        MarsShortsSession.begin(Arrays.asList(first, second), first);
        assertTrue(MarsShortsSession.isActiveFor(first));
        assertEquals(second.videoId, MarsShortsSession.step(first, 1).videoId);
        assertNull(MarsShortsSession.step(second, 1));
        assertEquals(first.videoId, MarsShortsSession.step(second, -1).videoId);
    }

    @Test
    public void unrelatedRegularVideosCannotHijackAnExistingShortsFeed() {
        Video first = video("ddddddddddd", true);
        Video regular = video("hhhhhhhhhhh", false);
        MarsShortsSession.begin(Collections.singletonList(first), first);
        assertFalse(MarsShortsSession.contains(regular));
        assertFalse(MarsShortsSession.isActiveFor(regular));
        assertNull(MarsShortsSession.step(regular, 1));
    }

    @Test
    public void unflaggedItemsInAnActualShortsSectionStillWork() {
        // The source section is TYPE_SHORTS, even when individual API flags are inconsistent.
        Video first = video("iiiiiiiiiii", false);
        Video second = video("jjjjjjjjjjj", false);
        MarsShortsSession.begin(Arrays.asList(first, second), first);
        assertTrue(MarsShortsSession.isActiveFor(first));
        assertEquals(second.videoId, MarsShortsSession.step(first, 1).videoId);
    }

    @Test
    public void unrelatedVideoIsNeverAcceptedAsCurrentShort() {
        Video shortVideo = video("eeeeeeeeeee", true);
        Video other = video("fffffffffff", false);
        MarsShortsSession.begin(Collections.singletonList(shortVideo), shortVideo);
        assertFalse(MarsShortsSession.isActiveFor(other));
        assertNull(MarsShortsSession.step(other, 1));
    }

    @Test
    public void rapidNextDoesNotFallBackToRegularPlayerWhilePreparing() {
        Video first = video("mmmmmmmmmmm", true);
        Video second = video("nnnnnnnnnnn", true);
        Video third = video("ooooooooooo", true);
        MarsShortsSession.begin(Arrays.asList(first, second, third), first);
        assertEquals(second.videoId, MarsShortsSession.step(first, 1).videoId);
        // The player can still report first during the next prepare.
        assertTrue(MarsShortsSession.isActiveFor(first));
        assertEquals(third.videoId, MarsShortsSession.step(first, 1).videoId);
        assertNull(MarsShortsSession.step(second, 1));
    }

    @Test
    public void explicitClearReleasesPreviousQueue() {
        Video first = video("ggggggggggg", true);
        MarsShortsSession.begin(Collections.singletonList(first), first);
        assertTrue(MarsShortsSession.contains(first));
        MarsShortsSession.clear();
        assertFalse(MarsShortsSession.contains(first));
        assertFalse(MarsShortsSession.isActiveFor(first));
    }
}
