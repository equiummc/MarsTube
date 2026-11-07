package com.newtube.mobile.ui.common;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * Non-focusable, lightweight right-side label: the rail always remains narrow.
 * No blur, layout expansion or animation in the D-pad input path.
 */
public final class MarsTvRailLabel {
    private MarsTvRailLabel() { }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    public static TextView attach(Activity activity, FrameLayout host, int railWidthDp) {
        TextView label = new TextView(activity);
        label.setTextSize(14f);
        label.setTextColor(Color.WHITE);
        label.setSingleLine(true);
        label.setEllipsize(android.text.TextUtils.TruncateAt.END);
        label.setGravity(Gravity.CENTER_VERTICAL);
        label.setPadding(dp(activity, 12), 0, dp(activity, 14), 0);
        label.setFocusable(false);
        label.setClickable(false);
        label.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        GradientDrawable background = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xF0184674, 0xF009223E});
        background.setCornerRadius(dp(activity, 10));
        background.setStroke(dp(activity, 1), 0xBB478DC3);
        label.setBackground(background);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                dp(activity, 180), dp(activity, 38), Gravity.TOP | Gravity.START);
        lp.leftMargin = dp(activity, railWidthDp + 8);
        host.addView(label, lp);
        label.setVisibility(View.GONE);
        return label;
    }

    public static void show(TextView label, View anchor, String title) {
        if (label == null || anchor == null || !anchor.isShown()) return;
        if (!(label.getParent() instanceof FrameLayout)) return;
        FrameLayout host = (FrameLayout) label.getParent();
        int[] target = new int[2], parent = new int[2];
        anchor.getLocationOnScreen(target);
        host.getLocationOnScreen(parent);
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) label.getLayoutParams();
        int top = target[1] - parent[1] + (anchor.getHeight() - lp.height) / 2;
        lp.topMargin = Math.max(0, Math.min(top, Math.max(0, host.getHeight() - lp.height)));
        label.setLayoutParams(lp);
        label.setText(title);
        // Only the initial appearance fades; repeated D-pad steps update instantly.
        if (label.getVisibility() != View.VISIBLE) {
            label.animate().cancel();
            label.setAlpha(0f);
            label.setVisibility(View.VISIBLE);
            label.animate().alpha(1f).setDuration(125L).start();
        }
    }

    public static void hide(TextView label) {
        if (label == null || label.getVisibility() != View.VISIBLE) return;
        label.animate().cancel();
        label.setVisibility(View.GONE);
        label.setAlpha(1f);
    }
}
