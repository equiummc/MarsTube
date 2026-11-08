package com.newtube.mobile.ui.playback;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * TV-only right-hand player submenus. This is just a lightweight overlay:
 * NO Player, Surface, decoder, network request, WebView or bitmap duplication.
 * Existing YouTube/Media3 actions remain the sole source of truth.
 *
 * An action that needs account state must go through the existing channel
 * screen; we do not invent subscription or notification status.
 */
public final class MarsTvPlayerPanels034 {
    public interface Action {
        void invoke(String id);
        default java.util.List<String> choices(String page) {
            return java.util.Collections.emptyList();
        }
        default void choose(String page, int index, String videoId) { }
        default boolean subscribed() { return false; }
    }

    private static final int BLUE = 0xFF16B8FF;
    private static final int WHITE = 0xFFF4F8FF;
    private static final int MUTED = 0xFFAFC5DB;
    private static final int DARK = 0xFF081729;
    private final Activity activity;
    private final FrameLayout root;
    private final FrameLayout sheet;
    private final LinearLayout rows;
    private final ScrollView scroller;
    private final TextView heading;
    private final TextView subheading;
    private final Action onAction;
    private final Runnable onStateChanged;
    private final List<Option> options = new ArrayList<>();
    private final List<TextView> buttons = new ArrayList<>();
    private int selected;
    private boolean visible;
    private boolean subscribed;
    private boolean audioAvailable;
    private String channelName = "Aktuelles Video";
    private String videoTitle = "";
    private String videoId;
    private String page = "";
    private final ArrayDeque<String> backStack = new ArrayDeque<>();

    private static final class Option {
        final String name;
        final String detail;
        final String action;
        Option(String name, String detail, String action) {
            this.name = name;
            this.detail = detail;
            this.action = action;
        }
    }

    private int dp(float value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    private static GradientDrawable fill(int color, int border, int radius) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(radius);
        if (border != 0) bg.setStroke(1, border);
        return bg;
    }

    public MarsTvPlayerPanels034(Activity activity, FrameLayout host,
                                  Runnable onStateChanged, Action onAction) {
        this.activity = activity;
        this.root = host;
        this.onStateChanged = onStateChanged;
        this.onAction = onAction;
        sheet = new FrameLayout(activity);
        sheet.setBackground(new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xF2092039, 0xFB061527}));
        sheet.setFocusable(false);
        final int screenWidth = activity.getResources().getDisplayMetrics().widthPixels;
        FrameLayout.LayoutParams sheetLp = new FrameLayout.LayoutParams(
                Math.round(screenWidth * 0.41f), ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.RIGHT | Gravity.TOP);
        root.addView(sheet, sheetLp);

        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(22), dp(30), dp(24), dp(22));
        sheet.addView(content, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        heading = new TextView(activity);
        heading.setTextSize(25f);
        heading.setTextColor(WHITE);
        heading.setTypeface(null, android.graphics.Typeface.BOLD);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        content.addView(heading, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(42)));
        subheading = new TextView(activity);
        subheading.setTextSize(13f);
        subheading.setTextColor(MUTED);
        subheading.setMaxLines(2);
        subheading.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams subtitleLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(45));
        subtitleLp.bottomMargin = dp(12);
        content.addView(subheading, subtitleLp);

        View divider = new View(activity);
        divider.setBackgroundColor(0x7753B7E8);
        LinearLayout.LayoutParams dividerLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        dividerLp.bottomMargin = dp(14);
        content.addView(divider, dividerLp);

        scroller = new ScrollView(activity);
        scroller.setFillViewport(false);
        scroller.setVerticalScrollBarEnabled(false);
        scroller.setFocusable(false);
        rows = new LinearLayout(activity);
        rows.setOrientation(LinearLayout.VERTICAL);
        scroller.addView(rows);
        content.addView(scroller, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        TextView footer = new TextView(activity);
        footer.setText("↑ ↓ Auswahl    OK Bestätigen    ← Zurück");
        footer.setTextSize(11f);
        footer.setTextColor(MUTED);
        footer.setGravity(Gravity.CENTER_VERTICAL);
        content.addView(footer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(32)));
        sheet.setVisibility(View.GONE);
    }

    private void add(String name, String detail, String action) {
        options.add(new Option(name, detail, action));
    }

    public boolean isOpen() {
        return visible;
    }

    public void setCapabilities(boolean isSubscribed, boolean hasAudioAlternatives) {
        subscribed = isSubscribed;
        audioAvailable = hasAudioAlternatives;
    }

    public String videoId() { return videoId; }

    /** Opening from transport starts a new stack; child pages keep Back. */
    public void open(String kind, @Nullable Video video) {
        backStack.clear();
        if (video != null) {
            videoId = video.videoId;
            channelName = TextUtils.isEmpty(video.getAuthor()) ? "Unbekannter Kanal" : video.getAuthor();
            videoTitle = video.getTitle() == null ? "" : video.getTitle();
        }
        showPage(kind);
    }

    private void goTo(String kind) {
        if (!visible) return;
        backStack.push(page);
        showPage(kind);
    }

    private void showPage(String kind) {
        page = kind;
        options.clear();
        selected = 0;
        heading.setText(kind);
        subheading.setText("Kanal".equals(kind) ? channelName : videoTitle);
        switch (kind) {
            case "Kanal":
                add(channelName, "Kanal öffnen und Videos ansehen", "channel");
                add(onAction.subscribed() ? "Abonniert · Abo ändern" : "Abonnieren",
                    "Anmeldung erforderlich", "subscribe");
                add("Benachrichtigungen", "Glocke auf Kanalseite verwalten", "channel");
                break;
            case "Kommentare":
                add("Kommentare öffnen", "Rechts neben dem laufenden Video", "comments");
                break;
            case "Merken":
                add("Zu Playlist hinzufügen", "Wiedergabeliste auswählen", "save");
                add("Später ansehen", "Über Wiedergabelisten auswählen", "save");
                break;
            case "Einstellungen":
                add("Videoqualität", "Auflösung dieses Videos", "page:Qualität");
                if (audioAvailable) add("Audiotrack", "Sprache dieses Videos", "page:Audiotrack");
                add("Untertitel", "Sprache und Ein/Aus", "page:Untertitel");
                add("Wiedergabegeschwindigkeit", "Tempo auswählen", "page:Tempo");
                add("Untertitel-Design", "Schrift und Darstellung", "caption_style");
                add("Videoinformationen", "Codec, Stream und Puffer", "info");
                break;
            case "Qualität":
            case "Audiotrack":
            case "Untertitel":
            case "Tempo": {
                java.util.List<String> choices = onAction.choices(kind);
                for (int i = 0; i < choices.size(); i++) {
                    add(choices.get(i), "", "choose:" + kind + ":" + i);
                }
                if (choices.isEmpty()) add("Keine Auswahl verfügbar",
                        "Für dieses Video nicht verfügbar", "");
                break;
            }
            case "Mehr":
            default:
                add("Kanal", "Kanal, Abonnement und Glocke", "page:Kanal");
                add("Kommentare", "Diskussion zum aktuellen Video", "page:Kommentare");
                add("Merken", "Playlists und Später ansehen", "page:Merken");
                add("Einstellungen", "Video, Audio, Untertitel und Tempo", "page:Einstellungen");
                add("Gefällt mir", "Video positiv bewerten", "like");
                add("Gefällt mir nicht", "Video negativ bewerten", "dislike");
                add("Nicht interessiert", "Empfehlungsoptionen öffnen", "no_interest");
                add("Vollbild", "Untermenü schließen", "fullscreen");
                break;
        }
        rows.removeAllViews();
        buttons.clear();
        for (int index = 0; index < options.size(); index++) {
            final int position = index;
            Option item = options.get(index);
            TextView button = new TextView(activity);
            String title = item.name;
            if (item.action.startsWith("page:")) title = title + "   ›";
            button.setText(title + (TextUtils.isEmpty(item.detail) ? "" : "\n" + item.detail));
            button.setTextSize(item.detail.isEmpty() ? 17f : 16f);
            button.setMaxLines(2);
            button.setGravity(Gravity.CENTER_VERTICAL);
            button.setPadding(dp(18), dp(6), dp(14), dp(6));
            button.setFocusable(false);
            button.setClickable(true);
            button.setOnClickListener(v -> {
                selected = position;
                activateSelected();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(item.detail.isEmpty() ? 49 : 64));
            lp.bottomMargin = dp(5);
            rows.addView(button, lp);
            buttons.add(button);
        }
        visible = true;
        sheet.setVisibility(View.VISIBLE);
        root.bringChildToFront(sheet);
        refreshSelection();
        onStateChanged.run();
    }

    private void refreshSelection() {
        for (int i = 0; i < buttons.size(); i++) {
            TextView button = buttons.get(i);
            boolean active = i == selected;
            button.setTextColor(active ? DARK : WHITE);
            button.setBackground(fill(active ? 0xFFF1F8FF : 0xD31A2F48,
                    active ? BLUE : 0x004A78A7, dp(12)));
        }
        if (selected >= 0 && selected < buttons.size()) {
            final View target = buttons.get(selected);
            scroller.post(() -> {
                if (visible) scroller.smoothScrollTo(0, target.getTop() - dp(10));
            });
        }
    }

    private void activateSelected() {
        if (!visible || selected < 0 || selected >= options.size()) return;
        String id = options.get(selected).action;
        if (id.isEmpty()) return;
        if (id.startsWith("page:")) {
            goTo(id.substring(5));
            return;
        }
        if (id.startsWith("choose:")) {
            int at = id.lastIndexOf(':');
            String target = id.substring(7, at);
            int index = Integer.parseInt(id.substring(at + 1));
            onAction.choose(target, index, videoId);
            if (visible) showPage(target);
            return;
        }
        if (id.equals("subscribe")) {
            onAction.invoke(id);
            if (visible) showPage("Kanal");
            return;
        }
        close();
        onAction.invoke(id);
    }

    /** BACK returns to the previous submenu, not immediately to the movie. */
    public void back() {
        if (!visible) return;
        if (backStack.isEmpty()) close();
        else showPage(backStack.pop());
    }

    public void close() {
        if (!visible) return;
        visible = false;
        backStack.clear();
        sheet.setVisibility(View.GONE);
        onStateChanged.run();
    }

    public void dispose() {
        visible = false;
        sheet.setVisibility(View.GONE);
        rows.removeAllViews();
        if (sheet.getParent() instanceof ViewGroup) {
            ((ViewGroup) sheet.getParent()).removeView(sheet);
        }
    }

    /** Returns false for hardware media keys, so the actual player still receives them. */
    public boolean handle(KeyEvent event) {
        if (!visible) return false;
        final int key = event.getKeyCode();
        boolean navigation = key == KeyEvent.KEYCODE_DPAD_UP
                || key == KeyEvent.KEYCODE_DPAD_DOWN
                || key == KeyEvent.KEYCODE_DPAD_LEFT
                || key == KeyEvent.KEYCODE_DPAD_RIGHT
                || key == KeyEvent.KEYCODE_DPAD_CENTER
                || key == KeyEvent.KEYCODE_ENTER
                || key == KeyEvent.KEYCODE_BACK
                || key == KeyEvent.KEYCODE_MENU;
        if (!navigation) return false;
        if (event.getAction() == KeyEvent.ACTION_UP) return true;
        if (event.getRepeatCount() > 0) return true;
        switch (key) {
            case KeyEvent.KEYCODE_BACK:
            case KeyEvent.KEYCODE_MENU:
            case KeyEvent.KEYCODE_DPAD_LEFT:
                back();
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
                selected = Math.max(0, selected - 1);
                refreshSelection();
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                selected = Math.min(buttons.size() - 1, selected + 1);
                refreshSelection();
                return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                activateSelected();
                return true;
            default:
                return true;
        }
    }
}
