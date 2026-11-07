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
import android.widget.ScrollView;
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
    private final TextView elapsed;
    private final TextView durationText;
    private final View progress;
    private final List<ImageView> rightButtons = new ArrayList<>();
    private final List<LinearLayout> rightRows = new ArrayList<>();
    private final List<TextView> rightLabels = new ArrayList<>();
    private final ScrollView rightScroll;
    private final TextView autoButton;
    private final TextView likeCountLabel;
    private final TextView dislikeCountLabel;
    private final TextView commentsCountLabel;
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
                             Runnable onSeekBack, Runnable onSeekForward,
                             Runnable onComments, Runnable onChannel, Runnable onCaptions,
                             Runnable onSpeed, Runnable onSearch,
                             Runnable onNoInterest) {
        this.activity = activity;
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
                Math.max(dp(160), videoLeft - dp(32)),
                screenHeight - dp(76), Gravity.LEFT | Gravity.CENTER_VERTICAL);
        leftLp.leftMargin = dp(16);
        root.addView(left, leftLp);

        previewColumn = new LinearLayout(activity);
        previewColumn.setOrientation(LinearLayout.VERTICAL);
        left.addView(previewColumn, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        int previewImageWidth = Math.min(Math.round((videoLeft - dp(32)) * 0.41f),
                Math.max(dp(78), Math.round((screenHeight - dp(115)) * 9f / 32f)));
        for (int i = 0; i < 2; i++) {
            Preview preview = new Preview();
            LinearLayout row = new LinearLayout(activity);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(6), dp(6), dp(9), dp(6));
            row.setBackground(chrome(false));
            row.setClipToOutline(true);
            ImageView image = new ImageView(activity);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            GradientDrawable portraitCorners = new GradientDrawable();
            portraitCorners.setColor(0xFF09243B);
            portraitCorners.setCornerRadius(dp(5));
            image.setBackground(portraitCorners);
            image.setClipToOutline(true);
            row.addView(image, new LinearLayout.LayoutParams(
                    previewImageWidth, ViewGroup.LayoutParams.MATCH_PARENT));
            LinearLayout texts = new LinearLayout(activity);
            texts.setOrientation(LinearLayout.VERTICAL);
            texts.setPadding(dp(7), 0, 0, 0);
            TextView title = label(13, WHITE);
            title.setMaxLines(4);
            title.setEllipsize(TextUtils.TruncateAt.END);
            TextView author = label(12, BLUE);
            author.setMaxLines(2);
            author.setEllipsize(TextUtils.TruncateAt.END);
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
            previews.add(preview);
        }

        // Compact round actions; free space to their right is reserved for comments.
        rightScroll = new ScrollView(activity);
        rightScroll.setVerticalScrollBarEnabled(false);
        rightScroll.setFocusable(false);
        rightScroll.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        final int controlWidth = dp(70);
        FrameLayout.LayoutParams controlLp = new FrameLayout.LayoutParams(
                controlWidth, screenHeight - dp(30), Gravity.TOP | Gravity.RIGHT);
        controlLp.topMargin = dp(15);
        controlLp.rightMargin = Math.max(dp(12),
                screenWidth - (videoLeft + videoWidth + dp(10) + controlWidth));
        root.addView(rightScroll, controlLp);
        LinearLayout controls = new LinearLayout(activity);
        controls.setOrientation(LinearLayout.VERTICAL);
        controls.setGravity(Gravity.CENTER_HORIZONTAL);
        controls.setPadding(dp(2), dp(4), dp(2), dp(4));
        rightScroll.addView(controls, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        clock = label(14, WHITE);
        clock.setGravity(Gravity.CENTER);
        controls.addView(clock, new LinearLayout.LayoutParams(-1, dp(27)));

        addRoundAction(controls, com.liskovsoft.smartyoutubetv2.tv.R.drawable.ic_player_repeat,
                "Nächstes Short oder Wiederholung", "Weiter",
                () -> { toggleAutoNext(); onAutoChange.run(); });
        addRoundAction(controls, com.liskovsoft.smartyoutubetv2.tv.R.drawable.ic_watch_thumb_up_outline,
                "Gefällt mir", "–", onLike);
        addRoundAction(controls, com.liskovsoft.smartyoutubetv2.tv.R.drawable.ic_watch_thumb_down_outline,
                "Gefällt mir nicht", "–", onDislike);
        addRoundAction(controls, com.liskovsoft.smartyoutubetv2.tv.R.drawable.ic_watch_comment,
                "Kommentare", "Kommentare", onComments);
        addRoundAction(controls, com.liskovsoft.smartyoutubetv2.tv.R.drawable.mars_nav_profile_025,
                "Kanal öffnen", "Kanal", onChannel);
        addRoundAction(controls, com.liskovsoft.smartyoutubetv2.tv.R.drawable.ic_player_playlist_add,
                "Zur Playlist hinzufügen", "Merken", onSave);
        addRoundAction(controls, com.liskovsoft.smartyoutubetv2.tv.R.drawable.mars_tv_ic_cc,
                "Untertitel", "CC", onCaptions);
        addRoundAction(controls, com.liskovsoft.smartyoutubetv2.tv.R.drawable.ic_player_speed,
                "Wiedergabegeschwindigkeit", "Tempo", onSpeed);
        addRoundAction(controls, com.liskovsoft.smartyoutubetv2.tv.R.drawable.mars_nav_search_025,
                "Suche öffnen", "Suche", onSearch);
        addRoundAction(controls, android.R.drawable.ic_menu_close_clear_cancel,
                "Kein Interesse", "Ablehnen", onNoInterest);
        likeCountLabel = rightLabels.get(1);
        dislikeCountLabel = rightLabels.get(2);
        commentsCountLabel = rightLabels.get(3);
        autoButton = label(12, WHITE); // Persisted user mode; not another row.
        elapsed = label(12, WHITE);
        elapsed.setGravity(Gravity.CENTER);
        durationText = label(10, 0xFFC0D5ED);
        durationText.setGravity(Gravity.CENTER);
        LinearLayout timeBox = new LinearLayout(activity);
        timeBox.setGravity(Gravity.CENTER);
        timeBox.setOrientation(LinearLayout.VERTICAL);
        timeBox.addView(elapsed, new LinearLayout.LayoutParams(-1, dp(18)));
        timeBox.addView(durationText, new LinearLayout.LayoutParams(-1, dp(15)));
        controls.addView(timeBox, new LinearLayout.LayoutParams(-1, dp(35)));

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
                videoWidth - dp(24), dp(4), Gravity.TOP | Gravity.LEFT);
        progressLp.leftMargin = videoLeft + dp(12);
        progressLp.topMargin = videoTop + videoHeight + dp(3);
        root.addView(progress, progressLp);
        updateAutoText();
    }

    private void addRoundAction(LinearLayout parent, int icon, String description,
                                String caption, Runnable onClick) {
        LinearLayout row = new LinearLayout(activity);
        row.setGravity(Gravity.CENTER);
        row.setOrientation(LinearLayout.VERTICAL);
        ImageView button = new ImageView(activity);
        button.setImageResource(icon);
        button.setScaleType(ImageView.ScaleType.FIT_CENTER);
        button.setColorFilter(WHITE);
        button.setPadding(dp(7), dp(7), dp(7), dp(7));
        button.setBackground(circle(false));
        button.setContentDescription(description);
        button.setOnClickListener(v -> onClick.run());
        row.addView(button, new LinearLayout.LayoutParams(dp(35), dp(35)));
        TextView label = label(10, WHITE);
        label.setGravity(Gravity.CENTER);
        label.setSingleLine(true);
        label.setEllipsize(TextUtils.TruncateAt.END);
        label.setText(caption);
        row.addView(label, new LinearLayout.LayoutParams(dp(66), dp(16)));
        parent.addView(row, new LinearLayout.LayoutParams(-1, dp(52)));
        rightButtons.add(button);
        rightRows.add(row);
        rightLabels.add(label);
    }

    private GradientDrawable circle(boolean focused) {
        GradientDrawable result = new GradientDrawable();
        result.setShape(GradientDrawable.OVAL);
        result.setColor(focused ? 0xEE176FA9 : 0xDD123858);
        result.setStroke(dp(focused ? 2 : 1), focused ? 0xFF7CD8FF : 0x886D9CC2);
        return result;
    }

    public void updateCounts(@Nullable Video video, @Nullable String comments) {
        likeCountLabel.setText(video == null || TextUtils.isEmpty(video.likeCount)
                ? "–" : video.likeCount);
        // YouTube does not publish official dislike totals; unknown stays unknown.
        dislikeCountLabel.setText(video == null || TextUtils.isEmpty(video.dislikeCount)
                ? "–" : video.dislikeCount);
        commentsCountLabel.setText(TextUtils.isEmpty(comments) ? "Kommentare" : comments);
    }

    public int reactionCount() { return rightButtons.size(); }

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
        if (!rightLabels.isEmpty()) rightLabels.get(0).setText(enabled ? "Weiter" : "Wdh.");
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
        for (int i = 0; i < rightButtons.size(); i++) {
            boolean selected = focused && selection == i;
            rightButtons.get(i).setBackground(circle(selected));
            rightButtons.get(i).setColorFilter(selected ? BLUE : WHITE);
        }
        if (focused && selection >= 0 && selection < rightRows.size()) {
            View chosen = rightRows.get(selection);
            int y = chosen.getTop() - rightScroll.getHeight() / 2
                    + chosen.getHeight() / 2;
            rightScroll.scrollTo(0, Math.max(0, y));
        }
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
        TextView author;
    }
}
