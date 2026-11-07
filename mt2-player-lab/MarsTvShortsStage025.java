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
        int margin = Math.round(22f * density);
        int barSpace = Math.round(10f * density);
        return Math.round(Math.max(1, height - margin * 2 - barSpace) * 9f / 16f);
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
        final int screenHeight = activity.getResources().getDisplayMetrics().heightPixels;
        final int videoWidth = Math.min(screenWidth, videoWidth(activity));
        final int videoLeft = (screenWidth - videoWidth) / 2;
        final int videoTop = dp(22);
        final int videoHeight = Math.max(dp(80), screenHeight - dp(54));

        // The existing single Media3 PlayerView is centered in this transparent
        // framed window by the companion MobilePlaybackActivity patch.
        View videoFrame = new View(activity);
        GradientDrawable frameBorder = new GradientDrawable();
        frameBorder.setColor(0x00000000);
        frameBorder.setCornerRadius(dp(18));
        frameBorder.setStroke(dp(1), 0xA673ABD4);
        videoFrame.setBackground(frameBorder);
        videoFrame.setFocusable(false);
        FrameLayout.LayoutParams videoLp = new FrameLayout.LayoutParams(
                videoWidth, videoHeight, Gravity.TOP | Gravity.LEFT);
        videoLp.leftMargin = videoLeft;
        videoLp.topMargin = videoTop;
        root.addView(videoFrame, videoLp);

        // Exactly two real 9:16 thumbnails; no secondary playback decoder.
        LinearLayout left = new LinearLayout(activity);
        left.setOrientation(LinearLayout.VERTICAL);
        left.setGravity(Gravity.CENTER_VERTICAL);
        left.setPadding(dp(8), dp(12), dp(8), dp(12));
        GradientDrawable previewGlass = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{0xD9153757, 0xDB0A1C35});
        previewGlass.setCornerRadius(dp(18));
        previewGlass.setStroke(dp(1), 0x80669BBF);
        left.setBackground(previewGlass);
        FrameLayout.LayoutParams leftLp = new FrameLayout.LayoutParams(
                Math.max(dp(160), videoLeft - dp(104)),
                screenHeight - dp(76), Gravity.LEFT | Gravity.CENTER_VERTICAL);
        leftLp.leftMargin = dp(86);
        root.addView(left, leftLp);

        previewColumn = new LinearLayout(activity);
        previewColumn.setOrientation(LinearLayout.VERTICAL);
        left.addView(previewColumn, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        int previewImageWidth = Math.min(Math.round((videoLeft - dp(104)) * 0.40f),
                Math.max(dp(78), Math.round((screenHeight - dp(90)) * 9f / 32f)));
        for (int i = 0; i < 2; i++) {
            Preview preview = new Preview();
            LinearLayout row = new LinearLayout(activity);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(6), dp(6), dp(9), dp(6));
            row.setBackground(chrome(false));
            row.setClipToOutline(true);
            ImageView image = new ImageView(activity);
            image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            image.setClipToOutline(true);
            image.setBackground(chrome(false));
            row.addView(image, new LinearLayout.LayoutParams(
                    previewImageWidth, ViewGroup.LayoutParams.MATCH_PARENT));
            LinearLayout texts = new LinearLayout(activity);
            texts.setOrientation(LinearLayout.VERTICAL);
            texts.setPadding(dp(10), 0, 0, 0);
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
            rowLp.bottomMargin = dp(10);
            previewColumn.addView(row, rowLp);
            preview.row = row;
            preview.image = image;
            preview.title = title;
            preview.author = author;
            preview.number = number;
            previews.add(preview);
        }

        // A separate, compact blue floating actions ribbon to the right of
        // the central Short. No comments, share, autoplay or next button.
        LinearLayout controls = new LinearLayout(activity);
        controls.setOrientation(LinearLayout.VERTICAL);
        controls.setGravity(Gravity.CENTER_HORIZONTAL);
        controls.setPadding(dp(8), dp(14), dp(8), dp(14));
        GradientDrawable controlsGlass = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{0xE414395A, 0xE0081C35});
        controlsGlass.setCornerRadius(dp(18));
        controlsGlass.setStroke(dp(1), 0x8D6F9FC6);
        controls.setBackground(controlsGlass);
        final int controlWidth = dp(100);
        FrameLayout.LayoutParams controlLp = new FrameLayout.LayoutParams(
                controlWidth, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        controlLp.rightMargin = Math.max(dp(10),
                screenWidth - (videoLeft + videoWidth + dp(24) + controlWidth));
        root.addView(controls, controlLp);

        clock = label(17, WHITE);
        clock.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams clockLp = new LinearLayout.LayoutParams(-1, dp(38));
        clockLp.bottomMargin = dp(16);
        controls.addView(clock, clockLp);

        likeButton = actionButton("👍", "Gefällt mir", onLike);
        dislikeButton = actionButton("👎", "Gefällt mir nicht", onDislike);
        addAction(controls, likeButton);
        addAction(controls, dislikeButton);

        // Preserve stored autoplay behaviour but remove its on-screen button.
        autoButton = label(12, WHITE);
        autoButton.setOnClickListener(v -> {
            toggleAutoNext();
            onAutoChange.run();
        });
        saveButton = actionButton("★", "Playlist", onSave);
        seekBackButton = actionButton("⏪", "Fünf Sekunden zurück", onSeekBack);
        seekForwardButton = actionButton("⏩", "Fünf Sekunden vor", onSeekForward);

        LinearLayout times = new LinearLayout(activity);
        times.setGravity(Gravity.CENTER_HORIZONTAL);
        times.setOrientation(LinearLayout.VERTICAL);
        times.setPadding(dp(3), dp(8), dp(3), dp(5));
        times.setBackground(chrome(false));
        elapsed = label(14, WHITE);
        elapsed.setGravity(Gravity.CENTER);
        durationText = label(12, 0xFFC0D5ED);
        durationText.setGravity(Gravity.CENTER);
        times.addView(elapsed, new LinearLayout.LayoutParams(-1, dp(22)));
        times.addView(durationText, new LinearLayout.LayoutParams(-1, dp(21)));
        LinearLayout.LayoutParams timeLp = new LinearLayout.LayoutParams(-1, dp(57));
        timeLp.topMargin = dp(8);
        controls.addView(times, timeLp);

        // A slim progress line sits directly below the inset video frame.
        progress = new View(activity) {
            private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            @Override protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                final float width = getWidth(), height = getHeight();
                paint.setShader(null);
                paint.setColor(progressFocused ? 0xFF9EC8E8 : 0xFF24516E);
                canvas.drawRoundRect(0f, 0f, width, height, height / 2, height / 2, paint);
                float fraction = currentDuration <= 0 || currentDuration == C.TIME_UNSET
                        ? 0f : Math.max(0f, Math.min(1f,
                        (float) currentPosition / currentDuration));
                paint.setShader(new LinearGradient(0f, 0f, Math.max(1f, width), 0f,
                        BLUE, 0xFF354FEA, Shader.TileMode.CLAMP));
                canvas.drawRoundRect(0f, 0f, width * fraction, height,
                        height / 2, height / 2, paint);
                paint.setShader(null);
            }
        };
        FrameLayout.LayoutParams progressLp = new FrameLayout.LayoutParams(
                videoWidth, dp(4), Gravity.TOP | Gravity.LEFT);
        progressLp.leftMargin = videoLeft;
        progressLp.topMargin = videoTop + videoHeight + dp(3);
        root.addView(progress, progressLp);
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
