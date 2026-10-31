package com.liskovsoft.smartyoutubetv2.common.app.models.playback.controllers;

import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup;

import java.util.List;

/**
 * MT-007: navigate *only* within the current Shorts shelf.
 * Never fall through to the generic playlist / suggested next video.
 */
public final class ShortsNavigator {
    private ShortsNavigator() {
    }

    public static boolean isPlayableShort(Video video) {
        return video != null && video.isShorts && video.hasVideo()
                && !video.isUpcoming && !video.isLive;
    }

    public static Video adjacent(Video current, boolean forward) {
        if (!isPlayableShort(current)) {
            return null;
        }
        VideoGroup group = current.getGroup();
        if (group == null || group.isEmpty()) {
            return null;
        }
        List<Video> videos = group.getVideos();
        int position = -1;
        for (int i = 0; i < videos.size(); i++) {
            if (same(videos.get(i), current)) {
                position = i;
                break;
            }
        }
        if (position < 0) {
            return null;
        }
        for (int i = position + (forward ? 1 : -1);
             i >= 0 && i < videos.size(); i += forward ? 1 : -1) {
            Video candidate = videos.get(i);
            if (isPlayableShort(candidate) && !same(candidate, current)) {
                return candidate;
            }
        }
        return null;
    }

    private static boolean same(Video a, Video b) {
        return a == b || (a != null && b != null && a.videoId != null
                && a.videoId.equals(b.videoId));
    }
}
