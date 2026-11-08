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
public final class MarsTvPlayerPanels033 {
    public interface Action {
        void invoke(String id);
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

    public MarsTvPlayerPanels033(Activity activity, FrameLayout host,
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
        footer.setText("↑ ↓ Auswahl     OK Öffnen     ← Zurück");
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

    public void open(String kind, @Nullable Video video) {
        options.clear();
        selected = 0;
        String channel = video == null || TextUtils.isEmpty(video.getAuthor())
                ? "Aktuelles Video" : video.getAuthor();
        heading.setText(kind);
        subheading.setText(channel);
        switch (kind) {
            case "Kanal":
                add("Kanal öffnen", "Kanal ansehen und Abo verwalten", "channel");
                add("Abonnement", "Abo-Status auf der Kanalseite prüfen", "channel");
                add("Benachrichtigungen", "Glocke auf der Kanalseite einstellen", "channel");
                break;
            case "Kommentare":
                add("Kommentare anzeigen", "Kommentare und Antworten zum Video", "comments");
                break;
            case "Merken":
                add("Zu Playlist hinzufügen", "Bestehende Wiedergabeliste auswählen", "save");
                add("Später ansehen", "Über die vorhandene Speicherfunktion", "save");
                break;
            case "Einstellungen":
                add("Qualität", "Verfügbare echte Videoauflösungen", "quality");
                add("Untertitel", "Verfügbare Untertitelspuren", "captions");
                add("Geschwindigkeit", "Abspieltempo einstellen", "speed");
                add("Videoinformationen", "Codec, Auflösung, Bitrate und Puffer", "info");
                break;
            case "Mehr":
            default:
                add("Kanal", "Kanaldetails und Abonnement", "channel_panel");
                add("Kommentare", "Zum Diskussionsbereich", "comments_panel");
                add("Merken", "Playlist oder Später ansehen", "save_panel");
                add("Einstellungen", "Qualität, Untertitel und Tempo", "settings_panel");
                add("Gefällt mir", "Dieses Video bewerten", "like");
                add("Gefällt mir nicht", "Dieses Video bewerten", "dislike");
                add("Nicht interessiert", "Empfehlungsoptionen öffnen", "no_interest");
                add("Vollbild", "Zum laufenden Video zurück", "fullscreen");
                break;
        }
        rows.removeAllViews();
        buttons.clear();
        for (int index = 0; index < options.size(); index++) {
            final int position = index;
            Option item = options.get(index);
            TextView button = new TextView(activity);
            button.setText(item.name + (TextUtils.isEmpty(item.detail)
                    ? "" : "\n" + item.detail));
            button.setTextSize(17f);
            button.setMaxLines(2);
            button.setGravity(Gravity.CENTER_VERTICAL);
            button.setPadding(dp(15), dp(7), dp(12), dp(7));
            button.setFocusable(false);
            button.setClickable(true);
            button.setOnClickListener(v -> {
                selected = position;
                activateSelected();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(64));
            lp.bottomMargin = dp(7);
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
        if (id.endsWith("_panel")) {
            switch (id) {
                case "channel_panel": open("Kanal", null); return;
                case "comments_panel": open("Kommentare", null); return;
                case "save_panel": open("Merken", null); return;
                case "settings_panel": open("Einstellungen", null); return;
                default: break;
            }
        }
        close();
        onAction.invoke(id);
    }

    public void close() {
        if (!visible) return;
        visible = false;
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
                close();
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
