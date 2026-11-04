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
        Video regular = video("ccccccccccc", false);
        MarsShortsSession.begin(Arrays.asList(first, regular, second), first);
        assertTrue(MarsShortsSession.isActiveFor(first));
        assertEquals(second.videoId, MarsShortsSession.step(first, 1).videoId);
        assertNull(MarsShortsSession.step(second, 1));
        assertEquals(first.videoId, MarsShortsSession.step(second, -1).videoId);
    }

    @Test
    public void portraitRegularVideosCannotCreateShortsSession() {
        Video regular = video("ddddddddddd", false);
        MarsShortsSession.begin(Collections.singletonList(regular), regular);
        assertFalse(MarsShortsSession.isActiveFor(regular));
        assertNull(MarsShortsSession.step(regular, 1));
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
    public void explicitClearReleasesPreviousQueue() {
        Video first = video("ggggggggggg", true);
        MarsShortsSession.begin(Collections.singletonList(first), first);
        assertTrue(MarsShortsSession.contains(first));
        MarsShortsSession.clear();
        assertFalse(MarsShortsSession.contains(first));
        assertFalse(MarsShortsSession.isActiveFor(first));
    }
}
