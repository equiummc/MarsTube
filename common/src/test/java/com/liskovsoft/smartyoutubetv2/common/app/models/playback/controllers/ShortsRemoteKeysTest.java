package com.liskovsoft.smartyoutubetv2.common.app.models.playback.controllers;

import android.view.KeyEvent;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class ShortsRemoteKeysTest {
    @Test public void remoteUpDownSwipeOnlyForShorts() {
        assertEquals(1, ShortsRemoteKeys.direction(KeyEvent.KEYCODE_DPAD_DOWN, true, false));
        assertEquals(-1, ShortsRemoteKeys.direction(KeyEvent.KEYCODE_DPAD_UP, true, false));
        assertEquals(0, ShortsRemoteKeys.direction(KeyEvent.KEYCODE_DPAD_DOWN, false, false));
    }

    @Test public void menusTakePrecedenceAndLeftRightStayForSeeking() {
        assertEquals(0, ShortsRemoteKeys.direction(KeyEvent.KEYCODE_DPAD_UP, true, true));
        assertEquals(0, ShortsRemoteKeys.direction(KeyEvent.KEYCODE_DPAD_RIGHT, true, false));
        assertEquals(0, ShortsRemoteKeys.direction(KeyEvent.KEYCODE_DPAD_LEFT, true, false));
    }

    @Test public void pauseAndBackAreNotHijacked() {
        assertEquals(0, ShortsRemoteKeys.direction(KeyEvent.KEYCODE_DPAD_CENTER, true, false));
        assertEquals(0, ShortsRemoteKeys.direction(KeyEvent.KEYCODE_BACK, true, false));
    }
}
