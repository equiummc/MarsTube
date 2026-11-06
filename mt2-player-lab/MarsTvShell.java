package com.newtube.mobile.ui.playback;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
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
import androidx.media3.common.Format;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.newtube.mobile.player.MarsTvPlayerPrefs;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * MarsTube 0.0.12 TV player chrome.
 *
 * <p>Dark-blue translucent bar, white/cyan vector glyphs, rounded focus, no live blur and
 * no permanent animation. The Media3 surface, the SABR adapter and the decoder stay
 * untouched: this class only draws over them and translates remote keys.</p>
 *
 * <p>Remote contract:</p>
 * <ul>
 *   <li>OK short = play/pause; OK held = temporary 2x, restored on release.</li>
 *   <li>BACK always opens the left rail while playback continues; it never
 *       minimizes the player or sends a user back through every video tile.</li>
 *   <li>BACK in a dialog is handled by the dialog before this shell sees it.</li>
 *   <li>D-pad up/down switches Shorts when no control is focused; Media next/previous
 *       always switches. Left/right keep seeking.</li>
 * </ul>
 */
public final class MarsTvShell {
    public interface PlayerSource {
        ExoPlayer current();
    }

    public interface Actions {
        void openVideo(Video video);

        void navigate(int destination);

        void previous();

        void next();

        void toggle();

        void captions();

        void fullscreen();

        void like();

        void dislike();

        void save();

        void quality();

        /** Is this playback from the dedicated Shorts feed (not merely portrait)? */
        default boolean isShortsPlayback() { return false; }
        /** Shift the existing 9:16 video for the Shorts split layout. */
        default void positionShortsVideo(boolean shorts) { }
        /** Dock the current player to in-app mini mode, maintaining its session. */
        default void minimize() { }
        /** Temporary 2x while OK is held. */
        default void beginHold() {
        }

        /** Restore the chosen speed; never leaves a pause loop. */
        default void endHold() {
        }

        /** The protocol that is actually open, for the Info panel. */
        default String sourceProtocol() {
            return "";
        }

        /** Extra diagnostics (fallback, buffer, track) for the Info panel. */
        default String sourceDiagnostics() {
            return "";
        }
    }

    private enum Focus {
        VIDEO, CARDS, CONTROLS, PROGRESS, RAIL,
        SHORTS_PREVIEWS, SHORTS_REACTIONS, SHORTS_AUTO, SHORTS_PROGRESS
    }

    private static final int CYAN = 0xFF16B8FF;
    private static final int WHITE = 0xFFF1F7FF;
    private static final int MUTED = 0xFF9FB6CC;
    private static final int PANEL_TOP = 0xA6112E54;
    private static final int PANEL_BOTTOM = 0xF0091B37;

    private static final long HOLD_DELAY_MS = 420L;
    private static final long AUTO_HIDE_MS = 10_000L;

    private static final int CONTROL_LIKE = 0;
    private static final int CONTROL_DISLIKE = 1;
    private static final int CONTROL_PREVIOUS = 2;
    private static final int CONTROL_REWIND = 3;
    private static final int CONTROL_PLAY = 4;
    private static final int CONTROL_FORWARD = 5;
    private static final int CONTROL_NEXT = 6;
    private static final int CONTROL_CC = 7;
    private static final int CONTROL_INFO = 8;
    private static final int CONTROL_HD = 9;
    private static final int CONTROL_FULLSCREEN = 10;

    private static final int[] CONTROL_ICONS = {
            R.drawable.mars_tv_ic_like,
            R.drawable.mars_tv_ic_dislike,
            R.drawable.mars_tv_ic_previous,
            R.drawable.mars_tv_ic_rewind10,
            R.drawable.mars_tv_ic_play,
            R.drawable.mars_tv_ic_forward10,
            R.drawable.mars_tv_ic_next,
            R.drawable.mars_tv_ic_cc,
            R.drawable.mars_tv_ic_info,
            R.drawable.mars_tv_ic_hd_settings,
            R.drawable.mars_tv_ic_fullscreen
    };

    private static final String[] CONTROL_LABELS = {
            "Gefällt mir", "Gefällt mir nicht", "Vorheriges", "10 Sekunden zurück",
            "Pause oder Wiedergabe", "10 Sekunden vor", "Nächstes", "Untertitel",
            "Videoinfos", "Videoqualität", "Vollbild"
    };

    private final Activity activity;
    private final PlayerSource player;
    private final Actions actions;
    private final FrameLayout root;
    private final LinearLayout rail;
    private final LinearLayout railItems;
    private final LinearLayout controls;
    private final FrameLayout panel;
    private final RecyclerView cards;
    private final Carousel adapter;
    private final View progress;
    private final TextView elapsed;
    private final TextView remaining;
    private final TextView clock;
    private final TextView railHint;
    private final MarsTvShortsStage shortsStage;
    private final FrameLayout infoPanel;
    private final TextView infoText;
    private final ImageView playPauseIcon;
    private final List<ImageView> controlButtons = new ArrayList<>();
    private final List<TextView> railButtons = new ArrayList<>();
    private final List<RailItem> railDestinations = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Focus focus = Focus.VIDEO;
    private int cardPosition;
    private int controlPosition = CONTROL_PLAY;
    private int shortsReactionPosition;
    /** Pending D-pad seek position; committed only with OK. */
    private long seekPreviewMs = -1L;
    private int seekHoldKey = -1;
    private long seekHoldStarted;
    private long seekHoldOrigin;
    private boolean lastShortsPositioned;
    private boolean shortsAutoAdvanceArmed = true;
    private int railPosition;
    private int lastHighlightedCard = -1;
    private boolean pip;
    private boolean released;
    private boolean infoOpen;
    private boolean railEverOpened;
    private boolean okDown;
    private boolean holdTriggered;
    private final Runnable ticker = this::tick;
    private final Runnable autoHide = () -> {
        if (!infoOpen && focus != Focus.RAIL && (focus == Focus.CARDS || focus == Focus.CONTROLS)) {
            focus = Focus.VIDEO;
            render();
        }
    };
    private final Runnable holdRunnable;
    private final int cardWidthPx;
    private final int thumbnailHeightPx;
    private final int gapPx;

    private static final class RailItem {
        final String label;
        final int destination;

        RailItem(String label, int destination) {
            this.label = label;
            this.destination = destination;
        }
    }

    public MarsTvShell(Activity activity, ViewGroup host, PlayerSource source, Actions callbacks) {
        this.activity = activity;
        this.player = source;
        this.actions = callbacks;
        this.holdRunnable = () -> {
            if (okDown && !holdTriggered && canHold()) {
                holdTriggered = true;
                actions.beginHold();
            }
        };

        int width = activity.getResources().getDisplayMetrics().widthPixels;
        gapPx = dp(10);
        cardWidthPx = Math.max(dp(96), (width - dp(88) - gapPx * 5) / 6);
        thumbnailHeightPx = cardWidthPx * 9 / 16;

        root = new FrameLayout(activity);
        root.setClipChildren(false);
        root.setClipToPadding(false);
        root.setFocusable(false);
        host.addView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        clock = text(18, WHITE);
        clock.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        FrameLayout.LayoutParams clockLp = new FrameLayout.LayoutParams(
                dp(120), dp(36), Gravity.TOP | Gravity.RIGHT);
        clockLp.topMargin = dp(10);
        clockLp.rightMargin = dp(24);
        root.addView(clock, clockLp);

        infoPanel = new FrameLayout(activity);
        infoPanel.setBackground(shape(0xF00A1D33, CYAN, 1, 14));
        infoPanel.setPadding(dp(18), dp(14), dp(18), dp(14));
        FrameLayout.LayoutParams infoLp = new FrameLayout.LayoutParams(
                dp(430), ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.RIGHT);
        infoLp.topMargin = dp(52);
        infoLp.rightMargin = dp(22);
        root.addView(infoPanel, infoLp);
        infoText = text(13, WHITE);
        infoText.setLineSpacing(dp(3), 1f);
        infoText.setGravity(Gravity.START);
        infoPanel.addView(infoText, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        // A translucent Shorts stage above the one existing player surface.
        shortsStage = new MarsTvShortsStage(activity, root, this::render,
                actions::like, actions::dislike, actions::save,
                () -> seek(-5_000L), () -> seek(5_000L));

        // Left rail. It is an overlay over the playing video, never a second player.
        rail = new LinearLayout(activity);
        rail.setOrientation(LinearLayout.VERTICAL);
        rail.setGravity(Gravity.CENTER_HORIZONTAL);
        rail.setBackground(com.newtube.mobile.ui.common.MarsTvNavigationTheme.railBackground());
        rail.setPadding(dp(4), dp(9), dp(4), dp(6));
        FrameLayout.LayoutParams railLp = new FrameLayout.LayoutParams(
                dp(com.newtube.mobile.ui.common.MarsTvNavigationTheme.WIDTH_DP), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.LEFT | Gravity.TOP);
        root.addView(rail, railLp);

        ImageView logo = new ImageView(activity);
        logo.setImageResource(R.drawable.mars_m_logo);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        logo.setContentDescription("MarsTube");
        rail.addView(logo, new LinearLayout.LayoutParams(dp(38), dp(40)));
        TextView version = text(11, MUTED);
        version.setText("0.0.15");
        version.setGravity(Gravity.CENTER);
        rail.addView(version, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(16)));

        railItems = new LinearLayout(activity);
        railItems.setOrientation(LinearLayout.VERTICAL);
        railItems.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        rail.addView(railItems, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        addRailItem("Start", 0);
        addRailItem("Shorts", 8);
        addRailItem("Suche", 1);
        addRailItem("Abos", 2);
        addRailItem("Verlauf", 3);
        addRailItem("Profil", 6);
        addRailItem("Einstellungen", 5);
        addRailItem("Shorts Auto", -18);

        railHint = text(12, WHITE);
        railHint.setGravity(Gravity.CENTER_VERTICAL);
        railHint.setPadding(dp(14), 0, dp(14), 0);
        railHint.setBackground(shape(0xF00A1D33, CYAN, 1, 10));
        rail.addView(railHint, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(28)));

        // Translucent paint, not a GPU blur. The video keeps rendering behind it.
        panel = new FrameLayout(activity);
        GradientDrawable chrome = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xD0135483, PANEL_TOP, PANEL_BOTTOM});
        chrome.setCornerRadius(dp(22));
        chrome.setStroke(dp(1), 0xAA21A8EB);
        panel.setBackground(chrome);
        int panelHeight = dp(4) + dp(32) + dp(6) + dp(44) + dp(8)
                + thumbnailHeightPx + dp(34) + dp(8);
        FrameLayout.LayoutParams playerPanelLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, panelHeight, Gravity.BOTTOM);
        playerPanelLp.leftMargin = dp(12);
        playerPanelLp.rightMargin = dp(12);
        playerPanelLp.bottomMargin = dp(12);
        root.addView(panel, playerPanelLp);

        LinearLayout panelContent = new LinearLayout(activity);
        panelContent.setOrientation(LinearLayout.VERTICAL);
        panelContent.setPadding(dp(22), dp(4), dp(22), dp(4));
        panel.addView(panelContent, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // Large progress bar above the icon row.
        LinearLayout progressRow = new LinearLayout(activity);
        progressRow.setGravity(Gravity.CENTER_VERTICAL);
        elapsed = text(13, WHITE);
        progressRow.addView(elapsed, new LinearLayout.LayoutParams(dp(72), dp(30)));
        progress = new View(activity) {
            final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

            @Override
            protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                ExoPlayer exo = player.current();
                long duration = exo == null ? 0 : exo.getDuration();
                long position = seekPreviewMs >= 0L ? seekPreviewMs
                        : exo == null ? 0 : exo.getCurrentPosition();
                float fraction = duration <= 0 || duration == C.TIME_UNSET ? 0f
                        : Math.max(0f, Math.min(1f, (float) position / duration));
                float y = getHeight() / 2f;
                paint.setShader(null);
                paint.setStrokeCap(Paint.Cap.ROUND);
                paint.setStrokeWidth(dp(4));
                paint.setColor(0xFF5F7893);
                canvas.drawLine(0, y, getWidth(), y, paint);
                paint.setShader(new LinearGradient(0f, y, Math.max(1f, getWidth()), y,
                        CYAN, 0xFF354FEA, Shader.TileMode.CLAMP));
                paint.setStrokeWidth(dp(6));
                canvas.drawLine(0, y, getWidth() * fraction, y, paint);
                paint.setShader(null);
                paint.setColor(focus == Focus.PROGRESS ? CYAN : WHITE);
                canvas.drawCircle(getWidth() * fraction, y,
                        dp(focus == Focus.PROGRESS ? 8 : 5), paint);
            }
        };
        progressRow.addView(progress, new LinearLayout.LayoutParams(0, dp(30), 1f));
        remaining = text(13, WHITE);
        remaining.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        progressRow.addView(remaining, new LinearLayout.LayoutParams(dp(72), dp(30)));
        // Progress is inserted after the control buttons, not above them.

        controls = new LinearLayout(activity);
        controls.setGravity(Gravity.CENTER);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams controlsLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(44));
        controlsLp.topMargin = dp(5);
        panelContent.addView(controls, controlsLp);
        progressRow.setContentDescription("Fortschritt: links/rechts jeweils fünf Prozent, OK bestätigt");
        panelContent.addView(progressRow, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(32)));

        for (int i = 0; i < CONTROL_ICONS.length; i++) {
            final int index = i;
            ImageView button = new ImageView(activity);
            button.setImageResource(CONTROL_ICONS[i]);
            button.setScaleType(ImageView.ScaleType.FIT_CENTER);
            button.setContentDescription(CONTROL_LABELS[i]);
            button.setColorFilter(WHITE);
            button.setBackground(buttonChrome(false, index == CONTROL_PLAY));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    dp(index == CONTROL_PLAY ? 38 : 32),
                    dp(index == CONTROL_PLAY ? 38 : 32));
            lp.leftMargin = dp(3);
            lp.rightMargin = dp(3);
            controls.addView(button, lp);
            button.setOnClickListener(v -> {
                controlPosition = index;
                executeControl();
            });
            controlButtons.add(button);
        }
        playPauseIcon = controlButtons.get(CONTROL_PLAY);

        cards = new RecyclerView(activity);
        cards.setLayoutManager(new LinearLayoutManager(activity, RecyclerView.HORIZONTAL, false));
        cards.setHasFixedSize(true);
        cards.setItemAnimator(null);
        cards.setOverScrollMode(View.OVER_SCROLL_NEVER);
        cards.setClipToPadding(false);
        cards.setFocusable(false);
        adapter = new Carousel();
        cards.setAdapter(adapter);
        LinearLayout.LayoutParams cardsLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, thumbnailHeightPx + dp(34));
        cardsLp.topMargin = dp(8);
        panelContent.addView(cards, cardsLp);

        root.bringChildToFront(rail);
        root.bringChildToFront(infoPanel);
        root.bringChildToFront(clock);
        render();
        tick();
    }

    private int dp(float value) {
        return (int) (value * activity.getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView text(int sp, int color) {
        TextView view = new TextView(activity);
        view.setTextColor(color);
        view.setTextSize(sp);
        view.setIncludeFontPadding(false);
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }

    private GradientDrawable shape(int background, int outline, int outlineDp, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(background);
        drawable.setCornerRadius(dp(radiusDp));
        if (outlineDp > 0) {
            drawable.setStroke(dp(outlineDp), outline);
        }
        return drawable;
    }

    /** Light neon/cyan gloss; no GPU blur, animations or additional decoder. */
    private GradientDrawable buttonChrome(boolean selected, boolean primary) {
        GradientDrawable bg = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                selected ? new int[]{0xFF16D6FA, 0xFF067BC1, 0xFF073B80}
                        : primary ? new int[]{0xEE1899C8, 0xD8084274, 0xF007244C}
                        : new int[]{0xCC1473A4, 0xDC0A3C67, 0xE3072445});
        bg.setCornerRadius(dp(primary ? 18 : 14));
        bg.setStroke(dp(selected || primary ? 2 : 1),
                selected ? 0xFF9FFFFF : primary ? 0xFF27E8FD : 0xAA2CB1EC);
        return bg;
    }

    private void addRailItem(String label, int destination) {
        final int index = railButtons.size();
        TextView button = text(15, WHITE);
        button.setText("");
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(3), dp(3), dp(3), dp(3));
        int railIcon = com.newtube.mobile.ui.common.MarsTvNavigationTheme.iconFor(destination);
        android.graphics.drawable.Drawable glyph =
                androidx.core.content.ContextCompat.getDrawable(activity, railIcon);
        if (glyph != null) {
            glyph = glyph.mutate();
            glyph.setBounds(0, 0, dp(20), dp(20));
            button.setCompoundDrawables(null, glyph, null, null);
        }
        button.setCompoundDrawableTintList(
                android.content.res.ColorStateList.valueOf(WHITE));
        button.setContentDescription(label);
        button.setBackground(com.newtube.mobile.ui.common.MarsTvNavigationTheme.itemBackground(false,
                activity.getResources().getDisplayMetrics().density));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(com.newtube.mobile.ui.common.MarsTvNavigationTheme.ITEM_HEIGHT_DP));
        lp.topMargin = dp(3);
        railItems.addView(button, lp);
        button.setOnClickListener(v -> {
            railPosition = index;
            activateRail();
        });
        railButtons.add(button);
        railDestinations.add(new RailItem(label, destination));
    }

    public void setRecommendations(List<Video> videos) {
        if (released) {
            return;
        }
        int limit = MarsTvPlayerPrefs.getCarouselPreviewCount(activity);
        List<Video> clean = new ArrayList<>();
        if (videos != null) {
            for (Video video : videos) {
                if (video != null && video.videoId != null) {
                    clean.add(video);
                }
                if (clean.size() >= limit) {
                    break;
                }
            }
        }
        cardPosition = 0;
        adapter.items = clean;
        adapter.notifyDataSetChanged();
        cards.scrollToPosition(0);
        render();
    }

    public void hideUi() {
        cancelHold();
        infoOpen = false;
        focus = Focus.VIDEO;
        render();
    }

    public void setPictureInPicture(boolean value) {
        pip = value;
        if (pip) {
            focus = Focus.VIDEO;
        }
        render();
    }

    /**
     * Predictive-back entry point. Mirrors {@link #handleDown(int)} for BACK so a gesture
     * that never produces a key event still closes the Info panel, then the rail, then
     * opens the rail once, and only then lets the activity minimize.
     *
     * @return true when the shell consumed the gesture
     */
    /** Single BACK state machine for key and Android dispatcher. */
    /** BACK is the universal navigation shortcut, not a minimize/exit shortcut. */
    public boolean handleBack() {
        if (released || pip) return false;
        cancelHold();
        cancelSeekHold();
        openRail();
        render();
        return true;
    }

    public boolean handle(KeyEvent event) {
        if (released || pip) {
            return false;
        }
        int key = event.getKeyCode();
        int action = event.getAction();
        boolean okKey = key == KeyEvent.KEYCODE_DPAD_CENTER
                || key == KeyEvent.KEYCODE_ENTER
                || key == KeyEvent.KEYCODE_NUMPAD_ENTER;
        boolean mapped = okKey
                || key == KeyEvent.KEYCODE_DPAD_LEFT
                || key == KeyEvent.KEYCODE_DPAD_RIGHT
                || key == KeyEvent.KEYCODE_DPAD_UP
                || key == KeyEvent.KEYCODE_DPAD_DOWN
                || key == KeyEvent.KEYCODE_BACK
                || key == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                || key == KeyEvent.KEYCODE_MEDIA_PLAY
                || key == KeyEvent.KEYCODE_MEDIA_PAUSE
                || key == KeyEvent.KEYCODE_MEDIA_REWIND
                || key == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD
                || key == KeyEvent.KEYCODE_MEDIA_NEXT
                || key == KeyEvent.KEYCODE_MEDIA_PREVIOUS
                || key == KeyEvent.KEYCODE_MENU;
        if (!mapped) {
            return false;
        }

        if (action == KeyEvent.ACTION_DOWN) {
            if (isSeekHoldKey(key) && isSeekHoldContext(key)) {
                stageSeekHold(key);
                render();
                return true;
            }
            if (event.getRepeatCount() > 0) {
                return true;
            }
            if (okKey) {
                okDown = true;
                holdTriggered = false;
                if (canHold()) {
                    handler.postDelayed(holdRunnable, HOLD_DELAY_MS);
                }
                return true;
            }
            cancelHold();
            return handleDown(key);
        }
        if (action == KeyEvent.ACTION_UP) {
            if (key == seekHoldKey) {
                finishSeekHold();
                render();
                return true;
            }
            if (okKey && okDown) {
                handler.removeCallbacks(holdRunnable);
                if (holdTriggered) {
                    holdTriggered = false;
                    actions.endHold();
                } else {
                    executeOkShort();
                }
                okDown = false;
                render();
                return true;
            }
            return true;
        }
        return true;
    }

    private boolean isSeekHoldKey(int key) {
        return key == KeyEvent.KEYCODE_DPAD_LEFT || key == KeyEvent.KEYCODE_DPAD_RIGHT
                || key == KeyEvent.KEYCODE_MEDIA_REWIND
                || key == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD;
    }

    private boolean isSeekHoldContext(int key) {
        if (infoOpen || focus == Focus.RAIL) return false;
        if (key == KeyEvent.KEYCODE_MEDIA_REWIND
                || key == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD) {
            return actions.isShortsPlayback();
        }
        return actions.isShortsPlayback() ? focus == Focus.SHORTS_PROGRESS
                : focus == Focus.VIDEO || focus == Focus.PROGRESS;
    }

    /** Move the indicated target during key-repeat; issue one seek only on release. */
    private void stageSeekHold(int key) {
        ExoPlayer exo = player.current();
        if (exo == null || !exo.isCurrentMediaItemSeekable()) return;
        long duration = exo.getDuration();
        if (duration <= 0L || duration == C.TIME_UNSET) return;
        if (seekHoldKey != key) {
            seekHoldKey = key;
            seekHoldOrigin = exo.getCurrentPosition();
            seekHoldStarted = android.os.SystemClock.uptimeMillis();
        }
        long heldMs = Math.max(0L, android.os.SystemClock.uptimeMillis() - seekHoldStarted);
        // Shorts are often only 20–60s: 10s skips too much. Seek only once
        // on key UP, while repeated key-down events stage a precise preview.
        boolean shortVideo = actions.isShortsPlayback();
        long base = shortVideo ? 5_000L : 10_000L;
        long midSpeed = shortVideo ? 8L : 20L;
        long highSpeed = shortVideo ? 30L : 80L;
        long distance = heldMs < 650L ? base
                : heldMs < 1_800L ? base + (heldMs - 650L) * midSpeed
                : base + 1_150L * midSpeed + (heldMs - 1_800L) * highSpeed;
        boolean backwards = key == KeyEvent.KEYCODE_DPAD_LEFT
                || key == KeyEvent.KEYCODE_MEDIA_REWIND;
        long target = backwards ? seekHoldOrigin - distance : seekHoldOrigin + distance;
        seekPreviewMs = Math.max(0L, Math.min(duration, target));
        progress.invalidate();
    }

    private void finishSeekHold() {
        ExoPlayer exo = player.current();
        if (exo != null && exo.isCurrentMediaItemSeekable() && seekPreviewMs >= 0L) {
            exo.seekTo(seekPreviewMs);
        }
        cancelSeekHold();
    }

    private void cancelSeekHold() {
        seekHoldKey = -1;
        seekPreviewMs = -1L;
    }

    private boolean handleDown(int key) {
        // TV Shorts: browsing is decoupled from playback. D-pad controls are
        // deliberately distinct from the normal player carousel.
        if (actions.isShortsPlayback() && focus != Focus.RAIL && !infoOpen) {
            switch (key) {
                case KeyEvent.KEYCODE_DPAD_UP:
                    if (focus == Focus.SHORTS_PREVIEWS) {
                        shortsStage.movePreview(-1);
                    } else if (focus == Focus.SHORTS_REACTIONS) {
                        if (shortsReactionPosition >= 3) shortsReactionPosition = 2;
                        else if (shortsReactionPosition > 0) shortsReactionPosition--;
                        else focus = Focus.SHORTS_AUTO;
                    } else if (focus == Focus.SHORTS_PROGRESS) {
                        focus = Focus.SHORTS_REACTIONS;
                        shortsReactionPosition = 2;
                    } else if (focus != Focus.SHORTS_AUTO) {
                        actions.previous();
                    }
                    render();
                    return true;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    if (focus == Focus.SHORTS_PREVIEWS) {
                        shortsStage.movePreview(1);
                    } else if (focus == Focus.SHORTS_AUTO) {
                        focus = Focus.SHORTS_REACTIONS;
                        shortsReactionPosition = 0;
                    } else if (focus == Focus.SHORTS_REACTIONS) {
                        if (shortsReactionPosition < 3) shortsReactionPosition++;
                        else focus = Focus.SHORTS_PROGRESS;
                    } else if (focus == Focus.SHORTS_PROGRESS) {
                        focus = Focus.VIDEO;
                    } else {
                        actions.next();
                    }
                    render();
                    return true;
                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                    actions.previous();
                    render();
                    return true;
                case KeyEvent.KEYCODE_MEDIA_NEXT:
                    actions.next();
                    render();
                    return true;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    if (focus == Focus.VIDEO || focus == Focus.SHORTS_PROGRESS) {
                        focus = Focus.SHORTS_REACTIONS;
                        shortsReactionPosition = 0;
                    } else if (focus == Focus.SHORTS_REACTIONS) {
                        if (shortsReactionPosition == 4) shortsReactionPosition = 3;
                        else focus = Focus.SHORTS_PREVIEWS;
                    } else if (focus == Focus.SHORTS_AUTO) {
                        focus = Focus.SHORTS_PREVIEWS;
                    } else if (focus == Focus.SHORTS_PREVIEWS) {
                        openRail();
                    }
                    render();
                    return true;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    if (focus == Focus.SHORTS_PREVIEWS) {
                        focus = Focus.SHORTS_REACTIONS;
                        shortsReactionPosition = 0;
                    } else if (focus == Focus.SHORTS_REACTIONS) {
                        if (shortsReactionPosition == 3) shortsReactionPosition = 4;
                        else focus = Focus.VIDEO;
                    } else if (focus == Focus.SHORTS_AUTO) {
                        focus = Focus.VIDEO;
                    } else if (focus == Focus.VIDEO) {
                        focus = Focus.SHORTS_PROGRESS;
                    }
                    render();
                    return true;
                default:
                    break; // BACK and MENU still use the unchanged global rail.
            }
        }
        switch (key) {
            case KeyEvent.KEYCODE_BACK:
                return handleBack();
            case KeyEvent.KEYCODE_MENU:
                openRail();
                render();
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (focus == Focus.RAIL) {
                    focus = Focus.VIDEO;
                } else if (focus == Focus.PROGRESS) {
                    moveSeekPreview(-1);
                } else if (focus == Focus.CARDS && cardPosition > 0) {
                    cardPosition--;
                    cards.scrollToPosition(cardPosition);
                } else if (focus == Focus.CONTROLS && controlPosition > 0) {
                    controlPosition--;
                } else {
                    seek(-10_000L);
                }
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (focus == Focus.RAIL) {
                    focus = actions.isShortsPlayback() ? Focus.VIDEO
                            : adapter.getItemCount() > 0 ? Focus.CARDS : Focus.CONTROLS;
                } else if (focus == Focus.PROGRESS) {
                    moveSeekPreview(1);
                } else if (focus == Focus.CARDS
                        && cardPosition < adapter.getItemCount() - 1) {
                    cardPosition++;
                    cards.scrollToPosition(cardPosition);
                } else if (focus == Focus.CONTROLS) {
                    controlPosition = Math.min(CONTROL_ICONS.length - 1, controlPosition + 1);
                } else {
                    seek(10_000L);
                }
                break;
            case KeyEvent.KEYCODE_DPAD_UP:
                if (focus == Focus.RAIL) {
                    railPosition = (railPosition + railButtons.size() - 1) % railButtons.size();
                } else if (focus == Focus.VIDEO) {
                    if (actions.isShortsPlayback()) {
                        actions.previous();
                    } else {
                        focus = Focus.CONTROLS;
                    }
                } else if (focus == Focus.CARDS) {
                    focus = Focus.PROGRESS;
                } else if (focus == Focus.PROGRESS) {
                    seekPreviewMs = -1L;
                    focus = Focus.CONTROLS;
                } else {
                    focus = Focus.VIDEO;
                }
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (focus == Focus.RAIL) {
                    railPosition = (railPosition + 1) % railButtons.size();
                } else if (focus == Focus.VIDEO) {
                    if (actions.isShortsPlayback()) {
                        actions.next();
                    } else {
                        focus = adapter.getItemCount() > 0 ? Focus.CARDS : Focus.CONTROLS;
                    }
                } else if (focus == Focus.CONTROLS) {
                    focus = Focus.PROGRESS;
                } else if (focus == Focus.PROGRESS) {
                    seekPreviewMs = -1L;
                    focus = adapter.getItemCount() > 0 ? Focus.CARDS : Focus.VIDEO;
                } else {
                    focus = Focus.VIDEO;
                }
                break;
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                actions.toggle();
                break;
            case KeyEvent.KEYCODE_MEDIA_PLAY:
                if (player.current() != null) player.current().play();
                break;
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
                if (player.current() != null) player.current().pause();
                break;
            case KeyEvent.KEYCODE_MEDIA_REWIND:
                seek(-10_000L);
                break;
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                seek(10_000L);
                break;
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                actions.previous();
                break;
            case KeyEvent.KEYCODE_MEDIA_NEXT:
                actions.next();
                break;
            default:
                return false;
        }
        render();
        return true;
    }

    private void executeOkShort() {
        // Shorts previews are selection-only until OK; meanwhile video keeps playing.
        if (actions.isShortsPlayback() && focus != Focus.RAIL) {
            if (focus == Focus.SHORTS_PREVIEWS) {
                Video selected = shortsStage.choosePreview();
                if (selected != null) actions.openVideo(selected);
            } else if (focus == Focus.SHORTS_REACTIONS) {
                if (shortsReactionPosition == 0) actions.like();
                else if (shortsReactionPosition == 1) actions.dislike();
                else if (shortsReactionPosition == 2) actions.save();
                else if (shortsReactionPosition == 3) seek(-5_000L);
                else if (shortsReactionPosition == 4) seek(5_000L);
            } else if (focus == Focus.SHORTS_AUTO) {
                shortsStage.toggleAutoNext();
            } else if (focus == Focus.SHORTS_PROGRESS) {
                ExoPlayer exo = player.current();
                if (exo != null && exo.isCurrentMediaItemSeekable()
                        && seekPreviewMs >= 0L) exo.seekTo(seekPreviewMs);
                cancelSeekHold();
            } else {
                actions.toggle();
            }
            return;
        }
        if (focus == Focus.RAIL) {
            activateRail();
        } else if (focus == Focus.CARDS && adapter.getItemCount() > cardPosition) {
            actions.openVideo(adapter.items.get(cardPosition));
        } else if (focus == Focus.CONTROLS) {
            executeControl();
        } else if (focus == Focus.PROGRESS) {
            ExoPlayer exo = player.current();
            if (exo != null && seekPreviewMs >= 0L && exo.isCurrentMediaItemSeekable()) {
                exo.seekTo(seekPreviewMs);
            }
            seekPreviewMs = -1L;
        } else {
            actions.toggle();
            // OK is also the natural way to reveal the controls; the next OK still
            // lands on play/pause, so "short OK = play/pause" stays true.
            focus = Focus.CONTROLS;
            controlPosition = CONTROL_PLAY;
        }
    }

    private boolean canHold() {
        if (actions.isShortsPlayback()) return focus == Focus.VIDEO;
        return focus == Focus.VIDEO
                || (focus == Focus.CONTROLS && controlPosition == CONTROL_PLAY);
    }

    private void cancelHold() {
        handler.removeCallbacks(holdRunnable);
        if (holdTriggered) {
            holdTriggered = false;
            actions.endHold();
        }
        okDown = false;
    }

    private void openRail() {
        cancelHold();
        cancelSeekHold();
        infoOpen = false;
        focus = Focus.RAIL;
        railEverOpened = true;
        railPosition = Math.max(0, Math.min(railPosition, railButtons.size() - 1));
    }

    /** Stage a coarse seek, retaining the original playback position until OK. */
    private void moveSeekPreview(int direction) {
        ExoPlayer exo = player.current();
        if (exo == null || !exo.isCurrentMediaItemSeekable()) return;
        long duration = exo.getDuration();
        if (duration <= 0L || duration == C.TIME_UNSET) return;
        long base = seekPreviewMs >= 0L ? seekPreviewMs : exo.getCurrentPosition();
        long step = Math.max(10_000L, duration / 20L);
        long offset = direction < 0 ? -step : step;
        seekPreviewMs = Math.max(0L, Math.min(duration, base + offset));
        progress.invalidate();
    }

    private void seek(long delta) {
        ExoPlayer exo = player.current();
        if (exo == null || !exo.isCurrentMediaItemSeekable()) {
            return;
        }
        long duration = exo.getDuration();
        long target = Math.max(0L, exo.getCurrentPosition() + delta);
        if (duration > 0 && duration != C.TIME_UNSET) {
            target = Math.min(duration, target);
        }
        exo.seekTo(target);
    }

    private void executeControl() {
        switch (controlPosition) {
            case CONTROL_LIKE:
                actions.like();
                break;
            case CONTROL_DISLIKE:
                actions.dislike();
                break;
            case CONTROL_PREVIOUS:
                actions.previous();
                break;
            case CONTROL_REWIND:
                seek(-10_000L);
                break;
            case CONTROL_PLAY:
                actions.toggle();
                break;
            case CONTROL_FORWARD:
                seek(10_000L);
                break;
            case CONTROL_NEXT:
                actions.next();
                break;
            case CONTROL_CC:
                actions.captions();
                break;
            case CONTROL_INFO:
                infoOpen = !infoOpen;
                if (infoOpen) {
                    updateInfo();
                }
                break;
            case CONTROL_HD:
                actions.quality();
                break;
            case CONTROL_FULLSCREEN:
                actions.fullscreen();
                break;
            default:
                break;
        }
    }

    private void activateRail() {
        if (railPosition < 0 || railPosition >= railDestinations.size()) {
            return;
        }
        int destination = railDestinations.get(railPosition).destination;
        if (destination == -18) {
            shortsStage.toggleAutoNext();
            focus = Focus.VIDEO;
            render();
            return;
        }
        actions.navigate(destination);
    }

    private void render() {
        if (released) {
            return;
        }
        root.setVisibility(pip ? View.GONE : View.VISIBLE);
        boolean shortsMode = actions.isShortsPlayback();
        if (lastShortsPositioned != (shortsMode && !pip)) {
            lastShortsPositioned = shortsMode && !pip;
            actions.positionShortsVideo(lastShortsPositioned);
        }
        shortsStage.setPreviewsFocused(shortsMode && focus == Focus.SHORTS_PREVIEWS);
        shortsStage.setReactionsFocused(shortsMode && focus == Focus.SHORTS_REACTIONS,
                shortsReactionPosition);
        shortsStage.setAutoFocused(shortsMode && focus == Focus.SHORTS_AUTO);
        shortsStage.setProgressFocused(shortsMode && focus == Focus.SHORTS_PROGRESS);
        shortsStage.show(shortsMode && !pip, player.current(), seekPreviewMs);
        railPosition = Math.min(railPosition,
                shortsMode ? railButtons.size() - 1 : railButtons.size() - 2);
        // Dedicated Shorts canvas has no regular-video carousel/control overlay.
        boolean panelVisible = !pip && !shortsMode
                && focus != Focus.VIDEO && focus != Focus.RAIL;
        panel.setVisibility(panelVisible ? View.VISIBLE : View.GONE);
        clock.setVisibility(!pip && !shortsMode && (panelVisible || focus == Focus.RAIL || infoOpen)
                ? View.VISIBLE : View.GONE);
        boolean railOpen = !pip && focus == Focus.RAIL;
        int desiredRailVisibility = railOpen ? View.VISIBLE : View.GONE;
        if (rail.getVisibility() != desiredRailVisibility) {
            rail.animate().cancel();
            if (railOpen) {
                rail.setTranslationX(-dp(com.newtube.mobile.ui.common.MarsTvNavigationTheme.WIDTH_DP));
                rail.setVisibility(View.VISIBLE);
                rail.animate().translationX(0f).setDuration(150L).start();
            } else {
                rail.setVisibility(View.GONE);
                rail.setTranslationX(0f);
            }
        }
        railHint.setVisibility(View.GONE); // no clipped labels on narrow video rail
        infoPanel.setVisibility(!pip && infoOpen ? View.VISIBLE : View.GONE);

        if (focus == Focus.RAIL) {
            railHint.setText(railDestinations.get(railPosition).label);
        }
        for (int i = 0; i < railButtons.size(); i++) {
            if (railDestinations.get(i).destination == -18) {
                railButtons.get(i).setVisibility(shortsMode ? View.VISIBLE : View.GONE);
                railButtons.get(i).setContentDescription(shortsStage.isAutoNext()
                        ? "Shorts Automatisch weiter: Ein" : "Shorts Automatisch weiter: Aus");
            }
            boolean selected = focus == Focus.RAIL && railPosition == i;
            railButtons.get(i).setTextColor(selected ? CYAN : WHITE);
            railButtons.get(i).setBackground(
                    com.newtube.mobile.ui.common.MarsTvNavigationTheme.itemBackground(
                            selected, activity.getResources().getDisplayMetrics().density));
        }
        for (int i = 0; i < controlButtons.size(); i++) {
            boolean selected = focus == Focus.CONTROLS && controlPosition == i;
            controlButtons.get(i).setColorFilter(selected ? CYAN : WHITE);
            controlButtons.get(i).setBackground(
                    buttonChrome(selected, i == CONTROL_PLAY));
            controlButtons.get(i).setPadding(dp(4), dp(4), dp(4), dp(4));
        }

        int highlight = focus == Focus.CARDS ? cardPosition : -1;
        if (lastHighlightedCard != highlight) {
            if (lastHighlightedCard >= 0 && lastHighlightedCard < adapter.getItemCount()) {
                adapter.notifyItemChanged(lastHighlightedCard);
            }
            if (highlight >= 0 && highlight < adapter.getItemCount()) {
                adapter.notifyItemChanged(highlight);
            }
            lastHighlightedCard = highlight;
        }

        progress.invalidate();
        handler.removeCallbacks(autoHide);
        if (!infoOpen && (focus == Focus.CARDS || focus == Focus.CONTROLS)) {
            handler.postDelayed(autoHide, AUTO_HIDE_MS);
        }
    }

    private static String formatTime(long ms) {
        long seconds = Math.max(0L, ms) / 1000;
        long minutes = seconds / 60;
        if (minutes >= 60) {
            return String.format(Locale.ROOT, "%d:%02d:%02d",
                    minutes / 60, minutes % 60, seconds % 60);
        }
        return String.format(Locale.ROOT, "%02d:%02d", minutes, seconds % 60);
    }

    private void tick() {
        if (released) {
            return;
        }
        ExoPlayer exo = player.current();
        long duration = exo == null ? 0 : exo.getDuration();
        long position = seekPreviewMs >= 0L ? seekPreviewMs
                : exo == null ? 0 : exo.getCurrentPosition();
        elapsed.setText(formatTime(position));
        remaining.setText(duration <= 0 || duration == C.TIME_UNSET
                ? "--:--" : formatTime(Math.max(0L, duration - position)));
        clock.setText(new SimpleDateFormat("HH:mm", Locale.GERMANY).format(new Date()));
        boolean playing = exo != null && exo.getPlayWhenReady();
        playPauseIcon.setImageResource(playing
                ? R.drawable.mars_tv_ic_pause : R.drawable.mars_tv_ic_play);
        if (infoOpen) {
            updateInfo();
        }
        if (panel.getVisibility() == View.VISIBLE) {
            progress.invalidate();
        }
        boolean shorts = actions.isShortsPlayback();
        shortsStage.show(shorts && !pip, exo, seekPreviewMs);
        if (shorts && exo != null
                && exo.getPlaybackState() == androidx.media3.common.Player.STATE_ENDED) {
            if (shortsAutoAdvanceArmed) {
                shortsAutoAdvanceArmed = false;
                if (shortsStage.isAutoNext()
                        && com.newtube.mobile.ui.common.MarsShortsSession.hasNext()) {
                    actions.next();
                }
            }
        } else if (exo != null
                && exo.getPlaybackState() == androidx.media3.common.Player.STATE_READY) {
            shortsAutoAdvanceArmed = true;
        }
        handler.postDelayed(ticker, 1000L);
    }

    private void updateInfo() {
        ExoPlayer exo = player.current();
        Format video = exo != null ? exo.getVideoFormat() : null;
        StringBuilder text = new StringBuilder();
        if (video != null && video.height > 0) {
            text.append(video.width).append('x').append(video.height)
                    .append(" · ").append(Math.round(video.frameRate)).append(" fps")
                    .append(" · ").append(codecLabel(video.codecs, video.sampleMimeType))
                    .append(isHdr(video) ? " · HDR" : " · SDR");
            if (video.bitrate > 0) {
                text.append('\n').append("Bitrate: ")
                        .append(String.format(Locale.GERMANY, "%.1f Mbit/s",
                                video.bitrate / 1_000_000f));
            }
        } else {
            text.append("Videoformat wird ermittelt");
        }
        String protocol = actions.sourceProtocol();
        if (!TextUtils.isEmpty(protocol)) {
            text.append('\n').append("Protokoll: ").append(protocol);
        }
        String diagnostics = actions.sourceDiagnostics();
        if (!TextUtils.isEmpty(diagnostics)) {
            text.append('\n').append(diagnostics);
        }
        text.append('\n').append("Puffer: ").append(bufferLabel(
                MarsTvPlayerPrefs.getBufferPreset(activity)));
        infoText.setText(text.toString());
    }

    private static String bufferLabel(int value) {
        switch (value) {
            case MarsTvPlayerPrefs.BUFFER_LOW:
                return "Klein";
            case MarsTvPlayerPrefs.BUFFER_HIGH:
                return "Groß";
            case MarsTvPlayerPrefs.BUFFER_NORMAL:
            default:
                return "Normal";
        }
    }

    private static String codecLabel(String codecs, String mime) {
        String value = codecs != null ? codecs : (mime != null ? mime : "");
        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.contains("vp09") || lower.contains("vp9")) {
            return "VP9";
        }
        if (lower.contains("av01")) {
            return "AV1";
        }
        if (lower.contains("avc") || lower.contains("h264")) {
            return "H.264";
        }
        return value;
    }

    private static boolean isHdr(Format format) {
        return format.colorInfo != null
                && format.colorInfo.colorTransfer != C.COLOR_TRANSFER_SDR
                && format.colorInfo.colorTransfer != Format.NO_VALUE;
    }

    public void release() {
        released = true;
        cancelSeekHold();
        actions.positionShortsVideo(false);
        handler.removeCallbacks(ticker);
        handler.removeCallbacks(autoHide);
        handler.removeCallbacks(holdRunnable);
        if (holdTriggered) {
            holdTriggered = false;
            actions.endHold();
        }
        adapter.items.clear();
        root.removeAllViews();
        ViewGroup parent = (ViewGroup) root.getParent();
        if (parent != null) {
            parent.removeView(root);
        }
    }

    private final class Carousel extends RecyclerView.Adapter<Carousel.Holder> {
        private List<Video> items = new ArrayList<>();

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout card = new LinearLayout(activity);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setFocusable(false);
            RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(
                    cardWidthPx, thumbnailHeightPx + dp(34));
            lp.rightMargin = gapPx;
            card.setLayoutParams(lp);

            FrameLayout imageFrame = new FrameLayout(activity);
            imageFrame.setBackground(shape(0xFF0A1B2B, 0, 0, 12));
            imageFrame.setClipToOutline(true);
            ImageView image = new ImageView(activity);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setBackground(shape(0xFF1C3447, 0, 0, 12));
            image.setClipToOutline(true);
            imageFrame.addView(image, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            TextView badge = text(11, WHITE);
            badge.setGravity(Gravity.CENTER);
            badge.setPadding(dp(5), dp(2), dp(5), dp(2));
            badge.setBackground(shape(0xDB071521, 0, 0, 6));
            FrameLayout.LayoutParams badgeLp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, dp(22), Gravity.RIGHT | Gravity.BOTTOM);
            badgeLp.rightMargin = dp(6);
            badgeLp.bottomMargin = dp(5);
            imageFrame.addView(badge, badgeLp);
            card.addView(imageFrame, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, thumbnailHeightPx));
            TextView title = text(12, WHITE);
            title.setGravity(Gravity.TOP);
            title.setMaxLines(1);
            title.setEllipsize(TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(26));
            titleLp.topMargin = dp(4);
            card.addView(title, titleLp);
            Holder holder = new Holder(card, imageFrame, image, badge, title);
            card.setOnClickListener(v -> {
                int position = holder.getBindingAdapterPosition();
                if (position == RecyclerView.NO_POSITION || position >= items.size()) {
                    return;
                }
                cardPosition = position;
                focus = Focus.CARDS;
                render();
                actions.openVideo(items.get(position));
            });
            return holder;
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            Video video = items.get(position);
            holder.title.setText(video.getTitle());
            String badgeText = video.isLive ? "LIVE" : video.badge;
            holder.badge.setText(badgeText);
            holder.badge.setVisibility(badgeText == null || badgeText.isEmpty()
                    ? View.GONE : View.VISIBLE);
            boolean selected = focus == Focus.CARDS && position == cardPosition;
            holder.imageFrame.setBackground(selected
                    ? shape(0xFF153653, CYAN, 2, 12)
                    : shape(0xFF0A1B2B, 0, 0, 12));
            holder.imageFrame.setPadding(dp(3), dp(3), dp(3), dp(3));
            Glide.with(activity).load(video.getCardImageUrl())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .format(DecodeFormat.PREFER_RGB_565)
                    .override(cardWidthPx, thumbnailHeightPx)
                    .centerCrop()
                    .dontAnimate()
                    .into(holder.image);
        }

        @Override
        public void onViewRecycled(@NonNull Holder holder) {
            Glide.with(activity).clear(holder.image);
            super.onViewRecycled(holder);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        final class Holder extends RecyclerView.ViewHolder {
            final FrameLayout imageFrame;
            final ImageView image;
            final TextView badge;
            final TextView title;

            Holder(View view, FrameLayout frame, ImageView imageView, TextView badgeView,
                    TextView titleView) {
                super(view);
                imageFrame = frame;
                image = imageView;
                badge = badgeView;
                title = titleView;
            }
        }
    }
}
