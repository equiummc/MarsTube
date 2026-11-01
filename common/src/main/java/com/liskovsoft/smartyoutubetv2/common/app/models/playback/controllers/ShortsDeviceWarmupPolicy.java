package com.liskovsoft.smartyoutubetv2.common.app.models.playback.controllers;

/**
 * MT-020: Thomson's three-ahead metadata window is independent of sample preloading.
 * Only the immediately next eligible SABR Short may use the existing bounded
 * single-player sample queue when the foreground and memory are healthy.
 */
public final class ShortsDeviceWarmupPolicy {
    public static final long MIN_AVAILABLE_MEMORY_BYTES = 160L * 1024 * 1024;

    private ShortsDeviceWarmupPolicy() {}

    public static boolean isThomsonTarget(String device, String model) {
        return "nagata".equalsIgnoreCase(device) && model != null &&
                model.toLowerCase(java.util.Locale.ROOT).contains("thomson");
    }

    public static boolean shouldPreloadThomsonSample(
            String device, String model, int distance, boolean playing,
            boolean loading, boolean lowMemory, long availableMemoryBytes) {
        return isThomsonTarget(device, model) &&
                distance == 1 && playing && !loading && !lowMemory &&
                availableMemoryBytes >= MIN_AVAILABLE_MEMORY_BYTES;
    }

    public static boolean allowSecondSampleCandidate(String device, String model) {
        // Fire's experimental two-slot behavior is unchanged; Thomson gets exactly one.
        return !isThomsonTarget(device, model);
    }
}
