package com.liskovsoft.smartyoutubetv2.common.app.models.playback.controllers;

import android.view.KeyEvent;

/** MT-007: Up/down mean swipe only while a Short plays without an open overlay. */
public final class ShortsRemoteKeys {
    private ShortsRemoteKeys() {
    }

    /** -1 previous, +1 next, 0 leave the key to existing UI behavior. */
    public static int direction(int keyCode, boolean isShort, boolean overlayOrDialog) {
        if (!isShort || overlayOrDialog) return 0;
        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) return 1;
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP) return -1;
        return 0;
    }
}
