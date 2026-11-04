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
import android.widget.ImageView;
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
    private static final String TAG = "mars.tv.rail.v008";
    private static final int BLUE = 0xFF0B9BFF;
    private static final int BG = 0xFF071521;
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

        // Identical Mars "M" image to the playback rail, never a plain letter.
        ImageView branding = new ImageView(activity);
        branding.setImageResource(com.liskovsoft.smartyoutubetv2.tv.R.drawable.mars_m_logo);
        branding.setScaleType(ImageView.ScaleType.FIT_CENTER);
        addView(branding, new LinearLayout.LayoutParams(-1, dp(52)));
        TextView version = new TextView(activity);
        version.setText("0.0.8");
        version.setTextSize(12);
        version.setTextColor(BLUE);
        version.setGravity(Gravity.CENTER);
        addView(version, new LinearLayout.LayoutParams(-1, dp(23)));

        addAction("Start", 0);
        addAction("Suche", 1);
        addAction("Abos", 2);
        addAction("Verlauf", 3);
        addAction("Downloads", 4);
        addAction("Profil", 6);
        addAction("Einst.", 5);
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
        button.setTextSize(13);
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
        if (destination == 1) {
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
        bindMiniPlayer(activity);
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

    /** The shared in-app mini card is a real D-pad target on television. */
    public static void bindMiniPlayer(Activity activity) {
        if (!MarsDeviceMode.isTelevision(activity)) return;
        View mini = activity.findViewById(com.liskovsoft.smartyoutubetv2.tv.R.id.mobile_mini_player);
        if (mini == null) return;
        mini.setFocusable(true);
        mini.setFocusableInTouchMode(false);
        mini.setContentDescription("Kleines Video – OK zum Vergrößern");
        mini.setOnFocusChangeListener((v, hasFocus) -> {
            GradientDrawable border = new GradientDrawable();
            border.setColor(Color.TRANSPARENT);
            border.setCornerRadius(12);
            if (hasFocus) border.setStroke(3, BLUE);
            v.setForeground(hasFocus ? border : null);
        });
    }

    public static boolean focusMiniPlayer(Activity activity) {
        if (!MarsDeviceMode.isTelevision(activity) ||
                !com.newtube.mobile.ui.playback.MiniPlayerBridge.isActive()) return false;
        View mini = activity.findViewById(com.liskovsoft.smartyoutubetv2.tv.R.id.mobile_mini_player);
        return mini != null && mini.getVisibility() == View.VISIBLE && mini.requestFocus();
    }

    public static boolean handleMiniKey(Activity activity, KeyEvent event) {
        if (!MarsDeviceMode.isTelevision(activity)) return false;
        View mini = activity.findViewById(com.liskovsoft.smartyoutubetv2.tv.R.id.mobile_mini_player);
        if (mini == null || mini.getVisibility() != View.VISIBLE) return false;
        View focus = activity.getCurrentFocus();
        if (focus != mini) return false;
        int code = event.getKeyCode();
        if (code == KeyEvent.KEYCODE_DPAD_CENTER ||
                code == KeyEvent.KEYCODE_ENTER ||
                code == KeyEvent.KEYCODE_NUMPAD_ENTER) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) mini.performClick();
            return true;
        }
        if (code == KeyEvent.KEYCODE_DPAD_LEFT || code == KeyEvent.KEYCODE_BACK) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) focus(activity);
            return true;
        }
        return false;
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
            if (code == KeyEvent.KEYCODE_DPAD_RIGHT && focusMiniPlayer(activity)) return true;
            rail.returnToContent();
        } else if (code == KeyEvent.KEYCODE_ENTER || code == KeyEvent.KEYCODE_NUMPAD_ENTER
                || code == KeyEvent.KEYCODE_DPAD_CENTER) {
            rail.actions.get(index).performClick();
        }
        return true;
    }
}
