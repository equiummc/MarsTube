package com.newtube.mobile.ui.playback;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup;
import com.liskovsoft.smartyoutubetv2.common.app.models.search.MediaServiceSearchTagProvider;
import com.liskovsoft.smartyoutubetv2.common.app.models.search.vineyard.Tag;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.SearchPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.views.SearchView;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * On-demand YouTube search in the right-hand Shorts pane. It uses the existing
 * SearchPresenter and ContentService, not a second video decoder or a WebView.
 * Two cards fit vertically; D-pad focus/scroll reveals additional results.
 *
 * The existing Shorts session is kept while browsing. Selecting a result is
 * explicit and delegates to the application's existing video-open action.
 */
public final class MarsTvShortsSearchPanel implements SearchView {
    public static final int VOICE_REQUEST = 6271;
    private static final int CYAN = 0xFF33C5FF;
    private final Activity activity;
    private final FrameLayout parent;
    private final LinearLayout results;
    private final EditText query;
    private final ScrollView scroll;
    private final TextView status;
    private final java.util.function.Consumer<Video> openVideo;
    private final List<Video> videos = new ArrayList<>();
    private final Set<String> seenIds = new HashSet<>();
    private final int width;
    private SearchPresenter presenter;
    private boolean visible;
    private boolean paginating;
    private int pagedAt = -1;

    public MarsTvShortsSearchPanel(Activity owner, ViewGroup host,
                                   java.util.function.Consumer<Video> onOpen) {
        activity = owner;
        openVideo = onOpen;
        int fullWidth = owner.getResources().getDisplayMetrics().widthPixels;
        int videoWidth = MarsTvShortsStage.videoWidth(owner);
        int start = (fullWidth + videoWidth) / 2 + dp(86);
        width = Math.max(dp(170), fullWidth - start - dp(26));
        parent = new FrameLayout(owner);
        parent.setVisibility(View.GONE);
        parent.setFocusable(false);
        parent.setBackground(rounded(0xF50C2239, dp(12)));
        parent.setElevation(dp(14));
        FrameLayout.LayoutParams box = new FrameLayout.LayoutParams(
                width, ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        box.rightMargin = dp(10);
        host.addView(parent, box);

        LinearLayout layout = new LinearLayout(owner);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(12), dp(14), dp(12), dp(12));
        parent.addView(layout, new FrameLayout.LayoutParams(-1, -1));
        TextView heading = text("YouTube-Suche", 17, CYAN);
        layout.addView(heading);
        LinearLayout form = new LinearLayout(owner);
        form.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams formLp = new LinearLayout.LayoutParams(-1, dp(52));
        formLp.topMargin = dp(8);
        layout.addView(form, formLp);

        query = new EditText(owner);
        query.setSingleLine(true);
        query.setTextSize(14);
        query.setHint("Suchen …");
        query.setHintTextColor(0xFF9CAFC1);
        query.setTextColor(0xFFF2F8FF);
        query.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        query.setBackground(rounded(0xFF183A5A, dp(7)));
        query.setPadding(dp(9), 0, dp(9), 0);
        form.addView(query, new LinearLayout.LayoutParams(0, dp(43), 1f));

        TextView mic = action("🎤", "Spracheingabe");
        LinearLayout.LayoutParams micLp = new LinearLayout.LayoutParams(dp(42), dp(43));
        micLp.leftMargin = dp(6);
        form.addView(mic, micLp);
        mic.setOnClickListener(v -> startVoiceRecognition());

        TextView close = action("×", "Suche schließen");
        LinearLayout.LayoutParams closeLp = new LinearLayout.LayoutParams(dp(36), dp(43));
        closeLp.leftMargin = dp(5);
        form.addView(close, closeLp);
        close.setOnClickListener(v -> hide());

        status = text("Suchbegriff eingeben und mit OK bestätigen", 12, 0xFFA6C1DB);
        LinearLayout.LayoutParams statusLp = new LinearLayout.LayoutParams(-1, dp(43));
        layout.addView(status, statusLp);

        scroll = new ScrollView(owner);
        scroll.setFillViewport(false);
        scroll.setVerticalScrollBarEnabled(true);
        layout.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        results = new LinearLayout(owner);
        results.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(results, new ScrollView.LayoutParams(-1, -2));

        query.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH
                    || (event != null && event.getAction() == KeyEvent.ACTION_DOWN
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                submit();
                return true;
            }
            return false;
        });
        scroll.setOnScrollChangeListener((v, x, y, oldX, oldY) -> maybeMore());
    }

    private int dp(float n) {
        return Math.round(n * activity.getResources().getDisplayMetrics().density);
    }

    private static TextView text(String value, int size, int color, Activity owner) {
        TextView label = new TextView(owner);
        label.setText(value);
        label.setTextSize(size);
        label.setTextColor(color);
        label.setIncludeFontPadding(false);
        return label;
    }

    private TextView text(String value, int size, int color) {
        return text(value, size, color, activity);
    }

    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        d.setStroke(dp(1), 0x804A8BB7);
        return d;
    }

    private TextView action(String caption, String description) {
        TextView button = text(caption, 20, 0xFFF2F8FF);
        button.setGravity(Gravity.CENTER);
        button.setFocusable(true);
        button.setContentDescription(description);
        button.setBackground(rounded(0xFF164771, dp(8)));
        return button;
    }

    public boolean isOpen() {
        return visible;
    }

    public void show() {
        if (visible) { query.requestFocus(); return; }
        visible = true;
        parent.setVisibility(View.VISIBLE);
        parent.bringToFront();
        presenter = SearchPresenter.instance(activity);
        presenter.setView(this);
        query.requestFocus();
    }

    public void hide() {
        if (!visible) return;
        visible = false;
        parent.setVisibility(View.GONE);
        query.clearFocus();
        if (presenter != null && presenter.getView() == this) {
            presenter.onViewDestroyed(); // cancel any in-flight search/pagination
            presenter.setView(null);
        }
        presenter = null;
    }

    public void release() {
        hide();
        if (parent.getParent() instanceof ViewGroup)
            ((ViewGroup) parent.getParent()).removeView(parent);
    }

    private void submit() {
        String term = query.getText().toString().trim();
        if (term.isEmpty()) {
            status.setText("Bitte einen Suchbegriff eingeben.");
            return;
        }
        if (!visible || presenter == null) return;
        paginating = false;
        pagedAt = -1;
        status.setText("Suche nach: " + term);
        presenter.onSearch(term);
        query.clearFocus();
        scroll.requestFocus();
    }

    public boolean onVoiceResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode != VOICE_REQUEST) return false;
        if (visible && resultCode == Activity.RESULT_OK && data != null) {
            ArrayList<String> phrases = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            if (phrases != null && !phrases.isEmpty()) {
                query.setText(phrases.get(0));
                submit();
            }
        }
        return true;
    }

    private void maybeMore() {
        if (!visible || presenter == null || videos.isEmpty() || paginating) return;
        if (scroll.getScrollY() + scroll.getHeight() < results.getHeight() - dp(180)) return;
        if (pagedAt == videos.size()) return;
        Video last = videos.get(videos.size() - 1);
        if (last.getGroup() == null) return;
        pagedAt = videos.size();
        paginating = true;
        presenter.onScrollEnd(last);
    }

    private void add(Video v) {
        if (v == null || TextUtils.isEmpty(v.videoId) || !seenIds.add(v.videoId)) return;
        if (videos.size() >= 100) return;
        videos.add(v);
        final LinearLayout row = new LinearLayout(activity);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(7), dp(7), dp(7), dp(7));
        row.setFocusable(true);
        row.setBackground(rounded(0xFF153A60, dp(8)));
        final int thumbnailHeight = dp(116);
        ImageView thumbnail = new ImageView(activity);
        thumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
        thumbnail.setBackground(rounded(0xFF0A2038, dp(6)));
        thumbnail.setClipToOutline(true);
        row.addView(thumbnail, new LinearLayout.LayoutParams(dp(72), thumbnailHeight));
        Glide.with(activity).load(v.getCardImageUrl())
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .format(DecodeFormat.PREFER_RGB_565)
                .centerCrop().dontAnimate().into(thumbnail);
        LinearLayout copy = new LinearLayout(activity);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setPadding(dp(9), 0, 0, 0);
        TextView title = text(v.getTitle(), 13, 0xFFF2F8FF);
        title.setMaxLines(4);
        title.setEllipsize(TextUtils.TruncateAt.END);
        TextView author = text(v.getAuthor(), 12, CYAN);
        author.setMaxLines(2);
        copy.addView(title);
        copy.addView(author);
        row.addView(copy, new LinearLayout.LayoutParams(0, -2, 1f));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(130));
        lp.bottomMargin = dp(8);
        results.addView(row, lp);
        row.setContentDescription("Suchergebnis: " + v.getTitle());
        row.setOnFocusChangeListener((view, focused) -> {
            row.setBackground(rounded(focused ? 0xFF1767A1 : 0xFF153A60, dp(8)));
            if (focused) scroll.smoothScrollTo(0, row.getTop());
        });
        row.setOnClickListener(view -> {
            hide();
            openVideo.accept(v);
        });
    }

    @Override public void updateSearch(VideoGroup group) {
        if (group == null) return;
        activity.runOnUiThread(() -> {
            if (!visible || presenter == null || presenter.getView() != this) return;
            if (group.getVideos() != null) {
                for (Video v : group.getVideos()) add(v);
            }
            status.setText(videos.isEmpty() ? "Keine Ergebnisse" :
                    videos.size() + " Treffer · mit Runter weiterscrollen");
            paginating = false;
        });
    }

    @Override public void clearSearch() {
        activity.runOnUiThread(() -> {
            videos.clear();
            seenIds.clear();
            results.removeAllViews();
            scroll.scrollTo(0, 0);
        });
    }

    @Override public void clearSearchTags() { }
    @Override public void removeSearchTag(Tag tag) { }
    @Override public void setTagsProvider(MediaServiceSearchTagProvider provider) { }
    @Override public void showProgressBar(boolean busy) {
        activity.runOnUiThread(() -> {
            if (!visible) return;
            if (busy) status.setText("Suche läuft …");
            else {
                paginating = false;
                if (videos.isEmpty()) status.setText("Keine Treffer oder Verbindung nicht verfügbar.");
            }
        });
    }
    @Override public void showLoadFailure(int state) {
        activity.runOnUiThread(() -> status.setText(
                "Suche nicht verfügbar. Verbindung prüfen oder erneut suchen."));
    }
    @Override public void startSearch(String term) {
        if (term != null) {
            query.setText(term);
            submit();
        }
    }
    @Override public String getSearchText() { return query.getText().toString(); }
    @Override public void startVoiceRecognition() {
        if (!visible) return;
        boolean available = false;
        try { available = SpeechRecognizer.isRecognitionAvailable(activity); }
        catch (RuntimeException ignored) { }
        if (!available) {
            query.requestFocus();
            status.setText("Keine Spracheingabe verfügbar – bitte Text eingeben.");
            return;
        }
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "de-DE");
        i.putExtra(RecognizerIntent.EXTRA_PROMPT, "Shorts durchsuchen");
        try { activity.startActivityForResult(i, VOICE_REQUEST); }
        catch (ActivityNotFoundException | SecurityException e) {
            query.requestFocus();
            status.setText("Spracherkennung nicht vorhanden – Textsuche verfügbar.");
        }
    }
    @Override public void finishReally() { hide(); }
}
