package com.liskovsoft.smartyoutubetv2.tv.ui.browse;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Outline;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.PageRow;
import com.bumptech.glide.Glide;
import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup;
import com.liskovsoft.mediaserviceinterfaces.oauth.Account;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.SearchPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.dialogs.AccountSelectionPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.settings.AccountSettingsPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.settings.GeneralSettingsPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.dialogs.menu.SectionMenuPresenter;
import com.liskovsoft.smartyoutubetv2.common.misc.MediaServiceManager;
import com.liskovsoft.smartyoutubetv2.tv.R;

import java.util.LinkedHashMap;
import java.util.Map;

/** Fixed 68dp rail. Uses the user's existing, editable sidebar section order. */
final class MarsFixedRail {
    private final BrowseFragment browse;
    private final FrameLayout overlay;
    private final FrameLayout rail;
    private final LinearLayout icons;
    private final ScrollView scroll;
    private final TextView tooltip;
    private final ImageView profile;
    private final ImageView settings;
    private final Map<Long, ImageView> buttons = new LinkedHashMap<>();
    private final Runnable rebuild;
    private boolean destroyed;
    private long selectedSection = MediaGroup.TYPE_HOME;

    MarsFixedRail(BrowseFragment browse, FrameLayout overlay) {
        this.browse = browse;
        this.overlay = overlay;
        Context ctx = overlay.getContext();

        rail = new FrameLayout(ctx);
        rail.setBackgroundResource(R.drawable.mars_fixed_rail);
        FrameLayout.LayoutParams railLayout = new FrameLayout.LayoutParams(dp(68), -1, Gravity.START);
        overlay.addView(rail, railLayout);

        profile = button(R.drawable.browse_title_account, ctx.getString(R.string.settings_accounts));
        FrameLayout.LayoutParams profileLayout = new FrameLayout.LayoutParams(dp(48), dp(48), Gravity.TOP | Gravity.START);
        profileLayout.topMargin = dp(86);
        profileLayout.leftMargin = dp(10);
        rail.addView(profile, profileLayout);
        profile.setOnClickListener(v -> {
            AccountSelectionPresenter.instance(ctx).nextAccountOrDialog();
            profile.postDelayed(this::refreshProfile, 600);
        });
        profile.setOnLongClickListener(v -> {
            AccountSettingsPresenter.instance(ctx).show();
            return true;
        });
        profile.setOnFocusChangeListener((v, focused) -> showTooltip(profile, focused ? ctx.getString(R.string.settings_accounts) : null));
        profile.setOnKeyListener((v, keyCode, event) -> handleDirection(v, event, keyCode));
        refreshProfile();

        scroll = new ScrollView(ctx);
        scroll.setFillViewport(false);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setFocusable(false);
        FrameLayout.LayoutParams scrollLayout = new FrameLayout.LayoutParams(dp(58), -1, Gravity.TOP | Gravity.START);
        scrollLayout.leftMargin = dp(5);
        scrollLayout.topMargin = dp(148);
        scrollLayout.bottomMargin = dp(83);
        rail.addView(scroll, scrollLayout);

        icons = new LinearLayout(ctx);
        icons.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(icons, new ScrollView.LayoutParams(-1, -2));

        settings = button(R.drawable.icon_settings, ctx.getString(R.string.header_settings));
        FrameLayout.LayoutParams gearLayout = new FrameLayout.LayoutParams(dp(48), dp(48), Gravity.BOTTOM | Gravity.START);
        gearLayout.bottomMargin = dp(15);
        gearLayout.leftMargin = dp(10);
        rail.addView(settings, gearLayout);
        settings.setOnClickListener(v -> browse.selectFixedRailSection(MediaGroup.TYPE_SETTINGS, true));
        settings.setOnLongClickListener(v -> {
            GeneralSettingsPresenter.instance(ctx).showSidebarSections();
            return true;
        });
        settings.setOnFocusChangeListener((v, focused) -> {
            settings.setSelected(focused || selectedSection == MediaGroup.TYPE_SETTINGS);
            showTooltip(settings, focused ? ctx.getString(R.string.header_settings) : null);
        });
        settings.setOnKeyListener((v, keyCode, event) -> handleDirection(v, event, keyCode));

        tooltip = new TextView(ctx);
        tooltip.setTextColor(Color.WHITE);
        tooltip.setTextSize(15);
        tooltip.setPadding(dp(14), dp(7), dp(14), dp(7));
        tooltip.setBackgroundResource(R.drawable.mars_glass_navigation);
        tooltip.setVisibility(View.GONE);
        tooltip.setFocusable(false);
        FrameLayout.LayoutParams tipLayout = new FrameLayout.LayoutParams(-2, -2, Gravity.START | Gravity.TOP);
        tipLayout.leftMargin = dp(78);
        overlay.addView(tooltip, tipLayout);
        tooltip.setElevation(dp(8));

        rebuild = () -> {
            if (!destroyed) update();
        };
    }

    private int dp(int value) {
        return Math.round(value * overlay.getResources().getDisplayMetrics().density);
    }

    private ImageView button(int drawable, String title) {
        ImageView view = new ImageView(overlay.getContext());
        view.setImageResource(drawable);
        view.setScaleType(ImageView.ScaleType.FIT_CENTER);
        view.setPadding(dp(11), dp(11), dp(11), dp(11));
        view.setBackgroundResource(R.drawable.mars_glass_navigation);
        view.setColorFilter(0xFFC2D9F3);
        view.setContentDescription(title);
        view.setFocusable(true);
        view.setClickable(true);
        return view;
    }

    private boolean handleDirection(View source, KeyEvent event, int code) {
        if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
        if (code == KeyEvent.KEYCODE_DPAD_RIGHT) {
            browse.focusOnContent();
            return true;
        }
        if (code == KeyEvent.KEYCODE_DPAD_LEFT) return true;
        if (code == KeyEvent.KEYCODE_DPAD_DOWN && source == profile) {
            if (icons.getChildCount() > 0) icons.getChildAt(0).requestFocus();
            else settings.requestFocus();
            return true;
        }
        if (code == KeyEvent.KEYCODE_DPAD_DOWN &&
                icons.getChildCount() > 0 && source == icons.getChildAt(icons.getChildCount() - 1)) {
            settings.requestFocus();
            return true;
        }
        if (code == KeyEvent.KEYCODE_DPAD_UP && source == settings) {
            if (icons.getChildCount() > 0)
                icons.getChildAt(icons.getChildCount() - 1).requestFocus();
            else profile.requestFocus();
            return true;
        }
        if (code == KeyEvent.KEYCODE_DPAD_UP &&
                icons.getChildCount() > 0 && source == icons.getChildAt(0)) {
            profile.requestFocus();
            return true;
        }
        return false;
    }

    private void showTooltip(View source, String label) {
        if (tooltip == null) return;
        if (label == null) {
            tooltip.setVisibility(View.GONE);
            return;
        }
        tooltip.setText(label);
        int[] pos = new int[2];
        int[] origin = new int[2];
        source.getLocationOnScreen(pos);
        overlay.getLocationOnScreen(origin);
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) tooltip.getLayoutParams();
        params.topMargin = Math.max(dp(12), pos[1] - origin[1] + dp(7));
        tooltip.setLayoutParams(params);
        tooltip.setVisibility(View.VISIBLE);
    }

    void scheduleUpdate() {
        if (destroyed) return;
        overlay.removeCallbacks(rebuild);
        overlay.postDelayed(rebuild, 80);
    }

    void update() {
        if (destroyed) return;
        ArrayObjectAdapter adapter = browse.fixedRailAdapter();
        if (adapter == null) return;
        long focusId = Long.MIN_VALUE;
        for (Map.Entry<Long, ImageView> entry : buttons.entrySet()) {
            if (entry.getValue().hasFocus()) focusId = entry.getKey();
        }
        icons.removeAllViews();
        buttons.clear();

        for (int i = 0; i < adapter.size(); i++) {
            Object item = adapter.get(i);
            if (!(item instanceof PageRow)) continue;
            HeaderItem header = ((PageRow) item).getHeaderItem();
            if (!(header instanceof SectionHeaderItem)) continue;
            SectionHeaderItem section = (SectionHeaderItem) header;
            if (section.getId() == MediaGroup.TYPE_SETTINGS) continue;

            ImageView icon = button(section.getResId() > 0 ? section.getResId() : R.drawable.icon_playlist, section.getName().toString());
            LinearLayout.LayoutParams iconLayout = new LinearLayout.LayoutParams(dp(48), dp(48));
            iconLayout.gravity = Gravity.CENTER_HORIZONTAL;
            iconLayout.bottomMargin = dp(7);
            icons.addView(icon, iconLayout);
            long id = section.getId();
            buttons.put(id, icon);
            icon.setSelected(id == selectedSection);
            icon.setOnClickListener(v -> browse.selectFixedRailSection(id, true));
            icon.setOnLongClickListener(v -> {
                SectionMenuPresenter.instance(overlay.getContext()).showMenu(section.getSection());
                return true;
            });
            icon.setOnFocusChangeListener((v, focused) -> {
                icon.setSelected(focused || id == selectedSection);
                icon.setColorFilter(focused ? 0xFF6AD5FF : 0xFFC2D9F3);
                showTooltip(icon, focused ? section.getName().toString() : null);
            });
            icon.setOnKeyListener((v, keyCode, event) -> handleDirection(v, event, keyCode));

            if (id == MediaGroup.TYPE_HOME) {
                ImageView search = button(R.drawable.mars_rail_search,
                        overlay.getContext().getString(R.string.settings_search));
                LinearLayout.LayoutParams searchLayout = new LinearLayout.LayoutParams(dp(48), dp(48));
                searchLayout.gravity = Gravity.CENTER_HORIZONTAL;
                searchLayout.bottomMargin = dp(7);
                icons.addView(search, searchLayout);
                search.setOnClickListener(v -> SearchPresenter.instance(overlay.getContext()).startSearch(null));
                search.setOnFocusChangeListener((v, focused) -> showTooltip(search,
                        focused ? overlay.getContext().getString(R.string.settings_search) : null));
                search.setOnKeyListener((v, keyCode, event) -> handleDirection(v, event, keyCode));
            }
        }
        settings.setSelected(selectedSection == MediaGroup.TYPE_SETTINGS);
        if (buttons.containsKey(focusId)) buttons.get(focusId).requestFocus();
        refreshProfile();
    }

    void setSelectedSection(long id) {
        selectedSection = id;
        for (Map.Entry<Long, ImageView> entry : buttons.entrySet()) {
            entry.getValue().setSelected(entry.getKey() == id || entry.getValue().hasFocus());
        }
        settings.setSelected(id == MediaGroup.TYPE_SETTINGS || settings.hasFocus());
    }

    void focusSelected() {
        View target = buttons.get(selectedSection);
        if (target == null) target = buttons.get(MediaGroup.TYPE_HOME);
        if (target == null) target = profile;
        target.requestFocus();
    }

    void refreshProfile() {
        if (destroyed || profile == null) return;
        Account account = MediaServiceManager.instance().getSelectedAccount();
        String url = account != null ? account.getAvatarImageUrl() : null;
        if (url != null && !url.isEmpty()) {
            profile.clearColorFilter();
            Glide.with(profile).load(url).circleCrop()
                    .placeholder(R.drawable.browse_title_account)
                    .into(profile);
        } else {
            Glide.with(profile).clear(profile);
            profile.setImageResource(R.drawable.browse_title_account);
            profile.setColorFilter(0xFFC2D9F3);
        }
    }

    void destroy() {
        destroyed = true;
        overlay.removeCallbacks(rebuild);
        Glide.with(profile).clear(profile);
        tooltip.setVisibility(View.GONE);
        buttons.clear();
        overlay.removeView(tooltip);
        overlay.removeView(rail);
    }
}
