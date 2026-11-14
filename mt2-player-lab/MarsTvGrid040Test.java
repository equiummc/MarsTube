package com.newtube.mobile.ui.browse;

import android.app.Activity;
import android.app.Application;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup;
import com.liskovsoft.smartyoutubetv2.tv.R;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,manifest=Config.NONE,application=Application.class)
@LooperMode(LooperMode.Mode.PAUSED)
public class MarsTvGrid040Test {
    @Test public void actualInflatedCardsKeepAspectAndTextPlacementAtTvViewports() {
        Application app = RuntimeEnvironment.getApplication();
        app.setTheme(com.google.android.material.R.style.Theme_MaterialComponents);
        app.getSharedPreferences(com.newtube.mobile.player.MarsTvPlayerPrefs.PREFS,0).edit().clear().commit();
        for (int[] viewport : new int[][]{{1700,960},{1080,620},{3400,1920}}) {
            for (int section : new int[]{MediaGroup.TYPE_HOME,MediaGroup.TYPE_HISTORY,
                    MediaGroup.TYPE_LIVE,MediaGroup.TYPE_MUSIC,MediaGroup.TYPE_SUBSCRIPTIONS,
                    MediaGroup.TYPE_SHORTS}) {
                boolean shorts = section == MediaGroup.TYPE_SHORTS;
                RecyclerView grid = new RecyclerView(app);
                grid.setTag(R.id.mobile_content_grid,section);
                grid.layout(0,0,viewport[0],viewport[1]);
                View card = LayoutInflater.from(app).inflate(R.layout.item_video_card,grid,false);
                card.setTag(R.id.mobile_content_grid,grid);
                ((TextView)card.findViewById(R.id.video_title)).setText("A real title that wraps to several lines");
                ((TextView)card.findViewById(R.id.video_meta)).setText("Channel information");
                MarsTvGrid040.bind(card);
                int cellWidth = viewport[0]/(shorts ? 2 : 3);
                int height = card.getLayoutParams().height;
                card.measure(View.MeasureSpec.makeMeasureSpec(cellWidth,View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));
                card.layout(0,0,cellWidth,height);
                View image = card.findViewById(R.id.video_thumbnail_frame);
                View title = card.findViewById(R.id.video_title);
                assertTrue("image must have an area",image.getWidth()>0 && image.getHeight()>0);
                double aspect = (double)image.getWidth()/image.getHeight();
                assertEquals(shorts ? 9d/16 : 16d/9,aspect,0.02);
                if (shorts) assertTrue("Shorts title right of image",title.getLeft()>=image.getRight());
                else assertTrue("video title below image",title.getTop()>=image.getBottom());
                ViewGroup.MarginLayoutParams lp=(ViewGroup.MarginLayoutParams)card.getLayoutParams();
                assertTrue("two complete rows fit",2*(height+lp.topMargin+lp.bottomMargin)<=viewport[1]);
            }
        }
    }

    @Test public void loaderCancelsFastLoadsCompletionAndWindowDetach() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        MarsKnightRider040 loader = new MarsKnightRider040(activity);
        activity.setContentView(loader);
        loader.setLoading(true);
        shadowOf(android.os.Looper.getMainLooper()).idleFor(200,TimeUnit.MILLISECONDS);
        assertEquals(View.GONE,loader.getVisibility());
        loader.setLoading(false);
        shadowOf(android.os.Looper.getMainLooper()).idleFor(150,TimeUnit.MILLISECONDS);
        assertEquals(View.GONE,loader.getVisibility());
        loader.setLoading(true);
        shadowOf(android.os.Looper.getMainLooper()).idleFor(350,TimeUnit.MILLISECONDS);
        assertEquals(View.VISIBLE,loader.getVisibility());
        loader.setLoading(false);
        assertEquals(View.GONE,loader.getVisibility());
        loader.setLoading(true);
        activity.setContentView(new FrameLayout(activity));
        shadowOf(android.os.Looper.getMainLooper()).idleFor(400,TimeUnit.MILLISECONDS);
        assertEquals(View.GONE,loader.getVisibility());
    }
}
