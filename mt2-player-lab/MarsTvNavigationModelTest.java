package com.newtube.mobile.ui.common;

import org.junit.Test;
import static org.junit.Assert.*;

public class MarsTvNavigationModelTest {
    @Test public void homeIsFirstAndReplacesHouseButton() {
        MarsTvNavigationModel.Entry[] entries = MarsTvNavigationModel.main();
        assertEquals(7, entries.length);
        assertEquals(0, entries[0].destination);
        assertEquals("Startseite", entries[0].label);
        assertEquals(5, entries[6].destination);
    }
    @Test public void wrappingWorksInBothDirections() {
        assertEquals(0, MarsTvNavigationModel.wrap(6, 1, 7));
        assertEquals(6, MarsTvNavigationModel.wrap(0, -1, 7));
        assertEquals(2, MarsTvNavigationModel.wrap(1, 8, 7));
        assertEquals(0, MarsTvNavigationModel.wrap(0, -1, 0));
    }
    @Test public void shortsExtraOnlyExistsDuringShorts() {
        assertEquals(7, MarsTvNavigationModel.playerCount(false));
        assertEquals(8, MarsTvNavigationModel.playerCount(true));
        assertEquals(0, MarsTvNavigationModel.wrap(6, 1, MarsTvNavigationModel.playerCount(false)));
        assertEquals(7, MarsTvNavigationModel.wrap(0, -1, MarsTvNavigationModel.playerCount(true)));
    }
    @Test public void modelEntriesCannotBeReorderedExternally() {
        MarsTvNavigationModel.Entry[] entries = MarsTvNavigationModel.main();
        entries[0] = entries[3];
        assertEquals(0, MarsTvNavigationModel.main()[0].destination);
    }
}
