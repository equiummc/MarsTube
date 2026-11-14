package com.newtube.mobile.ui.browse;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.liskovsoft.mediaserviceinterfaces.oauth.Account;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.YTSignInPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.dialogs.AccountSelectionPresenter;
import com.liskovsoft.smartyoutubetv2.common.misc.MediaServiceManager;
import com.liskovsoft.smartyoutubetv2.tv.R;
import java.util.List;

/** TV-only picker: stored YouTube identities, no fake brand profiles or token copies. */
public final class MarsTvAccountPicker037 {
    private static final int BLUE = 0xFF29B7FF;
    private MarsTvAccountPicker037() { }

    private static int dp(Activity a, float v) {
        return Math.round(v * a.getResources().getDisplayMetrics().density);
    }

    private static GradientDrawable background(int color, int border, Activity activity) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(activity, 12));
        shape.setStroke(dp(activity, 1.5f), border);
        return shape;
    }

    public static void show(Activity activity, Runnable afterSwitch) {
        MediaServiceManager.instance().loadAccounts(accounts ->
                activity.runOnUiThread(() -> {
                    if (activity.isFinishing() || activity.isDestroyed()) return;
                    openDialog(activity, accounts, afterSwitch);
                }));
    }

    private static void openDialog(Activity a, List<Account> accounts, Runnable changed) {
        Dialog dialog = new Dialog(a);
        LinearLayout content = new LinearLayout(a);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setBackground(background(0xFF0A1F37, 0xFF2669A1, a));
        content.setPadding(dp(a, 25), dp(a, 19), dp(a, 25), dp(a, 20));

        TextView title = new TextView(a);
        title.setText("YouTube-Profil auswählen");
        title.setTextColor(Color.WHITE);
        title.setTextSize(23);
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(dp(a, 8), dp(a, 4), dp(a, 8), dp(a, 10));
        content.addView(title);

        TextView desc = new TextView(a);
        desc.setText("Verfügbare Konten und Kanalidentitäten");
        desc.setTextColor(0xFFB5D5ED);
        desc.setTextSize(13);
        desc.setPadding(dp(a, 8), 0, dp(a, 8), dp(a, 18));
        content.addView(desc);

        ScrollView scroll = new ScrollView(a);
        scroll.setFillViewport(false);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout list = new LinearLayout(a);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);
        content.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        final View[] initial = {null};
        if (accounts != null) for (Account account : accounts) {
            String name = account.getName() != null ? account.getName() :
                    account.getEmail() != null ? account.getEmail() : "YouTube-Konto";
            boolean selected = account.isSelected();
            View row = accountRow(a, list,
                    name + (selected ? "    ✓" : ""),
                    account.getEmail(), account.getAvatarImageUrl(), () -> {
                        dialog.dismiss();
                        AccountSelectionPresenter.instance(a).selectAccount(account);
                        changed.run();
                    });
            if (selected) initial[0] = row;
        }
        if (accounts == null || accounts.isEmpty()) {
            TextView empty = new TextView(a);
            empty.setText("Noch kein YouTube-Konto gespeichert.");
            empty.setTextColor(0xFFBDDCF4);
            empty.setPadding(dp(a, 12), dp(a, 12), 0, dp(a, 12));
            list.addView(empty);
        }
        View without = accountRow(a, list, "Ohne Konto verwenden",
                "Gespeicherte Anmeldungen bleiben erhalten", null, () -> {
                    dialog.dismiss();
                    AccountSelectionPresenter.instance(a).selectAccount(null);
                    changed.run();
                });
        if (initial[0] == null) initial[0] = without;
        View add = accountRow(a, list, "+ Konto hinzufügen",
                "Über die vorhandene YouTube-Anmeldung", null, () -> {
                    dialog.dismiss();
                    YTSignInPresenter.instance(a).start();
                });
        if (accounts == null || accounts.isEmpty()) initial[0] = add;

        dialog.setContentView(content);
        android.view.Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(Math.min(dp(a, 650),
                    a.getResources().getDisplayMetrics().widthPixels - dp(a, 80)),
                    Math.min(dp(a, 600),
                    a.getResources().getDisplayMetrics().heightPixels - dp(a, 80)));
        }
        dialog.show();
        if (window != null) window.setLayout(
                Math.min(dp(a, 650), a.getResources().getDisplayMetrics().widthPixels - dp(a, 80)),
                Math.min(dp(a, 600), a.getResources().getDisplayMetrics().heightPixels - dp(a, 80)));
        View focus = initial[0];
        if (focus != null) focus.requestFocus();
    }

    private static View accountRow(Activity a, LinearLayout parent, String name,
                                   String subline, String avatarUrl, Runnable chosen) {
        LinearLayout row = new LinearLayout(a);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(a, 10), dp(a, 7), dp(a, 10), dp(a, 7));
        row.setFocusable(true);
        row.setClickable(true);
        row.setBackground(background(0xFF143653, 0xFF3B6381, a));
        row.setOnFocusChangeListener((v, focused) ->
                row.setBackground(background(focused ? 0xFFE5F7FF : 0xFF143653,
                        focused ? BLUE : 0xFF3B6381, a)));
        ImageView icon = new ImageView(a);
        icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
        icon.setImageResource(R.drawable.ic_mobile_account);
        if (avatarUrl != null && !avatarUrl.isEmpty())
            Glide.with(a).load(avatarUrl).circleCrop().into(icon);
        LinearLayout.LayoutParams avatar = new LinearLayout.LayoutParams(dp(a, 43), dp(a, 43));
        avatar.rightMargin = dp(a, 13);
        row.addView(icon, avatar);
        LinearLayout texts = new LinearLayout(a);
        texts.setOrientation(LinearLayout.VERTICAL);
        TextView label = new TextView(a);
        label.setText(name);
        label.setTextSize(18);
        label.setSingleLine(true);
        label.setEllipsize(android.text.TextUtils.TruncateAt.END);
        label.setTextColor(0xFFFFFFFF);
        TextView detail = new TextView(a);
        detail.setText(subline == null ? "" : subline);
        detail.setTextSize(12);
        detail.setTextColor(0xFFBDDCF2);
        detail.setSingleLine(true);
        detail.setEllipsize(android.text.TextUtils.TruncateAt.END);
        texts.addView(label);
        texts.addView(detail);
        row.addView(texts, new LinearLayout.LayoutParams(0, -2, 1f));
        row.setOnFocusChangeListener((v, focused) -> {
            row.setBackground(background(focused ? 0xFFE5F7FF : 0xFF143653,
                    focused ? BLUE : 0xFF3B6381, a));
            label.setTextColor(focused ? 0xFF092B46 : 0xFFFFFFFF);
            detail.setTextColor(focused ? 0xFF234D70 : 0xFFBDDCF2);
        });
        row.setOnClickListener(v -> chosen.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(a, 65));
        lp.bottomMargin = dp(a, 7);
        parent.addView(row, lp);
        return row;
    }
}
