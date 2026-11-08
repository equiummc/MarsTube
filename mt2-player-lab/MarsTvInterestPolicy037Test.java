package com.newtube.mobile.ui.common;

import org.junit.Test;
import static org.junit.Assert.*;

public final class MarsTvInterestPolicy037Test {
    @Test public void capacityIsOneHundredThousandPerProfile() {
        assertEquals(100000, MarsTvInterestPolicy037.MAX_IDS);
        assertEquals(0, MarsTvInterestPolicy037.overflow(99999));
        assertEquals(0, MarsTvInterestPolicy037.overflow(100000));
        assertEquals(1, MarsTvInterestPolicy037.overflow(100001));
        assertEquals(150, MarsTvInterestPolicy037.overflow(100150));
    }
    @Test public void accountAndChannelHaveIndependentKeys() {
        String a=MarsTvInterestPolicy037.profileKey("google-a|Channel 1");
        String b=MarsTvInterestPolicy037.profileKey("google-a|Channel 2");
        String c=MarsTvInterestPolicy037.profileKey("google-b|Channel 1");
        assertNotEquals(a,b);
        assertNotEquals(a,c);
        assertEquals(a,MarsTvInterestPolicy037.profileKey("google-a|Channel 1"));
        assertFalse(a.contains("Channel 1"));
    }
}
