package com.newtube.mobile.ui.playback;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.media3.common.C;
import androidx.media3.exoplayer.ExoPlayer;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.newtube.mobile.ui.common.MarsShortsSession;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * TV-only Shorts page chrome. One existing Media3 player continues to render the video.
 * This overlay supplies two portrait previews on the left, a smaller wall clock, a
 * remote-accessible autoplay preference, and a thin progress indicator on the right.
 * No extra decoder, WebView or GPU blur is created.
 */
public final class MarsTvShortsStage {
    private static final int BLUE = 0xFF16B8FF;
    private static final int WHITE = 0xFFF1F7FF;
    private static final String PREF = "mars_tv_shorts";
    private static final String AUTO_NEXT = "auto_next";
    private final Activity activity;
    private final FrameLayout root;
    private final TextView clock;
    private final TextView autoButton;
    private final TextView elapsed;
    private final TextView durationText;
    private final View progress;
    private final TextView likeButton;
    private final TextView dislikeButton;
    private final TextView saveButton;
    private final TextView seekBackButton;
    private final TextView seekForwardButton;
    private final Runnable onSave;
    private boolean previewsFocused;
    private boolean progressFocused;
    private boolean reactionsFocused;
    private boolean autoFocused;
    private int reactionIndex;
    private int selectedIndex = -1;
    private int lastRenderedStart = -1;
    private boolean lastRenderedFocus;
    private int lastRenderedSelection = -1;
    private final LinearLayout previewColumn;
    private final List<Preview> previews = new ArrayList<>();
    private final SimpleDateFormat clockFormat = new SimpleDateFormat("HH:mm", Locale.GERMANY);
    private String lastPreviewFingerprint = "";
    private int lastQueuePosition = -1;
    private long currentPosition;
    private long currentDuration;

    /**
     * Right-hand video width is derived from a 9:16 picture at screen height.
     * The visual scrub bar owns the bottom 7dp; the video never draws over it.
     */
    public static int videoWidth(Activity activity) {
        int height = activity.getResources().getDisplayMetrics().heightPixels;
        float density = activity.getResources().getDisplayMetrics().density;
        return Math.round(Math.max(1, height - Math.round(7 * density)) * 9f / 16f);
    }

    public MarsTvShortsStage(Activity activity, FrameLayout host, Runnable onAutoChange,
                             Runnable onLike, Runnable onDislike, Runnable onSave,
                             Runnable onSeekBack, Runnable onSeekForward) {
        this.activity = activity;
        this.onSave = onSave;
        root = new FrameLayout(activity);
        root.setVisibility(View.GONE);
        root.setFocusable(false);
        host.addView(root, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        final int screenWidth = activity.getResources().getDisplayMetrics().widthPixels;
        final int videoWidth = Math.min(screenWidth, videoWidth(activity));
        final int menuWidth = screenWidth - videoWidth;
        // Slightly slimmer control column; leaves more room for the Shorts cards.
        final int listWidth = Math.round(menuWidth * 0.75f);

        LinearLayout surface = new LinearLayout(activity);
        surface.setOrientation(LinearLayout.HORIZONTAL);
        surface.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFF081B34, 0xFF0B3761, 0xFF064D83}));
        root.addView(surface, new FrameLayout.LayoutParams(
                menuWidth, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.LEFT | Gravity.TOP));

        // Left: three portrait preview cards, no bottom key legend.
        LinearLayout left = new LinearLayout(activity);
        left.setOrientation(LinearLayout.VERTICAL);
        left.setPadding(dp(18), dp(22), dp(12), dp(19));
        surface.addView(left, new LinearLayout.LayoutParams(listWidth,
                ViewGroup.LayoutParams.MATCH_PARENT));

        previewColumn = new LinearLayout(activity);
        previewColumn.setOrientation(LinearLayout.VERTICAL);
        left.addView(previewColumn, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        // Preserve the visible 9:16 thumbnail instead of slicing it vertically.
        final int screenHeight = activity.getResources().getDisplayMetrics().heightPixels;
        final int previewImageWidth = Math.min(Math.round(listWidth * 0.35f),
                Math.max(dp(100), Math.round(Math.max(1, screenHeight - dp(70)) * 9f / 32f)));
        for (int i = 0; i < 2; i++) {
            Preview preview = new Preview();
            LinearLayout row = new LinearLayout(activity);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(5), dp(5), dp(9), dp(5));
            row.setBackground(chrome(false));
            ImageView image = new ImageView(activity);
            image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            image.setClipToOutline(true);
            image.setBackground(chrome(false));
            row.addView(image, new LinearLayout.LayoutParams(previewImageWidth,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            LinearLayout texts = new LinearLayout(activity);
            texts.setOrientation(LinearLayout.VERTICAL);
            texts.setPadding(dp(11), 0, 0, 0);
            TextView number = label(11, BLUE);
            TextView title = label(14, WHITE);
            title.setMaxLines(3);
            title.setEllipsize(TextUtils.TruncateAt.END);
            TextView author = label(12, 0xFFC0D5ED);
            author.setMaxLines(2);
            author.setEllipsize(TextUtils.TruncateAt.END);
            texts.addView(number);
            texts.addView(title);
            texts.addView(author);
            row.addView(texts, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
            rowLp.bottomMargin = dp(9);
            previewColumn.addView(row, rowLp);
            preview.row = row;
            preview.image = image;
            preview.title = title;
            preview.author = author;
            preview.number = number;
            previews.add(preview);
        }

        // Centre: brand and clock, compact autoplay, time capsule, real YouTube actions.
        LinearLayout centre = new LinearLayout(activity);
        centre.setOrientation(LinearLayout.VERTICAL);
        centre.setGravity(Gravity.CENTER_HORIZONTAL);
        centre.setPadding(dp(9), dp(11), dp(10), dp(11));
        surface.addView(centre, new LinearLayout.LayoutParams(
                Math.max(0, menuWidth - listWidth), ViewGroup.LayoutParams.MATCH_PARENT));

        TextView heading = label(19, WHITE);
        heading.setText("▶ SHORTS");
        heading.setGravity(Gravity.CENTER);
        heading.setLetterSpacing(0.06f);
        heading.setContentDescription("MarsTube Shorts");
        centre.addView(heading, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(34)));

        clock = label(18, WHITE);
        clock.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams clockLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(33));
        clockLp.bottomMargin = dp(12);
        centre.addView(clock, clockLp);

        autoButton = label(15, WHITE);
        autoButton.setGravity(Gravity.CENTER);
        autoButton.setPadding(dp(6), 0, dp(6), 0);
        autoButton.setClickable(true);
        autoButton.setOnClickListener(v -> {
            toggleAutoNext();
            onAutoChange.run();
        });
        LinearLayout.LayoutParams autoLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(39));
        autoLp.bottomMargin = dp(13);
        centre.addView(autoButton, autoLp);

        LinearLayout times = new LinearLayout(activity);
        times.setGravity(Gravity.CENTER_HORIZONTAL);
        times.setOrientation(LinearLayout.VERTICAL);
        times.setPadding(dp(6), dp(7), dp(6), dp(7));
        times.setBackground(chrome(false));
        elapsed = label(15, WHITE);
        elapsed.setGravity(Gravity.CENTER);
        durationText = label(13, 0xFFC0D5ED);
        durationText.setGravity(Gravity.CENTER);
        View timeDivider = new View(activity);
        timeDivider.setBackgroundColor(0x7767B9EC);
        times.addView(elapsed, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(22)));
        LinearLayout.LayoutParams dividerLp = new LinearLayout.LayoutParams(dp(50), dp(1));
        dividerLp.topMargin = dp(2);
        dividerLp.bottomMargin = dp(2);
        times.addView(timeDivider, dividerLp);
        times.addView(durationText, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(20)));
        LinearLayout.LayoutParams timeLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(63));
        centre.addView(times, timeLp);

        View spacer = new View(activity);
        centre.addView(spacer, new LinearLayout.LayoutParams(1, 0, 1f));

        likeButton = actionButton("👍", "Short gefällt mir", onLike);
        dislikeButton = actionButton("👎", "Short gefällt mir nicht", onDislike);
        saveButton = actionButton("★", "Short in YouTube-Playlist speichern", onSave);
        addAction(centre, likeButton);
        addAction(centre, dislikeButton);
        addAction(centre, saveButton);
        // Compact on-screen 5-second seek buttons. Stay entirely inside menu.
        LinearLayout seekActions = new LinearLayout(activity);
        seekActions.setOrientation(LinearLayout.HORIZONTAL);
        seekActions.setGravity(Gravity.CENTER);
        seekBackButton = actionButton("⏪", "Fünf Sekunden zurückspulen", onSeekBack);
        seekForwardButton = actionButton("⏩", "Fünf Sekunden vorspulen", onSeekForward);
        seekBackButton.setTextSize(21);
        seekForwardButton.setTextSize(21);
        LinearLayout.LayoutParams seekBackLp = new LinearLayout.LayoutParams(dp(50), dp(42));
        seekBackLp.rightMargin = dp(5);
        seekActions.addView(seekBackButton, seekBackLp);
        seekActions.addView(seekForwardButton, new LinearLayout.LayoutParams(dp(50), dp(42)));
        centre.addView(seekActions, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(43)));
        View lowerSpace = new View(activity);
        centre.addView(lowerSpace, new LinearLayout.LayoutParams(1, 0, 1f));

        // The bar is *below* the right-hand video, exactly at the screen edge.
        progress = new View(activity) {
            private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            @Override protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                final float width = getWidth(), height = getHeight();
                paint.setShader(null);
                paint.setColor(progressFocused ? 0xFF9EC8E8 : 0xFF24516E);
                canvas.drawRect(0f, 0f, width, height, paint);
                float fraction = currentDuration <= 0 || currentDuration == C.TIME_UNSET
                        ? 0f : Math.max(0f, Math.min(1f,
                        (float) currentPosition / currentDuration));
                paint.setShader(new LinearGradient(0f, 0f, Math.max(1f, width), 0f,
                        BLUE, 0xFF354FEA, Shader.TileMode.CLAMP));
                canvas.drawRect(0f, 0f, width * fraction, height, paint);
                paint.setShader(null);
            }
        };
        root.addView(progress, new FrameLayout.LayoutParams(
                videoWidth, dp(7), Gravity.RIGHT | Gravity.BOTTOM));
        updateAutoText();
    }

    private TextView actionButton(String glyph, String description, Runnable action) {
        TextView button = label(27, 0xFFFFD456);
        button.setText(glyph);
        button.setGravity(Gravity.CENTER);
        button.setBackground(chrome(false));
        button.setContentDescription(description);
        button.setClickable(true);
        button.setOnClickListener(v -> action.run());
        return button;
    }

    private void addAction(LinearLayout parent, TextView button) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                dp(67), dp(45));
        lp.bottomMargin = dp(9);
        parent.addView(button, lp);
    }

    public boolean isAutoNext() {
        return activity.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .getBoolean(AUTO_NEXT, true);
    }

    public void toggleAutoNext() {
        activity.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
                .putBoolean(AUTO_NEXT, !isAutoNext()).apply();
        updateAutoText();
    }

    private void updateAutoText() {
        boolean enabled = isAutoNext();
        autoButton.setText(enabled ? "↻  EIN" : "↻  AUS");
        autoButton.setContentDescription(enabled
                ? "Automatisch zum nächsten Short: Ein" : "Automatisch zum nächsten Short: Aus");
        autoButton.setBackground(chrome(autoFocused));
    }

    public void show(boolean visible, ExoPlayer exo, long pendingSeek) {
        root.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (!visible) {
            lastPreviewFingerprint = "";
            return;
        }
        clock.setText(clockFormat.format(new Date()));
        currentPosition = pendingSeek >= 0 ? pendingSeek : exo == null ? 0 : exo.getCurrentPosition();
        currentDuration = exo == null ? 0 : exo.getDuration();
        elapsed.setText(formatTime(currentPosition));
        durationText.setText(currentDuration <= 0 || currentDuration == C.TIME_UNSET
                ? "--:--" : formatTime(currentDuration));
        progress.invalidate();
        updatePreviews();
    }

    public void setPreviewsFocused(boolean focused) {
        if (previewsFocused == focused) return;
        previewsFocused = focused;
        if (focused) {
            int minimum = MarsShortsSession.currentIndex() + 1;
            if (selectedIndex < minimum || selectedIndex >= MarsShortsSession.size()) {
                selectedIndex = minimum;
            }
        }
        lastPreviewFingerprint = "";
        updatePreviews();
    }

    public boolean movePreview(int direction) {
        int minimum = MarsShortsSession.currentIndex() + 1;
        if (minimum >= MarsShortsSession.size()) return false;
        if (selectedIndex < minimum) selectedIndex = minimum;
        int next = Math.max(minimum, Math.min(MarsShortsSession.size() - 1,
                selectedIndex + (direction < 0 ? -1 : 1)));
        if (next == selectedIndex) return false;
        selectedIndex = next;
        lastPreviewFingerprint = "";
        updatePreviews();
        return true;
    }

    @Nullable
    public Video choosePreview() {
        if (!previewsFocused) return null;
        return MarsShortsSession.select(selectedIndex);
    }

    public void setProgressFocused(boolean focused) {
        progressFocused = focused;
        progress.invalidate();
    }

    public void setReactionsFocused(boolean focused, int selection) {
        reactionsFocused = focused;
        reactionIndex = selection;
        likeButton.setBackground(chrome(focused && selection == 0));
        dislikeButton.setBackground(chrome(focused && selection == 1));
        saveButton.setBackground(chrome(focused && selection == 2));
        seekBackButton.setBackground(chrome(focused && selection == 3));
        seekForwardButton.setBackground(chrome(focused && selection == 4));
    }

    public void setAutoFocused(boolean focused) {
        if (autoFocused == focused) return;
        autoFocused = focused;
        updateAutoText();
    }

    private void updatePreviews() {
        int position = MarsShortsSession.currentIndex();
        int minimum = position + 1;
        if (selectedIndex < minimum || selectedIndex >= MarsShortsSession.size()) {
            selectedIndex = minimum;
        }
        int count = previews.size();
        int start = previewsFocused ? Math.max(minimum, selectedIndex - (count - 1)) : minimum;
        if (start + count > MarsShortsSession.size()) {
            start = Math.max(minimum, MarsShortsSession.size() - count);
        }
        StringBuilder fingerprint = new StringBuilder();
        for (int i = 0; i < previews.size(); i++) {
            Video v = MarsShortsSession.at(start + i);
            fingerprint.append(v == null ? "" : v.videoId).append('|');
        }
        if (lastPreviewFingerprint.contentEquals(fingerprint)
                && lastQueuePosition == position && lastRenderedStart == start
                && lastRenderedFocus == previewsFocused
                && lastRenderedSelection == selectedIndex) return;
        lastPreviewFingerprint = fingerprint.toString();
        lastQueuePosition = position;
        lastRenderedStart = start;
        lastRenderedFocus = previewsFocused;
        lastRenderedSelection = selectedIndex;
        for (int i = 0; i < previews.size(); i++) {
            Preview preview = previews.get(i);
            int index = start + i;
            Video item = MarsShortsSession.at(index);
            if (item == null) {
                preview.row.setVisibility(View.INVISIBLE);
                Glide.with(activity).clear(preview.image);
                continue;
            }
            preview.row.setVisibility(View.VISIBLE);
            boolean selected = previewsFocused && index == selectedIndex;
            preview.row.setBackground(chrome(selected));
            preview.number.setText(String.format(Locale.GERMANY,
                    "%02d  ·  %s", index + 1, selected ? "AUSWÄHLEN" : "VORSCHAU"));
            preview.title.setText(item.getTitle());
            preview.author.setText(TextUtils.isEmpty(item.getAuthor())
                    ? "YouTube Short" : item.getAuthor());
            preview.row.setContentDescription("Short " + (index + 1) + ": "
                    + item.getTitle() + ", " + preview.author.getText());
            Glide.with(activity).load(item.getCardImageUrl())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .format(DecodeFormat.PREFER_RGB_565)
                    .fitCenter().dontAnimate().into(preview.image);
        }
    }

    private GradientDrawable chrome(boolean selected) {
        GradientDrawable shape = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{selected ? 0xE6165483 : 0xBB193D67, 0xAA0A213B});
        shape.setCornerRadius(dp(10));
        shape.setStroke(dp(1), selected ? BLUE : 0x665B90BD);
        return shape;
    }

    private TextView label(int size, int color) {
        TextView text = new TextView(activity);
        text.setTextSize(size);
        text.setTextColor(color);
        text.setIncludeFontPadding(false);
        return text;
    }

    private int dp(float value) {
        return (int) (value * activity.getResources().getDisplayMetrics().density + 0.5f);
    }

    private static String formatTime(long ms) {
        long sec = Math.max(0L, ms) / 1000;
        return sec >= 3600 ? String.format(Locale.ROOT, "%d:%02d:%02d",
                sec / 3600, (sec / 60) % 60, sec % 60)
                : String.format(Locale.ROOT, "%02d:%02d", sec / 60, sec % 60);
    }

    private static final class Preview {
        LinearLayout row;
        ImageView image;
        TextView title;
        TextView number;
        TextView author;
    }
}
