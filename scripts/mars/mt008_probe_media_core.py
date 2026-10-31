#!/usr/bin/env python3
"""MT-008 CI-only performance probe for the pinned MediaServiceCore submodule.

This instruments latency; it does NOT alter client order, retry policy, auth,
tokens, responses, caching, request URLs or concurrent access. The working
submodule becomes temporarily dirty in CI. Never commit it as a new gitlink.
"""
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[2]
CORE = ROOT / "MediaServiceCore"
EXPECTED_SHA = "40fec843eb24bc264e9c8b429fcee25068e230e2"
TAG = '"MarsTubePerf"'

sha = subprocess.check_output(
    ["git", "-C", str(CORE), "rev-parse", "HEAD"], text=True
).strip()
if sha != EXPECTED_SHA:
    raise SystemExit("STOP: MediaServiceCore submodule SHA changed; review probe anchors before use")

def patch(relative, replacements):
    path = CORE / relative
    original = path.read_text(encoding="utf-8")
    edited = original
    for label, before, after in replacements:
        if edited.count(before) != 1:
            raise SystemExit(f"STOP: MT-008 {relative} anchor {label} mismatch")
        edited = edited.replace(before, after)
    if original == edited:
        raise SystemExit("STOP: probe did not change the submodule")
    path.write_text(edited, encoding="utf-8", newline="\n")
    print(f"MT-008 instrumented: {relative}")

patch(
    "youtubeapi/src/main/java/com/liskovsoft/youtubeapi/service/internal/FormatInfoWrapper.kt",
    [
        (
            "lock",
            """        try {
            synchronized(lock) {
                return selectPlaybackFormatInfo(videoId, clickTrackingParams)
            }
        } finally {
            ReloadPlaybackGate.reset(videoId)
        }""",
            """        val waitStarted = android.os.SystemClock.elapsedRealtime()
        try {
            synchronized(lock) {
                val acquired = android.os.SystemClock.elapsedRealtime()
                try {
                    return selectPlaybackFormatInfo(videoId, clickTrackingParams)
                } finally {
                    android.util.Log.i("MarsTubePerf",
                        "MT-008 upstream lock_wait_ms=" + (acquired - waitStarted) +
                        " lock_held_ms=" + (android.os.SystemClock.elapsedRealtime() - acquired))
                }
            }
        } finally {
            ReloadPlaybackGate.reset(videoId)
        }""",
        ),
        (
            "providers",
            """    private fun selectPlaybackFormatInfo(videoId: String, clickTrackingParams: String?): MediaItemFormatInfo? {
        var formatInfo = getPrimaryProvider()(videoId, clickTrackingParams)

        if (formatInfo != null && formatInfo.isUnplayable()) {
            formatInfo = getSecondaryProvider()(videoId, clickTrackingParams)
        }

        return formatInfo
    }""",
            """    private fun selectPlaybackFormatInfo(videoId: String, clickTrackingParams: String?): MediaItemFormatInfo? {
        val primaryStarted = android.os.SystemClock.elapsedRealtime()
        var formatInfo = getPrimaryProvider()(videoId, clickTrackingParams)
        val primaryMs = android.os.SystemClock.elapsedRealtime() - primaryStarted

        var fallbackMs = -1L
        if (formatInfo != null && formatInfo.isUnplayable()) {
            val fallbackStarted = android.os.SystemClock.elapsedRealtime()
            formatInfo = getSecondaryProvider()(videoId, clickTrackingParams)
            fallbackMs = android.os.SystemClock.elapsedRealtime() - fallbackStarted
        }
        android.util.Log.i("MarsTubePerf",
            "MT-008 upstream primary_ms=" + primaryMs + " fallback_ms=" + fallbackMs)
        return formatInfo
    }""",
        ),
        (
            "auth",
            """    private fun checkSigned() {
        getSignInService().checkAuth()
    }""",
            """    private fun checkSigned() {
        val started = android.os.SystemClock.elapsedRealtime()
        try {
            getSignInService().checkAuth()
        } finally {
            android.util.Log.i("MarsTubePerf",
                "MT-008 upstream auth_ms=" + (android.os.SystemClock.elapsedRealtime() - started))
        }
    }""",
        ),
    ],
)

patch(
    "youtubeapi/src/main/java/com/liskovsoft/youtubeapi/videoinfo/V2/VideoInfoService.java",
    [
        (
            "playable",
            """        VideoInfo result = firstPlayable(videoId, clickTrackingParams);

        if (result == null) {""",
            """        long mt008Started = android.os.SystemClock.elapsedRealtime();
        VideoInfo result = firstPlayable(videoId, clickTrackingParams);
        android.util.Log.i("MarsTubePerf", "MT-008 api playable_ms=" +
                (android.os.SystemClock.elapsedRealtime() - mt008Started));
        if (result == null) {""",
        ),
        (
            "fixes",
            """        applyFixesIfNeeded(result, videoId, clickTrackingParams);

        transformFormats(result);""",
            """        long mt008FixStarted = android.os.SystemClock.elapsedRealtime();
        applyFixesIfNeeded(result, videoId, clickTrackingParams);
        long mt008FixMs = android.os.SystemClock.elapsedRealtime() - mt008FixStarted;
        long mt008TransformStarted = android.os.SystemClock.elapsedRealtime();
        transformFormats(result);
        android.util.Log.i("MarsTubePerf", "MT-008 api fixes_ms=" + mt008FixMs +
                " transform_ms=" + (android.os.SystemClock.elapsedRealtime() - mt008TransformStarted));""",
        ),
        (
            "client_try",
            """            VideoInfo result = getVideoInfoWithRentFix(nextClient, videoId, clickTrackingParams);

            if (result != null && infoTester.test(result)) {""",
            """            long mt008ClientStart = android.os.SystemClock.elapsedRealtime();
            VideoInfo result = getVideoInfoWithRentFix(nextClient, videoId, clickTrackingParams);
            android.util.Log.i("MarsTubePerf", "MT-008 api client_attempt_ms=" +
                    (android.os.SystemClock.elapsedRealtime() - mt008ClientStart) +
                    " playable=" + (result != null && !result.isUnplayable()));

            if (result != null && infoTester.test(result)) {""",
        ),
    ],
)

patch(
    "youtubeapi/src/main/java/com/liskovsoft/youtubeapi/app/PoTokenGate.kt",
    [
        (
            "pot",
            """        releaseOtherProviders(client)
        return when {
            client.isWebPotRequired -> if (videoId != null) getWebContentPoToken(videoId) else getWebSessionPoToken()
            client.isTvPotRequired -> getTvPoToken()
            else -> null
        }""",
            """        val mt008Started = android.os.SystemClock.elapsedRealtime()
        try {
            releaseOtherProviders(client)
            return when {
                client.isWebPotRequired -> if (videoId != null) getWebContentPoToken(videoId) else getWebSessionPoToken()
                client.isTvPotRequired -> getTvPoToken()
                else -> null
            }
        } finally {
            android.util.Log.i("MarsTubePerf", "MT-008 upstream potoken_ms=" +
                (android.os.SystemClock.elapsedRealtime() - mt008Started) +
                " required=" + (client.isTvPotRequired || client.isWebPotRequired))
        }""",
        ),
    ],
)

print("MT-008 probe enabled: only durations/booleans, no account, token, ID or URL")
