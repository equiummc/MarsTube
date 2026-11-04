#!/usr/bin/env python3
"""MT2-010: MarsTube 0.0.10 TV player, settings and Shorts/layout overhaul.

Applied AFTER 009 on a disposable, pinned Media3 reference checkout. The overlay is
fail-closed: every anchor must exist exactly once, and the script refuses to run
against anything but the 0.0.9 test package. Existing 0.0.9 installs stay separate.
"""
from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit("Usage: patch_010_player_settings_ui.py <pinned checkout>")
root = Path(sys.argv[1]).resolve()
mobile = root / "smarttubetv/src/stmobile"
tv = mobile / "java/com/newtube/mobile/ui"
if not (root / "sabr-media3").is_dir():
    raise SystemExit("MT2-010 requires pinned SABR Media3 checkout")


def replace_once(path, before, after):
    if not path.is_file():
        raise SystemExit(f"Missing {path}")
    data = path.read_text(encoding="utf-8")
    n = data.count(before)
    if n != 1:
        raise SystemExit(
            f"MT2-010 fail-closed: {path.name}: expected 1 anchor, saw {n}: {before[:120]!r}")
    path.write_text(data.replace(before, after, 1), encoding="utf-8")


def replace_all(path, before, after, expected):
    if not path.is_file():
        raise SystemExit(f"Missing {path}")
    data = path.read_text(encoding="utf-8")
    n = data.count(before)
    if n != expected:
        raise SystemExit(
            f"MT2-010 fail-closed: {path.name}: expected {expected} anchors, saw {n}: {before[:120]!r}")
    path.write_text(data.replace(before, after), encoding="utf-8")


def copy_overlay(name, dest):
    source = Path(__file__).resolve().parent / name
    if not source.is_file():
        raise SystemExit(f"Missing overlay source {name}")
    dest.parent.mkdir(parents=True, exist_ok=True)
    dest.write_text(source.read_text(encoding="utf-8"), encoding="utf-8")


# ---------------------------------------------------------------------------
# Identity: a new side-by-side test package, never an in-place 0.0.9 upgrade.
# ---------------------------------------------------------------------------
gradle = root / "smarttubetv/build.gradle"
manifest = mobile / "AndroidManifest.xml"
replace_once(gradle, 'applicationId "de.equiummc.marstube2.firetest009"',
             'applicationId "de.equiummc.marstube2.firetest010"')
replace_once(gradle, 'versionCode 12009', 'versionCode 12010')
replace_once(gradle, 'versionName "0.0.9"', 'versionName "0.0.10"')
replace_once(manifest, 'android:label="MarsTube 0.0.9"',
             'android:label="MarsTube 0.0.10"')

# ---------------------------------------------------------------------------
# Overlay sources and vector glyphs.
# ---------------------------------------------------------------------------
for filename, dest in [
    ("MarsTvShell.java", tv / "playback/MarsTvShell.java"),
    ("MarsTvRail.java", tv / "common/MarsTvRail.java"),
    ("MarsTvPlayerPrefs.java", tv / "../player/MarsTvPlayerPrefs.java"),
    ("MarsTvSettingsPages.java", tv / "settings/MarsTvSettingsPages.java"),
]:
    if not dest.is_file() and filename not in ("MarsTvPlayerPrefs.java", "MarsTvSettingsPages.java"):
        raise SystemExit(f"Missing existing TV UI source {dest}")
    copy_overlay(filename, dest)

drawable_dir = mobile / "res/drawable"
for source in sorted((Path(__file__).resolve().parent / "assets/drawable").glob("mars_tv_ic_*.xml")):
    (drawable_dir / source.name).write_text(source.read_text(encoding="utf-8"), encoding="utf-8")

copy_overlay("MarsTvPlayerPrefsTest.java",
             root / "smarttubetv/src/test/java/com/newtube/mobile/player/MarsTvPlayerPrefsTest.java")

# The 0.0.9 TV-quality overlay deliberately raised the recent-throughput seed cap to
# 10 Mbit/s (see patch_007_tv_quality.py). Its upstream test still asserted the old 4 Mbit/s
# ceiling, so the app unit suite failed before 0.0.10 even started. Keep the test honest.
replace_once(root / "smarttubetv/src/test/java/com/newtube/mobile/player/StartupBandwidthMeterTest.java",
             'assertEquals(4_000_000, StartupBandwidthMeter.seedFor(27_000_000, 999_000, 1_000_000));',
             'assertEquals(10_000_000, StartupBandwidthMeter.seedFor(27_000_000, 999_000, 1_000_000));')

# ---------------------------------------------------------------------------
# Settings: TV root/page tree, phone tree untouched, search hidden on TV.
# ---------------------------------------------------------------------------
settings_pages = tv / "settings/SettingsPages.java"
replace_once(settings_pages,
'''    @NonNull
    static Page build(@NonNull Context context, @NonNull String id) {
        switch (id) {''',
'''    @NonNull
    static Page build(@NonNull Context context, @NonNull String id) {
        if (com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(context)) {
            return MarsTvSettingsPages.build(context, id);
        }
        return buildPhone(context, id);
    }

    @NonNull
    static Page buildPhone(@NonNull Context context, @NonNull String id) {
        switch (id) {''')
replace_all(settings_pages, 'github.com/equiummc/mars-smarttube',
            'github.com/equiummc/MarsTube', 2)

settings_fragment = tv / "settings/SettingsPageFragment.java"
replace_once(settings_fragment,
'''        if (SettingsPages.ROOT.equals(mPageId)) {
            View search = view.findViewById(R.id.settings_search_field);''',
'''        if (SettingsPages.ROOT.equals(mPageId)
                && !com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(requireContext())) {
            View search = view.findViewById(R.id.settings_search_field);''')

# ---------------------------------------------------------------------------
# TV player callbacks: hold-to-2x, source diagnostics, BACK chain, Shorts aspect.
# ---------------------------------------------------------------------------
activity = tv / "playback/MobilePlaybackActivity.java"
replace_once(activity,
'''                    @Override public void quality() {
                        showQualitySheet();
                    }
                });''',
'''                    @Override public void quality() {
                        showQualitySheet();
                    }
                    @Override public void beginHold() {
                        if (mExoPlayerController != null && mPlayer != null
                                && mPlayer.getPlayWhenReady()) {
                            mExoPlayerController.beginHoldSpeed(2f);
                        }
                    }
                    @Override public void endHold() {
                        if (mExoPlayerController != null
                                && mExoPlayerController.isHoldSpeedOn()) {
                            mExoPlayerController.endHoldSpeed();
                        }
                    }
                    @Override public String sourceProtocol() {
                        return mExoPlayerController != null
                                ? mExoPlayerController.getSourceProtocol() : "";
                    }
                    @Override public String sourceDiagnostics() {
                        return mExoPlayerController != null
                                ? mExoPlayerController.getSourceDiagnostics() : "";
                    }
                });''')

replace_once(activity,
'''        if (mMarsTvShell != null
                && com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(this)
                && !mIsInPip && !mPipEnterPending) {
            // Return to the TV browse page with the *same* playing Media3 session.
            // No portrait-only morph or artificial orientation change.
            minimizeByDrag();
            return;
        }''',
'''        if (mMarsTvShell != null
                && com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(this)
                && !mIsInPip && !mPipEnterPending) {
            // 0.0.10: BACK first closes Info, then the rail, then opens the rail while the
            // video and audio continue. Only after that does it minimize the player.
            if (mMarsTvShell.handleBack()) {
                return;
            }
            // Return to the TV browse page with the *same* playing Media3 session.
            // No portrait-only morph or artificial orientation change.
            minimizeByDrag();
            return;
        }''')

replace_once(activity,
'''    @Override
    public void setResizeMode(int mode) {
        if (mPlayerView != null) {
            mPlayerView.setResizeMode(mode);
        }
        applyControlsInsets(); // fill/zoom: the controls span the width (controlsStrip)
    }''',
'''    /** 0.0.10: TV Shorts keep their 9:16 frame and side bars; never stretch the picture. */
    private boolean marsTvShortsPlayback() {
        return com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(this)
                && ((getVideo() != null && getVideo().isShorts) || isVerticalVideo());
    }

    @Override
    public void setResizeMode(int mode) {
        int applied = mode;
        if (marsTvShortsPlayback() && mode == RESIZE_MODE_STRETCH) {
            applied = RESIZE_MODE_DEFAULT;
        }
        if (mPlayerView != null) {
            mPlayerView.setResizeMode(applied);
        }
        applyControlsInsets(); // fill/zoom: the controls span the width (controlsStrip)
    }''')

replace_once(activity,
'''            if (videoSize.width > 0 && videoSize.height > 0) {
                mVideoAspect = videoSize.width * videoSize.pixelWidthHeightRatio / videoSize.height;
                applyControlsInsets();
            }''',
'''            if (videoSize.width > 0 && videoSize.height > 0) {
                mVideoAspect = videoSize.width * videoSize.pixelWidthHeightRatio / videoSize.height;
                applyControlsInsets();
                if (marsTvShortsPlayback() && getResizeMode() == RESIZE_MODE_STRETCH) {
                    setResizeMode(RESIZE_MODE_DEFAULT);
                }
            }''')

# ---------------------------------------------------------------------------
# Media3: TV ceilings/codec/HDR, subtitle policy, start buffer and protocol info.
# ---------------------------------------------------------------------------
adapter = tv / "../player/Media3TrackAdapter.java"
replace_once(adapter,
'''import android.text.TextUtils;

import androidx.annotation.Nullable;''',
'''import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.Nullable;''')
replace_once(adapter,
'''    private final DefaultTrackSelector mTrackSelector;
    /** What the app asked for, per renderer index. Never the momentary ABR rung. */''',
'''    private final DefaultTrackSelector mTrackSelector;
    @Nullable private Context mContext;
    /** What the app asked for, per renderer index. Never the momentary ABR rung. */''')
replace_once(adapter,
'''    Media3TrackAdapter(DefaultTrackSelector trackSelector) {
        mTrackSelector = trackSelector;
    }''',
'''    Media3TrackAdapter(DefaultTrackSelector trackSelector) {
        mTrackSelector = trackSelector;
    }

    /** Set by the controller so TV-only ceilings never leak into the touch player. */
    void setContext(@Nullable Context context) {
        mContext = context != null ? context.getApplicationContext() : null;
    }''')
replace_once(adapter,
'''        // No explicit audio pick anywhere: steer media3's default to the untranslated track
        // (YouTube auto-dubs put the dubbed language first otherwise).
        if (mPreferOriginalAudio && mTargets[FormatItem.TYPE_AUDIO] == null) {
            applyOriginalAudioDefault();
        }
    }''',
'''        // No explicit audio pick anywhere: steer media3's default to the untranslated track
        // (YouTube auto-dubs put the dubbed language first otherwise).
        if (mPreferOriginalAudio && mTargets[FormatItem.TYPE_AUDIO] == null) {
            applyOriginalAudioDefault();
        }

        applyTvSubtitlePolicyIfNeeded();
    }''')
replace_once(adapter,
'''            builder.setMaxVideoSize(ceiling, ceiling);
            builder.setMaxVideoFrameRate(item.getFrameRate() > 0 ? Math.round(item.getFrameRate()) + 1 : Integer.MAX_VALUE);
            // Soft preference; media3 falls back to whatever the stream actually has.
            MediaTrack track = item.getTrack();
            String codec = track != null && track.format != null ? track.format.codecs : null;
            builder.setPreferredVideoMimeTypes(mimeTypesForCodec(codec));
        } else {''',
'''            int maxFps = item.getFrameRate() > 0
                    ? Math.round(item.getFrameRate()) + 1 : Integer.MAX_VALUE;
            // Soft preference; media3 falls back to whatever the stream actually has.
            MediaTrack track = item.getTrack();
            String codec = track != null && track.format != null ? track.format.codecs : null;
            String[] mimes = mimeTypesForCodec(codec);
            int bitrateLimit = 0;

            if (mContext != null
                    && com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(mContext)) {
                int tvResolution = MarsTvPlayerPrefs.getResolutionCeiling(mContext);
                if (tvResolution > 0) {
                    ceiling = VideoRung.ceilingSide(tvResolution);
                }
                int tvFps = MarsTvPlayerPrefs.getMaxFps(mContext);
                if (tvFps > 0) {
                    maxFps = tvFps + 1;
                }
                bitrateLimit = MarsTvPlayerPrefs.bitrateLimitBps(mContext);
                if (bitrateLimit > 0) {
                    builder.setMaxVideoBitrate(bitrateLimit);
                }
                String[] tvMimes = MarsTvPlayerPrefs.preferredVideoMimeTypes(mContext);
                if (tvMimes.length > 0) {
                    mimes = tvMimes;
                }
            }

            builder.setMaxVideoSize(ceiling, ceiling);
            builder.setMaxVideoFrameRate(maxFps);
            builder.setPreferredVideoMimeTypes(mimes);
            if (mContext != null
                    && com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(mContext)
                    && MarsTvPlayerPrefs.getHdrMode(mContext) == MarsTvPlayerPrefs.HDR_OFF) {
                applyTvHdrOff(builder, ceiling, maxFps, bitrateLimit, mimes);
            }
        } else {''')

replace_once(adapter,
'''    private void applyAudioTarget(FormatItem item) {''',
'''    /**
     * Media3 has no HDR constraint. Best effort: pin the best SDR video track inside the same
     * resolution/FPS/bitrate/codec ceilings. If the source only offers HDR, leave Auto alone.
     */
    private void applyTvHdrOff(DefaultTrackSelector.Parameters.Builder builder, int ceiling,
            int maxFps, int bitrateLimit, String[] mimes) {
        TrackLocation best = null;
        int bestScore = -1;
        TrackLocation fallback = null;
        int fallbackScore = -1;
        for (Tracks.Group group : mTracks.getGroups()) {
            if (group.getType() != C.TRACK_TYPE_VIDEO) {
                continue;
            }
            for (int i = 0; i < group.length; i++) {
                if (!group.isTrackSupported(i)) {
                    continue;
                }
                androidx.media3.common.Format format = group.getTrackFormat(i);
                if (!isSdr(format)) {
                    continue;
                }
                if (format.width > 0 && format.height > 0
                        && (format.width > ceiling || format.height > ceiling)) {
                    continue;
                }
                if (maxFps != Integer.MAX_VALUE && format.frameRate > maxFps) {
                    continue;
                }
                if (bitrateLimit > 0 && format.bitrate > bitrateLimit) {
                    continue;
                }
                int score = (format.height > 0 ? format.height : 0) * 1000
                        + Math.max(0, format.bitrate / 1000);
                TrackLocation candidate = new TrackLocation(group, i);
                if (mimeMatches(format, mimes)) {
                    if (score > bestScore) {
                        bestScore = score;
                        best = candidate;
                    }
                } else if (score > fallbackScore) {
                    fallbackScore = score;
                    fallback = candidate;
                }
            }
        }
        TrackLocation chosen = best != null ? best : fallback;
        if (chosen != null) {
            builder.setOverrideForType(
                    new TrackSelectionOverride(chosen.group.getMediaTrackGroup(), chosen.trackIndex));
        }
    }

    private static boolean isSdr(androidx.media3.common.Format format) {
        return format.colorInfo == null
                || format.colorInfo.colorTransfer == C.COLOR_TRANSFER_SDR
                || format.colorInfo.colorTransfer == androidx.media3.common.Format.NO_VALUE;
    }

    private static boolean mimeMatches(androidx.media3.common.Format format, String[] mimes) {
        if (mimes == null || mimes.length == 0) {
            return false;
        }
        for (String mime : mimes) {
            if (mime != null && mime.equals(format.sampleMimeType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 0.0.10 subtitle policy. Only applied while the app's subtitle target is still the default
     * "None": a manual CC choice always wins. Forced-only never substitutes an arbitrary track.
     */
    private void applyTvSubtitlePolicyIfNeeded() {
        if (mContext == null
                || !com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(mContext)) {
            return;
        }
        FormatItem target = mTargets[FormatItem.TYPE_SUBTITLE];
        if (target != null && !target.isDefault()) {
            return;
        }
        int policy = MarsTvPlayerPrefs.getSubtitlePolicy(mContext);
        DefaultTrackSelector.Parameters.Builder builder = mTrackSelector.buildUponParameters();
        if (policy == MarsTvPlayerPrefs.SUBTITLES_OFF) {
            builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true);
            builder.clearOverridesOfType(C.TRACK_TYPE_TEXT);
            mTrackSelector.setParameters(builder);
            return;
        }
        if (policy == MarsTvPlayerPrefs.SUBTITLES_FORCED_ONLY) {
            TrackLocation forced = findForcedSubtitle();
            if (forced != null) {
                builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false);
                builder.setOverrideForType(new TrackSelectionOverride(
                        forced.group.getMediaTrackGroup(), forced.trackIndex));
            } else {
                builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true);
                builder.clearOverridesOfType(C.TRACK_TYPE_TEXT);
            }
            mTrackSelector.setParameters(builder);
            return;
        }
        // Regular: enable the renderer, but do not pin a language the user did not choose.
        builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false);
        builder.clearOverridesOfType(C.TRACK_TYPE_TEXT);
        mTrackSelector.setParameters(builder);
    }

    @Nullable
    private TrackLocation findForcedSubtitle() {
        for (Tracks.Group group : mTracks.getGroups()) {
            if (group.getType() != C.TRACK_TYPE_TEXT) {
                continue;
            }
            for (int i = 0; i < group.length; i++) {
                if (!group.isTrackSupported(i)) {
                    continue;
                }
                androidx.media3.common.Format format = group.getTrackFormat(i);
                if ((format.selectionFlags & C.SELECTION_FLAG_FORCED) != 0) {
                    return new TrackLocation(group, i);
                }
            }
        }
        return null;
    }

    private void applyAudioTarget(FormatItem item) {''')

initializer = tv / "../player/Media3PlayerInitializer.java"
replace_once(initializer,
'''    private void alignBufferDefaultOnce() {
        SharedPreferences prefs = mContext.getSharedPreferences(PLAYER_PREFS_NAME, Context.MODE_PRIVATE);
        boolean v1Done = prefs.getBoolean(KEY_BUFFER_DEFAULT_ALIGNED, false);''',
'''    private void alignBufferDefaultOnce() {
        SharedPreferences prefs = mContext.getSharedPreferences(PLAYER_PREFS_NAME, Context.MODE_PRIVATE);
        if (com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(mContext)) {
            // 0.0.10 TV default is Normal. Never overwrite a deliberate buffer pick; the shared
            // PlayerData flag is set by the in-player radio and by MarsTvPlayerPrefs.
            PlayerData tvPlayerData = PlayerData.instance(mContext);
            if (!prefs.getBoolean(PlayerData.KEY_BUFFER_USER_CHOSEN, false)
                    && tvPlayerData.getVideoBufferType() != PlayerData.BUFFER_MEDIUM) {
                tvPlayerData.setVideoBufferType(PlayerData.BUFFER_MEDIUM);
                tvPlayerData.persistNow();
            }
            markBufferDefaultAligned(prefs);
            return;
        }
        boolean v1Done = prefs.getBoolean(KEY_BUFFER_DEFAULT_ALIGNED, false);''')
replace_once(initializer,
'''        int autoHeight = com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(mContext)
                ? PlayerData.instance(mContext).getDefaultVideoFormat().getHeight() : 1080;
        int ceiling = VideoRung.ceilingSide(autoHeight);''',
'''        int autoHeight;
        if (com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(mContext)) {
            int tvResolution = MarsTvPlayerPrefs.getResolutionCeiling(mContext);
            autoHeight = tvResolution > 0
                    ? tvResolution
                    : PlayerData.instance(mContext).getDefaultVideoFormat().getHeight();
        } else {
            autoHeight = 1080;
        }
        int ceiling = VideoRung.ceilingSide(autoHeight);''')
replace_once(initializer,
'''        int startBufferMs = START_BUFFER_MS;
        int startAfterRebufferMs = START_BUFFER_AFTER_REBUFFER_MS;''',
'''        int startBufferMs = com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(mContext)
                ? MarsTvPlayerPrefs.startBufferMs(mContext) : START_BUFFER_MS;
        int startAfterRebufferMs = START_BUFFER_AFTER_REBUFFER_MS;''')

controller = tv / "../player/Media3PlayerController.java"
replace_once(controller,
'''    private boolean mSabrSourceActive;
    /** Whether the active SABR source is the automatic fallback rather than the opt-in preference. */''',
'''    private boolean mSabrSourceActive;
    /** The source path actually opened, for the TV Info panel. */
    private String mLastNetPathType = "";
    /** Whether the active SABR source is the automatic fallback rather than the opt-in preference. */''')
replace_once(controller,
'''        resetPlayerState(); // same video-artifact fix as the legacy controller
        mSabrSourceActive = "sabr-vod".equals(netPathType);''',
'''        resetPlayerState(); // same video-artifact fix as the legacy controller
        mSabrSourceActive = "sabr-vod".equals(netPathType);
        mLastNetPathType = netPathType != null ? netPathType : "";''')
replace_once(controller,
'''    public boolean allowsAutomaticSourceRecovery() { return !mSabrSourceActive || mSabrWasFallback; }''',
'''    public boolean allowsAutomaticSourceRecovery() { return !mSabrSourceActive || mSabrWasFallback; }

    /** Human-readable protocol that is actually open, never a promise from the settings. */
    public String getSourceProtocol() {
        String type = mLastNetPathType;
        if (type.startsWith("sabr-vod")) {
            return "SABR";
        }
        if (type.startsWith("sabr-fallback")) {
            return "SABR-Fallback (DASH/HLS)";
        }
        if (type.startsWith("dash-mpd")) {
            return type.contains("hls") ? "DASH + HLS" : "DASH";
        }
        if (type.startsWith("hls")) {
            return "HLS";
        }
        return type.isEmpty() ? "unbekannt" : type;
    }

    /** Extra facts for the Info panel: active SABR, a visible fallback and recovery state. */
    public String getSourceDiagnostics() {
        StringBuilder result = new StringBuilder();
        result.append("SABR aktiv: ").append(mSabrSourceActive ? "ja" : "nein");
        if (mSabrSourceActive) {
            result.append(" · Fallback: ").append(mSabrWasFallback ? "ja" : "nein");
        }
        result.append(" · Auto-Recovery: ")
                .append(allowsAutomaticSourceRecovery() ? "ja" : "nein");
        return result.toString();
    }''')
replace_once(controller,
'''        mTrackSelector = trackSelector;
        mTrackAdapter = new Media3TrackAdapter(trackSelector);
        mTrackAdapter.setPreferOriginalAudio(true); // NEWTUBE(mobile): match the legacy default''',
'''        mTrackSelector = trackSelector;
        mTrackAdapter = new Media3TrackAdapter(trackSelector);
        mTrackAdapter.setContext(mContext);
        mTrackAdapter.setPreferOriginalAudio(true); // NEWTUBE(mobile): match the legacy default''')

# ---------------------------------------------------------------------------
# Browse: TV grids, Shorts, Downloads/Miniplayer hiding, profile grid.
# ---------------------------------------------------------------------------
browse = tv / "browse/MobileBrowseActivity.java"
replace_once(browse,
'''    private LinearLayout mYouRows;''',
'''    private LinearLayout mYouRows;
    @Nullable private LinearLayout mYouGridRow;''')
replace_once(browse,
'''    private void syncMiniPlayer() {
        finishMiniClose(); // a card closing when this runs is closed, not brought back''',
'''    private void syncMiniPlayer() {
        if (com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(this)
                && !com.newtube.mobile.player.MarsTvPlayerPrefs.isMiniPlayerEnabled(this)) {
            hideMiniPlayer();
            return;
        }
        finishMiniClose(); // a card closing when this runs is closed, not brought back''')
replace_once(browse,
'''        int destination = intent.getIntExtra("mars_tv_section", 0);
        intent.removeExtra("mars_tv_section");
        if (destination == 1) {''',
'''        int destination = intent.getIntExtra("mars_tv_section", 0);
        intent.removeExtra("mars_tv_section");
        if (destination == 4 && com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(this)) {
            // Downloads are a phone feature; never expose a hidden legacy route on TV.
            return;
        }
        if (destination == 1) {''')
replace_once(browse,
'''        mYouRows.removeAllViews();
        mYouUpdateRow = null;''',
'''        mYouRows.removeAllViews();
        mYouGridRow = null;
        mYouUpdateRow = null;''')
replace_once(browse,
'''            if (!section.isEnabled()
                    || navSections.contains(section)
                    || section.getId() == MediaGroup.TYPE_SETTINGS
                    || section.getId() == MediaGroup.TYPE_SHORTS) {
                continue;
            }''',
'''            if (!section.isEnabled()
                    || navSections.contains(section)
                    || section.getId() == MediaGroup.TYPE_SETTINGS
                    || section.getId() == MediaGroup.TYPE_SHORTS
                    || (com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(this)
                            && section.getId() == VideoDownloads.SECTION_ID)) {
                continue;
            }''')
replace_once(browse,
'''    private void addYouUpdateRow() {
        View row = LayoutInflater.from(this).inflate(R.layout.item_mobile_you_update_row, mYouRows, false);''',
'''    private void addYouUpdateRow() {
        flushYouGridRow();
        View row = LayoutInflater.from(this).inflate(R.layout.item_mobile_you_update_row, mYouRows, false);''')
replace_once(browse,
'''    private void addYouGroupLabel(CharSequence text) {
        TextView label = new TextView(this);''',
'''    private void addYouGroupLabel(CharSequence text) {
        flushYouGridRow();
        TextView label = new TextView(this);''')
replace_once(browse,
'''    private void addYouDivider() {
        View divider = new View(this);''',
'''    private void addYouDivider() {
        flushYouGridRow();
        View divider = new View(this);''')
replace_once(browse,
'''        row.setFocusable(true); // 0.0.5: Fire TV remote can select profile options
        row.setOnClickListener(v -> action.run());
        mYouRows.addView(row);
        return row;
    }''',
'''        row.setFocusable(true); // 0.0.5: Fire TV remote can select profile options
        row.setOnClickListener(v -> action.run());
        addYouRowView(row);
        return row;
    }

    /** TV profile grid: two section rows side by side; phone keeps the original list. */
    private void addYouRowView(View row) {
        if (!com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(this)
                || !com.newtube.mobile.player.MarsTvPlayerPrefs.isProfileGrid(this)) {
            mYouRows.addView(row);
            return;
        }
        if (mYouGridRow == null) {
            mYouGridRow = new LinearLayout(this);
            mYouGridRow.setOrientation(LinearLayout.HORIZONTAL);
            mYouGridRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
            mYouRows.addView(mYouGridRow, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        float density = getResources().getDisplayMetrics().density;
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, Math.round(52 * density), 1f);
        int gap = Math.round(6 * density);
        lp.setMargins(gap, Math.round(2 * density), gap, Math.round(2 * density));
        row.setLayoutParams(lp);
        mYouGridRow.addView(row);
    }

    private void flushYouGridRow() {
        mYouGridRow = null;
    }''')
replace_once(browse,
'''    private void onSectionChosen(int sectionId) {
        // The Settings section has no video-grid renderer on the mobile shell (its content''',
'''    private void onSectionChosen(int sectionId) {
        if (sectionId == VideoDownloads.SECTION_ID
                && com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(this)) {
            sectionId = MediaGroup.TYPE_HOME;
        }
        // The Settings section has no video-grid renderer on the mobile shell (its content''')
replace_once(browse,
'''    private int computeSpanCount() {
        return com.newtube.mobile.ui.common.MobileGrid.computeSpanCount(this);
    }''',
'''    private int computeSpanCount() {
        return computeSpanCount(getResources().getConfiguration());
    }

    /**
     * TV grid policy: Home 4/6/9, Shorts 3 (2 on small screens), History and Subscriptions 3.
     * Phones keep the untouched MobileGrid heuristic.
     */
    private int computeSpanCount(Configuration configuration) {
        if (com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(this)) {
            if (mCurrentSectionId == MediaGroup.TYPE_SHORTS) {
                return com.newtube.mobile.player.MarsTvPlayerPrefs.shortsSpanCount(this);
            }
            if (mCurrentSectionId == MediaGroup.TYPE_HISTORY
                    && com.newtube.mobile.player.MarsTvPlayerPrefs.isHistoryGrid(this)) {
                return 3;
            }
            if (mCurrentSectionId == MediaGroup.TYPE_SUBSCRIPTIONS
                    && com.newtube.mobile.player.MarsTvPlayerPrefs.isSubscriptionsGrid(this)) {
                return 3;
            }
            if (mCurrentSectionId == MediaGroup.TYPE_HOME) {
                return com.newtube.mobile.player.MarsTvPlayerPrefs.getHomeTileCount(this);
            }
        }
        return com.newtube.mobile.ui.common.MobileGrid.computeSpanCount(configuration);
    }''')
replace_once(browse,
'''        updateGridSpanCount(com.newtube.mobile.ui.common.MobileGrid.computeSpanCount(newConfig));''',
'''        updateGridSpanCount(computeSpanCount(newConfig));''')
replace_once(browse,
'''    private void handleOpenDownloads(Intent intent) {
        if (intent == null || !ACTION_OPEN_DOWNLOADS.equals(intent.getAction())) {
            return;
        }''',
'''    private void handleOpenDownloads(Intent intent) {
        if (intent == null || !ACTION_OPEN_DOWNLOADS.equals(intent.getAction())) {
            return;
        }
        if (com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(this)) {
            intent.setAction(null);
            return;
        }''')
replace_once(browse,
'''    private void openPendingDownloads() {
        if (!mPendingDownloadsOpen) {
            return;
        }''',
'''    private void openPendingDownloads() {
        if (!mPendingDownloadsOpen) {
            return;
        }
        if (com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(this)) {
            mPendingDownloadsOpen = false;
            return;
        }''')
replace_once(browse,
'''        for (BrowseSection section : mSections) {
            if (section.isEnabled() && section.getId() != MediaGroup.TYPE_SETTINGS) {
                enabledSections.add(section);
            }
        }''',
'''        boolean tv = com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(this);
        for (BrowseSection section : mSections) {
            if (section.isEnabled() && section.getId() != MediaGroup.TYPE_SETTINGS
                    && !(tv && section.getId() == VideoDownloads.SECTION_ID)) {
                enabledSections.add(section);
            }
        }''')
replace_once(browse,
'''        for (Video video : videos) {
            if (isVisibleFeedItem(video)) {
                visible.add(video);
            }
        }
        return visible;
    }''',
'''        for (Video video : videos) {
            if (isVisibleFeedItem(video)) {
                visible.add(video);
            }
        }
        if (mCurrentSectionId == MediaGroup.TYPE_SUBSCRIPTIONS
                && com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(this)
                && com.newtube.mobile.player.MarsTvPlayerPrefs.isSubscriptionsChannelFilter(this)) {
            visible = filterSubscriptionsByChannel(visible);
        }
        return visible;
    }

    /** Channel filter: only the newest video per channel in the Subscriptions grid. */
    private static List<Video> filterSubscriptionsByChannel(List<Video> videos) {
        List<Video> result = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (Video video : videos) {
            String key = video.channelId != null ? video.channelId : video.getAuthor();
            if (key == null || seen.add(key)) {
                result.add(video);
            }
        }
        return result;
    }''')
replace_once(browse,
'''            BrowseSection section = mSections.get(index);

            // Same guard as onSectionChosen(): Settings has no video-grid renderer on the mobile''',
'''            BrowseSection section = mSections.get(index);

            if (section.getId() == VideoDownloads.SECTION_ID
                    && com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(this)) {
                hideYouPanel();
                onSectionChosen(MediaGroup.TYPE_HOME);
                return;
            }

            // Same guard as onSectionChosen(): Settings has no video-grid renderer on the mobile''')

# ---------------------------------------------------------------------------
# Cards: high-resolution portrait Shorts thumbnails, no blur-up, rounded focus.
# ---------------------------------------------------------------------------
cards = tv / "browse/VideoCardAdapter.java"
replace_once(cards,
'''            mAvatar = itemView.findViewById(R.id.video_channel_avatar);

            itemView.setOnClickListener(v -> {
                if (mVideo != null && clickListener != null) {''',
'''            mAvatar = itemView.findViewById(R.id.video_channel_avatar);

            if (com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(itemView.getContext())) {
                final android.graphics.drawable.Drawable original = itemView.getForeground();
                itemView.setOnFocusChangeListener((view, hasFocus) -> {
                    if (!hasFocus) {
                        view.setForeground(original);
                        return;
                    }
                    float density = view.getResources().getDisplayMetrics().density;
                    android.graphics.drawable.GradientDrawable focus =
                            new android.graphics.drawable.GradientDrawable();
                    focus.setColor(0x22305B9E);
                    focus.setCornerRadius(12f * density);
                    focus.setStroke(Math.max(2, Math.round(2f * density)), 0xFF16B8FF);
                    view.setForeground(focus);
                });
            }
            itemView.setOnClickListener(v -> {
                if (mVideo != null && clickListener != null) {''')
replace_once(cards,
'''        public static com.bumptech.glide.RequestBuilder<android.graphics.drawable.Drawable> thumbnailRequest(
                com.bumptech.glide.RequestManager glide, Context context, Video video) {
            int w = thumbWidth(context);
            return glide
                    .load(thumbnailUrl(context, video))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .format(DecodeFormat.PREFER_RGB_565)
                    .override(w, sThumbH)
                    .centerCrop();
        }

        public static String thumbnailUrl(Context context, Video video) {
            // NEWTUBE(ux): entries saved without an image (local history, link opens) get the
            // i.ytimg.com fallback here, so the preloader and the bind ask for the same URL.
            com.newtube.mobile.ui.common.LocalThumbnails.fill(video);
            return ClickbaitRemover.updateThumbnail(video, MainUIData.instance(context).getThumbQuality());
        }

        private void bindThumbnail(Context context, Video video) {
            String thumbnailUrl = thumbnailUrl(context, video);
            mBoundThumbUrl = thumbnailUrl;

            int w = thumbWidth(context);
            com.bumptech.glide.RequestBuilder<android.graphics.drawable.Drawable> request =
                    thumbnailRequest(Glide.with(context), context, video)
                    // Network loads fade in over the placeholder-colored card instead of popping;
                    // memory-cache hits skip the transition entirely, so scrolling stays crisp.
                    .transition(com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade(150));

            // NEWTUBE(startup): release-visible "first thumbnail on screen" milestone. The listener
            // is attached only until it has fired once per process.
            if (com.newtube.mobile.LaunchMilestones.wantsFirstThumb()) {
                request = request.addListener(FIRST_THUMB_LISTENER);
            }

            // At the default thumb quality the primary URL IS the card URL, so a fallback request
            // would be identical - only pay the second RequestBuilder when it can actually differ.
            String fallbackUrl = video.getCardImageUrl();
            if (fallbackUrl != null && !fallbackUrl.equals(thumbnailUrl)) {
                request = request.error(Glide.with(context)
                        .load(fallbackUrl)
                        .format(DecodeFormat.PREFER_RGB_565)
                        .override(w, sThumbH)
                        .centerCrop());
            }

            request.into(mThumbnail);
        }''',
'''        public static com.bumptech.glide.RequestBuilder<android.graphics.drawable.Drawable> thumbnailRequest(
                com.bumptech.glide.RequestManager glide, Context context, Video video) {
            int[] size = thumbnailSize(context, video);
            return glide
                    .load(thumbnailUrl(context, video))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .format(DecodeFormat.PREFER_RGB_565)
                    .override(size[0], size[1])
                    .centerCrop();
        }

        public static String thumbnailUrl(Context context, Video video) {
            // NEWTUBE(ux): entries saved without an image (local history, link opens) get the
            // i.ytimg.com fallback here, so the preloader and the bind ask for the same URL.
            com.newtube.mobile.ui.common.LocalThumbnails.fill(video);
            String resolved = ClickbaitRemover.updateThumbnail(
                    video, MainUIData.instance(context).getThumbQuality());
            if (com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(context)
                    && video != null && video.isShorts && video.altCardImageUrl == null
                    && !video.isLive && !video.isUpcoming) {
                String highRes = shortsHighResUrl(video.getCardImageUrl());
                if (highRes != null) {
                    return highRes;
                }
            }
            return resolved;
        }

        /** Shorts portrait size: the card's real width, capped by the 9:16 height budget. */
        private static int[] thumbnailSize(Context context, Video video) {
            if (com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(context)
                    && video != null && video.isShorts) {
                android.util.DisplayMetrics dm = context.getResources().getDisplayMetrics();
                int rail = Math.round(100 * dm.density);
                int span = Math.max(1, com.newtube.mobile.player.MarsTvPlayerPrefs
                        .shortsSpanCount(context));
                int available = Math.max(1, dm.widthPixels - rail);
                int w = Math.min(available / span, Math.max(1, dm.heightPixels * 9 / 16));
                return new int[]{Math.max(1, w), Math.max(1, w * 16 / 9)};
            }
            return new int[]{thumbWidth(context), sThumbH};
        }

        /** i.ytimg.com/vi/<id>/<anything> -> the original-aspect oar2 image. */
        private static String shortsHighResUrl(String url) {
            if (url == null || !url.contains("ytimg.com")) {
                return null;
            }
            int marker = url.indexOf("/vi/");
            if (marker < 0) {
                return null;
            }
            int slash = url.indexOf('/', marker + 4);
            if (slash < 0) {
                return null;
            }
            return url.substring(0, slash + 1) + "oar2.jpg";
        }

        private void bindThumbnail(Context context, Video video) {
            String thumbnailUrl = thumbnailUrl(context, video);
            mBoundThumbUrl = thumbnailUrl;
            int[] size = thumbnailSize(context, video);
            boolean tvShorts = com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(context)
                    && video != null && video.isShorts;
            com.bumptech.glide.RequestBuilder<android.graphics.drawable.Drawable> request =
                    thumbnailRequest(Glide.with(context), context, video);
            if (!tvShorts) {
                // Phones keep the short network fade; TV Shorts must never blur-up/upscale.
                request = request.transition(com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade(150));
            } else {
                request = request.dontAnimate();
            }

            // NEWTUBE(startup): release-visible "first thumbnail on screen" milestone. The listener
            // is attached only until it has fired once per process.
            if (com.newtube.mobile.LaunchMilestones.wantsFirstThumb()) {
                request = request.addListener(FIRST_THUMB_LISTENER);
            }

            // At the default thumb quality the primary URL IS the card URL, so a fallback request
            // would be identical - only pay the second RequestBuilder when it can actually differ.
            String fallbackUrl = video.getCardImageUrl();
            if (fallbackUrl != null && !fallbackUrl.equals(thumbnailUrl)) {
                request = request.error(Glide.with(context)
                        .load(fallbackUrl)
                        .format(DecodeFormat.PREFER_RGB_565)
                        .override(size[0], size[1])
                        .centerCrop());
            }

            request.into(mThumbnail);
        }''')

topbar = tv / "browse/BrowseTopBar.java"
replace_once(topbar,
'''    void show(boolean subScreen, CharSequence title) {
        mBack.setVisibility(subScreen ? View.VISIBLE : View.GONE);
        mTitle.setText(subScreen && title != null && title.length() > 0
                ? title : mTitle.getContext().getString(R.string.app_name));''',
'''    void show(boolean subScreen, CharSequence title) {
        mBack.setVisibility(subScreen ? View.VISIBLE : View.GONE);
        // 0.0.10: TV uses the M rail brand; the duplicated "MarsTube" wordmark header goes away.
        mTitle.setVisibility(!subScreen
                && com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(mTitle.getContext())
                ? View.GONE : View.VISIBLE);
        mTitle.setText(subScreen && title != null && title.length() > 0
                ? title : mTitle.getContext().getString(R.string.app_name));''')

# ---------------------------------------------------------------------------
# Repo URL replacement in the German about strings.
# ---------------------------------------------------------------------------
replace_all(mobile / "res/values-de/strings_mobile.xml",
            'github.com/equiummc/mars-smarttube', 'github.com/equiummc/MarsTube', 2)

print("MT2-010 verified patch: 0.0.10 TV player/settings/Shorts, Media3 prefs, downloads hidden.")
