package com.newtube.mobile.ui.browse;

/** Pixel policy based on the measured content viewport, not physical display resolution. */
public final class MarsTvGridPolicy040 {
    private MarsTvGridPolicy040() { }
    public static int rowHeight(int viewport, int top, int bottom) {
        return Math.max(1, (viewport - top - bottom) / 2);
    }
    public static int imageHeight(int cellWidth, int rowHeight, int textBudget, boolean portrait) {
        int available = Math.max(1, rowHeight - textBudget);
        return portrait ? available : Math.max(1, Math.min(available, cellWidth * 9 / 16));
    }
    public static boolean shouldPauseHidden(boolean tv, boolean pip, boolean visibleMini) {
        return tv && !pip && !visibleMini;
    }
}
