package com.newtube.mobile.ui.common;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * MarsTube TV-only persistent navigation. Kept outside the video player and
 * telephone UI. The hidden source BottomNavigationView remains the real
 * data/section controller; a single vertical rail is the TV control surface.
 */
public final class MarsTvRail extends LinearLayout {
    private static final String TAG = "mars.tv.rail.v015";
    private static final int BLUE = Color.rgb(48, 157, 250);
    private static final int BG = Color.rgb(13, 21, 37);
    private static final int SELECTED = Color.rgb(24, 55, 93);
    private final Activity activity;
    private final boolean expandedSettings;
    private final List<TextView> actions = new ArrayList<>();
    private WeakReference<View> previousFocus = new WeakReference<>(null);
    private TextView miniAction;

    private MarsTvRail(Activity activity) {
        super(activity);
        this.activity = activity;
        expandedSettings = "MobileSettingsActivity".equals(activity.getClass().getSimpleName());
        setTag(TAG);
        setOrientation(VERTICAL);
        setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        setBackground(MarsTvNavigationTheme.railBackground());
        setFocusable(false);
        setClipToPadding(false);
        setPadding(dp(4), dp(10), dp(4), dp(6));

        android.widget.ImageView branding = new android.widget.ImageView(activity);
        branding.setImageResource(com.liskovsoft.smartyoutubetv2.tv.R.drawable.mars_m_logo);
        branding.setContentDescription("MarsTube");
        branding.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
        addView(branding, new LinearLayout.LayoutParams(-1, dp(38)));
        TextView version = new TextView(activity);
        version.setText("0.0.15");
        version.setTextSize(11);
        version.setTextColor(Color.LTGRAY);
        version.setGravity(Gravity.CENTER);
        addView(version, new LinearLayout.LayoutParams(-1, dp(16)));

        addAction("Start", 0);
        addAction("Shorts", 8);
        addAction("Suche", 1);
        addAction("Abos", 2);
        addAction("Verlauf", 3);
        // Downloads remain phone-only. The running-video shortcut appears dynamically
        // only while there is an actual, reachable mini playback session.
        addAction("Profil", 6);
        addAction("Einst.", 5);
        syncMiniAction();
    }

    /**
     * Only show "Laufendes Video" when a playable in-app mini session exists.
     * A dynamic entry is accessible from any Browse focus position via the TV rail.
     */
    private void syncMiniAction() {
        View mini = activity.findViewById(
                com.liskovsoft.smartyoutubetv2.tv.R.id.mobile_mini_player);
        boolean enabled = com.newtube.mobile.player.MarsTvPlayerPrefs
                .isMiniPlayerEnabled(activity);
        boolean active = enabled && mini != null
                && mini.getVisibility() == View.VISIBLE
                && com.newtube.mobile.ui.playback.MiniPlayerBridge.isActive();
        if (active && miniAction == null) {
            addAction("Laufendes Video", 7);
            miniAction = actions.get(actions.size() - 1);
        } else if (!active && miniAction != null) {
            if (miniAction.hasFocus() && !actions.isEmpty()) {
                actions.get(0).requestFocus();
            }
            actions.remove(miniAction);
            removeView(miniAction);
            miniAction = null;
        }
    }

    public static void updateMini(Activity activity) {
        MarsTvRail rail = get(activity);
        if (rail != null) rail.syncMiniAction();
    }

    private int dp(float d) {
        return (int) (d * getResources().getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable focusBackground(boolean focus) {
        return MarsTvNavigationTheme.itemBackground(focus,
                getResources().getDisplayMetrics().density);
    }

    private void addAction(String label, int destination) {
        TextView button = new TextView(activity);
        button.setText(expandedSettings ? (label.equals("Einst.") ? "Einstellungen" : label) : "");
        button.setTextSize(expandedSettings ? 10 : 9);
        button.setMaxLines(1);
        button.setEllipsize(android.text.TextUtils.TruncateAt.END);
        int iconRes = MarsTvNavigationTheme.iconFor(destination);
        // Never trust icon intrinsic sizes (search icon used to overlap the rail).
        android.graphics.drawable.Drawable glyph =
                androidx.core.content.ContextCompat.getDrawable(activity, iconRes);
        if (glyph != null) {
            glyph = glyph.mutate();
            glyph.setBounds(0, 0, dp(20), dp(20));
            button.setCompoundDrawables(null, glyph, null, null);
        }
        button.setCompoundDrawableTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));
        button.setCompoundDrawablePadding(expandedSettings ? dp(4) : 0);
        button.setTextColor(Color.WHITE);
        button.setGravity(Gravity.CENTER);
        button.setContentDescription(label.equals("Einst.") ? "Einstellungen" : label);
        button.setFocusable(true);
        button.setFocusableInTouchMode(false);
        button.setBackground(focusBackground(false));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1,
                dp(expandedSettings ? 58 : MarsTvNavigationTheme.ITEM_HEIGHT_DP));
        params.bottomMargin = dp(3);
        addView(button, params);
        actions.add(button);
        button.setOnFocusChangeListener((v, focused) ->
                button.setBackground(focusBackground(focused)));
        button.setOnClickListener(v -> {
            android.util.Log.i("MarsTubeTV", "rail destination=" + destination);
            navigate(destination);
        });
    }

    private void navigate(int destination) {
        if (destination == 7) {
            View mini = activity.findViewById(com.liskovsoft.smartyoutubetv2.tv.R.id.mobile_mini_player);
            if (mini != null && mini.getVisibility() == View.VISIBLE
                    && com.newtube.mobile.ui.playback.MiniPlayerBridge.isActive()) {
                mini.performClick();
            } else {
                android.widget.Toast.makeText(activity, "Kein Miniplayer aktiv",
                        android.widget.Toast.LENGTH_SHORT).show();
            }
        } else if (destination == 1) {
            activity.startActivity(new Intent(activity,
                    com.newtube.mobile.ui.search.MobileSearchActivity.class));
        } else if (destination == 5) {
            activity.startActivity(com.newtube.mobile.ui.settings.MobileSettingsActivity.intent(activity, null));
        } else if (activity instanceof com.newtube.mobile.ui.browse.MobileBrowseActivity) {
            ((com.newtube.mobile.ui.browse.MobileBrowseActivity) activity).marsTvRoute(destination);
        } else {
            Intent intent = new Intent(activity, com.newtube.mobile.ui.browse.MobileBrowseActivity.class);
            intent.putExtra("mars_tv_section", destination);
            activity.startActivity(intent);
        }
    }

    public static void install(MobileActivity activity) {
        if (!MarsDeviceMode.isTelevision(activity)) return;
        String name = activity.getClass().getSimpleName();
        if (!("MobileBrowseActivity".equals(name)
                || "MobileSearchActivity".equals(name)
                || "MobileSettingsActivity".equals(name)
                || "MobileChannelActivity".equals(name)
                || "MobileChannelUploadsActivity".equals(name))) return;
        View contentView = activity.findViewById(android.R.id.content);
        if (!(contentView instanceof FrameLayout)) return;
        FrameLayout content = (FrameLayout) contentView;
        if (content.findViewWithTag(TAG) != null || content.getChildCount() == 0) return;
        View page = content.getChildAt(0);
        final int railWidth = (int) ((("MobileSettingsActivity".equals(name)
                ? MarsTvNavigationTheme.SETTINGS_WIDTH_DP : MarsTvNavigationTheme.WIDTH_DP)
                * activity.getResources().getDisplayMetrics().density) + 0.5f);
        page.setPadding(page.getPaddingLeft() + railWidth, page.getPaddingTop(),
                page.getPaddingRight(), page.getPaddingBottom());
        // Keep the original bottom nav's menu and presenter alive, but never show
        // two contradictory navigations on TV.
        View bottom = activity.findViewById(com.liskovsoft.smartyoutubetv2.tv.R.id.mobile_bottom_nav);
        if (bottom != null) bottom.setVisibility(View.GONE);
        MarsTvRail rail = new MarsTvRail(activity);
        FrameLayout.LayoutParams layout = new FrameLayout.LayoutParams(railWidth,
                ViewGroup.LayoutParams.MATCH_PARENT, Gravity.START);
        content.addView(rail, layout);
        android.util.Log.i("MarsTubeTV", "rail installed activity=" + name);
    }

    private static MarsTvRail get(Activity activity) {
        View content = activity.findViewById(android.R.id.content);
        if (content == null) return null;
        View rail = content.findViewWithTag(TAG);
        return rail instanceof MarsTvRail ? (MarsTvRail) rail : null;
    }

    public static boolean isInstalled(Activity activity) {
        return get(activity) != null;
    }

    public static boolean focus(Activity activity) {
        MarsTvRail rail = get(activity);
        if (rail == null || rail.actions.isEmpty()) return false;
        rail.syncMiniAction();
        View old = activity.getCurrentFocus();
        if (old != null && !rail.containsFocus(old)) rail.previousFocus = new WeakReference<>(old);
        View selected = rail.actions.get(rail.expandedSettings ? 6 : 0);
        selected.requestFocus();
        return true;
    }

    private boolean containsFocus(View view) {
        for (TextView button : actions) if (button == view) return true;
        return false;
    }

    private void focusSettingsContent() {
        View root = activity.findViewById(com.liskovsoft.smartyoutubetv2.tv.R.id.settings_list);
        if (!(root instanceof androidx.recyclerview.widget.RecyclerView)) return;
        androidx.recyclerview.widget.RecyclerView list = (androidx.recyclerview.widget.RecyclerView) root;
        list.post(new Runnable() {
            int retries;
            @Override public void run() {
                if (activity.isFinishing() || activity.isDestroyed()) return;
                for (int i = 0; i < list.getChildCount(); i++) {
                    View row = list.getChildAt(i);
                    if (row != null && row.isEnabled() && row.isFocusable()
                            && row.getVisibility() == View.VISIBLE && row.requestFocus()) {
                        return;
                    }
                }
                if (++retries < 12) list.postDelayed(this, 75L);
            }
        });
    }

    private void returnToContent() {
        if (expandedSettings) {
            focusSettingsContent();
            return;
        }
        if (activity instanceof com.newtube.mobile.ui.browse.MobileBrowseActivity) {
            ((com.newtube.mobile.ui.browse.MobileBrowseActivity) activity)
                    .marsTvRestoreContentFocus();
            return;
        }
        View old = previousFocus.get();
        if (old != null && old.isAttachedToWindow() && old.isFocusable()) {
            old.requestFocus();
        } else {
            // A newly opened section may have replaced the card list.
            View main = ((FrameLayout) activity.findViewById(android.R.id.content)).getChildAt(0);
            View target = main != null ? main.focusSearch(View.FOCUS_RIGHT) : null;
            if (target != null) target.requestFocus();
        }
    }

    public static boolean handleKey(Activity activity, KeyEvent event) {
        MarsTvRail rail = get(activity);
        if (rail == null) return false;
        rail.syncMiniAction();
        int code = event.getKeyCode();
        // BACK and MENU always target the rail, even after scrolling 20+ tiles.
        // This must run before testing whether the current focus is a rail item.
        if (code == KeyEvent.KEYCODE_BACK || code == KeyEvent.KEYCODE_MENU) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) focus(activity);
            return true;
        }
        View current = activity.getCurrentFocus();
        int index = rail.actions.indexOf(current);
        if (index < 0) return false;
        boolean supported = code == KeyEvent.KEYCODE_DPAD_UP
                || code == KeyEvent.KEYCODE_DPAD_DOWN
                || code == KeyEvent.KEYCODE_DPAD_LEFT
                || code == KeyEvent.KEYCODE_DPAD_RIGHT
                || code == KeyEvent.KEYCODE_ENTER
                || code == KeyEvent.KEYCODE_NUMPAD_ENTER
                || code == KeyEvent.KEYCODE_DPAD_CENTER;
        if (!supported) return false;
        if (event.getAction() != KeyEvent.ACTION_DOWN) return true;
        if (code == KeyEvent.KEYCODE_DPAD_UP) {
            rail.actions.get(Math.max(0, index - 1)).requestFocus();
        } else if (code == KeyEvent.KEYCODE_DPAD_DOWN) {
            rail.actions.get(Math.min(rail.actions.size() - 1, index + 1)).requestFocus();
        } else if (code == KeyEvent.KEYCODE_DPAD_RIGHT) {
            rail.returnToContent();
        } else if (code == KeyEvent.KEYCODE_ENTER || code == KeyEvent.KEYCODE_NUMPAD_ENTER
                || code == KeyEvent.KEYCODE_DPAD_CENTER) {
            rail.actions.get(index).performClick();
        }
        return true;
    }
}
