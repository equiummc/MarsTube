package com.newtube.mobile.ui.browse;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup;
import com.liskovsoft.smartyoutubetv2.tv.R;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/**
 * MT2-035 Punkt B: TV browse visuals, without a second service/player/grid.
 * The channel rail is derived only from already loaded subscriptions. It never
 * pretends to be a complete account inventory, nor starts background requests.
 */
public final class MarsTvBrowseB035 {
    public interface SectionRouter { void open(String section); }
    private static final int BLUE = 0xFF21BAFF;
    private static final int WHITE = 0xFFF1F7FF;
    private static final int MUTED = 0xFFAFC6DD;
    private final MobileBrowseActivity activity;
    private final ConstraintLayout root;
    private final ViewGroup swipe;
    private final RecyclerView grid;
    private final Runnable filterChanged;
    private final Runnable gridFocus;
    private final SectionRouter router;
    private final LinearLayout leftPane;
    private final LinearLayout channelRows;
    private final TextView clock, date;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ArrayList<TextView> channelButtons = new ArrayList<>();
    private final LinkedHashMap<String,String> channels = new LinkedHashMap<>();
    private final int initialLeftMargin;
    private final int baseStartPad, baseTopPad, baseEndPad, baseBottomPad;
    private String chosenChannel = "";
    private String channelSignature = "";
    private int currentSection = -1;
    private boolean running;
    private boolean libraryVisible;
    private final Runnable updateTime = new Runnable() {
        @Override public void run() {
            if (!running) return;
            Date now = new Date();
            clock.setText(new SimpleDateFormat("HH:mm", Locale.GERMANY).format(now));
            date.setText(new SimpleDateFormat("EEE, dd.MM.yyyy", Locale.GERMANY).format(now));
            handler.postDelayed(this, Math.max(500L, 60_000L
                    - (System.currentTimeMillis() % 60_000L)));
        }
    };

    private int dp(float value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    private static GradientDrawable background(int fill, int stroke, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(radius);
        d.setStroke(2, stroke);
        return d;
    }

    public MarsTvBrowseB035(MobileBrowseActivity activity, ConstraintLayout root,
                             ViewGroup swipe, RecyclerView grid,
                             Runnable filterChanged, Runnable gridFocus,
                             SectionRouter router) {
        this.activity = activity;
        this.root = root;
        this.swipe = swipe;
        this.grid = grid;
        this.filterChanged = filterChanged;
        this.gridFocus = gridFocus;
        this.router = router;
        ConstraintLayout.LayoutParams swipeLp =
                (ConstraintLayout.LayoutParams) swipe.getLayoutParams();
        initialLeftMargin = swipeLp.leftMargin;
        baseStartPad = grid.getPaddingStart();
        baseTopPad = grid.getPaddingTop();
        baseEndPad = grid.getPaddingEnd();
        baseBottomPad = grid.getPaddingBottom();

        LinearLayout timeBox = new LinearLayout(activity);
        timeBox.setId(View.generateViewId());
        timeBox.setOrientation(LinearLayout.VERTICAL);
        timeBox.setGravity(Gravity.END);
        timeBox.setPadding(0, dp(3), 0, 0);
        clock = new TextView(activity);
        clock.setTextColor(WHITE);
        clock.setTextSize(21);
        clock.setTypeface(null, Typeface.BOLD);
        clock.setGravity(Gravity.END);
        date = new TextView(activity);
        date.setTextColor(MUTED);
        date.setTextSize(12);
        date.setGravity(Gravity.END);
        timeBox.addView(clock, new LinearLayout.LayoutParams(-1, dp(27)));
        timeBox.addView(date, new LinearLayout.LayoutParams(-1, dp(22)));
        ConstraintLayout.LayoutParams timeLp = new ConstraintLayout.LayoutParams(
                dp(175), ViewGroup.LayoutParams.WRAP_CONTENT);
        timeLp.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
        timeLp.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
        timeLp.rightMargin = dp(20);
        timeLp.topMargin = dp(3);
        root.addView(timeBox, timeLp);

        // Preserve search and cast: move their existing controls left of clock.
        View search = root.findViewById(R.id.mobile_search_button);
        if (search != null && search.getLayoutParams() instanceof ConstraintLayout.LayoutParams) {
            ConstraintLayout.LayoutParams lp =
                    (ConstraintLayout.LayoutParams) search.getLayoutParams();
            lp.endToEnd = ConstraintLayout.LayoutParams.UNSET;
            lp.endToStart = timeBox.getId();
            lp.rightMargin = dp(6);
            search.setLayoutParams(lp);
        }

        leftPane = new LinearLayout(activity);
        leftPane.setOrientation(LinearLayout.VERTICAL);
        leftPane.setPadding(dp(9), dp(10), dp(9), dp(8));
        leftPane.setBackground(new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xE1091C31, 0xF20E2B47}));
        leftPane.setVisibility(View.GONE);
        ConstraintLayout.LayoutParams paneLp = new ConstraintLayout.LayoutParams(
                dp(sideWidth()), 0);
        paneLp.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
        paneLp.topToBottom = R.id.mobile_title_bar;
        paneLp.bottomToTop = R.id.mobile_bottom_nav;
        paneLp.leftMargin = dp(com.newtube.mobile.ui.common.MarsTvNavigationTheme.WIDTH_DP + 12);
        root.addView(leftPane, paneLp);

        TextView headline = new TextView(activity);
        headline.setText("Deine Abos");
        headline.setTextColor(WHITE);
        headline.setTextSize(19);
        headline.setTypeface(null, Typeface.BOLD);
        headline.setPadding(dp(10), dp(4), dp(6), dp(5));
        leftPane.addView(headline, new LinearLayout.LayoutParams(-1, dp(42)));
        TextView caption = new TextView(activity);
        caption.setText("Kanäle aus geladenen Videos");
        caption.setTextColor(MUTED);
        caption.setTextSize(11);
        caption.setMaxLines(2);
        caption.setPadding(dp(10), 0, dp(4), dp(8));
        leftPane.addView(caption, new LinearLayout.LayoutParams(-1, dp(39)));
        View divider = new View(activity);
        divider.setBackgroundColor(0xFF386181);
        leftPane.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(false);
        scroll.setVerticalScrollBarEnabled(false);
        channelRows = new LinearLayout(activity);
        channelRows.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(channelRows);
        leftPane.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
    }

    private int sideWidth() {
        int dpWidth = activity.getResources().getConfiguration().screenWidthDp;
        return dpWidth >= 850 ? 210 : 155;
    }

    private static String key(Video video) {
        if (video == null || video.videoId == null) return null;
        if (!TextUtils.isEmpty(video.channelId)) return "id:" + video.channelId;
        String name = video.getAuthor();
        return TextUtils.isEmpty(name) ? null : "name:" + name.toLowerCase(Locale.ROOT);
    }

    public List<Video> visible(List<Video> input, int section) {
        if (section != MediaGroup.TYPE_SUBSCRIPTIONS || chosenChannel.isEmpty()) return input;
        ArrayList<Video> result = new ArrayList<>();
        if (input != null) for (Video video : input) {
            if (chosenChannel.equals(key(video))) result.add(video);
        }
        return result;
    }

    public void onSection(int section) {
        if (section != currentSection) {
            currentSection = section;
            chosenChannel = "";
            channelSignature = "";
            channels.clear();
        }
        boolean subs = section == MediaGroup.TYPE_SUBSCRIPTIONS;
        leftPane.setVisibility(subs && !libraryVisible ? View.VISIBLE : View.GONE);
        ConstraintLayout.LayoutParams lp =
                (ConstraintLayout.LayoutParams) swipe.getLayoutParams();
        int margin = subs ? dp(com.newtube.mobile.ui.common.MarsTvNavigationTheme.WIDTH_DP
                + sideWidth() + 24) : initialLeftMargin;
        if (lp.leftMargin != margin) {
            lp.leftMargin = margin;
            swipe.setLayoutParams(lp);
        }
        grid.setPaddingRelative(baseStartPad + dp(subs ? 11 : 20),
                baseTopPad + dp(8), baseEndPad + dp(15), baseBottomPad);
        if (!subs) channelButtons.clear();
    }

    public void setLibraryVisible(boolean visible) {
        libraryVisible = visible;
        leftPane.setVisibility(!visible && currentSection == MediaGroup.TYPE_SUBSCRIPTIONS
                ? View.VISIBLE : View.GONE);
    }

    public void onVideos(List<Video> videos) {
        if (currentSection != MediaGroup.TYPE_SUBSCRIPTIONS) return;
        LinkedHashMap<String,String> fresh = new LinkedHashMap<>();
        fresh.put("", "Alle");
        if (videos != null) for (Video v : videos) {
            String k = key(v);
            if (k != null && !fresh.containsKey(k)) {
                fresh.put(k, TextUtils.isEmpty(v.getAuthor()) ? "Kanal" : v.getAuthor());
            }
            if (fresh.size() >= 41) break;
        }
        String signature = fresh.toString();
        if (signature.equals(channelSignature)) return;
        channelSignature = signature;
        channels.clear();
        channels.putAll(fresh);
        if (!chosenChannel.isEmpty() && !channels.containsKey(chosenChannel))
            chosenChannel = "";
        rebuildChannels();
    }

    private void rebuildChannels() {
        channelRows.removeAllViews();
        channelButtons.clear();
        for (java.util.Map.Entry<String,String> item : channels.entrySet()) {
            final String key = item.getKey();
            TextView button = new TextView(activity);
            button.setText(item.getValue());
            button.setTextSize(15);
            button.setSingleLine(true);
            button.setEllipsize(TextUtils.TruncateAt.END);
            button.setGravity(Gravity.CENTER_VERTICAL);
            button.setPadding(dp(12), 0, dp(8), 0);
            button.setFocusable(true);
            button.setClickable(true);
            button.setContentDescription(item.getValue());
            button.setOnFocusChangeListener((v, focused) ->
                    paintButton(button, focused || key.equals(chosenChannel)));
            button.setOnClickListener(v -> {
                chosenChannel = key;
                refreshButtons();
                filterChanged.run();
                gridFocus.run();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(46));
            lp.bottomMargin = dp(5);
            channelRows.addView(button, lp);
            channelButtons.add(button);
            paintButton(button, key.equals(chosenChannel));
        }
    }

    private void paintButton(TextView button, boolean focused) {
        button.setTextColor(focused ? 0xFF07213A : WHITE);
        button.setBackground(background(focused ? 0xFFE7F8FF : 0xCF203C58,
                focused ? BLUE : 0x335E8BAC, dp(10)));
    }

    private void refreshButtons() {
        int i = 0;
        for (String key : channels.keySet()) {
            if (i < channelButtons.size()) {
                TextView button = channelButtons.get(i++);
                paintButton(button, button.hasFocus() || key.equals(chosenChannel));
            }
        }
    }

    public boolean handleKey(KeyEvent event, int selected, int columns) {
        if (currentSection != MediaGroup.TYPE_SUBSCRIPTIONS
                || leftPane.getVisibility() != View.VISIBLE) return false;
        int code = event.getKeyCode();
        View focused = activity.getCurrentFocus();
        boolean inPane = focused != null && isDescendant(focused, leftPane);
        if (inPane) {
            boolean control = code == KeyEvent.KEYCODE_DPAD_LEFT
                    || code == KeyEvent.KEYCODE_DPAD_RIGHT
                    || code == KeyEvent.KEYCODE_BACK
                    || code == KeyEvent.KEYCODE_DPAD_UP
                    || code == KeyEvent.KEYCODE_DPAD_DOWN
                    || code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER;
            if (!control) return false;
            if (event.getAction() != KeyEvent.ACTION_DOWN || event.getRepeatCount() > 0) return true;
            if (code == KeyEvent.KEYCODE_DPAD_RIGHT) gridFocus.run();
            else if (code == KeyEvent.KEYCODE_DPAD_LEFT || code == KeyEvent.KEYCODE_BACK)
                com.newtube.mobile.ui.common.MarsTvRail.focus(activity);
            else if (code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER)
                focused.performClick();
            else {
                int index = channelButtons.indexOf(focused);
                int target = index + (code == KeyEvent.KEYCODE_DPAD_UP ? -1 : 1);
                if (target >= 0 && target < channelButtons.size())
                    channelButtons.get(target).requestFocus();
            }
            return true;
        }
        if (code == KeyEvent.KEYCODE_DPAD_LEFT && selected >= 0
                && selected % Math.max(1, columns) == 0
                && (grid.hasFocus() || focused == grid)) {
            if (event.getAction() == KeyEvent.ACTION_DOWN && !channelButtons.isEmpty())
                channelButtons.get(0).requestFocus();
            return true;
        }
        return false;
    }

    private boolean isDescendant(View focus, ViewGroup ancestor) {
        View cursor = focus;
        while (cursor != null) {
            if (cursor == ancestor) return true;
            android.view.ViewParent parent = cursor.getParent();
            cursor = parent instanceof View ? (View) parent : null;
        }
        return false;
    }

    public void insertLibraryTiles(LinearLayout container) {
        LinearLayout row = new LinearLayout(activity);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(18), dp(9), dp(18), dp(20));
        addShortcut(row, "Verlauf", "history", 0xFF4F4B9B);
        addShortcut(row, "Später ansehen", "later", 0xFF137F9C);
        addShortcut(row, "Playlists", "playlists", 0xFF087C77);
        addShortcut(row, "Musik", "music", 0xFFAB4C38);
        // The update notice, if present, remains first; shortcuts appear below it.
        container.addView(row, Math.min(container.getChildCount(), 1),
                new LinearLayout.LayoutParams(-1, dp(130)));
    }

    private void addShortcut(LinearLayout parent, String title, String destination, int accent) {
        TextView tile = new TextView(activity);
        tile.setText(title);
        tile.setTextSize(15);
        tile.setTypeface(null, Typeface.BOLD);
        tile.setTextColor(WHITE);
        tile.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
        tile.setPadding(dp(12), 0, dp(5), 0);
        tile.setFocusable(true);
        tile.setClickable(true);
        tile.setBackground(background(accent, 0xAA4FB7E6, dp(14)));
        tile.setOnFocusChangeListener((v, focus) -> {
            tile.setTextColor(focus ? 0xFF063252 : WHITE);
            tile.setBackground(background(focus ? 0xFFDCF6FF : accent,
                    focus ? BLUE : 0xAA4FB7E6, dp(14)));
        });
        tile.setOnClickListener(v -> router.open(destination));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(83), 1f);
        lp.rightMargin = dp(8);
        parent.addView(tile, lp);
    }

    public void resume() {
        running = true;
        handler.removeCallbacks(updateTime);
        updateTime.run();
    }

    public void pause() {
        running = false;
        handler.removeCallbacks(updateTime);
    }
}
