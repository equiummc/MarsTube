package com.newtube.mobile.player;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import com.liskovsoft.smartyoutubetv2.common.prefs.PlayerData;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.ConscryptMode;

/**
 * MT2-010: the TV preference model must keep 0.0.9 defaults (Normal buffer, subtitles off,
 * four home tiles) and expose the documented ceilings/mappings without touching phone data.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, manifest = Config.NONE, application = Application.class)
@ConscryptMode(ConscryptMode.Mode.OFF)
public class MarsTvPlayerPrefsTest {
    private Application app;
    private SharedPreferences prefs;

    @Before
    public void setUp() {
        app = RuntimeEnvironment.getApplication();
        prefs = app.getSharedPreferences(MarsTvPlayerPrefs.PREFS, Context.MODE_PRIVATE);
        prefs.edit().clear().commit();
    }

    @After
    public void tearDown() {
        prefs.edit().clear().commit();
    }

    @Test
    public void defaultsMatchTheDocumented019Behaviour() {
        assertEquals(MarsTvPlayerPrefs.RES_AUTO, MarsTvPlayerPrefs.getResolutionCeiling(app));
        assertEquals(MarsTvPlayerPrefs.FPS_AUTO, MarsTvPlayerPrefs.getMaxFps(app));
        assertEquals(MarsTvPlayerPrefs.CODEC_AUTO, MarsTvPlayerPrefs.getPreferredCodec(app));
        assertEquals(MarsTvPlayerPrefs.HDR_AUTO, MarsTvPlayerPrefs.getHdrMode(app));
        assertEquals(MarsTvPlayerPrefs.BITRATE_AUTO, MarsTvPlayerPrefs.getBitrateLimitMbps(app));
        assertEquals(MarsTvPlayerPrefs.BUFFER_NORMAL, MarsTvPlayerPrefs.getBufferPreset(app));
        assertEquals(MarsTvPlayerPrefs.START_QUALITY_FIRST, MarsTvPlayerPrefs.getStartStrategy(app));
        assertEquals(MarsTvPlayerPrefs.SUBTITLES_OFF, MarsTvPlayerPrefs.getSubtitlePolicy(app));
        assertEquals(MarsTvPlayerPrefs.TILES_4, MarsTvPlayerPrefs.getHomeTileCount(app));
        assertTrue(MarsTvPlayerPrefs.isProfileGrid(app));
        assertTrue(MarsTvPlayerPrefs.isHistoryGrid(app));
        assertTrue(MarsTvPlayerPrefs.isSubscriptionsGrid(app));
        assertFalse(MarsTvPlayerPrefs.isSubscriptionsChannelFilter(app));
        assertTrue(MarsTvPlayerPrefs.isMiniPlayerEnabled(app));
        assertTrue(MarsTvPlayerPrefs.areDownloadsHidden(app));
    }

    @Test
    public void startGateSpendsMoreTimeOnlyWhenQualityFirstIsChosen() {
        MarsTvPlayerPrefs.setStartStrategy(app, MarsTvPlayerPrefs.START_QUALITY_FIRST);
        assertEquals(700, MarsTvPlayerPrefs.startBufferMs(app));

        MarsTvPlayerPrefs.setStartStrategy(app, MarsTvPlayerPrefs.START_FAST);
        assertEquals(250, MarsTvPlayerPrefs.startBufferMs(app));
    }

    @Test
    public void bufferPresetsMapOntoTheSharedPlayerDataVocabulary() {
        assertEquals(PlayerData.BUFFER_LOW,
                MarsTvPlayerPrefs.bufferPlayerDataValue(MarsTvPlayerPrefs.BUFFER_LOW));
        assertEquals(PlayerData.BUFFER_MEDIUM,
                MarsTvPlayerPrefs.bufferPlayerDataValue(MarsTvPlayerPrefs.BUFFER_NORMAL));
        assertEquals(PlayerData.BUFFER_HIGH,
                MarsTvPlayerPrefs.bufferPlayerDataValue(MarsTvPlayerPrefs.BUFFER_HIGH));
        assertEquals(PlayerData.BUFFER_MEDIUM,
                MarsTvPlayerPrefs.bufferPlayerDataValue(99));
    }

    @Test
    public void codecPreferenceBecomesASoftMedia3MimePreference() {
        MarsTvPlayerPrefs.setPreferredCodec(app, MarsTvPlayerPrefs.CODEC_AUTO);
        assertEquals(0, MarsTvPlayerPrefs.preferredVideoMimeTypes(app).length);

        MarsTvPlayerPrefs.setPreferredCodec(app, MarsTvPlayerPrefs.CODEC_H264);
        assertEquals("video/avc", MarsTvPlayerPrefs.preferredVideoMimeTypes(app)[0]);

        MarsTvPlayerPrefs.setPreferredCodec(app, MarsTvPlayerPrefs.CODEC_VP9);
        assertEquals("video/x-vnd.on2.vp9", MarsTvPlayerPrefs.preferredVideoMimeTypes(app)[0]);

        MarsTvPlayerPrefs.setPreferredCodec(app, MarsTvPlayerPrefs.CODEC_AV1);
        assertEquals("video/av01", MarsTvPlayerPrefs.preferredVideoMimeTypes(app)[0]);
    }

    @Test
    public void bitrateIsStoredInMbpsButHandedToMedia3InBitsPerSecond() {
        MarsTvPlayerPrefs.setBitrateLimitMbps(app, 0);
        assertEquals(0, MarsTvPlayerPrefs.bitrateLimitBps(app));

        MarsTvPlayerPrefs.setBitrateLimitMbps(app, 10);
        assertEquals(10_000_000, MarsTvPlayerPrefs.bitrateLimitBps(app));
    }

    @Test
    public void homeTileCountRejectsUndocumentedValues() {
        MarsTvPlayerPrefs.setHomeTileCount(app, MarsTvPlayerPrefs.TILES_6);
        assertEquals(6, MarsTvPlayerPrefs.getHomeTileCount(app));
        MarsTvPlayerPrefs.setHomeTileCount(app, MarsTvPlayerPrefs.TILES_9);
        assertEquals(9, MarsTvPlayerPrefs.getHomeTileCount(app));
        MarsTvPlayerPrefs.setHomeTileCount(app, 5);
        assertEquals(MarsTvPlayerPrefs.TILES_4, MarsTvPlayerPrefs.getHomeTileCount(app));
    }

    @Test
    public void shortsGridIsThreeColumnsOnWideTvAndTwoOnSmallViewports() {
        Configuration configuration = app.getResources().getConfiguration();
        int original = configuration.screenWidthDp;
        try {
            configuration.screenWidthDp = 960;
            assertEquals(3, MarsTvPlayerPrefs.shortsSpanCount(app));
            configuration.screenWidthDp = 640;
            assertEquals(2, MarsTvPlayerPrefs.shortsSpanCount(app));
        } finally {
            configuration.screenWidthDp = original;
        }
    }

    @Test
    public void userChoicesSurviveAFreshRead() {
        MarsTvPlayerPrefs.setResolutionCeiling(app, MarsTvPlayerPrefs.RES_1080);
        MarsTvPlayerPrefs.setMaxFps(app, MarsTvPlayerPrefs.FPS_30);
        MarsTvPlayerPrefs.setHdrMode(app, MarsTvPlayerPrefs.HDR_OFF);
        MarsTvPlayerPrefs.setSubtitlePolicy(app, MarsTvPlayerPrefs.SUBTITLES_FORCED_ONLY);
        MarsTvPlayerPrefs.setSubscriptionsChannelFilter(app, true);
        MarsTvPlayerPrefs.setMiniPlayerEnabled(app, false);

        assertEquals(MarsTvPlayerPrefs.RES_1080, MarsTvPlayerPrefs.getResolutionCeiling(app));
        assertEquals(MarsTvPlayerPrefs.FPS_30, MarsTvPlayerPrefs.getMaxFps(app));
        assertEquals(MarsTvPlayerPrefs.HDR_OFF, MarsTvPlayerPrefs.getHdrMode(app));
        assertEquals(MarsTvPlayerPrefs.SUBTITLES_FORCED_ONLY,
                MarsTvPlayerPrefs.getSubtitlePolicy(app));
        assertTrue(MarsTvPlayerPrefs.isSubscriptionsChannelFilter(app));
        assertFalse(MarsTvPlayerPrefs.isMiniPlayerEnabled(app));
    }
}
