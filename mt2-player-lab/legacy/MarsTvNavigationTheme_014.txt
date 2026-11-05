package com.newtube.mobile.ui.common;

import android.graphics.drawable.GradientDrawable;
import com.liskovsoft.smartyoutubetv2.tv.R;

/** One common visual contract across player overlay, Home, Shorts and Settings. */
public final class MarsTvNavigationTheme {
    public static final int WIDTH_DP = 64;
    public static final int ITEM_HEIGHT_DP = 38;
    private static final int BACKGROUND_TOP = 0xFF112D52;
    private static final int BACKGROUND_BOTTOM = 0xFF08182F;
    private static final int SELECTED = 0xFF18375D;
    private static final int BLUE = 0xFF309DFA;
    private MarsTvNavigationTheme() {}

    public static GradientDrawable railBackground() {
        return new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{BACKGROUND_TOP, BACKGROUND_BOTTOM});
    }

    public static GradientDrawable itemBackground(boolean selected, float density) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(selected ? SELECTED : 0xFF0D1525);
        drawable.setCornerRadius(Math.max(1, 10f * density));
        if (selected) drawable.setStroke(Math.max(2, Math.round(2f * density)), BLUE);
        return drawable;
    }

    public static int iconFor(int destination) {
        switch (destination) {
            case 0: return R.drawable.ic_nav_home;
            case 8: return R.drawable.icon_shorts;
            case 1: return R.drawable.ic_mobile_search;
            case 2: return R.drawable.ic_nav_subscriptions;
            case 3: return R.drawable.ic_nav_history;
            case 5: return R.drawable.ic_mobile_settings;
            case 6: return R.drawable.ic_nav_you;
            case 7: return R.drawable.ic_player_fullscreen;
            default: return R.drawable.ic_nav_home;
        }
    }
}
