package com.newtube.mobile.ui.playback;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.media3.common.C;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.tv.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * MT2-005: TV-first chrome over the existing, UNCHANGED Media3/TextureView playback surface.
 * No permanent background art, blur, animations, side scrims or extra video decoders.
 * Six real 16:9 thumbnails at a time; native D-pad state machine; one optional slim icon rail.
 * Only installed in the isolated MarsTube 2.0 TV UI lab package.
 */
public final class MarsTvShell {
    public interface PlayerSource { ExoPlayer current(); }
    public interface Actions {
        void openVideo(Video video);
        void navigate(int destination);
        void previous();
        void next();
        void toggle();
        void captions();
        void fullscreen();
    }

    private enum Focus { VIDEO, CARDS, CONTROLS, RAIL }

    private static final int BLUE = 0xFF0B9BFF;
    private static final int WHITE = 0xFFF1F7FF;
    private static final int MUTED = 0xFFB5C7D8;
    private static final int[] RAIL_GLYPHS = {0, 1, 2, 3, 4, 5};
    private static final String[] RAIL_LABELS = {
            "Start", "Suche", "Abos", "Verlauf", "Bibliothek", "Einstellungen"
    };
    private static final int[] CONTROL_GLYPHS = {6, 7, 8, 9, 10, 11, 12};

    private final Activity activity;
    private final PlayerSource player;
    private final Actions actions;
    private final FrameLayout root;
    private final LinearLayout rail;
    private final LinearLayout controls;
    private final FrameLayout panel;
    private final RecyclerView cards;
    private final Carousel adapter;
    private final View progress;
    private final TextView elapsed;
    private final TextView remaining;
    private final TextView clock;
    private int lastHighlightedCard = -1;
    private final TextView railHint;
    private final List<View> railButtons = new ArrayList<>();
    private final List<View> controlButtons = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable ticker = this::tick;
    private Focus focus = Focus.VIDEO;
    private final Runnable autoHide = () -> {
        if (focus == Focus.CARDS || focus == Focus.CONTROLS) {
            focus = Focus.VIDEO;
            render();
        }
    };
    private int cardPosition;
    private int controlPosition = 2;
    private int railPosition;
    private boolean pip;
    private boolean released;
    private final int cardWidthPx;
    private final int thumbnailHeightPx;
    private final int gapPx;

    public MarsTvShell(Activity a, ViewGroup host, PlayerSource source, Actions callbacks) {
        activity = a;
        player = source;
        actions = callbacks;
        int width = a.getResources().getDisplayMetrics().widthPixels;
        gapPx = dp(12);
        cardWidthPx = Math.max(dp(106), (width - dp(92) - gapPx * 5) / 6);
        thumbnailHeightPx = cardWidthPx * 9 / 16;

        root = new FrameLayout(a);
        root.setClipChildren(false);
        root.setClipToPadding(false);
        root.setFocusable(false);
        host.addView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // Simple clock, low cost. The video still fills the entire display behind the chrome.
        clock = text(19, WHITE);
        clock.setGravity(Gravity.RIGHT);
        FrameLayout.LayoutParams clockLp = new FrameLayout.LayoutParams(
                dp(120), dp(39), Gravity.TOP | Gravity.RIGHT);
        clockLp.topMargin = dp(8);
        clockLp.rightMargin = dp(22);
        root.addView(clock, clockLp);

        // 66 dp rail, NOT the large left panel used by phone/navigation shells.
        rail = new LinearLayout(a);
        rail.setOrientation(LinearLayout.VERTICAL);
        rail.setGravity(Gravity.CENTER_HORIZONTAL);
        rail.setBackgroundColor(0xEE071521);
        rail.setPadding(dp(5), dp(8), dp(5), dp(10));
        FrameLayout.LayoutParams railLp = new FrameLayout.LayoutParams(
                dp(67), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.LEFT | Gravity.TOP);
        root.addView(rail, railLp);

        ImageView logo = new ImageView(a);
        logo.setImageResource(R.drawable.mars_m_logo);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams logoLp = new LinearLayout.LayoutParams(dp(48), dp(61));
        logoLp.bottomMargin = dp(12);
        rail.addView(logo, logoLp);

        for (int i = 0; i < RAIL_GLYPHS.length; i++) {
            final int index = i;
            Glyph button = new Glyph(a, RAIL_GLYPHS[i], dp(24));
            button.setFocusable(false);
            button.setContentDescription(RAIL_LABELS[i]);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(51), dp(49));
            lp.bottomMargin = dp(9);
            rail.addView(button, lp);
            button.setOnClickListener(v -> { railPosition = index; activateRail(); });
            railButtons.add(button);
        }

        railHint = text(16, WHITE);
        railHint.setGravity(Gravity.CENTER_VERTICAL);
        railHint.setPadding(dp(12), 0, dp(12), 0);
        railHint.setBackground(shape(0xEE071521, BLUE, 1));
        FrameLayout.LayoutParams hintLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(45), Gravity.LEFT | Gravity.TOP);
        hintLp.leftMargin = dp(76);
        hintLp.topMargin = dp(77);
        root.addView(railHint, hintLp);

        // Bottom panel is translucent paint, not GPU-expensive blur/glow.
        panel = new FrameLayout(a);
        panel.setBackground(new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x96051322, 0xF207121E}));
        // 0.0.5: strip stays close to the lower image edge, not halfway up the video.
        FrameLayout.LayoutParams panelLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(151) + thumbnailHeightPx,
                Gravity.BOTTOM);
        root.addView(panel, panelLp);

        LinearLayout panelContent = new LinearLayout(a);
        panelContent.setOrientation(LinearLayout.VERTICAL);
        panelContent.setPadding(dp(30), dp(7), dp(30), dp(9));
        panel.addView(panelContent, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // One slim BLUE seek line, real progress and clock; seek driven by key events.
        LinearLayout progressRow = new LinearLayout(a);
        progressRow.setGravity(Gravity.CENTER_VERTICAL);
        panelContent.addView(progressRow, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(28)));
        elapsed = text(13, WHITE);
        progressRow.addView(elapsed, new LinearLayout.LayoutParams(dp(65), dp(29)));
        progress = new View(a) {
            final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            @Override protected void onDraw(Canvas c) {
                super.onDraw(c);
                ExoPlayer exo = player.current();
                long duration = exo == null ? 0 : exo.getDuration();
                long pos = exo == null ? 0 : exo.getCurrentPosition();
                float fraction = duration <= 0 || duration == C.TIME_UNSET ? 0 :
                        Math.max(0, Math.min(1, (float) pos / duration));
                float y = getHeight() / 2f;
                paint.setStrokeWidth(dp(3));
                paint.setStrokeCap(Paint.Cap.ROUND);
                paint.setColor(0xFF7A8796);
                c.drawLine(0, y, getWidth(), y, paint);
                paint.setColor(BLUE);
                c.drawLine(0, y, getWidth() * fraction, y, paint);
                paint.setColor(WHITE);
                c.drawCircle(getWidth() * fraction, y, dp(5), paint);
            }
        };
        progressRow.addView(progress, new LinearLayout.LayoutParams(
                0, dp(30), 1f));
        remaining = text(13, WHITE);
        remaining.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        progressRow.addView(remaining, new LinearLayout.LayoutParams(dp(65), dp(29)));

        controls = new LinearLayout(a);
        controls.setGravity(Gravity.CENTER);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams controlsLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(40));
        controlsLp.bottomMargin = dp(7);
        panelContent.addView(controls, controlsLp);

        for (int i = 0; i < CONTROL_GLYPHS.length; i++) {
            final int index = i;
            Glyph button = new Glyph(a, CONTROL_GLYPHS[i], dp(17));
            button.setFocusable(false);
            button.setContentDescription(new String[]{
                    "Vorheriges", "10 Sekunden zurück", "Pause oder Wiedergabe",
                    "10 Sekunden vor", "Nächstes", "Untertitel", "Vollbild"
            }[i]);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(44), dp(36));
            lp.leftMargin = dp(8);
            lp.rightMargin = dp(8);
            controls.addView(button, lp);
            button.setOnClickListener(v -> { controlPosition = index; executeControl(); });
            controlButtons.add(button);
        }

        cards = new RecyclerView(a);
        cards.setLayoutManager(new LinearLayoutManager(a, RecyclerView.HORIZONTAL, false));
        cards.setHasFixedSize(true);
        cards.setItemAnimator(null);
        cards.setOverScrollMode(View.OVER_SCROLL_NEVER);
        cards.setClipToPadding(false);
        cards.setFocusable(false);
        adapter = new Carousel();
        cards.setAdapter(adapter);
        panelContent.addView(cards, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, thumbnailHeightPx + dp(63)));

        root.bringChildToFront(rail);
        root.bringChildToFront(railHint);
        root.bringChildToFront(clock);
        render();
        tick();
    }

    private int dp(float n) {
        return (int) (n * activity.getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView text(int sp, int color) {
        TextView v = new TextView(activity);
        v.setTextColor(color);
        v.setTextSize(sp);
        v.setFontFeatureSettings("kern");
        v.setIncludeFontPadding(false);
        v.setGravity(Gravity.CENTER_VERTICAL);
        return v;
    }

    private GradientDrawable shape(int background, int outline, int outlineDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(background);
        d.setCornerRadius(dp(10));
        if (outlineDp > 0) d.setStroke(dp(outlineDp), outline);
        return d;
    }

    public void setRecommendations(List<Video> videos) {
        if (released) return;
        List<Video> clean = new ArrayList<>();
        if (videos != null) for (Video video : videos) {
            if (video != null && video.videoId != null) clean.add(video);
            if (clean.size() >= 80) break; // RecyclerView binds only visible thumbnails
        }
        cardPosition = 0;
        adapter.items = clean;
        adapter.notifyDataSetChanged();
        cards.scrollToPosition(0);
        render();
    }

    public void hideUi() {
        focus = Focus.VIDEO;
        render();
    }

    public void setPictureInPicture(boolean value) {
        pip = value;
        if (pip) focus = Focus.VIDEO;
        render();
    }

    public boolean handle(KeyEvent e) {
        if (released || pip) return false;
        int key = e.getKeyCode();
        boolean mapped = key == KeyEvent.KEYCODE_DPAD_LEFT
                || key == KeyEvent.KEYCODE_DPAD_RIGHT
                || key == KeyEvent.KEYCODE_DPAD_UP
                || key == KeyEvent.KEYCODE_DPAD_DOWN
                || key == KeyEvent.KEYCODE_DPAD_CENTER
                || key == KeyEvent.KEYCODE_ENTER
                || key == KeyEvent.KEYCODE_NUMPAD_ENTER
                || key == KeyEvent.KEYCODE_BACK
                || key == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                || key == KeyEvent.KEYCODE_MEDIA_REWIND
                || key == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD
                || key == KeyEvent.KEYCODE_MENU;
        if (!mapped) return false;
        if (e.getAction() != KeyEvent.ACTION_DOWN) return true;
        switch (key) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (focus == Focus.CARDS && cardPosition > 0) {
                    cardPosition--; cards.scrollToPosition(cardPosition);
                } else if (focus == Focus.CONTROLS && controlPosition > 0) {
                    controlPosition--;
                } else {
                    focus = Focus.RAIL;
                }
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (focus == Focus.RAIL) {
                    focus = adapter.getItemCount() > 0 ? Focus.CARDS : Focus.CONTROLS;
                } else if (focus == Focus.CARDS && cardPosition < adapter.getItemCount() - 1) {
                    cardPosition++; cards.scrollToPosition(cardPosition);
                } else if (focus == Focus.CONTROLS) {
                    controlPosition = Math.min(CONTROL_GLYPHS.length - 1, controlPosition + 1);
                } else {
                    focus = adapter.getItemCount() > 0 ? Focus.CARDS : Focus.CONTROLS;
                }
                break;
            case KeyEvent.KEYCODE_DPAD_UP:
                if (focus == Focus.RAIL) {
                    railPosition = (railPosition + RAIL_GLYPHS.length - 1) % RAIL_GLYPHS.length;
                } else if (focus == Focus.CARDS) {
                    focus = Focus.CONTROLS;
                } else if (focus == Focus.CONTROLS) {
                    focus = Focus.VIDEO;
                } else {
                    focus = Focus.CONTROLS;
                }
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (focus == Focus.RAIL) {
                    railPosition = (railPosition + 1) % RAIL_GLYPHS.length;
                } else if (focus == Focus.CONTROLS && adapter.getItemCount() > 0) {
                    focus = Focus.CARDS;
                } else {
                    focus = adapter.getItemCount() > 0 ? Focus.CARDS : Focus.CONTROLS;
                }
                break;
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
            case KeyEvent.KEYCODE_DPAD_CENTER:
                if (focus == Focus.RAIL) activateRail();
                else if (focus == Focus.CARDS && adapter.getItemCount() > cardPosition) {
                    actions.openVideo(adapter.items.get(cardPosition));
                } else if (focus == Focus.CONTROLS) executeControl();
                else focus = Focus.CONTROLS;
                break;
            case KeyEvent.KEYCODE_BACK:
                if (focus == Focus.VIDEO) return false; // preserve app's video -> mini-player Back
                if (focus == Focus.RAIL) focus = adapter.getItemCount() > 0
                        ? Focus.CARDS : Focus.CONTROLS;
                else focus = Focus.VIDEO;
                break;
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                actions.toggle(); break;
            case KeyEvent.KEYCODE_MEDIA_REWIND:
                seek(-10000); break;
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                seek(10000); break;
            case KeyEvent.KEYCODE_MENU:
                focus = Focus.RAIL; break;
            default: return false;
        }
        render();
        return true;
    }

    private void seek(long delta) {
        ExoPlayer exo = player.current();
        if (exo == null || !exo.isCurrentMediaItemSeekable()) return;
        long duration = exo.getDuration();
        long target = Math.max(0L, exo.getCurrentPosition() + delta);
        if (duration > 0 && duration != C.TIME_UNSET) target = Math.min(duration, target);
        exo.seekTo(target);
    }

    private void executeControl() {
        switch (controlPosition) {
            case 0: actions.previous(); break;
            case 1: seek(-10000); break;
            case 2: actions.toggle(); break;
            case 3: seek(10000); break;
            case 4: actions.next(); break;
            case 5: actions.captions(); break;
            case 6: actions.fullscreen(); break;
        }
    }

    private void activateRail() {
        actions.navigate(railPosition);
    }

    private void render() {
        if (released) return;
        root.setVisibility(pip ? View.GONE : View.VISIBLE);
        panel.setVisibility(!pip && focus != Focus.VIDEO ? View.VISIBLE : View.GONE);
        // No station watermark in video-only mode. Clock appears only with menu.
        clock.setVisibility(!pip && focus != Focus.VIDEO ? View.VISIBLE : View.GONE);
        rail.setVisibility(!pip && focus == Focus.RAIL ? View.VISIBLE : View.GONE);
        railHint.setVisibility(!pip && focus == Focus.RAIL ? View.VISIBLE : View.GONE);
        if (focus == Focus.RAIL) {
            railHint.setText(RAIL_LABELS[railPosition]);
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) railHint.getLayoutParams();
            lp.topMargin = dp(81 + railPosition * 58);
            railHint.setLayoutParams(lp);
        }
        for (int i = 0; i < railButtons.size(); i++)
            railButtons.get(i).setBackground(focus == Focus.RAIL && railPosition == i
                    ? shape(0x99225072, BLUE, 2) : null);
        for (int i = 0; i < controlButtons.size(); i++)
            controlButtons.get(i).setBackground(focus == Focus.CONTROLS && controlPosition == i
                    ? shape(0x99225072, BLUE, 2) : null);
        int newHighlight = focus == Focus.CARDS ? cardPosition : -1;
        if (lastHighlightedCard != newHighlight) {
            if (lastHighlightedCard >= 0 && lastHighlightedCard < adapter.getItemCount())
                adapter.notifyItemChanged(lastHighlightedCard);
            if (newHighlight >= 0 && newHighlight < adapter.getItemCount())
                adapter.notifyItemChanged(newHighlight);
            lastHighlightedCard = newHighlight;
        }
        handler.removeCallbacks(autoHide);
        if (focus == Focus.CARDS || focus == Focus.CONTROLS) handler.postDelayed(autoHide, 12000);
    }

    private static String format(long ms) {
        long secs = Math.max(0L, ms) / 1000;
        long minutes = secs / 60;
        if (minutes >= 60) return String.format(Locale.ROOT, "%d:%02d:%02d",
                minutes / 60, minutes % 60, secs % 60);
        return String.format(Locale.ROOT, "%02d:%02d", minutes, secs % 60);
    }

    private void tick() {
        if (released) return;
        ExoPlayer exo = player.current();
        long duration = exo == null ? 0 : exo.getDuration();
        long position = exo == null ? 0 : exo.getCurrentPosition();
        elapsed.setText(format(position));
        remaining.setText(duration <= 0 || duration == C.TIME_UNSET
                ? "--:--" : format(Math.max(0L, duration - position)));
        clock.setText(new SimpleDateFormat("HH:mm", Locale.GERMANY).format(new Date()));
        if (focus != Focus.VIDEO) progress.invalidate();
        handler.postDelayed(ticker, 1000);
    }

    public void release() {
        released = true;
        handler.removeCallbacks(ticker);
        handler.removeCallbacks(autoHide);
        adapter.items.clear();
        root.removeAllViews();
        ViewGroup parent = (ViewGroup) root.getParent();
        if (parent != null) parent.removeView(root);
    }

    private final class Carousel extends RecyclerView.Adapter<Carousel.Holder> {
        private List<Video> items = new ArrayList<>();
        @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            LinearLayout card = new LinearLayout(activity);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setFocusable(false);
            RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(
                    cardWidthPx, thumbnailHeightPx + dp(65));
            lp.rightMargin = gapPx;
            card.setLayoutParams(lp);

            // Real rounded corners, not a square preview under a floating selection border.
            FrameLayout imageFrame = new FrameLayout(activity);
            imageFrame.setBackground(shape(0xFF0A1B2B, 0, 0));
            imageFrame.setClipToOutline(true);
            ImageView image = new ImageView(activity);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setBackground(shape(0xFF1C3447, 0, 0));
            image.setClipToOutline(true);
            imageFrame.addView(image, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            TextView badge = text(11, WHITE);
            badge.setGravity(Gravity.CENTER);
            badge.setPadding(dp(5), dp(2), dp(5), dp(2));
            badge.setBackground(shape(0xDB071521, 0, 0));
            FrameLayout.LayoutParams badgeLp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, dp(23), Gravity.RIGHT | Gravity.BOTTOM);
            badgeLp.rightMargin = dp(6); badgeLp.bottomMargin = dp(5);
            imageFrame.addView(badge, badgeLp);
            card.addView(imageFrame, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, thumbnailHeightPx));
            TextView title = text(13, WHITE);
            title.setGravity(Gravity.TOP);
            title.setMaxLines(2);
            title.setMinLines(2);
            title.setEllipsize(TextUtils.TruncateAt.END);
            title.setLineSpacing(dp(2), 1f);
            LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(56));
            titleLp.topMargin = dp(6);
            card.addView(title, titleLp);
            Holder h = new Holder(card, imageFrame, image, badge, title);
            card.setOnClickListener(v -> {
                int position = h.getBindingAdapterPosition();
                if (position == RecyclerView.NO_POSITION || position >= items.size()) return;
                cardPosition = position;
                focus = Focus.CARDS;
                render();
                actions.openVideo(items.get(position));
            });
            return h;
        }

        @Override public void onBindViewHolder(@NonNull Holder h, int position) {
            Video v = items.get(position);
            h.title.setText(v.getTitle());
            String badgeText = v.isLive ? "LIVE" : v.badge;
            h.badge.setText(badgeText);
            h.badge.setVisibility(badgeText == null || badgeText.isEmpty()
                    ? View.GONE : View.VISIBLE);
            boolean selected = focus == Focus.CARDS && position == cardPosition;
            h.imageFrame.setBackground(selected
                    ? shape(0xFF153653, BLUE, 2) : shape(0xFF0A1B2B, 0, 0));
            h.imageFrame.setPadding(dp(3), dp(3), dp(3), dp(3));
            h.image.setClipToOutline(true);
            Glide.with(activity).load(v.getCardImageUrl())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .format(DecodeFormat.PREFER_RGB_565)
                    .centerCrop().into(h.image);
        }
        @Override public void onViewRecycled(@NonNull Holder h) {
            Glide.with(activity).clear(h.image);
            super.onViewRecycled(h);
        }
        @Override public int getItemCount() { return items.size(); }
        final class Holder extends RecyclerView.ViewHolder {
            final FrameLayout imageFrame;
            final ImageView image;
            final TextView badge, title;
            Holder(View view, FrameLayout f, ImageView i, TextView b, TextView t) {
                super(view); imageFrame=f; image=i; badge=b; title=t;
            }
        }
    }

    /** Tiny vector strokes: zero custom fonts, raster highlights or complex shaders. */
    private final class Glyph extends View {
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        final int id;
        final int size;
        Glyph(Context c, int iconId, int sizePx) {
            super(c); id=iconId; size=sizePx;
            setClickable(true);
        }
        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            c.save();
            float scale = size / 24f;
            c.translate((getWidth()-size)/2f, (getHeight()-size)/2f);
            c.scale(scale, scale);
            p.setColor(WHITE); p.setStrokeWidth(1.9f); p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeJoin(Paint.Join.ROUND); p.setStyle(Paint.Style.STROKE);
            Path path = new Path();
            switch (id) {
                case 0: // home
                    path.moveTo(2,11);path.lineTo(12,3);path.lineTo(22,11);
                    path.moveTo(5,10);path.lineTo(5,21);path.lineTo(19,21);
                    path.lineTo(19,10);path.moveTo(10,21);path.lineTo(10,15);
                    path.lineTo(14,15);path.lineTo(14,21);break;
                case 1: c.drawCircle(10.2f,10.2f,6.2f,p);
                    c.drawLine(15,15,21.5f,21.5f,p);break;
                case 2: c.drawRoundRect(4,6,22,20,2,2,p);
                    c.drawLine(2,3,18,3,p); path.moveTo(11,9);path.lineTo(16,13);
                    path.lineTo(11,17);path.close();break;
                case 3: c.drawArc(2,3,22,22,-75,300,false,p);
                    path.moveTo(2,4);path.lineTo(2,10);path.lineTo(8,10);
                    path.moveTo(12,6);path.lineTo(12,12);path.lineTo(16,14);break;
                case 4: c.drawRoundRect(3,8,21,21,2,2,p);
                    c.drawLine(5,4,19,4,p);path.moveTo(10,11);path.lineTo(16,14);
                    path.lineTo(10,18);path.close();break;
                case 5: c.drawCircle(12,12,4,p);
                    for (int i=0;i<8;i++){ double a=Math.PI*i/4;
                        c.drawLine((float)(12+7*Math.cos(a)),(float)(12+7*Math.sin(a)),
                            (float)(12+10*Math.cos(a)),(float)(12+10*Math.sin(a)),p);}
                    break;
                case 6: path.moveTo(8,6);path.lineTo(8,18);path.moveTo(18,6);
                    path.lineTo(9,12);path.lineTo(18,18);path.close();break;
                case 7: case 9:
                    c.drawArc(4,4,21,21,id==7?175:5,300,false,p);
                    path.moveTo(id==7?4:20,6);
                    path.lineTo(id==7?4:20,12);
                    c.drawText("10",7,15,p);
                    break;
                case 8: c.drawRoundRect(7,5,10,19,1,1,p);
                    c.drawRoundRect(14,5,17,19,1,1,p); break;
                case 10: path.moveTo(16,6);path.lineTo(16,18);
                    path.moveTo(6,6);path.lineTo(15,12);path.lineTo(6,18);path.close();break;
                case 11: c.drawRoundRect(2,6,22,18,2,2,p);
                    p.setStyle(Paint.Style.FILL);p.setTextSize(9);
                    c.drawText("CC",5,15,p);break;
                case 12:
                    path.moveTo(3,9);path.lineTo(3,3);path.lineTo(9,3);
                    path.moveTo(15,3);path.lineTo(21,3);path.lineTo(21,9);
                    path.moveTo(21,15);path.lineTo(21,21);path.lineTo(15,21);
                    path.moveTo(9,21);path.lineTo(3,21);path.lineTo(3,15);break;
            }
            p.setStyle(Paint.Style.STROKE);
            c.drawPath(path,p);
            c.restore();
        }
    }
}
