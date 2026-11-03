#!/usr/bin/env python3
"""MarsTube 0.0.5: television BACK + D-pad navigation through phone-flavor submenus.
Applied ONLY to the pinned disposable NewTube reference AFTER patch_tv_shell.py.
New intent route preserves the player on the Android back stack: NEVER CLEAR_TOP.
"""
from pathlib import Path
import sys

root = Path(sys.argv[1]).resolve()
base = root / "smarttubetv/src/stmobile/java/com/newtube/mobile/ui"
player = base / "playback/MobilePlaybackActivity.java"
browse = base / "browse/MobileBrowseActivity.java"
mobile = base / "common/MobileActivity.java"
settings = base / "settings/SettingsPageFragment.java"
adapter = base / "settings/SettingsAdapter.java"
if not (root / "sabr-media3").is_dir():
    raise SystemExit("Expected a disposable NewTube reference checkout")

def once(path, old, new):
    code = path.read_text(encoding="utf-8")
    count = code.count(old)
    if count != 1:
        raise SystemExit(f"UNSAFE navigation anchor in {path.name}: {count}: {old[:70]!r}")
    path.write_text(code.replace(old, new, 1), encoding="utf-8")

# Previous MT2-005 destroyed the video Activity via CLEAR_TOP. This made Back
# from Search/Settings sometimes jump to an unrelated, stale root screen.
once(player, '''                        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                                | android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);''',
'''                        // 0.0.5: preserve the active player beneath the destination.
                        // Native Back can now return to it through Browse/Search/Settings.
                        intent.putExtra("mars_tv_return_to_player", true);
                        startActivity(intent);''')

# Never let a nested TV Settings/Search/Browse destination incorrectly background
# or terminate the whole task through the legacy ViewManager stack.
once(mobile, '''    @Override
    public void finish() {
        if (!isBackStackScreen()) {''',
'''    @Override
    public void finish() {
        // 0.0.5: Android task stack is authoritative for native submenus.
        // Device BACK must return to the Activity underneath, not close the player.
        boolean tvChild = this instanceof com.newtube.mobile.ui.settings.MobileSettingsActivity
                || this instanceof com.newtube.mobile.ui.search.MobileSearchActivity
                || (getIntent() != null
                    && getIntent().getBooleanExtra("mars_tv_return_to_player", false));
        if (tvChild && !isTaskRoot()) {
            if (this instanceof com.newtube.mobile.ui.search.MobileSearchActivity) {
                ((com.newtube.mobile.ui.search.MobileSearchActivity) this)
                        .marsTvBeforeFinish();
            }
            super.finish();
            return;
        }
        if (!isBackStackScreen()) {''')

# The phone browse grid was keyboard-bridged in MT2-002, but it captured every
# arrow and gave NO pathway to bottom navigation. New TV bridge makes those tabs
# reachable with LEFT at the first card or MENU from anywhere in the grid.
navigation = r'''
    // MarsTube 0.0.5: D-pad navigation from video grid to submenus.
    private boolean mMarsNavFocused;
    private int mMarsNavIndex;
    private int mMarsNavPrevious = -1;

    private void marsHighlightNav() {
        if (mBottomNav == null || mBottomNav.getMenu().size() == 0) return;
        int size = mBottomNav.getMenu().size();
        mMarsNavIndex = Math.max(0, Math.min(mMarsNavIndex, size - 1));
        if (mMarsNavPrevious >= 0 && mMarsNavPrevious < size) {
            android.view.View previous = mBottomNav.findViewById(
                    mBottomNav.getMenu().getItem(mMarsNavPrevious).getItemId());
            if (previous != null) previous.setForeground(null);
        }
        mMarsNavPrevious = mMarsNavFocused ? mMarsNavIndex : -1;
        if (!mMarsNavFocused) return;
        android.view.View current = mBottomNav.findViewById(
                mBottomNav.getMenu().getItem(mMarsNavIndex).getItemId());
        if (current == null) return;
        android.graphics.drawable.GradientDrawable focus =
                new android.graphics.drawable.GradientDrawable();
        focus.setColor(0x3330A8FF);
        focus.setCornerRadius(12 * getResources().getDisplayMetrics().density);
        focus.setStroke(Math.max(2, (int) (2 * getResources().getDisplayMetrics().density)),
                0xFF00A3FF);
        current.setForeground(focus);
        current.requestFocus();
        android.util.Log.i("MarsTubeTV", "browse nav=" + mMarsNavIndex);
    }

    private boolean marsTvNavigationKey(KeyEvent event) {
        if (mBottomNav == null || mBottomNav.getMenu().size() == 0) return false;
        int key = event.getKeyCode();
        if (key == KeyEvent.KEYCODE_MENU) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                mMarsNavFocused = true;
                marsHighlightNav();
            }
            return true;
        }
        if (key == KeyEvent.KEYCODE_BACK && mMarsNavFocused) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                mMarsNavFocused = false;
                marsHighlightNav();
            }
            return true;
        }
        if (!mMarsNavFocused
                && key == KeyEvent.KEYCODE_DPAD_LEFT
                && mMarsTvSelection <= 0 && !mYouShowing) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                mMarsNavFocused = true;
                marsHighlightNav();
            }
            return true;
        }
        if (!mMarsNavFocused) return false;
        boolean handled = key == KeyEvent.KEYCODE_DPAD_UP
                || key == KeyEvent.KEYCODE_DPAD_DOWN
                || key == KeyEvent.KEYCODE_DPAD_LEFT
                || key == KeyEvent.KEYCODE_DPAD_RIGHT
                || key == KeyEvent.KEYCODE_ENTER
                || key == KeyEvent.KEYCODE_NUMPAD_ENTER
                || key == KeyEvent.KEYCODE_DPAD_CENTER;
        if (!handled) return false;
        if (event.getAction() != KeyEvent.ACTION_DOWN) return true;
        switch (key) {
            case KeyEvent.KEYCODE_DPAD_UP:
                mMarsNavIndex = Math.max(0, mMarsNavIndex - 1);
                marsHighlightNav();
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                mMarsNavIndex = Math.min(mBottomNav.getMenu().size() - 1, mMarsNavIndex + 1);
                marsHighlightNav();
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                mMarsNavFocused = false;
                marsHighlightNav();
                marsTvFocusCard(mMarsTvSelection);
                break;
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
            case KeyEvent.KEYCODE_DPAD_CENTER:
                if (mBottomNav != null && mBottomNav.getMenu().size() > mMarsNavIndex) {
                    int selectedId = mBottomNav.getMenu().getItem(mMarsNavIndex).getItemId();
                    mBottomNav.setSelectedItemId(selectedId);
                }
                mMarsNavFocused = false;
                marsHighlightNav();
                break;
        }
        return true;
    }
'''
once(browse,
'''    private void setupBottomNav() {''',
navigation + '''
    private void setupBottomNav() {''')
once(browse,
'''        if (marsTvBrowseKey(event)) return true;''',
'''        if (marsTvNavigationKey(event)) return true;
        // The native profile/settings panel must receive its own D-pad focus.
        if (!mYouShowing && marsTvBrowseKey(event)) return true;''')

# The profile panel was completely unreachable because the temporary TV grid
# swallowed all arrows/Enter even while the panel covered it.
once(browse,
'''        mYouPanel.setVisibility(View.VISIBLE);
        onYouPanelToggled();''',
'''        mYouPanel.setVisibility(View.VISIBLE);
        onYouPanelToggled();
        mYouRows.post(() -> {
            if (!mYouShowing) return;
            for (int i = 0; i < mYouRows.getChildCount(); i++) {
                android.view.View row = mYouRows.getChildAt(i);
                if (row.isFocusable()) { row.requestFocus(); break; }
            }
        });''')
once(browse,
'''        row.setOnClickListener(v -> action.run());
        mYouRows.addView(row);''',
'''        row.setFocusable(true); // 0.0.5: Fire TV remote can select profile options
        row.setOnClickListener(v -> action.run());
        mYouRows.addView(row);''')
once(browse,
'''        mYouPanel.setVisibility(View.GONE);
        onYouPanelToggled();''',
'''        mYouPanel.setVisibility(View.GONE);
        onYouPanelToggled();
        mContentGrid.post(() -> marsTvFocusCard(mMarsTvSelection));''')

# Newly routed Browse from player should not use its stale 'You' panel from an
# unrelated tab; on Back, a You panel should close first instead of task exit.
once(browse,
'''    private void handleBack() {
        // A grid section opened from a You-panel row:''',
'''    private void handleBack() {
        if (mMarsNavFocused) {
            mMarsNavFocused = false;
            marsHighlightNav();
            return;
        }
        // A grid section opened from a You-panel row:''')

# Make settings rows genuinely focusable with the remote. Their click listener is
# already connected to the real settings actions: no duplicate business logic.
once(adapter,
'''            itemView.setOnClickListener(enabled ? v -> listener.onRowClicked(row) : null);''',
'''            itemView.setFocusable(enabled);
            itemView.setFocusableInTouchMode(false);
            itemView.setOnClickListener(enabled ? v -> listener.onRowClicked(row) : null);''')

# SearchPresenter needs its own onFinish() once even when we bypass the old
# ViewManager parent launch (which was causing the back-navigation problem).
search = base / "search/MobileSearchActivity.java"
once(search,
'''    @Override
    public void finishReally() {''',
'''    // MarsTube 0.0.5: cleanup when returning via Android's real Activity stack.
    public void marsTvBeforeFinish() {
        if (mPresenter != null && mPresenter.getView() == this) {
            mPresenter.onFinish();
        }
    }

    @Override
    public void finishReally() {''')

print("MarsTube 0.0.5 TV navigation: native Back preserved + browse tabs accessible + settings row D-pad focus.")
