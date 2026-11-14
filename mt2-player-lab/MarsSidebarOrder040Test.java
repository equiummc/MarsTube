package com.liskovsoft.smartyoutubetv2.common.app.presenters.service;
import java.util.ArrayList;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

public class MarsSidebarOrder040Test {
    @Test public void swapsOnlyLegacySlotsAndIsIdempotent() {
        ArrayList<Integer> items = new ArrayList<>(Arrays.asList(1,2,3,0,8,9,10,6,5,12345));
        assertTrue(MarsSidebarOrder040.migrate(items,x->x,0,8));
        assertEquals(Arrays.asList(1,2,3,8,0,9,10,6,5,12345),items);
        assertFalse(MarsSidebarOrder040.migrate(items,x->x,0,8));
    }
    @Test public void respectsCustomOrderMissingAndEmptySections() {
        for (ArrayList<Integer> items : Arrays.asList(new ArrayList<Integer>(),
                new ArrayList<>(Arrays.asList(1,0,8,2,3,9,6,5)),
                new ArrayList<>(Arrays.asList(1,2,3,0,9,6,5)))) {
            ArrayList<Integer> before = new ArrayList<>(items);
            assertFalse(MarsSidebarOrder040.migrate(items,x->x,0,8));
            assertEquals(before,items);
        }
    }
}
