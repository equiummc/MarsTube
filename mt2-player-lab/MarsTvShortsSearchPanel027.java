package com.newtube.mobile.ui.playback;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup;
import com.liskovsoft.youtubeapi.service.YouTubeServiceManager;
import io.reactivex.rxjava3.disposables.Disposable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

/**
 * TV-only Shorts search overlay. It shares the established YouTube ContentService
 * and single playback surface; never opens MobileSearchActivity or a second player.
 * A voice recognizer is launched on demand via the platform speech activity,
 * without recording audio or requesting microphone permissions in MarsTube.
 */
public final class MarsTvShortsSearchPanel {
    public static final int VOICE_REQUEST = 6027;
    private static final int BLUE = 0xFF16B8FF;
    private static final int WHITE = 0xFFF3F7FF;
    private static final int MAX_RESULTS = 16;
    private final MobilePlaybackActivity activity;
    private final ViewGroup host;
    private final Consumer<Video> onChoose;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final LinearLayout panel;
    private final EditText input;
    private final TextView info;
    private final LinearLayout results;
    private final ScrollView scroll;
    private final Set<String> shownIds = new HashSet<>();
    private final Runnable debounced = this::search;
    private Disposable currentSearch;
    private int generation;

    public MarsTvShortsSearchPanel(MobilePlaybackActivity activity, ViewGroup host,
                                    Consumer<Video> onChoose) {
        this.activity = activity;
        this.host = host;
        this.onChoose = onChoose;
        panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(12), dp(15), dp(12), dp(12));
        GradientDrawable glass = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xF50A2948, 0xFA071729});
        glass.setCornerRadius(dp(14));
        glass.setStroke(dp(1), 0xAA6DA1C8);
        panel.setBackground(glass);
        panel.setVisibility(View.GONE);
        panel.setElevation(dp(15));

        TextView heading = text("Shorts durchsuchen", 15, BLUE);
        heading.setPadding(0, 0, 0, dp(9));
        panel.addView(heading, new LinearLayout.LayoutParams(-1, dp(29)));
        LinearLayout searchBar = new LinearLayout(activity);
        searchBar.setGravity(Gravity.CENTER_VERTICAL);
        panel.addView(searchBar, new LinearLayout.LayoutParams(-1, dp(44)));

        input = new EditText(activity);
        input.setSingleLine(true);
        input.setTextSize(14);
        input.setHint("Video oder Creator suchen");
        input.setHintTextColor(0xFF9DC8E6);
        input.setTextColor(WHITE);
        input.setSelectAllOnFocus(false);
        input.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        input.setPadding(dp(7), 0, dp(7), 0);
        searchBar.addView(input, new LinearLayout.LayoutParams(0, dp(43), 1f));

        ImageView mic = new ImageView(activity);
        mic.setImageResource(android.R.drawable.ic_btn_speak_now);
        mic.setColorFilter(WHITE);
        mic.setPadding(dp(7), dp(7), dp(7), dp(7));
        mic.setContentDescription("Sprachsuche starten");
        mic.setFocusable(true);
        mic.setOnClickListener(v -> startVoice());
        searchBar.addView(mic, new LinearLayout.LayoutParams(dp(40), dp(40)));

        info = text("Suchbegriff eingeben oder Mikrofon wählen", 11, 0xFFBAD3E8);
        info.setPadding(0, dp(9), 0, dp(8));
        panel.addView(info, new LinearLayout.LayoutParams(-1, dp(45)));

        scroll = new ScrollView(activity);
        scroll.setFillViewport(false);
        scroll.setVerticalScrollBarEnabled(false);
        results = new LinearLayout(activity);
        results.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(results, new ScrollView.LayoutParams(-1, -2));
        panel.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                handler.removeCallbacks(debounced);
                search();
                return true;
            }
            return false;
        });
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                handler.removeCallbacks(debounced);
                if (s != null && s.length() >= 2) handler.postDelayed(debounced, 400L);
            }
            @Override public void afterTextChanged(Editable e) {}
        });
    }

    public boolean isOpen() { return panel.getVisibility() == View.VISIBLE; }

    public void open() {
        if (panel.getParent() == null) {
            int screenWidth = activity.getResources().getDisplayMetrics().widthPixels;
            int videoWidth = MarsTvShortsStage.videoWidth(activity);
            int start = (screenWidth + videoWidth) / 2 + dp(86);
            int available = screenWidth - start - dp(15);
            int width = Math.max(dp(155), available);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    width, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.RIGHT);
            params.setMargins(0, dp(14), dp(12), dp(14));
            host.addView(panel, params);
        }
        panel.setVisibility(View.VISIBLE);
        panel.bringToFront();
        input.requestFocus();
    }

    public void close() {
        if (!isOpen()) return;
        panel.setVisibility(View.GONE);
        generation++;
        handler.removeCallbacks(debounced);
        if (currentSearch != null) currentSearch.dispose();
        currentSearch = null;
        shownIds.clear();
        results.removeAllViews();
    }

    private void search() {
        if (!isOpen()) return;
        String query = input.getText().toString().trim();
        if (query.length() < 2) {
            info.setText("Mindestens zwei Zeichen eingeben");
            return;
        }
        int ticket = ++generation;
        if (currentSearch != null) currentSearch.dispose();
        shownIds.clear();
        results.removeAllViews();
        scroll.scrollTo(0, 0);
        info.setText("Suche läuft …");
        currentSearch = YouTubeServiceManager.instance().getContentService()
                .getSearchObserve(query, 0)
                .subscribe(groups -> activity.runOnUiThread(() -> {
                    if (ticket != generation || !isOpen()) return;
                    for (MediaGroup group : groups) {
                        VideoGroup vg = VideoGroup.from(group);
                        if (vg == null || vg.getVideos() == null) continue;
                        for (Video video : vg.getVideos()) {
                            if (video == null || !video.hasVideo()
                                    || TextUtils.isEmpty(video.videoId)
                                    || !shownIds.add(video.videoId)) continue;
                            if (shownIds.size() <= MAX_RESULTS) addResult(video);
                        }
                    }
                    info.setText(shownIds.isEmpty() ? "Keine Ergebnisse" : "Ergebnisse · Hoch/Runter scrollen");
                }), error -> activity.runOnUiThread(() -> {
                    if (ticket == generation && isOpen())
                        info.setText("Suche fehlgeschlagen. Erneut versuchen.");
                }), () -> activity.runOnUiThread(() -> {
                    if (ticket == generation && isOpen() && shownIds.isEmpty())
                        info.setText("Keine Videos gefunden");
                }));
    }

    private void addResult(Video video) {
        LinearLayout card = new LinearLayout(activity);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(5), dp(7), dp(5), dp(7));
        card.setFocusable(true);
        card.setClickable(true);
        GradientDrawable normal = new GradientDrawable();
        normal.setColor(0xE3194265);
        normal.setCornerRadius(dp(9));
        normal.setStroke(dp(1), 0x886B9ABB);
        GradientDrawable focused = new GradientDrawable();
        focused.setColor(0xF0245F8C);
        focused.setCornerRadius(dp(9));
        focused.setStroke(dp(2), BLUE);
        card.setBackground(normal);
        card.setOnFocusChangeListener((v, hasFocus) -> {
            v.setBackground(hasFocus ? focused : normal);
            if (hasFocus) scroll.post(() -> scroll.smoothScrollTo(0, card.getTop()));
        });
        card.setOnClickListener(v -> {
            close();
            onChoose.accept(video);
        });
        ImageView thumbnail = new ImageView(activity);
        thumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
        GradientDrawable corners = new GradientDrawable();
        corners.setColor(0xFF102D46);
        corners.setCornerRadius(dp(5));
        thumbnail.setBackground(corners);
        thumbnail.setClipToOutline(true);
        card.addView(thumbnail, new LinearLayout.LayoutParams(dp(90), dp(152)));
        Glide.with(activity).load(video.getCardImageUrl())
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .format(DecodeFormat.PREFER_RGB_565)
                .centerCrop().dontAnimate().into(thumbnail);

        LinearLayout copy = new LinearLayout(activity);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setPadding(dp(9), 0, 0, 0);
        TextView title = text(video.getTitle(), 12, WHITE);
        title.setMaxLines(4);
        title.setEllipsize(TextUtils.TruncateAt.END);
        TextView author = text(TextUtils.isEmpty(video.getAuthor())
                ? "YouTube" : video.getAuthor(), 11, BLUE);
        author.setMaxLines(2);
        copy.addView(title);
        copy.addView(author);
        card.addView(copy, new LinearLayout.LayoutParams(0, -2, 1f));
        // Two cards visible without scrolling on the expected TV screen sizes.
        int h = Math.max(dp(170), (activity.getResources().getDisplayMetrics().heightPixels - dp(176)) / 2);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, h);
        lp.bottomMargin = dp(8);
        results.addView(card, lp);
    }

    private void startVoice() {
        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.GERMAN.toLanguageTag());
            activity.startActivityForResult(intent, VOICE_REQUEST);
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(activity, "Sprachsuche auf diesem Gerät nicht verfügbar",
                    Toast.LENGTH_SHORT).show();
            input.requestFocus();
        }
    }

    public boolean onVoiceResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != VOICE_REQUEST) return false;
        if (resultCode == Activity.RESULT_OK && data != null) {
            ArrayList<String> words = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (words != null && !words.isEmpty() && !TextUtils.isEmpty(words.get(0))) {
                input.setText(words.get(0));
                input.setSelection(input.length());
                handler.removeCallbacks(debounced);
                search();
            }
        }
        return true;
    }

    private TextView text(String value, int size, int color) {
        TextView v = new TextView(activity);
        v.setText(value);
        v.setTextSize(size);
        v.setTextColor(color);
        v.setMaxLines(4);
        v.setEllipsize(TextUtils.TruncateAt.END);
        return v;
    }

    private int dp(float v) {
        return (int) (v * activity.getResources().getDisplayMetrics().density + .5f);
    }
}
