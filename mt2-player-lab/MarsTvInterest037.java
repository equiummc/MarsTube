package com.newtube.mobile.ui.common;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
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

/** Persisted, bounded 50k negative recommendations; original 036 entries migrate safely.
 * Global to this TV, not falsely advertised as a YouTube account preference.
 */
public final class MarsTvInterest037 {
    private static final String DATABASE = "mars_tv_interest_037.db";
    private static final String TABLE = "hidden_video";
    private static final String LEGACY_PREFS = "mars_tv_interest_036";
    private static final String LEGACY_KEY = "hidden_video_ids";
    private static final String MIGRATED = "migrated_to_sqlite_037";
    private static final int LIMIT = 50_000;
    private static MarsTvInterest037 instance;
    private final SQLiteDatabase db;
    private final LinkedHashSet<String> hidden = new LinkedHashSet<>();
    private long sequence;

    private static final class Db extends SQLiteOpenHelper {
        Db(Context context) { super(context, DATABASE, null, 1); }
        @Override public void onCreate(SQLiteDatabase database) {
            database.execSQL("CREATE TABLE hidden_video (" +
                    "video_id TEXT PRIMARY KEY NOT NULL, seq INTEGER NOT NULL)");
            database.execSQL("CREATE INDEX idx_hidden_video_seq ON hidden_video(seq)");
        }
        @Override public void onUpgrade(SQLiteDatabase database, int old, int version) {
            // No destructive migrations; new schema changes must be handled explicitly.
        }
    }

    private MarsTvInterest037(Context context) {
        Context app = context.getApplicationContext();
        db = new Db(app).getWritableDatabase();
        // One small indexed SQL scan per process, not reserializing an XML string
        // every time the user rejects another Short.
        try (Cursor cursor = db.rawQuery(
                "SELECT video_id, seq FROM hidden_video ORDER BY seq ASC", null)) {
            while (cursor.moveToNext()) {
                hidden.add(cursor.getString(0));
                sequence = Math.max(sequence, cursor.getLong(1));
            }
        }
        SharedPreferences legacy = app.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE);
        if (!legacy.getBoolean(MIGRATED, false)) {
            String old = legacy.getString(LEGACY_KEY, "");
            db.beginTransaction();
            try {
                if (!TextUtils.isEmpty(old)) for (String id : old.split("\n")) {
                    if (!id.isEmpty() && !hidden.contains(id)) {
                        addRow(id);
                    }
                }
                trimDatabase();
                db.setTransactionSuccessful();
            } finally { db.endTransaction(); }
            // Only mark migration after the transaction commits. Original data stays.
            legacy.edit().putBoolean(MIGRATED, true).apply();
        }
        trimDatabase();
    }

    public static synchronized MarsTvInterest037 instance(Context context) {
        if (instance == null) instance = new MarsTvInterest037(context);
        return instance;
    }

    private void addRow(String videoId) {
        sequence = Math.max(sequence + 1, System.currentTimeMillis());
        ContentValues values = new ContentValues();
        values.put("video_id", videoId);
        values.put("seq", sequence);
        if (db.insertWithOnConflict(TABLE, null, values,
                SQLiteDatabase.CONFLICT_REPLACE) == -1)
            throw new android.database.SQLException("Could not save hidden video ID");
        hidden.remove(videoId);
        hidden.add(videoId);
    }

    private void trimDatabase() {
        while (hidden.size() > LIMIT) {
            String oldest = hidden.iterator().next();
            db.delete(TABLE, "video_id=?", new String[]{oldest});
            hidden.remove(oldest);
        }
    }

    public synchronized boolean isHidden(Video video) {
        return video != null && video.videoId != null
                && hidden.contains(video.videoId);
    }

    public synchronized Set<String> snapshot() {
        return new LinkedHashSet<>(hidden);
    }

    public synchronized boolean rejectLocal(Video video) {
        if (video == null || TextUtils.isEmpty(video.videoId)) return false;
        db.beginTransaction();
        try {
            addRow(video.videoId);
            trimDatabase();
            db.setTransactionSuccessful();
            return true;
        } finally { db.endTransaction(); }
    }

    public synchronized List<Video> filter(List<Video> videos) {
        if (videos == null || videos.isEmpty() || hidden.isEmpty())
            return videos == null ? Collections.emptyList() : videos;
        ArrayList<Video> result = new ArrayList<>(videos.size());
        for (Video video : videos) if (!isHidden(video)) result.add(video);
        return result;
    }

    public synchronized int count() { return hidden.size(); }
    public int limit() { return LIMIT; }

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
