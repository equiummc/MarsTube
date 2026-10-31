package com.liskovsoft.smartyoutubetv2.tv.ui.browse;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import androidx.fragment.app.Fragment;
import androidx.leanback.widget.HorizontalGridView;

import com.liskovsoft.sharedutils.helpers.MessageHelpers;
import com.liskovsoft.smartyoutubetv2.common.prefs.MainUIData;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.ui.common.LeanbackActivity;

public class BrowseActivity extends LeanbackActivity {
    private static final String TAG = BrowseActivity.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.fragment_main);
        } catch (NoClassDefFoundError e) {
            // Failed resolution of: Landroidx/lifecycle/ViewTreeLifecycleOwner;
            MessageHelpers.showMessage(this, e.getMessage());
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN
                && event.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT
                && isFirstVideoInRow(getCurrentFocus())) {
            Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.main_frame);
            if (fragment instanceof BrowseFragment) {
                ((BrowseFragment) fragment).focusFixedRail();
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    private boolean isFirstVideoInRow(View focused) {
        if (focused == null) return false;
        View child = focused;
        ViewParent parent = focused.getParent();
        while (parent instanceof ViewGroup) {
            if (parent instanceof HorizontalGridView) {
                int position = ((HorizontalGridView) parent).getChildAdapterPosition(child);
                return position == 0;
            }
            child = (View) parent;
            parent = child.getParent();
        }
        return false;
    }

    @Override
    protected void initTheme() {
        int browseThemeResId = MainUIData.instance(this).getColorScheme().browseThemeResId;
        if (browseThemeResId > 0) {
            setTheme(browseThemeResId);
        }
    }
}
