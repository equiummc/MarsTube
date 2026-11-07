#!/usr/bin/env python3
"""MT2-030: TV IME keyboard and opt-in in-app SEARCH text entry, no bridge.

Applied after 029 to a disposable pinned reference checkout. No Alexa/YouTube
intent interception, microphone permission, audio capture or background work.
"""
from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit("Usage: patch_030_inapp_search_input.py <pinned-checkout>")
root = Path(sys.argv[1]).resolve()
here = Path(__file__).resolve().parent
tv = root / "smarttubetv/src/stmobile/java/com/newtube/mobile/ui"
search = tv / "playback/MarsTvShortsSearchPanel.java"
player = tv / "playback/MobilePlaybackActivity.java"
manifest = root / "smarttubetv/src/stmobile/AndroidManifest.xml"
entry = tv / "playback/MarsTvSearchIntentEntry.java"
if not (root/"sabr-media3").is_dir():
    raise SystemExit("MT2-030 pinned source checkout missing")
if not (here/"MarsTvSearchIntentEntry030.java").is_file():
    raise SystemExit("MT2-030 search entry source missing")

def once(path, old, new):
    s = path.read_text(encoding="utf-8")
    count = s.count(old)
    if count != 1:
        raise SystemExit("MT2-030 unsafe anchor %s: count=%d: %r" %
                         (path.name, count, old[:110]))
    path.write_text(s.replace(old, new, 1), encoding="utf-8")

entry.write_bytes((here/"MarsTvSearchIntentEntry030.java").read_bytes())

once(search,
"""import android.view.inputmethod.EditorInfo;""",
"""import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;""")
once(search,
"""        input.setSingleLine(true);
        input.setTextSize(14);""",
"""        input.setSingleLine(true);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setTextSize(14);""")
once(search,
"""        input.requestFocus();
    }

    public void close() {""",
"""        showNativeKeyboard();
    }

    /**
     * Ask Fire OS for the keyboard owned by the active IME. If that keyboard
     * offers speech input it may enter recognized text into this EditText;
     * Amazon's global remote Alexa key is NOT interceptable by the app.
     */
    private void showNativeKeyboard() {
        if (!isOpen()) return;
        input.requestFocus();
        input.post(() -> {
            if (!isOpen()) return;
            InputMethodManager ime = (InputMethodManager) activity
                    .getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (ime != null) ime.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
        });
    }

    /** Receives text only, never raw audio; called by the opt-in search entry. */
    public void applyRecognizedText(String phrase) {
        if (phrase == null) return;
        String query = phrase.trim();
        if (query.length() < 2 || query.length() > 160) return;
        if (!isOpen()) open();
        input.setText(query);
        input.setSelection(input.length());
        handler.removeCallbacks(debounced);
        search();
        input.post(() -> {
            InputMethodManager ime = (InputMethodManager) activity
                    .getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (ime != null) ime.hideSoftInputFromWindow(input.getWindowToken(), 0);
        });
    }

    public void close() {""")
once(search,
"""            Toast.makeText(activity,
                    "Alexa-Mikrofon gehört zur Fire-TV-Suche. Bitte Text eingeben.",
                    Toast.LENGTH_LONG).show();
            input.requestFocus();
            return;""",
"""            info.setText("Fire-TV-Tastatur geöffnet. Sprachdiktat nur, falls die Tastatur es anbietet.");
            showNativeKeyboard();
            return;""")
once(player,
"""    private static java.lang.ref.WeakReference<MobilePlaybackActivity> sCurrent = new java.lang.ref.WeakReference<>(null);""",
"""    private static java.lang.ref.WeakReference<MobilePlaybackActivity> sCurrent = new java.lang.ref.WeakReference<>(null);

    /**
     * Lightweight search-Intent entry: only an ALREADY active TV Shorts player
     * may accept recognized text. Does not create a new player or seize the
     * YouTube package/Fire TV remote voice key.
     */
    public static boolean marsTvReceiveShortsSearchQuery(String rawQuery) {
        MobilePlaybackActivity active = sCurrent.get();
        if (active == null || rawQuery == null || rawQuery.trim().length() < 2
                || rawQuery.length() > 160 || active.isFinishing()
                || active.mIsStopped || active.mMarsTvShell == null
                || !com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(active)
                || !com.newtube.mobile.ui.common.MarsShortsSession.isActiveFor(active.getVideo())) {
            return false;
        }
        String query = rawQuery.trim();
        active.runOnUiThread(() -> {
            if (!active.isFinishing() && active.mMarsTvShell != null
                    && com.newtube.mobile.ui.common.MarsShortsSession.isActiveFor(active.getVideo())) {
                active.marsTvOpenShortsSearch(query);
            }
        });
        return true;
    }""")
once(player,
"""                    @Override public void search() {
                        // In-place panel: no navigation away from the active Short.
                        if (mCommentsPanel != null && mCommentsPanel.isOpen()) {
                            mCommentsPanel.marsTvBack();
                        }
                        if (mMarsShortsSearch == null) {
                            mMarsShortsSearch = new MarsTvShortsSearchPanel(
                                    MobilePlaybackActivity.this, mVideoArea,
                                    video -> onRelatedClicked(video));
                        }
                        mMarsShortsSearch.open();
                    }""",
"""                    @Override public void search() {
                        marsTvOpenShortsSearch(null);
                    }""")
once(player,
"""    private void marsTvOpenShortsComments() {""",
"""    private void marsTvOpenShortsSearch(@androidx.annotation.Nullable String query) {
        if (mCommentsPanel != null && mCommentsPanel.isOpen())
            mCommentsPanel.marsTvBack();
        if (mMarsShortsSearch == null) {
            mMarsShortsSearch = new MarsTvShortsSearchPanel(this, mVideoArea,
                    video -> onRelatedClicked(video));
        }
        mMarsShortsSearch.open();
        if (query != null) mMarsShortsSearch.applyRecognizedText(query);
    }

    private void marsTvOpenShortsComments() {""")
once(manifest,
"""        <!--
          Touch player (Wave 2).""",
"""        <!-- MT2-030 opt-in text receiver for active TV Shorts only. It does not
             take over YouTube/Alexa, declare a microphone or replace system search. -->
        <activity
            android:name="com.newtube.mobile.ui.playback.MarsTvSearchIntentEntry"
            android:exported="true"
            android:noHistory="true"
            android:theme="@style/Theme.NewTube.Dialog">
            <intent-filter>
                <action android:name="android.intent.action.SEARCH" />
                <category android:name="android.intent.category.DEFAULT" />
            </intent-filter>
        </activity>

        <!--
          Touch player (Wave 2).""")
stage = (tv/"playback/MarsTvShortsStage.java").read_text(encoding="utf-8")
assert "setVideoFocused(boolean focused)" in stage
assert "mars_short_next_029" in stage
assert "dislikeCountLabel" not in stage
assert "applicationId \"de.equiummc.marstube2\"" in (root/"smarttubetv/build.gradle").read_text(encoding="utf-8")
assert "versionCode 12030" in (root/"smarttubetv/build.gradle").read_text(encoding="utf-8")
assert "MarsTvSearchIntentEntry" in manifest.read_text(encoding="utf-8")
assert "marsTvReceiveShortsSearchQuery" in player.read_text(encoding="utf-8")
assert "InputMethodManager.SHOW_IMPLICIT" in search.read_text(encoding="utf-8")
assert "ACTION_SEARCH" in entry.read_text(encoding="utf-8")
assert "com.amazon.firetv.youtube" not in manifest.read_text(encoding="utf-8")
print("MT2-030: only native TV keyboard + guarded in-app recognized-text search entry, no Alexa bridge.")
