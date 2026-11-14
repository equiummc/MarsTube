package com.newtube.mobile.ui.browse;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;
import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup;
import com.liskovsoft.smartyoutubetv2.tv.R;

/** TV only. Two complete rows; all adapter items and pagination remain available. */
public final class MarsTvGrid040 {
    private MarsTvGrid040() { }
    public static void install(RecyclerView grid) {
        grid.setClipToPadding(true);
        grid.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob) -> {
            if (r-l != or-ol || b-t != ob-ot) grid.post(() -> {
                if (grid.getAdapter() != null && !grid.isComputingLayout())
                    grid.getAdapter().notifyDataSetChanged();
            });
        });
    }
    public static void bind(View card) {
        Object owner = card.getTag(R.id.mobile_content_grid);
        if (!(owner instanceof RecyclerView)) return;
        RecyclerView grid = (RecyclerView) owner;
        Object section = grid.getTag(R.id.mobile_content_grid);
        if (!(section instanceof Integer)) return;
        int id = (Integer) section;
        boolean shorts = id == MediaGroup.TYPE_SHORTS;
        if (!shorts && id != MediaGroup.TYPE_HOME && id != MediaGroup.TYPE_HISTORY
                && id != MediaGroup.TYPE_LIVE && id != MediaGroup.TYPE_MUSIC
                && id != MediaGroup.TYPE_SUBSCRIPTIONS) return;
        float density = card.getResources().getDisplayMetrics().density;
        int gap = Math.round(6*density);
        ViewGroup.MarginLayoutParams cp = (ViewGroup.MarginLayoutParams) card.getLayoutParams();
        int viewport = grid.getHeight() > 0 ? grid.getHeight()
                : card.getResources().getDisplayMetrics().heightPixels - Math.round(60*density);
        int tiles = com.newtube.mobile.player.MarsTvPlayerPrefs.getHomeTileCount(card.getContext());
        int rows = !shorts && tiles == 9 ? 3 : 2;
        int row = Math.max(1,(viewport-grid.getPaddingTop()-grid.getPaddingBottom())/rows);
        cp.height = Math.max(1, row - cp.topMargin - cp.bottomMargin);
        card.setPadding(0,0,0,0);
        card.setLayoutParams(cp);
        int columns = shorts || tiles == 4 ? 2 : 3;
        int cell = (grid.getWidth() - grid.getPaddingLeft() - grid.getPaddingRight()) / columns;
        if (cell <= 0) cell = card.getResources().getDisplayMetrics().widthPixels / columns;
        cell = Math.max(1, cell - cp.leftMargin - cp.rightMargin);
        int height = MarsTvGridPolicy040.imageHeight(cell, cp.height,
                Math.round((shorts ? 8 : 62)*density), shorts);
        View imageFrame = card.findViewById(R.id.video_thumbnail_frame);
        ConstraintLayout.LayoutParams fp = (ConstraintLayout.LayoutParams) imageFrame.getLayoutParams();
        fp.width = Math.max(1, shorts ? height*9/16 : height*16/9);
        fp.height = height;
        fp.dimensionRatio = null;
        fp.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
        fp.endToEnd = shorts ? ConstraintLayout.LayoutParams.UNSET : ConstraintLayout.LayoutParams.PARENT_ID;
        fp.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
        fp.horizontalBias = 0.5f;
        imageFrame.setLayoutParams(fp);
        ImageView image = card.findViewById(R.id.video_thumbnail);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        TextView title = card.findViewById(R.id.video_title);
        TextView meta = card.findViewById(R.id.video_meta);
        ConstraintLayout.LayoutParams tp = (ConstraintLayout.LayoutParams) title.getLayoutParams();
        tp.width = 0;
        tp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        tp.startToStart = shorts ? ConstraintLayout.LayoutParams.UNSET : ConstraintLayout.LayoutParams.PARENT_ID;
        tp.startToEnd = shorts ? R.id.video_thumbnail_frame : ConstraintLayout.LayoutParams.UNSET;
        tp.endToEnd = shorts ? ConstraintLayout.LayoutParams.PARENT_ID : ConstraintLayout.LayoutParams.UNSET;
        tp.endToStart = shorts ? ConstraintLayout.LayoutParams.UNSET : R.id.video_overflow;
        tp.topToTop = shorts ? R.id.video_thumbnail_frame : ConstraintLayout.LayoutParams.UNSET;
        tp.topToBottom = shorts ? ConstraintLayout.LayoutParams.UNSET : R.id.video_thumbnail_frame;
        tp.leftMargin = shorts ? gap*2 : gap;
        tp.rightMargin = gap;
        tp.goneStartMargin = tp.leftMargin;
        tp.topMargin = gap;
        title.setLayoutParams(tp);
        title.setTextSize(shorts ? 16 : 14);
        title.setMaxLines(shorts ? 4 : 2);
        ConstraintLayout.LayoutParams mp = (ConstraintLayout.LayoutParams) meta.getLayoutParams();
        mp.width = 0;
        mp.startToStart = R.id.video_title;
        mp.endToEnd = R.id.video_title;
        mp.startToEnd = mp.endToStart = ConstraintLayout.LayoutParams.UNSET;
        mp.topToTop = ConstraintLayout.LayoutParams.UNSET;
        mp.topToBottom = R.id.video_title;
        mp.bottomToBottom = ConstraintLayout.LayoutParams.UNSET;
        mp.leftMargin = mp.rightMargin = 0;
        meta.setLayoutParams(mp);
        meta.setTextSize(11);
        meta.setMaxLines(shorts ? 3 : 1);
        card.findViewById(R.id.video_channel_avatar).setVisibility(View.GONE);
        card.findViewById(R.id.video_overflow).setVisibility(shorts ? View.GONE : View.VISIBLE);
    }
}
