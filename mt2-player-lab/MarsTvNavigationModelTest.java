package com.newtube.mobile.ui.common;

import org.junit.Test;
import static org.junit.Assert.*;

public class MarsTvNavigationModelTest {
    @Test public void homeIsCentralAndUsesUnchangedMVector() {
        MarsTvNavigationModel.Entry[] entries = MarsTvNavigationModel.main();
        assertEquals(7, entries.length);
        assertEquals(1, entries[0].destination);
        assertEquals(3, MarsTvNavigationModel.HOME_INDEX);
        assertEquals(0, entries[MarsTvNavigationModel.HOME_INDEX].destination);
        assertEquals("Startseite", entries[MarsTvNavigationModel.HOME_INDEX].label);
        assertEquals(5, entries[6].destination);
    }
    @Test public void wrappingWorksInBothDirections() {
        assertEquals(0, MarsTvNavigationModel.wrap(6, 1, 7));
        assertEquals(6, MarsTvNavigationModel.wrap(0, -1, 7));
        assertEquals(2, MarsTvNavigationModel.wrap(1, 8, 7));
        assertEquals(0, MarsTvNavigationModel.wrap(0, -1, 0));
    }
    @Test public void shortsAndNormalVideoHaveIdenticalMainNavigation() {
        assertEquals(7, MarsTvNavigationModel.playerCount(false));
        assertEquals(7, MarsTvNavigationModel.playerCount(true));
        assertEquals(0, MarsTvNavigationModel.wrap(6, 1, MarsTvNavigationModel.playerCount(false)));
        assertEquals(6, MarsTvNavigationModel.wrap(0, -1, MarsTvNavigationModel.playerCount(true)));
    }
    @Test public void modelEntriesCannotBeReorderedExternally() {
        MarsTvNavigationModel.Entry[] entries = MarsTvNavigationModel.main();
        entries[0] = entries[3];
        assertEquals(1, MarsTvNavigationModel.main()[0].destination);
    }
}
