package com.liskovsoft.appupdatechecker2.core;

import com.liskovsoft.appupdatechecker2.ReleaseNotes;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Diagnostics-only regression suite against the actual public release JSON.
 * CI downloads the pinned release assets; no edited or mocked manifest is used.
 *
 * Proves parser and version comparison; DOES NOT prove the TV received the JSON
 * or that the UI launched its HTTP request. Those need live device logcat.
 */
public class MarsTubeUpdateRegressionTest {
    private static UpdateManifest load(String name, String abi) throws IOException, JSONException {
        try (InputStream in = MarsTubeUpdateRegressionTest.class.getResourceAsStream("/" + name)) {
            assertNotNull("Missing CI-downloaded JSON fixture: " + name, in);
            return UpdateManifest.parse(
                    new JSONObject(new String(in.readAllBytes(), StandardCharsets.UTF_8)),
                    abi, "de");
        }
    }

    @Test
    public void marsTube20MustSee21AsNewer() throws Exception {
        UpdateManifest m = load("marstube-v0.0.21.json", "arm64-v8a");
        assertEquals(12021, m.latest().versionCode);
        assertEquals("0.0.21", m.latest().versionName);
        List<ReleaseNotes> available = m.newerThan(12020);
        assertEquals("20 -> 21 must offer precisely one update", 1, available.size());
        assertEquals(12021, available.get(0).versionCode);
    }

    @Test
    public void marsTube18MustAlsoSee21() throws Exception {
        UpdateManifest m = load("marstube-v0.0.21.json", "armeabi-v7a");
        assertEquals(1, m.newerThan(12018).size());
        assertEquals(12021, m.newerThan(12018).get(0).versionCode);
    }

    @Test
    public void marsTube21MustNotOffer21Again() throws Exception {
        UpdateManifest m = load("marstube-v0.0.21.json", "arm64-v8a");
        assertTrue(m.newerThan(12021).isEmpty());
        assertTrue(m.newerThan(12022).isEmpty());
    }

    @Test
    public void missingInstalledVersionFromManifestIsAllowed() throws Exception {
        UpdateManifest m = load("marstube-v0.0.21.json", "arm64-v8a");
        assertNull("MarsTube publishes only the latest version", m.find(12020));
        assertEquals(12021, m.newerThan(12020).get(0).versionCode);
    }

    @Test
    public void everyTvArchitectureGetsOfficialUniversalApk() throws Exception {
        for (String abi : new String[]{"arm64-v8a", "armeabi-v7a", "x86_64"}) {
            UpdateManifest m = load("marstube-v0.0.21.json", abi);
            assertEquals(1, m.downloadUrls.size());
            assertEquals("https://github.com/equiummc/MarsTube/releases/download/v0.0.21/MarsTube-0.0.21.apk",
                    m.downloadUrls.get(0));
            assertTrue(m.downloadSize > 1_000_000L);
        }
    }

    @Test
    public void newTubeUpstreamParserStillDetectsNewerVersion() throws Exception {
        UpdateManifest m = load("newtube-v1.17.1.json", "arm64-v8a");
        assertEquals(11701, m.latest().versionCode);
        assertFalse(m.newerThan(11700).isEmpty());
        assertTrue(m.newerThan(11701).isEmpty());
        assertFalse(m.downloadUrls.isEmpty());
    }

    @Test
    public void newTubeFallbackAbiHasUsableUniversalApk() throws Exception {
        UpdateManifest m = load("newtube-v1.17.1.json", "x86_64");
        assertFalse(m.downloadUrls.isEmpty());
        assertTrue(m.downloadUrls.get(0).contains("NewTube_1.17.1_universal.apk"));
    }
}
