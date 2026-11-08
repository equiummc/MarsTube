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
    public void upcomingPreviewsTrackOnlyActualShortsInTheirOwnOrder() {
        Video first = video("ppppppppppp", true);
        Video second = video("qqqqqqqqqqq", true);
        Video third = video("rrrrrrrrrrr", true);
        Video fourth = video("sssssssssss", true);
        Video fifth = video("ttttttttttt", true);
        MarsShortsSession.begin(Arrays.asList(first, second, third, fourth, fifth), first);
        assertEquals(0, MarsShortsSession.currentIndex());
        assertEquals(Arrays.asList(second, third, fourth, fifth), MarsShortsSession.upcoming(4));
        assertTrue(MarsShortsSession.hasNext());
        assertEquals(second.videoId, MarsShortsSession.step(first, 1).videoId);
        assertEquals(1, MarsShortsSession.currentIndex());
        assertEquals(Arrays.asList(third, fourth), MarsShortsSession.upcoming(2));
        assertEquals(third.videoId, MarsShortsSession.step(second, 1).videoId);
        assertEquals(fourth.videoId, MarsShortsSession.step(third, 1).videoId);
        assertEquals(fifth.videoId, MarsShortsSession.step(fourth, 1).videoId);
        assertFalse(MarsShortsSession.hasNext());
        assertTrue(MarsShortsSession.upcoming(4).isEmpty());
    }

    @Test
    public void previewDoesNotChangePlayingShortUntilSelectionConfirmed() {
        Video first = video("aaaaaaaaaaa", true);
        Video second = video("bbbbbbbbbbb", true);
        Video third = video("ccccccccccc", true);
        Video fourth = video("ddddddddddd", true);
        MarsShortsSession.begin(Arrays.asList(first, second, third, fourth), first);
        assertEquals(4, MarsShortsSession.size());
        assertEquals(fourth, MarsShortsSession.at(3));
        assertEquals(0, MarsShortsSession.currentIndex());
        assertTrue(MarsShortsSession.isActiveFor(first));
        // Focus can browse all four cards without selecting any.
        assertNull(MarsShortsSession.at(4));
        assertNull(MarsShortsSession.at(-1));
        assertEquals(0, MarsShortsSession.currentIndex());
        // OK on a preview intentionally skips ahead without changing the feed.
        assertEquals(fourth, MarsShortsSession.select(3));
        assertEquals(3, MarsShortsSession.currentIndex());
        assertFalse(MarsShortsSession.hasNext());
        assertNull(MarsShortsSession.select(4));
        assertEquals(3, MarsShortsSession.currentIndex());
    }

    @Test
    public void emptyQueueIsSafeForAutoplayAndPreviews() {
        assertEquals(-1, MarsShortsSession.currentIndex());
        assertTrue(MarsShortsSession.upcoming(4).isEmpty());
        assertFalse(MarsShortsSession.hasNext());
        Video first = video("uuuuuuuuuuu", true);
        MarsShortsSession.begin(Collections.singletonList(first), first);
        assertEquals(0, MarsShortsSession.currentIndex());
        assertFalse(MarsShortsSession.hasNext());
        assertTrue(MarsShortsSession.upcoming(4).isEmpty());
        MarsShortsSession.clear();
        assertEquals(-1, MarsShortsSession.currentIndex());
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

    @Test
    public void discoveryAddsOnlyUnseenUniqueShortsAndKeepsCurrentSelection() {
        Video a=video("AaAaAaAaAaA",true);
        Video b=video("BbBbBbBbBbB",true);
        Video c=video("CcCcCcCcCcC",true);
        MarsShortsSession.begin(Arrays.asList(a,b),a);
        java.util.Set<String> seen=new java.util.HashSet<>();
        seen.add(c.videoId);
        assertEquals(0,MarsShortsSession.appendFresh(Arrays.asList(a,b,c),seen));
        assertFalse(MarsShortsSession.hasNext()==false);
        assertEquals(1,MarsShortsSession.appendFresh(
                Arrays.asList(video("DdDdDdDdDdD",true)),seen));
        assertEquals(3,MarsShortsSession.size());
        assertEquals(a.videoId,MarsShortsSession.at(0).videoId);
        assertEquals(2,MarsShortsSession.remaining());
    }

    @Test
    public void longDiscoveryFeedNeverKeepsMoreThan180VideoObjects() {
        Video first=video("00000000000",true);
        java.util.List<Video> items=new java.util.ArrayList<>();
        items.add(first);
        for (int i=0;i<160;i++) items.add(video(String.format("%011d",i+1),true));
        MarsShortsSession.begin(items,first);
        for (int i=0;i<154;i++) {
            Video playing=MarsShortsSession.at(MarsShortsSession.currentIndex());
            assertNotNull(MarsShortsSession.step(playing,1));
        }
        java.util.List<Video> next=new java.util.ArrayList<>();
        for(int i=160;i<500;i++) next.add(video(String.format("%011d",i+1),true));
        assertTrue(MarsShortsSession.appendFresh(next,Collections.emptySet())>0);
        assertTrue(MarsShortsSession.size()<=180);
        assertTrue(MarsShortsSession.currentIndex()>=3);
        assertEquals(first.videoId, "00000000000");
    }

    @Test
    public void feedGenerationChangesOnNewSelectionAndExplicitClear() {
        Video first=video("00000000000",true);
        int before=MarsShortsSession.epoch();
        MarsShortsSession.begin(Collections.singletonList(first),first);
        int opened=MarsShortsSession.epoch();
        assertTrue(opened!=before);
        MarsShortsSession.clear();
        assertTrue(MarsShortsSession.epoch()!=opened);
    }
}
