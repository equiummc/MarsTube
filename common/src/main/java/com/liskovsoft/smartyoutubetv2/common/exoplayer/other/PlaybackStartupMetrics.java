package com.liskovsoft.smartyoutubetv2.common.exoplayer.other;

import android.os.SystemClock;
import android.util.Log;

/**
 * Temporary release-compatible playback startup diagnostics for MarsTube.
 * Outputs elapsed timings only, never video IDs, titles, URLs or account data.
 */
public final class PlaybackStartupMetrics {
    private static final String TAG = "MarsTubePerf";
    private static int sSession;
    private static String sVideoId;
    private static long sOpenedMs;
    private static long sFormatRequestedMs;
    private static long sFormatResolvedMs;
    private static long sSourcePreparedMs;
    private static long sPlayerReadyMs;
    private static boolean sFirstFrameReported;

    private PlaybackStartupMetrics() {
    }

    public static synchronized void videoSelected(String videoId) {
        if (videoId == null) {
            return;
        }

        sSession++;
        sVideoId = videoId;
        sOpenedMs = SystemClock.elapsedRealtime();
        sFormatRequestedMs = -1;
        sFormatResolvedMs = -1;
        sSourcePreparedMs = -1;
        sPlayerReadyMs = -1;
        sFirstFrameReported = false;
        Log.i(TAG, "session=" + sSession + " selected");
    }

    public static synchronized void formatRequested(String videoId) {
        if (!matches(videoId)) {
            return;
        }

        sFormatRequestedMs = SystemClock.elapsedRealtime();
        Log.i(TAG, "session=" + sSession + " format_request since_select_ms=" + (sFormatRequestedMs - sOpenedMs));
    }

    public static synchronized void formatResolved(String videoId) {
        if (!matches(videoId) || sFormatRequestedMs < 0) {
            return;
        }

        sFormatResolvedMs = SystemClock.elapsedRealtime();
        Log.i(TAG, "session=" + sSession + " format_ready format_ms=" + (sFormatResolvedMs - sFormatRequestedMs)
                + " total_ms=" + (sFormatResolvedMs - sOpenedMs));
    }

    public static synchronized void sourcePrepared(String videoId, String sourceType) {
        if (!matches(videoId)) {
            return;
        }

        sSourcePreparedMs = SystemClock.elapsedRealtime();
        sPlayerReadyMs = -1;
        sFirstFrameReported = false;
        Log.i(TAG, "session=" + sSession + " player_prepare source=" + sourceType
                + " since_format_ms=" + (sFormatResolvedMs < 0 ? -1 : sSourcePreparedMs - sFormatResolvedMs)
                + " total_ms=" + (sSourcePreparedMs - sOpenedMs));
    }

    public static synchronized void playerReady(String videoId) {
        if (!matches(videoId) || sSourcePreparedMs < 0 || sPlayerReadyMs >= 0) {
            return;
        }

        sPlayerReadyMs = SystemClock.elapsedRealtime();
        Log.i(TAG, "session=" + sSession + " player_ready prepare_to_ready_ms=" + (sPlayerReadyMs - sSourcePreparedMs)
                + " total_ms=" + (sPlayerReadyMs - sOpenedMs));
    }

    public static synchronized void firstFrame(String videoId) {
        if (!matches(videoId) || sSourcePreparedMs < 0 || sFirstFrameReported) {
            return;
        }

        sFirstFrameReported = true;
        long now = SystemClock.elapsedRealtime();
        Log.i(TAG, "session=" + sSession + " first_frame ready_to_frame_ms="
                + (sPlayerReadyMs < 0 ? -1 : now - sPlayerReadyMs)
                + " total_ms=" + (now - sOpenedMs));
    }

    public static synchronized void failure(String videoId, String phase, String errorType) {
        if (!matches(videoId)) {
            return;
        }

        Log.i(TAG, "session=" + sSession + " error phase=" + phase
                + " type=" + errorType + " total_ms=" + (SystemClock.elapsedRealtime() - sOpenedMs));
    }

    private static boolean matches(String videoId) {
        return videoId != null && videoId.equals(sVideoId) && sOpenedMs > 0;
    }
}
