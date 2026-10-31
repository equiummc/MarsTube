package com.liskovsoft.smartyoutubetv2.tv.ui.browse;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;

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

    public void updateMarsHero(Video video) {
        Fragment main = getSupportFragmentManager().findFragmentById(R.id.main_frame);
        if (main instanceof BrowseFragment) {
            ((BrowseFragment) main).updateMarsHero(video);
        }
    }

    @Override
    protected void initTheme() {
        int browseThemeResId = MainUIData.instance(this).getColorScheme().browseThemeResId;
        if (browseThemeResId > 0) {
            setTheme(browseThemeResId);
        }
    }
}
