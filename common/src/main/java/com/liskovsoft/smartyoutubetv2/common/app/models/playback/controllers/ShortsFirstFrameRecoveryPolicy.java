package com.liskovsoft.smartyoutubetv2.common.app.models.playback.controllers;

/**
 * MT-019: strictly one fresh-source rescue per selected Short.
 * We need the actual first rendered frame, not merely ExoPlayer STATE_READY.
 * No URLs or account information are retained.
 */
public final class ShortsFirstFrameRecoveryPolicy {
    public static final long INITIAL_TIMEOUT_MS = 8_000L;
    public static final long RETRY_TIMEOUT_MS = 10_000L;
    private String currentVideoId;
    private boolean firstFrameReceived;
    private boolean retryClaimed;

    public void selected(String videoId) {
        currentVideoId = videoId;
        firstFrameReceived = false;
        retryClaimed = false;
    }

    public boolean waitingForFirstFrame(String videoId) {
        return videoId != null && videoId.equals(currentVideoId) && !firstFrameReceived;
    }

    public void firstFrame(String videoId) {
        if (waitingForFirstFrame(videoId)) firstFrameReceived = true;
    }

    public boolean claimFreshRetry(String videoId) {
        if (!waitingForFirstFrame(videoId) || retryClaimed) return false;
        retryClaimed = true;
        return true;
    }

    public boolean hasRetried() {
        return retryClaimed;
    }

    public void clear() {
        selected(null);
    }
}
