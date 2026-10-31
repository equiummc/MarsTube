package com.liskovsoft.smartyoutubetv2.common.app.models.playback.controllers;

import java.util.List;

/**
 * Selection policy for remote-control Shorts swipes. Deliberately does not
 * consult the general video queue, recommendations or mixed playlists.
 * No Android dependency, so edge cases can be unit tested on the JVM.
 */
public final class ShortsSwipeNavigator {
    public interface Eligible<T> {
        boolean accepts(T item);
    }

    private ShortsSwipeNavigator() {
    }

    /** Direction is +1 for down/next and -1 for up/previous. Never wraps. */
    public static <T> T adjacent(List<T> items, T current, int direction, Eligible<T> eligible) {
        if (items == null || current == null || eligible == null ||
                (direction != 1 && direction != -1)) {
            return null;
        }
        int index = items.indexOf(current);
        if (index < 0) {
            return null; // Unknown group: fail closed rather than opening a random video.
        }
        for (int i = index + direction; i >= 0 && i < items.size(); i += direction) {
            T candidate = items.get(i);
            if (candidate != null && eligible.accepts(candidate)) {
                return candidate;
            }
        }
        return null;
    }
}
