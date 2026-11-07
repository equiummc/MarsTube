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
    private static final String TAG = "mars.tv.rail.v025";
    private static final int BLUE = Color.rgb(48, 157, 250);
    private static final int BG = Color.rgb(13, 21, 37);
    private static final int SELECTED = Color.rgb(24, 55, 93);
    private final Activity activity;
    private final boolean expandedSettings;
    private final LinearLayout capsule;
    private final List<TextView> actions = new ArrayList<>();
    private WeakReference<View> previousFocus = new WeakReference<>(null);
    private TextView miniAction;
    private TextView labelFlyout;
    private int lastFocusedIndex;

    private MarsTvRail(Activity activity) {
        super(activity);
        this.activity = activity;
        expandedSettings = "MobileSettingsActivity".equals(activity.getClass().getSimpleName());
        setTag(TAG);
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER);
        setBackgroundColor(Color.TRANSPARENT);
        setFocusable(false);
        setClipToPadding(false);
        setPadding(dp(3), dp(7), dp(3), dp(7));
        capsule = new LinearLayout(activity);
        capsule.setOrientation(VERTICAL);
        capsule.setGravity(Gravity.CENTER_HORIZONTAL);
        capsule.setPadding(dp(4), dp(10), dp(4), dp(10));
        capsule.setBackground(MarsTvNavigationTheme.floatingBackground(
                getResources().getDisplayMetrics().density));
        addView(capsule, new LinearLayout.LayoutParams(-1,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        // M logo is the first FOCUSABLE navigation item, not a decorative header.
        // Both playback and browse use the exact same entry list and order.
        for (MarsTvNavigationModel.Entry entry : MarsTvNavigationModel.main()) {
            addAction(entry.label, entry.destination);
        }
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
            if (miniAction.hasFocus() && actions.size() > MarsTvNavigationModel.HOME_INDEX) {
                actions.get(MarsTvNavigationModel.HOME_INDEX).requestFocus();
            }
            actions.remove(miniAction);
            capsule.removeView(miniAction);
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
        button.setText("");
        button.setTextSize(12);
        button.setMaxLines(1);
        button.setEllipsize(android.text.TextUtils.TruncateAt.END);
        int iconRes = MarsTvNavigationTheme.iconFor(destination);
        // Never trust icon intrinsic sizes (search icon used to overlap the rail).
        android.graphics.drawable.Drawable glyph =
                androidx.core.content.ContextCompat.getDrawable(activity, iconRes);
        if (glyph != null) {
            glyph = glyph.mutate();
            int iconDp = destination == 0 ? 32 : 20;
            glyph.setBounds(0, 0, dp(iconDp), dp(iconDp));
            button.setCompoundDrawables(null, glyph, null, null);
        }
        // The original flat M vector is tinted, never replaced or redrawn.
        button.setCompoundDrawableTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));
        button.setCompoundDrawablePadding(0);
        button.setTextColor(Color.WHITE);
        button.setGravity(Gravity.CENTER);
        button.setContentDescription(label.equals("Einst.") ? "Einstellungen" : label);
        button.setFocusable(true);
        button.setFocusableInTouchMode(false);
        button.setBackground(focusBackground(false));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1,
                dp(MarsTvNavigationTheme.ITEM_HEIGHT_DP));
        params.bottomMargin = dp(3);
        capsule.addView(button, params);
        actions.add(button);
        button.setOnFocusChangeListener((v, focused) -> {
            button.setBackground(focusBackground(focused));
            button.setCompoundDrawableTintList(android.content.res.ColorStateList.valueOf(
                    focused ? 0xFF68C7FF : Color.WHITE));
            if (focused) {
                lastFocusedIndex = actions.indexOf(button);
                MarsTvRailLabel.show(labelFlyout, button, label);
            } else {
                // Deferred only until Android has assigned the next D-pad focus.
                post(() -> {
                    if (!hasFocus()) MarsTvRailLabel.hide(labelFlyout);
                });
            }
        });
        button.setOnClickListener(v -> {
            android.util.Log.i("MarsTubeTV", "rail destination=" + destination);
            if (destination == 0) {
                // One short matte-blue confirmation; it never delays navigation.
                button.setBackground(MarsTvNavigationTheme.homeActivatedBackground(
                        getResources().getDisplayMetrics().density));
                button.postDelayed(() -> {
                    if (button.isAttachedToWindow())
                        button.setBackground(focusBackground(button.hasFocus()));
                }, 120L);
            }
            navigate(destination);
        });
    }

    private void navigate(int destination) {
        MarsTvRailLabel.hide(labelFlyout);
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
            // Already in Settings: return to the existing root fragment; never stack a second Activity.
            if (activity instanceof com.newtube.mobile.ui.settings.MobileSettingsActivity) {
                ((com.newtube.mobile.ui.settings.MobileSettingsActivity) activity).marsTvGoToSettingsRoot();
                focusSettingsContent();
            } else {
                activity.startActivity(com.newtube.mobile.ui.settings.MobileSettingsActivity.intent(activity, null));
            }
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
        rail.labelFlyout = MarsTvRailLabel.attach(activity, content, MarsTvNavigationTheme.WIDTH_DP);
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
        View selected = rail.actions.get(MarsTvNavigationModel.HOME_INDEX);
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
        // Returning from the rail must not jump back to the first visible row.
        View last = previousFocus.get();
        if (last != null && last.isAttachedToWindow() && last.isShown()
                && belongsTo(last, list) && last.requestFocus()) return;
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

    private static boolean belongsTo(View candidate, ViewGroup parent) {
        View current = candidate;
        while (current != null) {
            if (current == parent) return true;
            android.view.ViewParent p = current.getParent();
            current = p instanceof View ? (View) p : null;
        }
        return false;
    }

    private void returnToContent() {
        MarsTvRailLabel.hide(labelFlyout);
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
        // On Settings, BACK belongs to the Fragment back stack, not to the rail.
        // MENU still opens the rail from any focus position.
        if (code == KeyEvent.KEYCODE_BACK && rail.expandedSettings) return false;
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
            rail.actions.get(MarsTvNavigationModel.wrap(index, -1, rail.actions.size())).requestFocus();
        } else if (code == KeyEvent.KEYCODE_DPAD_DOWN) {
            rail.actions.get(MarsTvNavigationModel.wrap(index, 1, rail.actions.size())).requestFocus();
        } else if (code == KeyEvent.KEYCODE_DPAD_RIGHT) {
            rail.returnToContent();
        } else if (code == KeyEvent.KEYCODE_ENTER || code == KeyEvent.KEYCODE_NUMPAD_ENTER
                || code == KeyEvent.KEYCODE_DPAD_CENTER) {
            rail.actions.get(index).performClick();
        }
        return true;
    }
}
