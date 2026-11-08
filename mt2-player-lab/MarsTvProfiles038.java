package com.newtube.mobile.ui.browse;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
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

/** TV-only accessible, actual account/channel picker. Never simulates accounts.
 * Uses the same account-selection backend as the existing AccountsSheet.
 */
public final class MarsTvProfiles038 {
    private static final int BLUE = 0xFF37B7F1;
    private static final int WHITE = 0xFFF0F6FC;
    private static final int MUTED = 0xFFAEC6DC;
    private MarsTvProfiles038() { }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
    private static GradientDrawable shape(int color, int stroke, int radius, int density) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius * density);
        if (stroke != 0) d.setStroke(Math.max(1,density),stroke);
        return d;
    }

    public static void show(Activity activity, Runnable onChanged) {
        if (activity.isFinishing() || activity.isDestroyed()) return;
        MediaServiceManager.instance().loadAccounts(accounts -> {
            if (activity.isFinishing() || activity.isDestroyed()) return;
            if (accounts == null || accounts.isEmpty()) {
                YTSignInPresenter.instance(activity).start();
                return;
            }
            render(activity, accounts, onChanged);
        });
    }

    private static void render(Activity activity, List<Account> accounts,
                               Runnable onChanged) {
        final int density = Math.max(1,Math.round(activity.getResources()
                .getDisplayMetrics().density));
        Dialog dialog = new Dialog(activity);
        LinearLayout host = new LinearLayout(activity);
        host.setOrientation(LinearLayout.VERTICAL);
        host.setPadding(dp(activity,22),dp(activity,20),
                dp(activity,22),dp(activity,16));
        host.setBackground(shape(0xFF0B2238,0xFF1C648F,18,density));
        TextView title = new TextView(activity);
        title.setText("YouTube-Profil auswählen");
        title.setTextColor(WHITE);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextSize(23);
        host.addView(title, new LinearLayout.LayoutParams(-1,dp(activity,40)));
        TextView subtitle = new TextView(activity);
        subtitle.setText("Aktives Profil bestimmt Abos, Empfehlungen und ausgeblendete Videos.");
        subtitle.setTextColor(MUTED);
        subtitle.setTextSize(13);
        subtitle.setPadding(0,0,0,dp(activity,14));
        host.addView(subtitle, new LinearLayout.LayoutParams(-1,dp(activity,50)));
        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(false);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout items = new LinearLayout(activity);
        items.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(items);
        host.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        View first = null;
        Account selected = null;
        for (Account account : accounts) {
            if (account.isSelected()) selected = account;
            String name = account.getName();
            if (name == null || name.trim().isEmpty()) name = account.getEmail();
            if (name == null || name.trim().isEmpty()) name = "YouTube-Profil";
            String email = account.getEmail();
            LinearLayout row = option(activity,items,name,
                    account.isSelected() ? "Aktiv" : email,
                    account.isSelected(),() -> {
                        dialog.dismiss();
                        if (!account.isSelected()) {
                            AccountSelectionPresenter.instance(activity).selectAccount(account);
                            if (onChanged != null) onChanged.run();
                        }
                    });
            if (first == null || account.isSelected()) first = row;
            final String avatar = account.getAvatarImageUrl();
            if (avatar != null && !avatar.isEmpty()) {
                // Only visible rows are loaded; no subscriber or video requests.
                ImageView icon = new ImageView(activity);
                icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        dp(activity,32),dp(activity,32));
                lp.rightMargin = dp(activity,14);
                row.addView(icon,0,lp);
                Glide.with(activity).load(avatar).circleCrop()
                        .placeholder(R.drawable.ic_mobile_account).into(icon);
            }
        }
        final boolean anonymous = selected == null;
        option(activity,items,"Ohne Konto","Eigene lokale Ablehnungsliste",anonymous,() -> {
            dialog.dismiss();
            if (!anonymous) {
                AccountSelectionPresenter.instance(activity).selectAccount(null);
                if (onChanged != null) onChanged.run();
            }
        });
        option(activity,items,"Konto hinzufügen","Über YouTube anmelden",false,() -> {
            dialog.dismiss();
            YTSignInPresenter.instance(activity).start();
        });
        option(activity,items,"Schließen","Zur Mediathek zurück",false,dialog::dismiss);
        dialog.setContentView(host);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            android.view.WindowManager.LayoutParams p = window.getAttributes();
            p.dimAmount = 0.7f;
            window.setAttributes(p);
        }
        dialog.show();
        if (window != null) {
            int available = activity.getResources().getDisplayMetrics().widthPixels;
            int width = Math.min(dp(activity,565), available - dp(activity,75));
            window.setLayout(width,Math.min(dp(activity,620),
                    activity.getResources().getDisplayMetrics().heightPixels
                    - dp(activity,60)));
            window.setGravity(Gravity.CENTER);
        }
        if (first != null) first.requestFocus();
    }

    /** Owns one Android-TV focus surface. The row is not a passive mobile label. */
    private static LinearLayout option(Activity activity,LinearLayout parent,
                                   String label,String sub,boolean active,Runnable onClick) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(activity,12),dp(activity,7),
                dp(activity,10),dp(activity,7));
        LinearLayout texts = new LinearLayout(activity);
        texts.setOrientation(LinearLayout.VERTICAL);
        TextView name = new TextView(activity);
        name.setText(label + (active ? "  ✓" : ""));
        name.setTextColor(WHITE);
        name.setTextSize(17);
        name.setSingleLine(true);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        TextView details = new TextView(activity);
        details.setText(sub == null ? "" : sub);
        details.setTextColor(MUTED);
        details.setTextSize(12);
        details.setSingleLine(true);
        details.setEllipsize(android.text.TextUtils.TruncateAt.END);
        texts.addView(name);
        texts.addView(details);
        row.addView(texts,new LinearLayout.LayoutParams(0,-2,1));
        // Use one focusable wrapper rather than a blank overlay behind the row.
        row.setFocusable(true);
        row.setClickable(true);
        row.setOnClickListener(v -> onClick.run());
        row.setContentDescription(label + (active ? ", aktiv" : ""));
        row.setBackground(shape(active ? 0xFF124A70 : 0xFF142F48,
                active ? BLUE : 0,11,Math.max(1,Math.round(activity.getResources()
                        .getDisplayMetrics().density))));
        row.setOnFocusChangeListener((v,focused)->{
            int d=Math.max(1,Math.round(activity.getResources().getDisplayMetrics().density));
            row.setBackground(shape(focused ? 0xFF175B86
                            : active ? 0xFF124A70 : 0xFF142F48,
                    focused ? 0xFF75D7FF : active ? BLUE : 0,11,d));
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,dp(activity,61));
        lp.bottomMargin = dp(activity,7);
        parent.addView(row,lp);
        return row;
    }
}
