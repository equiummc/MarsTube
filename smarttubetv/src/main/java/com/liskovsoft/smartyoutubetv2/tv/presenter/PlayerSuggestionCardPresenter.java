package com.liskovsoft.smartyoutubetv2.tv.presenter;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Pair;

import com.liskovsoft.smartyoutubetv2.tv.R;

/**
 * Player-only suggested video card dimensions.
 * Keep the browse/Home/Live/History grids unchanged.  The transport controls'
 * horizontal rail has room for seven whole cards and a half-card continuation cue.
 */
public final class PlayerSuggestionCardPresenter extends VideoCardPresenter {
    private static final float TARGET_VISIBLE_SLOTS = 7.5f;
    private static final float PLAYER_RAIL_WIDTH_FRACTION = 0.86f;

    @Override
    protected Pair<Integer, Integer> getCardDimensPx(Context context) {
        Pair<Integer, Integer> base = super.getCardDimensPx(context);
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        int displayWidth = Math.max(metrics.widthPixels, metrics.heightPixels);
        int itemSpacing = context.getResources().getDimensionPixelSize(R.dimen.grid_item_horizontal_spacing);

        int width = Math.max(1, Math.round(
                displayWidth * PLAYER_RAIL_WIDTH_FRACTION / TARGET_VISIBLE_SLOTS - itemSpacing));
        // Keep the 16:9 original preview's pixel proportions instead of stretching video artwork.
        int height = Math.max(1, Math.round(width * ((float) base.second / Math.max(1, base.first))));
        return Pair.create(width, height);
    }
}
