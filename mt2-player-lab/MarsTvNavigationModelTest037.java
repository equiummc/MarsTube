package com.newtube.mobile.ui.common;

import org.junit.Test;
import static org.junit.Assert.*;

/** Nine consistent TV destinations, with M/start page still centered. */
public class MarsTvNavigationModelTest {
    @Test public void homeStaysCentralAndLiveAndMusicAreRealDestinations() {
        MarsTvNavigationModel.Entry[] entries = MarsTvNavigationModel.main();
        assertEquals(9, entries.length);
        assertEquals(1, entries[0].destination);
        assertEquals(4, MarsTvNavigationModel.HOME_INDEX);
        assertEquals(0, entries[MarsTvNavigationModel.HOME_INDEX].destination);
        assertEquals("Startseite", entries[MarsTvNavigationModel.HOME_INDEX].label);
        assertEquals(9, entries[2].destination);
        assertEquals("Live", entries[2].label);
        assertEquals(10, entries[6].destination);
        assertEquals("Musik", entries[6].label);
        assertEquals(5, entries[8].destination);
    }
    @Test public void wrappingWorksInBothDirections() {
        assertEquals(0, MarsTvNavigationModel.wrap(8, 1, 9));
        assertEquals(8, MarsTvNavigationModel.wrap(0, -1, 9));
        assertEquals(2, MarsTvNavigationModel.wrap(1, 10, 9));
        assertEquals(0, MarsTvNavigationModel.wrap(0, -1, 0));
    }
    @Test public void shortsAndNormalVideosHaveTheSameNavigation() {
        assertEquals(9, MarsTvNavigationModel.playerCount(false));
        assertEquals(9, MarsTvNavigationModel.playerCount(true));
        assertEquals(0, MarsTvNavigationModel.wrap(8, 1,
                MarsTvNavigationModel.playerCount(false)));
        assertEquals(8, MarsTvNavigationModel.wrap(0, -1,
                MarsTvNavigationModel.playerCount(true)));
    }
    @Test public void modelEntriesCannotBeReorderedExternally() {
        MarsTvNavigationModel.Entry[] entries = MarsTvNavigationModel.main();
        entries[0] = entries[4];
        assertEquals(1, MarsTvNavigationModel.main()[0].destination);
    }
}
