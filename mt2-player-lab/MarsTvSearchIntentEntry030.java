package com.newtube.mobile.ui.playback;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

/**
 * Opt-in Android SearchManager text entry for an already visible TV Shorts player.
 * Does NOT register as the Fire TV/Alexa/YouTube app and never intercepts its
 * remote microphone key. No network, recorder, service or additional permission.
 *
 * A future, separately approved integration can supply a recognized query through
 * ACTION_SEARCH + SearchManager.QUERY without replacing the YouTube package.
 */
public final class MarsTvSearchIntentEntry extends Activity {
    public static final String ACTION_SHORTS_SEARCH =
            "de.equiummc.marstube2.action.SEARCH_SHORTS";

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        deliver(getIntent());
        finish();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        deliver(intent);
        finish();
    }

    private void deliver(Intent intent) {
        if (intent == null || !(
                Intent.ACTION_SEARCH.equals(intent.getAction())
                || ACTION_SHORTS_SEARCH.equals(intent.getAction()))) {
            return;
        }
        String query = intent.getStringExtra(SearchManager.QUERY);
        if (TextUtils.isEmpty(query)) {
            return;
        }
        query = query.trim();
        if (query.length() < 2 || query.length() > 160) {
            return;
        }
        // Drops the request when a TV Shorts player is not already active.
        // In particular this does not start a second player or system-wide search.
        MobilePlaybackActivity.marsTvReceiveShortsSearchQuery(query);
    }
}
