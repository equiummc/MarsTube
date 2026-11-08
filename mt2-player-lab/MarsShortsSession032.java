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
    private static int epoch;

    private MarsShortsSession() {}

    public static synchronized void begin(List<Video> feed, @Nullable Video selected) {
        ITEMS.clear();
        currentId = null;
        epoch++;
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
        epoch++;
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

    /** Upcoming items only; never cross into the general video suggestions. */
    public static synchronized List<Video> upcoming(int limit) {
        List<Video> upcoming = new ArrayList<>();
        if (currentId == null || limit <= 0) return upcoming;
        int start = currentIndex();
        if (start < 0) return upcoming;
        for (int i = start + 1; i < ITEMS.size() && upcoming.size() < limit; i++) {
            upcoming.add(ITEMS.get(i));
        }
        return upcoming;
    }

    public static synchronized int currentIndex() {
        if (currentId == null) return -1;
        for (int i = 0; i < ITEMS.size(); i++) {
            if (currentId.equals(ITEMS.get(i).videoId)) return i;
        }
        return -1;
    }

    /** A stable, bounded snapshot; the selected preview does not advance playback. */
    public static synchronized int size() {
        return ITEMS.size();
    }

    @Nullable
    public static synchronized Video at(int index) {
        return index >= 0 && index < ITEMS.size() ? ITEMS.get(index) : null;
    }

    /** Only a confirmed OK changes the current Shorts position. */
    @Nullable
    public static synchronized Video select(int index) {
        Video target = at(index);
        if (target != null) currentId = target.videoId;
        return target;
    }

    /** Changes when a different Shorts feed becomes the current session. */
    public static synchronized int epoch() {
        return epoch;
    }

    public static synchronized int remaining() {
        int index = currentIndex();
        return index < 0 ? 0 : Math.max(0, ITEMS.size() - index - 1);
    }

    /**
     * Extend the active feed with fresh discovery metadata only.
     * Hold at most 180 Video objects and preserve at least three prior Shorts.
     * The 1,000 seen IDs are stored separately, never as Video/media objects.
     */
    public static synchronized int appendFresh(List<Video> fresh, Set<String> alreadySeen) {
        if (currentId == null || fresh == null) return 0;
        Set<String> known = new HashSet<>();
        for (Video v : ITEMS) if (v != null && v.videoId != null) known.add(v.videoId);
        int added = 0;
        for (Video v : fresh) {
            if (v == null || v.videoId == null || !v.hasVideo()
                    || known.contains(v.videoId)
                    || (alreadySeen != null && alreadySeen.contains(v.videoId))) continue;
            if (ITEMS.size() >= MAX_SHORTS) {
                if (currentIndex() <= 3) break;
                ITEMS.remove(0);
            }
            ITEMS.add(v);
            known.add(v.videoId);
            added++;
        }
        return added;
    }

    public static synchronized boolean hasNext() {
        int index = currentIndex();
        return index >= 0 && index + 1 < ITEMS.size();
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
