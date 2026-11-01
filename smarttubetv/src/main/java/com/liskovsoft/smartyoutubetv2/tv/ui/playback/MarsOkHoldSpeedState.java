package com.liskovsoft.smartyoutubetv2.tv.ui.playback;

/**
 * MT-017: pure state for an OK key hold to temporarily boost playback speed.
 * The Activity owns long-press timing and forwards normal short taps unchanged.
 */
public final class MarsOkHoldSpeedState {
    private int keyCode = -1;
    private String videoId;
    private float previousSpeed = 1.0f;
    private boolean boosted;

    public boolean begin(int code, String id) {
        if (keyCode != -1 || id == null) return false;
        keyCode = code;
        videoId = id;
        boosted = false;
        return true;
    }

    public boolean owns(int code) {
        return keyCode == code && code != -1;
    }

    public boolean pending() {
        return keyCode != -1;
    }

    public boolean boosted() {
        return boosted;
    }

    public boolean isSameVideo(String id) {
        return id != null && id.equals(videoId);
    }

    public boolean beginBoost(float previous) {
        if (!pending() || boosted || previous <= 0 || Float.isNaN(previous)) {
            return false;
        }
        previousSpeed = previous;
        boosted = true;
        return true;
    }

    public float previousSpeed() {
        return previousSpeed;
    }

    public void reset() {
        keyCode = -1;
        videoId = null;
        previousSpeed = 1.0f;
        boosted = false;
    }
}
