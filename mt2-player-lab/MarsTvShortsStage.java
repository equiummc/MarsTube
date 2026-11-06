package com.newtube.mobile.ui.playback;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
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
    private final LinearLayout previewColumn;
    private final List<Preview> previews = new ArrayList<>();
    private final SimpleDateFormat clockFormat = new SimpleDateFormat("HH:mm", Locale.GERMANY);
    private String lastPreviewFingerprint = "";
    private int lastQueuePosition = -1;
    private long currentPosition;
    private long currentDuration;

    public MarsTvShortsStage(Activity activity, FrameLayout host, Runnable onAutoChange) {
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
        for (int i = 0; i < 4; i++) {
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
            row.addView(image, new LinearLayout.LayoutParams(dp(63), ViewGroup.LayoutParams.MATCH_PARENT));
            LinearLayout texts = new LinearLayout(activity);
            texts.setOrientation(LinearLayout.VERTICAL);
            texts.setPadding(dp(14), 0, 0, 0);
            TextView number = label(12, BLUE);
            TextView title = label(15, WHITE);
            title.setMaxLines(2);
            title.setEllipsize(TextUtils.TruncateAt.END);
            texts.addView(number);
            texts.addView(title);
            row.addView(texts, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
            rowLp.bottomMargin = dp(7);
            previewColumn.addView(row, rowLp);
            preview.row = row;
            preview.image = image;
            preview.title = title;
            preview.number = number;
            previews.add(preview);
        }

        TextView hint = label(12, 0xFFC0D5ED);
        hint.setText("↑ ↓  Short wechseln     ← →  Spulen     OK  Pause");
        hint.setGravity(Gravity.CENTER_VERTICAL);
        left.addView(hint, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(25)));

        LinearLayout timeline = new LinearLayout(activity);
        timeline.setOrientation(LinearLayout.VERTICAL);
        LinearLayout times = new LinearLayout(activity);
        times.setGravity(Gravity.CENTER_VERTICAL);
        elapsed = label(14, WHITE);
        durationText = label(14, WHITE);
        durationText.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        times.addView(elapsed, new LinearLayout.LayoutParams(0, dp(24), 1f));
        times.addView(durationText, new LinearLayout.LayoutParams(0, dp(24), 1f));
        timeline.addView(times);
        progress = new View(activity) {
            private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            @Override protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                final float width = getWidth();
                final float y = getHeight() / 2f;
                paint.setShader(null);
                paint.setColor(0xA4617591);
                paint.setStrokeWidth(dp(2));
                canvas.drawLine(0f, y, width, y, paint);
                float fraction = currentDuration <= 0 || currentDuration == C.TIME_UNSET
                        ? 0f : Math.max(0f, Math.min(1f,
                        (float) currentPosition / currentDuration));
                paint.setStrokeWidth(dp(3));
                paint.setShader(new LinearGradient(0f, y, Math.max(1f, width), y,
                        BLUE, 0xFF354FEA, Shader.TileMode.CLAMP));
                canvas.drawLine(0f, y, width * fraction, y, paint);
                paint.setShader(null);
            }
        };
        timeline.addView(progress, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(9)));
        FrameLayout.LayoutParams timeLp = new FrameLayout.LayoutParams(
                Math.round(activity.getResources().getDisplayMetrics().widthPixels * 0.32f),
                dp(37), Gravity.RIGHT | Gravity.BOTTOM);
        timeLp.rightMargin = dp(22);
        timeLp.bottomMargin = dp(8);
        root.addView(timeline, timeLp);
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

    private void updatePreviews() {
        List<Video> list = MarsShortsSession.upcoming(4);
        int position = MarsShortsSession.currentIndex();
        StringBuilder fingerprint = new StringBuilder();
        for (Video item : list) fingerprint.append(item.videoId).append('|');
        if (lastPreviewFingerprint.contentEquals(fingerprint) && lastQueuePosition == position) return;
        lastPreviewFingerprint = fingerprint.toString();
        // On every second advance, visually shift the forthcoming pair into view.
        if (lastQueuePosition >= 0 && position > lastQueuePosition && position % 2 == 0) {
            previewColumn.animate().cancel();
            previewColumn.setTranslationY(dp(10));
            previewColumn.animate().translationY(0f).setDuration(170L).start();
        }
        lastQueuePosition = position;
        for (int i = 0; i < previews.size(); i++) {
            Preview preview = previews.get(i);
            if (i >= list.size()) {
                preview.row.setVisibility(View.INVISIBLE);
                Glide.with(activity).clear(preview.image);
                continue;
            }
            Video item = list.get(i);
            preview.row.setVisibility(View.VISIBLE);
            preview.number.setText(String.format(Locale.GERMANY, "%02d  ·  Als Nächstes",
                    position + i + 2));
            preview.title.setText(item.getTitle());
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
    }
}
