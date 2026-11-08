package com.newtube.mobile.ui.common;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.widget.Toast;
import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup;
import com.liskovsoft.youtubeapi.service.YouTubeServiceManager;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * On-demand TV Shorts discovery. Request only one YouTube shorts metadata page at a
 * time; never download future streams, preload video surfaces or start a decoder.
 *
 * Exactly 1,000 IDs (not Video objects or watch URLs) are retained locally.
 * The separate playback session holds at most 180 real Video items.
 *
 * This is a best-effort continuous feed: if YouTube returns no unseen results
 * after bounded pagination, leave the current Short intact instead of looping
 * old videos, or falling back to normal video recommendations.
 */
public final class MarsShortsDiscovery {
    private static final int MAX_SEEN = 1000;
    private static final int MAX_EMPTY_PAGES = 4;
    private static final long EMPTY_BACKOFF_MS = 20_000L;
    private static final String PREFS = "mars_shorts_discovery_032";
    private static final String SEEN_KEY = "watched_ids";
    private final Context context;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final SharedPreferences prefs;
    private final LinkedHashSet<String> seen = new LinkedHashSet<>();
    private MediaGroup page;
    private Disposable disposable;
    private Runnable awaitingNext;
    private int epoch = -1;
    private int emptyPages;
    private int receivedFresh;
    private long coolUntil;
    private boolean inFlight;
    private boolean released;

    public MarsShortsDiscovery(Context source) {
        context = source.getApplicationContext();
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        MarsShortsSession.setExcluded(MarsTvInterest036.instance(context).snapshot());
        String stored = prefs.getString(SEEN_KEY, "");
        if (stored != null && !stored.isEmpty()) {
            for (String id : stored.split("\\n")) {
                if (!id.isEmpty() && seen.size() < MAX_SEEN) seen.add(id);
            }
        }
    }

    /** Call only after the current Shorts video rendered its first frame. */
    public void markWatched(Video video) {
        if (released || video == null || video.videoId == null
                || !MarsShortsSession.isActiveFor(video)) return;
        if (seen.remove(video.videoId)) {
            // Keep most recently watched IDs at the end.
        }
        seen.add(video.videoId);
        while (seen.size() > MAX_SEEN) {
            seen.remove(seen.iterator().next());
        }
        StringBuilder packed = new StringBuilder(seen.size() * 12);
        for (String id : seen) packed.append(id).append('\n');
        prefs.edit().putString(SEEN_KEY, packed.toString()).apply();
    }

    /** Called every few seconds by the existing player UI ticker. */
    public void maybeLoad() {
        if (released) return;
        synchronizeSession();
        if (MarsShortsSession.currentIndex() < 0
                || MarsShortsSession.remaining() > 3 || inFlight
                || SystemClock.uptimeMillis() < coolUntil) return;
        fetch();
    }

    /**
     * At the end of the current page: wait for fresh metadata, then invoke the
     * existing player-next callback ONCE. No generic next-video fallback.
     */
    public void requestNext(Runnable onNewItem) {
        if (released || onNewItem == null) return;
        synchronizeSession();
        if (MarsShortsSession.currentIndex() < 0) return;
        if (MarsShortsSession.hasNext()) {
            main.post(onNewItem);
            return;
        }
        // Multiple remote events while a page is loading do not queue many jumps.
        // Avoid hundreds of requests if the player reports END more than once.
        // A later manual next can retry after the bounded cooldown.
        if (awaitingNext == null) awaitingNext = onNewItem;
        if (!inFlight && SystemClock.uptimeMillis() >= coolUntil) {
            emptyPages = 0;
            fetch();
        }
    }

    private void synchronizeSession() {
        int latest = MarsShortsSession.epoch();
        if (epoch == latest) return;
        if (disposable != null) disposable.dispose();
        disposable = null;
        inFlight = false;
        awaitingNext = null;
        page = null;
        emptyPages = 0;
        receivedFresh = 0;
        coolUntil = 0L;
        MarsShortsSession.setExcluded(MarsTvInterest036.instance(context).snapshot());
        epoch = latest;
    }

    private void fetch() {
        if (released || inFlight || MarsShortsSession.currentIndex() < 0
                || SystemClock.uptimeMillis() < coolUntil) return;
        final int requestedEpoch = epoch;
        inFlight = true;
        Observable<MediaGroup> request;
        try {
            if (page != null && page.getNextPageKey() != null) {
                request = YouTubeServiceManager.instance().getContentService()
                        .continueGroupObserve(page);
            } else {
                request = YouTubeServiceManager.instance().getContentService()
                        .getShortsObserve();
            }
            disposable = request.take(1).subscribe(
                    group -> main.post(() -> receive(requestedEpoch, group)),
                    error -> main.post(() -> finish(requestedEpoch, false)),
                    () -> main.post(() -> finish(requestedEpoch, true)));
        } catch (RuntimeException error) {
            main.post(() -> finish(requestedEpoch, false));
        }
    }

    private void receive(int requestedEpoch, MediaGroup group) {
        if (released || requestedEpoch != MarsShortsSession.epoch() || group == null) return;
        page = group;
        VideoGroup videos = VideoGroup.from(group);
        if (videos == null || videos.getVideos() == null) return;
        Set<String> excluded = new HashSet<>(seen);
        excluded.addAll(MarsTvInterest036.instance(context).snapshot());
        receivedFresh += MarsShortsSession.appendFresh(videos.getVideos(), excluded);
    }

    private void finish(int requestedEpoch, boolean success) {
        if (released || requestedEpoch != MarsShortsSession.epoch()) return;
        disposable = null;
        inFlight = false;
        if (receivedFresh > 0) {
            receivedFresh = 0;
            emptyPages = 0;
            coolUntil = 0L;
            if (awaitingNext != null && MarsShortsSession.hasNext()) {
                Runnable next = awaitingNext;
                awaitingNext = null;
                main.post(next);
            }
            return;
        }
        receivedFresh = 0;
        emptyPages++;
        if (success && emptyPages < MAX_EMPTY_PAGES && awaitingNext != null) {
            // The current page contained only duplicates/watched IDs.
            fetch();
            return;
        }
        coolUntil = SystemClock.uptimeMillis() + EMPTY_BACKOFF_MS;
        if (awaitingNext != null) {
            awaitingNext = null;
            Toast.makeText(context, success
                    ? "Gerade keine weiteren Shorts verfügbar"
                    : "Shorts-Nachladen fehlgeschlagen",
                    Toast.LENGTH_SHORT).show();
        }
    }

    public void release() {
        released = true;
        if (disposable != null) disposable.dispose();
        disposable = null;
        awaitingNext = null;
        main.removeCallbacksAndMessages(null);
    }
}
