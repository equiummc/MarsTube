package com.liskovsoft.smartyoutubetv2.common.app.models.playback.controllers;

/**
 * MT-023: end-of-playback actions independent of the legacy repeat setting.
 * The setting never schedules a background timer or a speculative stream.
 */
public final class MarsAutoNextPolicy {
    public static final int KEEP_ORIGINAL = 0;
    public static final int NEXT = 1;
    public static final int PAUSE_FOR_REPLAY = 2;

    private MarsAutoNextPolicy() {}

    public static int decide(boolean shortVideo, boolean autoShorts, boolean autoVideos,
                             boolean repeatOne, boolean live, boolean finishOnEnded,
                             boolean embedded) {
        if (live || finishOnEnded || embedded || (!shortVideo && repeatOne)) {
            return KEEP_ORIGINAL;
        }
        return (shortVideo ? autoShorts : autoVideos) ? NEXT : PAUSE_FOR_REPLAY;
    }
}
