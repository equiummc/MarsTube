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
 * This overlay supplies four upcoming previews on the left, a large wall clock, a
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
    private boolean previewsFocused;
    private boolean progressFocused;
    private boolean reactionsFocused;
    private int reactionIndex;
    private int selectedIndex = -1;
    private int lastRenderedStart = -1;
    private boolean lastRenderedFocus;
    private final LinearLayout previewColumn;
    private final List<Preview> previews = new ArrayList<>();
    private final SimpleDateFormat clockFormat = new SimpleDateFormat("HH:mm", Locale.GERMANY);
    private String lastPreviewFingerprint = "";
    private int lastQueuePosition = -1;
    private long currentPosition;
    private long currentDuration;

    public MarsTvShortsStage(Activity activity, FrameLayout host, Runnable onAutoChange,
                             Runnable onLike, Runnable onDislike) {
        this.activity = activity;
        root = new FrameLayout(activity);
        root.setVisibility(View.GONE);
        root.setFocusable(false);
        host.addView(root, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout left = new LinearLayout(activity);
        left.setOrientation(LinearLayout.VERTICAL);
        left.setPadding(dp(29), dp(15), dp(17), dp(15));
        left.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFF081A33, 0xF6122E50, 0xE414355B}));
        FrameLayout.LayoutParams leftLp = new FrameLayout.LayoutParams(
                Math.round(activity.getResources().getDisplayMetrics().widthPixels * 0.57f),
                ViewGroup.LayoutParams.MATCH_PARENT, Gravity.LEFT | Gravity.TOP);
        root.addView(left, leftLp);

        LinearLayout header = new LinearLayout(activity);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);
        clock = label(27, WHITE);
        clock.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(clock, new LinearLayout.LayoutParams(0, dp(43), 1f));
        TextView heading = label(16, WHITE);
        heading.setText("SHORTS");
        heading.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        header.addView(heading, new LinearLayout.LayoutParams(dp(115), dp(43)));
        left.addView(header);

        autoButton = label(14, WHITE);
        autoButton.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
        autoButton.setPadding(dp(15), 0, dp(15), 0);
        autoButton.setContentDescription("Shorts automatisch weiter, Ein oder Aus");
        autoButton.setClickable(true);
        autoButton.setOnClickListener(v -> {
            toggleAutoNext();
            onAutoChange.run();
        });
        LinearLayout.LayoutParams autoLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(37));
        autoLp.bottomMargin = dp(11);
        left.addView(autoButton, autoLp);

        previewColumn = new LinearLayout(activity);
        previewColumn.setOrientation(LinearLayout.VERTICAL);
        previewColumn.setGravity(Gravity.TOP);
        left.addView(previewColumn, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        for (int i = 0; i < 3; i++) {
            final Preview preview = new Preview();
            LinearLayout row = new LinearLayout(activity);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(dp(7), dp(4), dp(9), dp(4));
            row.setBackground(chrome(false));
            ImageView image = new ImageView(activity);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setClipToOutline(true);
            image.setBackground(chrome(false));
            row.addView(image, new LinearLayout.LayoutParams(dp(91), ViewGroup.LayoutParams.MATCH_PARENT));
            LinearLayout texts = new LinearLayout(activity);
            texts.setOrientation(LinearLayout.VERTICAL);
            texts.setPadding(dp(14), 0, 0, 0);
            TextView number = label(12, BLUE);
            TextView title = label(15, WHITE);
            title.setMaxLines(3);
            title.setEllipsize(TextUtils.TruncateAt.END);
            TextView author = label(13, 0xFFC0D5ED);
            author.setMaxLines(1);
            author.setEllipsize(TextUtils.TruncateAt.END);
            texts.addView(number);
            texts.addView(title);
            texts.addView(author);
            row.addView(texts, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
            rowLp.bottomMargin = dp(7);
            previewColumn.addView(row, rowLp);
            preview.row = row;
            preview.image = image;
            preview.title = title;
            preview.author = author;
            preview.number = number;
            previews.add(preview);
        }

        TextView hint = label(12, 0xFFC0D5ED);
        hint.setText("← Vorschau   ↑ ↓ Blättern   OK Auswählen   → Video");
        hint.setGravity(Gravity.CENTER_VERTICAL);
        left.addView(hint, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(25)));

        // Ratings operate on the real YouTube actions, never a local fake rating.
        LinearLayout reactions = new LinearLayout(activity);
        reactions.setOrientation(LinearLayout.HORIZONTAL);
        reactions.setGravity(Gravity.CENTER_VERTICAL);
        likeButton = label(14, WHITE);
        likeButton.setText("👍  Gefällt mir");
        likeButton.setGravity(Gravity.CENTER);
        likeButton.setBackground(chrome(false));
        likeButton.setClickable(true);
        likeButton.setContentDescription("Short gefällt mir");
        likeButton.setOnClickListener(v -> onLike.run());
        reactions.addView(likeButton, new LinearLayout.LayoutParams(0, dp(39), 1f));
        dislikeButton = label(14, WHITE);
        dislikeButton.setText("👎  Gefällt mir nicht");
        dislikeButton.setGravity(Gravity.CENTER);
        dislikeButton.setBackground(chrome(false));
        dislikeButton.setClickable(true);
        dislikeButton.setContentDescription("Short gefällt mir nicht");
        dislikeButton.setOnClickListener(v -> onDislike.run());
        LinearLayout.LayoutParams dislikeLp = new LinearLayout.LayoutParams(0, dp(39), 1f);
        dislikeLp.leftMargin = dp(7);
        reactions.addView(dislikeButton, dislikeLp);
        LinearLayout.LayoutParams ratingLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(39));
        ratingLp.bottomMargin = dp(10);
        left.addView(reactions, ratingLp);

        // Separate time capsule at the right edge; no text overlaps the scrub bar.
        LinearLayout times = new LinearLayout(activity);
        times.setOrientation(LinearLayout.VERTICAL);
        times.setPadding(dp(12), dp(9), dp(12), dp(9));
        times.setBackground(chrome(false));
        elapsed = label(16, WHITE);
        elapsed.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        durationText = label(12, 0xFFC0D5ED);
        durationText.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        times.addView(elapsed);
        times.addView(durationText);
        FrameLayout.LayoutParams timeLp = new FrameLayout.LayoutParams(
                dp(116), ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.RIGHT | Gravity.TOP);
        timeLp.rightMargin = dp(15);
        timeLp.topMargin = dp(70);
        root.addView(times, timeLp);

        // The seek rail sits flush against the bottom of the right video area.
        // Remote focus is managed by MarsTvShell; this View is only its visual.
        progress = new View(activity) {
            private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            @Override protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                final float width = getWidth();
                final float y = getHeight() / 2f;
                paint.setShader(null);
                paint.setColor(progressFocused ? 0xFFB4DBFF : 0xA4617591);
                paint.setStrokeWidth(dp(progressFocused ? 5 : 3));
                canvas.drawLine(0f, y, width, y, paint);
                float fraction = currentDuration <= 0 || currentDuration == C.TIME_UNSET
                        ? 0f : Math.max(0f, Math.min(1f,
                        (float) currentPosition / currentDuration));
                paint.setStrokeWidth(dp(progressFocused ? 7 : 5));
                paint.setShader(new LinearGradient(0f, y, Math.max(1f, width), y,
                        BLUE, 0xFF354FEA, Shader.TileMode.CLAMP));
                canvas.drawLine(0f, y, width * fraction, y, paint);
                paint.setShader(null);
            }
        };
        FrameLayout.LayoutParams progressLp = new FrameLayout.LayoutParams(
                Math.round(activity.getResources().getDisplayMetrics().widthPixels * 0.43f),
                dp(10), Gravity.RIGHT | Gravity.BOTTOM);
        progressLp.bottomMargin = 0;
        progressLp.rightMargin = 0;
        root.addView(progress, progressLp);
        updateAutoText();
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
        autoButton.setText(enabled ? "Automatisch weiter  ● EIN" : "Automatisch weiter  ○ AUS");
        autoButton.setBackground(chrome(enabled));
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
    }

    private void updatePreviews() {
        int position = MarsShortsSession.currentIndex();
        int minimum = position + 1;
        if (selectedIndex < minimum || selectedIndex >= MarsShortsSession.size()) {
            selectedIndex = minimum;
        }
        int start = previewsFocused ? Math.max(minimum, selectedIndex - 1) : minimum;
        if (start + 3 > MarsShortsSession.size()) {
            start = Math.max(minimum, MarsShortsSession.size() - 3);
        }
        StringBuilder fingerprint = new StringBuilder();
        for (int i = 0; i < previews.size(); i++) {
            Video v = MarsShortsSession.at(start + i);
            fingerprint.append(v == null ? "" : v.videoId).append('|');
        }
        if (lastPreviewFingerprint.contentEquals(fingerprint)
                && lastQueuePosition == position && lastRenderedStart == start
                && lastRenderedFocus == previewsFocused) return;
        lastPreviewFingerprint = fingerprint.toString();
        lastQueuePosition = position;
        lastRenderedStart = start;
        lastRenderedFocus = previewsFocused;
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
                    .centerCrop().dontAnimate().into(preview.image);
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
