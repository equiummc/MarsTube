package com.newtube.mobile.ui.common;

import androidx.annotation.Nullable;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * TV Shorts browse handoff. Only videos explicitly opened from the dedicated Shorts
 * section may be traversed with DPAD_UP/DOWN. A portrait regular video is NOT a Short.
 *
 * This deliberately does not follow the generic next/previous recommendation queue.
 * The current feed snapshot is bounded to avoid holding unbounded Video objects.
 */
public final class MarsShortsSession {
    private static final int MAX_SHORTS = 180;
    private static final List<Video> ITEMS = new ArrayList<>();
    private static String currentId;

    private MarsShortsSession() {}

    public static synchronized void begin(List<Video> feed, @Nullable Video selected) {
        ITEMS.clear();
        currentId = null;
        if (selected == null || selected.videoId == null || feed == null) return;
        Set<String> seen = new HashSet<>();
        for (Video video : feed) {
            if (video != null && video.videoId != null && video.hasVideo()
                    && seen.add(video.videoId)) {
                ITEMS.add(video);
                if (ITEMS.size() >= MAX_SHORTS) break;
            }
        }
        // Never silently accept a non-Short or a video that isn't in the scoped feed.
        for (Video video : ITEMS) {
            if (selected.videoId.equals(video.videoId)) {
                currentId = video.videoId;
                return;
            }
        }
        ITEMS.clear();
    }

    public static synchronized void clear() {
        ITEMS.clear();
        currentId = null;
    }

    public static synchronized boolean contains(@Nullable Video video) {
        if (video == null || video.videoId == null) return false;
        for (Video candidate : ITEMS) {
            if (video.videoId.equals(candidate.videoId)) return true;
        }
        return false;
    }

    public static synchronized boolean isActiveFor(@Nullable Video video) {
        if (currentId == null || ITEMS.isEmpty()) return false;
        // During prepare, the playback presenter may not yet have bound the Video.
        // During a fast D-Pad switch, the player may still expose the previous
        // Short while the next media item is preparing. Both belong to this
        // scoped feed; normal recommendations are cleared before playback.
        return video == null || contains(video);
    }

    @Nullable
    public static synchronized Video step(@Nullable Video playing, int direction) {
        if (!isActiveFor(playing) || direction == 0) return null;
        for (int i = 0; i < ITEMS.size(); i++) {
            if (currentId.equals(ITEMS.get(i).videoId)) {
                int next = i + (direction > 0 ? 1 : -1);
                if (next < 0 || next >= ITEMS.size()) return null;
                Video video = ITEMS.get(next);
                currentId = video.videoId;
                return video;
            }
        }
        return null;
    }
}
