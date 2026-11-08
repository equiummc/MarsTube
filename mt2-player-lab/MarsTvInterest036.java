package com.newtube.mobile.ui.common;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.misc.MediaServiceManager;
import com.liskovsoft.youtubeapi.service.YouTubeServiceManager;
import com.liskovsoft.mediaserviceinterfaces.MediaItemService;
import com.liskovsoft.mediaserviceinterfaces.data.MediaItem;
import com.liskovsoft.mediaserviceinterfaces.data.FeedbackEndpoint;
import io.reactivex.rxjava3.core.Observable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Local, bounded, per-video negative recommendations. Never claims remote success.
 * When signed in and the original item has a feedback token, also sends the
 * standard YouTube feedback asynchronously; lack of token means LOCAL ONLY.
 */
public final class MarsTvInterest036 {
    private static final String PREFS = "mars_tv_interest_036";
    private static final String KEY = "hidden_video_ids";
    private static final int MAX_IDS = 1000;
    private static MarsTvInterest036 instance;
    private final SharedPreferences preferences;
    private final LinkedHashSet<String> hidden = new LinkedHashSet<>();
    private MarsTvInterest036(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String stored = preferences.getString(KEY, "");
        if (!TextUtils.isEmpty(stored)) {
            for (String id : stored.split("\n")) if (!id.isEmpty()) hidden.add(id);
        }
        trim();
    }

    public static synchronized MarsTvInterest036 instance(Context context) {
        if (instance == null) instance = new MarsTvInterest036(context);
        return instance;
    }

    public synchronized boolean isHidden(Video video) {
        return video != null && video.videoId != null && hidden.contains(video.videoId);
    }

    public synchronized Set<String> snapshot() {
        return new LinkedHashSet<>(hidden);
    }

    public synchronized boolean rejectLocal(Video video) {
        if (video == null || TextUtils.isEmpty(video.videoId)) return false;
        hidden.remove(video.videoId);
        hidden.add(video.videoId);
        trim();
        StringBuilder packed = new StringBuilder(hidden.size() * 12);
        for (String id : hidden) packed.append(id).append('\n');
        preferences.edit().putString(KEY, packed.toString()).apply();
        return true;
    }

    private void trim() {
        while (hidden.size() > MAX_IDS) hidden.remove(hidden.iterator().next());
    }

    public synchronized List<Video> filter(List<Video> videos) {
        if (videos == null || videos.isEmpty() || hidden.isEmpty())
            return videos == null ? Collections.emptyList() : videos;
        ArrayList<Video> result = new ArrayList<>(videos.size());
        for (Video item : videos) if (!isHidden(item)) result.add(item);
        return result;
    }

    /**
     * Optional remote feedback; never replaces the immediate local filter.
     * Returns true only when a legitimate signed-in feedback request was started.
     * Server rejection is logged; UI does not incorrectly report a remote success.
     */
    public boolean sendSignedInFeedback(Video video) {
        if (video == null || video.mediaItem == null
                || MediaServiceManager.instance().getSelectedAccount() == null) return false;
        MediaItem item = video.mediaItem;
        FeedbackEndpoint endpoint = item.getFeedbackEndpoint();
        if (endpoint == null && item.getFeedbackToken() == null) return false;
        try {
            MediaItemService service = YouTubeServiceManager.instance().getMediaItemService();
            Observable<String> token = endpoint != null
                    ? service.getFeedbackTokensObserve(endpoint).map(tokens -> {
                        if (tokens == null || tokens.isEmpty())
                            throw new IllegalStateException("No YouTube feedback token");
                        return tokens.get(0);
                    })
                    : Observable.just(item.getFeedbackToken());
            token.flatMap(service::sendFeedbackObserve)
                    .take(1)
                    .subscribe(result -> android.util.Log.i("MarsTubeTV",
                            "Not-interested feedback delivered=" + (result.getCode() >= 200
                                    && result.getCode() < 300)),
                            err -> android.util.Log.w("MarsTubeTV",
                                    "Not-interested remote feedback failed", err));
            return true;
        } catch (RuntimeException ex) {
            android.util.Log.w("MarsTubeTV", "Remote feedback unavailable; local only", ex);
            return false;
        }
    }
}
