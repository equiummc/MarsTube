package com.newtube.mobile.player;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import androidx.annotation.NonNull;

import com.liskovsoft.smartyoutubetv2.common.exoplayer.selector.FormatItem;
import com.liskovsoft.smartyoutubetv2.common.prefs.PlayerData;

/**
 * MarsTube 0.0.10 TV player and layout preferences.
 *
 * <p>Everything here is a <b>preference or ceiling</b>, never a promise that a stream exists.
 * The values are read by the Media3 track adapter, the player initializer and the browse grid.
 * Phone behaviour is deliberately untouched: every caller checks
 * {@code MarsDeviceMode.isTelevision(context)} first.</p>
 *
 * <p>The file has no Android UI dependencies so the decision logic is covered by a plain
 * Robolectric unit test ({@code MarsTvPlayerPrefsTest}).</p>
 */
public final class MarsTvPlayerPrefs {
    public static final String PREFS = "mars_tv_player_010";

    // Resolution ceiling (0 = automatic / device default).
    public static final int RES_AUTO = 0;
    public static final int RES_2160 = 2160;
    public static final int RES_1440 = 1440;
    public static final int RES_1080 = 1080;
    public static final int RES_720 = 720;
    public static final int RES_480 = 480;

    // Frame-rate ceiling (0 = automatic).
    public static final int FPS_AUTO = 0;
    public static final int FPS_60 = 60;
    public static final int FPS_30 = 30;

    // Soft codec preference.
    public static final String CODEC_AUTO = "auto";
    public static final String CODEC_H264 = "h264";
    public static final String CODEC_VP9 = "vp9";
    public static final String CODEC_AV1 = "av1";

    // HDR preference.
    public static final int HDR_AUTO = 0;
    public static final int HDR_OFF = 1;

    // Bitrate ceiling in Mbit/s (0 = automatic).
    public static final int BITRATE_AUTO = 0;

    // Buffer preset. Normal is the 0.0.10 default.
    public static final int BUFFER_LOW = 0;
    public static final int BUFFER_NORMAL = 1;
    public static final int BUFFER_HIGH = 2;

    // Start strategy.
    public static final int START_QUALITY_FIRST = 0;
    public static final int START_FAST = 1;

    // Subtitle policy.
    public static final int SUBTITLES_OFF = 0;
    public static final int SUBTITLES_FORCED_ONLY = 1;
    public static final int SUBTITLES_REGULAR = 2;

    // Layout.
    public static final int TILES_4 = 4;
    public static final int TILES_6 = 6;
    public static final int TILES_9 = 9;

    private static final String KEY_RESOLUTION = "resolution_ceiling";
    private static final String KEY_MAX_FPS = "max_fps";
    private static final String KEY_CODEC = "preferred_codec";
    private static final String KEY_HDR = "hdr_mode";
    private static final String KEY_BITRATE = "bitrate_limit_mbps";
    private static final String KEY_BUFFER = "buffer_preset";
    private static final String KEY_START = "start_strategy";
    private static final String KEY_SUBTITLES = "subtitle_policy";
    private static final String KEY_HOME_TILES = "home_tile_count";
    private static final String KEY_CAROUSEL = "carousel_preview_count";
    private static final String KEY_PROFILE_GRID = "profile_grid";
    private static final String KEY_HISTORY_GRID = "history_grid";
    private static final String KEY_SUBS_GRID = "subscriptions_grid";
    private static final String KEY_SUBS_CHANNEL_FILTER = "subscriptions_channel_filter";
    private static final String KEY_MINI_PLAYER = "mini_player_enabled";
    private static final String KEY_DOWNLOADS_HIDDEN = "downloads_hidden";

    private MarsTvPlayerPrefs() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static int getInt(Context context, String key, int fallback) {
        return prefs(context).getInt(key, fallback);
    }

    private static void putInt(Context context, String key, int value) {
        prefs(context).edit().putInt(key, value).apply();
    }

    private static boolean getBool(Context context, String key, boolean fallback) {
        return prefs(context).getBoolean(key, fallback);
    }

    private static void putBool(Context context, String key, boolean value) {
        prefs(context).edit().putBoolean(key, value).apply();
    }

    // ------------------------------------------------------------------
    // Video & playback
    // ------------------------------------------------------------------

    public static int getResolutionCeiling(Context context) {
        return getInt(context, KEY_RESOLUTION, RES_AUTO);
    }

    public static void setResolutionCeiling(Context context, int value) {
        putInt(context, KEY_RESOLUTION, value);
    }

    public static int getMaxFps(Context context) {
        return getInt(context, KEY_MAX_FPS, FPS_AUTO);
    }

    public static void setMaxFps(Context context, int value) {
        putInt(context, KEY_MAX_FPS, value);
    }

    @NonNull
    public static String getPreferredCodec(Context context) {
        String value = prefs(context).getString(KEY_CODEC, CODEC_AUTO);
        return value != null ? value : CODEC_AUTO;
    }

    public static void setPreferredCodec(Context context, String value) {
        prefs(context).edit().putString(KEY_CODEC,
                value != null ? value : CODEC_AUTO).apply();
    }

    public static int getHdrMode(Context context) {
        return getInt(context, KEY_HDR, HDR_AUTO);
    }

    public static void setHdrMode(Context context, int value) {
        putInt(context, KEY_HDR, value);
    }

    public static int getBitrateLimitMbps(Context context) {
        return getInt(context, KEY_BITRATE, BITRATE_AUTO);
    }

    public static void setBitrateLimitMbps(Context context, int value) {
        putInt(context, KEY_BITRATE, value);
    }

    public static int getBufferPreset(Context context) {
        return getInt(context, KEY_BUFFER, BUFFER_NORMAL);
    }

    public static void setBufferPreset(Context context, int value) {
        putInt(context, KEY_BUFFER, value);
        PlayerData.instance(context).setVideoBufferTypeByUser(context, bufferPlayerDataValue(value));
    }

    /** Maps the TV-facing three presets onto the shared PlayerData buffer constants. */
    public static int bufferPlayerDataValue(int preset) {
        switch (preset) {
            case BUFFER_LOW:
                return PlayerData.BUFFER_LOW;
            case BUFFER_HIGH:
                return PlayerData.BUFFER_HIGH;
            case BUFFER_NORMAL:
            default:
                return PlayerData.BUFFER_MEDIUM;
        }
    }

    public static int getStartStrategy(Context context) {
        return getInt(context, KEY_START, START_QUALITY_FIRST);
    }

    public static void setStartStrategy(Context context, int value) {
        putInt(context, KEY_START, value);
    }

    /**
     * Start gate in ms. Quality-first spends a little more cushion before the first frame;
     * fast-start keeps the 0.0.9 TTFF budget. Both stay far below the steady-state buffer.
     */
    public static int startBufferMs(Context context) {
        return getStartStrategy(context) == START_FAST ? 250 : 700;
    }

    // ------------------------------------------------------------------
    // Subtitles
    // ------------------------------------------------------------------

    public static int getSubtitlePolicy(Context context) {
        // 0.0.9 shipped with subtitles off; keep that default unless the user opts in.
        return getInt(context, KEY_SUBTITLES, SUBTITLES_OFF);
    }

    public static void setSubtitlePolicy(Context context, int value) {
        putInt(context, KEY_SUBTITLES, value);
    }

    // ------------------------------------------------------------------
    // Layout / browse
    // ------------------------------------------------------------------

    public static int getHomeTileCount(Context context) {
        // 4 keeps the 0.0.9 home density (see MobileGrid.computeSpanCountForWidthDp).
        int value = getInt(context, KEY_HOME_TILES, TILES_4);
        return value == TILES_6 || value == TILES_9 ? value : TILES_4;
    }

    public static void setHomeTileCount(Context context, int value) {
        putInt(context, KEY_HOME_TILES, value);
    }

    public static int getCarouselPreviewCount(Context context) {
        int value = getInt(context, KEY_CAROUSEL, 6);
        return Math.max(2, Math.min(12, value));
    }

    public static void setCarouselPreviewCount(Context context, int value) {
        putInt(context, KEY_CAROUSEL, value);
    }

    public static boolean isProfileGrid(Context context) {
        return getBool(context, KEY_PROFILE_GRID, true);
    }

    public static void setProfileGrid(Context context, boolean value) {
        putBool(context, KEY_PROFILE_GRID, value);
    }

    public static boolean isHistoryGrid(Context context) {
        return getBool(context, KEY_HISTORY_GRID, true);
    }

    public static void setHistoryGrid(Context context, boolean value) {
        putBool(context, KEY_HISTORY_GRID, value);
    }

    public static boolean isSubscriptionsGrid(Context context) {
        return getBool(context, KEY_SUBS_GRID, true);
    }

    public static void setSubscriptionsGrid(Context context, boolean value) {
        putBool(context, KEY_SUBS_GRID, value);
    }

    public static boolean isSubscriptionsChannelFilter(Context context) {
        return getBool(context, KEY_SUBS_CHANNEL_FILTER, false);
    }

    public static void setSubscriptionsChannelFilter(Context context, boolean value) {
        putBool(context, KEY_SUBS_CHANNEL_FILTER, value);
    }

    public static boolean isMiniPlayerEnabled(Context context) {
        return getBool(context, KEY_MINI_PLAYER, true);
    }

    public static void setMiniPlayerEnabled(Context context, boolean value) {
        putBool(context, KEY_MINI_PLAYER, value);
    }

    /** Downloads are never offered on TV; the flag exists only to keep the decision testable. */
    public static boolean areDownloadsHidden(Context context) {
        return getBool(context, KEY_DOWNLOADS_HIDDEN, true);
    }

    public static void setDownloadsHidden(Context context, boolean value) {
        putBool(context, KEY_DOWNLOADS_HIDDEN, value);
    }

    /**
     * Shorts overview: three fully visible 9:16 cards when the viewport is wide enough,
     * two on smaller screens. Never one: the category is a grid, not a phone feed.
     */
    public static int shortsSpanCount(Context context) {
        Configuration configuration = context.getResources().getConfiguration();
        int widthDp = configuration.screenWidthDp;
        if (widthDp == Configuration.SCREEN_WIDTH_DP_UNDEFINED) {
            widthDp = Math.round(context.getResources().getDisplayMetrics().widthPixels
                    / context.getResources().getDisplayMetrics().density);
        }
        return widthDp >= 840 ? 3 : 2;
    }

    // ------------------------------------------------------------------
    // Media3 helpers
    // ------------------------------------------------------------------

    /** Square Media3 ceiling side; 0 means "keep the device default". */
    public static int resolutionCeilingSide(Context context) {
        int value = getResolutionCeiling(context);
        if (value <= 0) {
            return 0;
        }
        // A square ceiling means the same class in portrait and landscape (see VideoRung).
        return value;
    }

    public static int bitrateLimitBps(Context context) {
        return getBitrateLimitMbps(context) * 1_000_000;
    }

    @NonNull
    public static String[] preferredVideoMimeTypes(Context context) {
        switch (getPreferredCodec(context)) {
            case CODEC_H264:
                return new String[]{"video/avc"};
            case CODEC_VP9:
                return new String[]{"video/x-vnd.on2.vp9"};
            case CODEC_AV1:
                return new String[]{"video/av01"};
            case CODEC_AUTO:
            default:
                return new String[0];
        }
    }

    /**
     * Maps the TV max-resolution preference onto the existing PlayerData preset vocabulary,
     * so the old quality sheet and the new settings page never disagree.
     */
    public static FormatItem resolutionAsFormatItem(Context context) {
        switch (getResolutionCeiling(context)) {
            case RES_480:
                return FormatItem.VIDEO_SD_AVC_30;
            case RES_720:
                return FormatItem.VIDEO_HD_AVC_30;
            case RES_1080:
                return FormatItem.VIDEO_FHD_VP9_60;
            case RES_1440:
            case RES_2160:
                return FormatItem.VIDEO_4K_VP9_60;
            case RES_AUTO:
            default:
                return PlayerData.instance(context).getDefaultVideoFormat();
        }
    }
}
