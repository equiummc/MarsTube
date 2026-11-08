package com.newtube.mobile.ui.common;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.misc.MediaServiceManager;
import com.liskovsoft.youtubeapi.service.YouTubeServiceManager;
import com.liskovsoft.mediaserviceinterfaces.MediaItemService;
import com.liskovsoft.mediaserviceinterfaces.data.MediaItem;
import com.liskovsoft.mediaserviceinterfaces.data.FeedbackEndpoint;
import io.reactivex.rxjava3.core.Observable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The same "Nicht interessiert" list filters normal videos AND Shorts.
 * Every signed-in YouTube profile has its own 100,000-item SQLite index;
 * the unsigned user has the separate anonymous profile. No delete/reset.
 *
 * All queries are bounded and only a page of IDs is materialized, never a
 * 100,000-element HashSet for each feed render or Shorts auto-next.
 */
public final class MarsTvInterest037 {
    private static final String TAG = "MarsTvInterest037";
    private static final String LEGACY_PREF = "mars_tv_interest_036";
    private static final String LEGACY_KEY = "hidden_video_ids";
    private static final String MIGRATED = "migrated_to_sqlite_037";
    private static final int MAX_PER_PROFILE = MarsTvInterestPolicy037.MAX_IDS;
    private static final int BATCH_SIZE = 250;
    private static MarsTvInterest037 singleton;
    private final Context app;
    private final Store store;
    private String lastProfile;
    private long lastCount;

    private static final class Store extends SQLiteOpenHelper {
        Store(Context context) { super(context, "mars_video_rejections_037.db", null, 1); }
        @Override public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE rejected (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "profile_key TEXT NOT NULL," +
                    "video_id TEXT NOT NULL," +
                    "UNIQUE(profile_key,video_id))");
            db.execSQL("CREATE INDEX rejected_profile_fifo ON rejected(profile_key,id)");
        }
        @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            throw new IllegalStateException("Unexpected rejection-database schema change");
        }
    }

    private MarsTvInterest037(Context context) {
        app = context.getApplicationContext();
        store = new Store(app);
    }
    public static synchronized MarsTvInterest037 instance(Context context) {
        if (singleton == null) singleton = new MarsTvInterest037(context);
        return singleton;
    }

    /**
     * The native Account ID and name distinguish channels under one Google
     * sign-in in the same way as NewTube's account-bound suggestions cache.
     * Store only an opaque SHA-256 key, not IDs, emails or names.
     */
    public synchronized String activeProfileKey() {
        try {
            if (MediaServiceManager.instance().getSelectedAccount() == null)
                return "anonymous";
            String id = String.valueOf(MediaServiceManager.instance().getSelectedAccount().getId());
            String name = MediaServiceManager.instance().getSelectedAccount().getName();
            if (TextUtils.isEmpty(id) && TextUtils.isEmpty(name))
                return "anonymous";
            return MarsTvInterestPolicy037.profileKey(
                    String.valueOf(id) + "|" + String.valueOf(name));
        } catch (RuntimeException e) {
            Log.w(TAG, "Account not yet resolved; anonymous local list", e);
            return "anonymous";
        }
    }

    /** Called on first access; previous 036 entries follow the account active at
     * migration time, because the old format contains no profile metadata.
     */
    private void ensureProfile(SQLiteDatabase db, String profile) {
        SharedPreferences old = app.getSharedPreferences(LEGACY_PREF, Context.MODE_PRIVATE);
        if (!old.getBoolean(MIGRATED, false)) {
            String previous = old.getString(LEGACY_KEY, "");
            if (!TextUtils.isEmpty(previous)) {
                db.beginTransaction();
                try {
                    for (String id : previous.split("\n")) {
                        if (!TextUtils.isEmpty(id)) {
                            ContentValues values = new ContentValues(2);
                            values.put("profile_key", profile);
                            values.put("video_id", id);
                            db.insertWithOnConflict("rejected", null, values,
                                    SQLiteDatabase.CONFLICT_IGNORE);
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }
            // Never delete the old data during migration. Keep a durable marker
            // so switching profile cannot duplicate legacy rejections.
            if (!old.edit().putBoolean(MIGRATED, true).commit())
                throw new IllegalStateException("Legacy migration marker could not be saved");
        }
        if (!profile.equals(lastProfile)) {
            lastProfile = profile;
            lastCount = DatabaseUtils.longForQuery(db,
                    "SELECT COUNT(*) FROM rejected WHERE profile_key=?",
                    new String[]{profile});
        }
    }

    public synchronized long count() {
        SQLiteDatabase db = store.getWritableDatabase();
        ensureProfile(db, activeProfileKey());
        return lastCount;
    }

    public synchronized boolean isHidden(Video video) {
        if (video == null || TextUtils.isEmpty(video.videoId)) return false;
        SQLiteDatabase db = store.getWritableDatabase();
        String profile = activeProfileKey();
        ensureProfile(db, profile);
        try (Cursor c = db.rawQuery(
                "SELECT 1 FROM rejected WHERE profile_key=? AND video_id=? LIMIT 1",
                new String[]{profile, video.videoId})) {
            return c.moveToFirst();
        }
    }

    public synchronized boolean rejectLocal(Video video) {
        if (video == null || TextUtils.isEmpty(video.videoId)) return false;
        SQLiteDatabase db = store.getWritableDatabase();
        String profile = activeProfileKey();
        ensureProfile(db, profile);
        db.beginTransaction();
        try {
            boolean existed;
            try (Cursor c = db.rawQuery(
                    "SELECT 1 FROM rejected WHERE profile_key=? AND video_id=? LIMIT 1",
                    new String[]{profile,video.videoId})) {
                existed = c.moveToFirst();
            }
            // Update recency on a repeated rejection. FIFO cap applies only
            // to this profile and has no effect on the other profiles.
            if (existed) db.delete("rejected",
                    "profile_key=? AND video_id=?", new String[]{profile, video.videoId});
            ContentValues values = new ContentValues(2);
            values.put("profile_key", profile);
            values.put("video_id", video.videoId);
            if (db.insertOrThrow("rejected", null, values) == -1)
                throw new IllegalStateException("Cannot persist rejected video");
            long count = lastCount + (existed ? 0 : 1);
            int overflow = MarsTvInterestPolicy037.overflow(count);
            if (overflow > 0) {
                db.execSQL("DELETE FROM rejected WHERE id IN (" +
                          "SELECT id FROM rejected WHERE profile_key=? " +
                          "ORDER BY id LIMIT ?)",
                          new Object[]{profile, overflow});
                count -= overflow;
            }
            db.setTransactionSuccessful();
            lastCount = count;
            return true;
        } finally {
            db.endTransaction();
        }
    }

    /** One indexed batch query per page, rather than 100,000 in-memory IDs. */
    public synchronized List<Video> filter(List<Video> videos) {
        if (videos == null || videos.isEmpty()) return Collections.emptyList();
        SQLiteDatabase db = store.getWritableDatabase();
        String profile = activeProfileKey();
        ensureProfile(db, profile);
        if (lastCount == 0) return videos;
        ArrayList<String> ids = new ArrayList<>(videos.size());
        for (Video video : videos) {
            if (video != null && !TextUtils.isEmpty(video.videoId)) ids.add(video.videoId);
        }
        if (ids.isEmpty()) return videos;
        Set<String> hidden = new HashSet<>();
        for (int from = 0; from < ids.size(); from += BATCH_SIZE) {
            int until = Math.min(ids.size(), from + BATCH_SIZE);
            StringBuilder q = new StringBuilder(
                    "SELECT video_id FROM rejected WHERE profile_key=? AND video_id IN (");
            String[] args = new String[until - from + 1];
            args[0] = profile;
            for (int i = from; i < until; i++) {
                if (i > from) q.append(',');
                q.append('?');
                args[i-from+1] = ids.get(i);
            }
            q.append(')');
            try (Cursor c = db.rawQuery(q.toString(), args)) {
                while (c.moveToNext()) hidden.add(c.getString(0));
            }
        }
        if (hidden.isEmpty()) return videos;
        ArrayList<Video> result = new ArrayList<>(videos.size());
        for (Video video : videos)
            if (video == null || video.videoId == null || !hidden.contains(video.videoId))
                result.add(video);
        return result;
    }

    /** Optional official feedback if signed in and an actual token is supplied.
     * Success is never claimed to the user before the service confirms it.
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
            token.flatMap(service::sendFeedbackObserve).take(1)
                    .subscribe(result -> Log.i(TAG, "Feedback accepted=" +
                            (result.getCode() >= 200 && result.getCode() < 300)),
                            err -> Log.w(TAG, "Remote rejection failed; local retained", err));
            return true;
        } catch (RuntimeException ex) {
            Log.w(TAG, "Remote feedback unavailable; local retained", ex);
            return false;
        }
    }
}
