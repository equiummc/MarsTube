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
    private static final String TAG = "mars.tv.rail.v007";
    private static final int BLUE = Color.rgb(48, 157, 250);
    private static final int BG = Color.rgb(13, 21, 37);
    private static final int SELECTED = Color.rgb(24, 55, 93);
    private final Activity activity;
    private final List<TextView> actions = new ArrayList<>();
    private WeakReference<View> previousFocus = new WeakReference<>(null);

    private MarsTvRail(Activity activity) {
        super(activity);
        this.activity = activity;
        setTag(TAG);
        setOrientation(VERTICAL);
        setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        setBackgroundColor(BG);
        setFocusable(false);
        setClipToPadding(false);
        setPadding(dp(6), dp(16), dp(6), dp(8));

        android.widget.ImageView branding = new android.widget.ImageView(activity);
        branding.setImageResource(com.liskovsoft.smartyoutubetv2.tv.R.drawable.mars_m_logo);
        branding.setContentDescription("MarsTube");
        branding.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
        addView(branding, new LinearLayout.LayoutParams(-1, dp(46)));
        TextView version = new TextView(activity);
        version.setText("0.0.8");
        version.setTextSize(11);
        version.setTextColor(Color.LTGRAY);
        version.setGravity(Gravity.CENTER);
        addView(version, new LinearLayout.LayoutParams(-1, dp(22)));

        addAction("Start", 0);
        addAction("Suche", 1);
        addAction("Abos", 2);
        addAction("Verlauf", 3);
        addAction("Downloads", 4);
        addAction("Profil", 6);
        addAction("Einst.", 5);
        addAction("Miniplayer", 7);
    }

    private int dp(float d) {
        return (int) (d * getResources().getDisplayMetrics().density + 0.5f);
    }

    private static GradientDrawable focusBackground(boolean focus) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(focus ? SELECTED : BG);
        background.setCornerRadius(12);
        if (focus) background.setStroke(2, BLUE);
        return background;
    }

    private void addAction(String label, int destination) {
        TextView button = new TextView(activity);
        button.setText(label);
        button.setTextSize(11);
        int iconRes;
        switch (destination) {
            case 0: iconRes = com.liskovsoft.smartyoutubetv2.tv.R.drawable.ic_nav_home; break;
            case 1: iconRes = com.liskovsoft.smartyoutubetv2.tv.R.drawable.ic_mobile_search; break;
            case 2: iconRes = com.liskovsoft.smartyoutubetv2.tv.R.drawable.ic_nav_subscriptions; break;
            case 3: iconRes = com.liskovsoft.smartyoutubetv2.tv.R.drawable.ic_nav_history; break;
            case 4: iconRes = com.liskovsoft.smartyoutubetv2.tv.R.drawable.ic_nav_downloads; break;
            case 5: iconRes = com.liskovsoft.smartyoutubetv2.tv.R.drawable.ic_mobile_settings; break;
            case 6: iconRes = com.liskovsoft.smartyoutubetv2.tv.R.drawable.ic_nav_you; break;
            default: iconRes = com.liskovsoft.smartyoutubetv2.tv.R.drawable.ic_player_fullscreen; break;
        }
        button.setCompoundDrawablesWithIntrinsicBounds(0, iconRes, 0, 0);
        button.setCompoundDrawableTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));
        button.setCompoundDrawablePadding(dp(2));
        button.setTextColor(Color.WHITE);
        button.setGravity(Gravity.CENTER);
        button.setContentDescription(label.equals("Einst.") ? "Einstellungen" : label);
        button.setFocusable(true);
        button.setFocusableInTouchMode(false);
        button.setBackground(focusBackground(false));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(46));
        params.bottomMargin = dp(4);
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
        final int railWidth = (int) (100 * activity.getResources().getDisplayMetrics().density + 0.5f);
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
        View old = activity.getCurrentFocus();
        if (old != null && !rail.containsFocus(old)) rail.previousFocus = new WeakReference<>(old);
        rail.actions.get(0).requestFocus();
        return true;
    }

    private boolean containsFocus(View view) {
        for (TextView button : actions) if (button == view) return true;
        return false;
    }

    private void returnToContent() {
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
        int code = event.getKeyCode();
        if (code == KeyEvent.KEYCODE_MENU) {
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
                || code == KeyEvent.KEYCODE_BACK
                || code == KeyEvent.KEYCODE_ENTER
                || code == KeyEvent.KEYCODE_NUMPAD_ENTER
                || code == KeyEvent.KEYCODE_DPAD_CENTER;
        if (!supported) return false;
        if (event.getAction() != KeyEvent.ACTION_DOWN) return true;
        if (code == KeyEvent.KEYCODE_DPAD_UP) {
            rail.actions.get(Math.max(0, index - 1)).requestFocus();
        } else if (code == KeyEvent.KEYCODE_DPAD_DOWN) {
            rail.actions.get(Math.min(rail.actions.size() - 1, index + 1)).requestFocus();
        } else if (code == KeyEvent.KEYCODE_DPAD_RIGHT || code == KeyEvent.KEYCODE_BACK) {
            rail.returnToContent();
        } else if (code == KeyEvent.KEYCODE_ENTER || code == KeyEvent.KEYCODE_NUMPAD_ENTER
                || code == KeyEvent.KEYCODE_DPAD_CENTER) {
            rail.actions.get(index).performClick();
        }
        return true;
    }
}
