#!/usr/bin/env python3
"""MT2-029: TV Shorts three-column polish, compact actions, focus and comments.

Applied AFTER 028 in disposable pinned NewTube tree. Anchors fail closed.
No changes to app package, updater sources, Media3 decoder or signing.
"""
from pathlib import Path
import sys, re, shutil

if len(sys.argv) != 2:
    raise SystemExit("Usage: patch_029_shorts_compact.py <pinned-checkout>")
root=Path(sys.argv[1]).resolve()
here=Path(__file__).resolve().parent
tv=root/"smarttubetv/src/stmobile/java/com/newtube/mobile/ui"
stage=tv/"playback/MarsTvShortsStage.java"
shell=tv/"playback/MarsTvShell.java"
playback=tv/"playback/MobilePlaybackActivity.java"
search=tv/"playback/MarsTvShortsSearchPanel.java"
comments=tv/"playback/CommentsPanel.java"
adapter=tv/"playback/CommentsAdapter.java"
layout=tv/"playback/CommentsPanelLayout.java"
if not (root/"sabr-media3").is_dir():
    raise SystemExit("MT2-029 pinned source checkout missing")
def once(path,old,new):
    s=path.read_text(encoding="utf-8")
    n=s.count(old)
    if n!=1: raise SystemExit("MT2-029 unsafe %s: count=%s for %r"%(path.name,n,old[:110]))
    path.write_text(s.replace(old,new,1),encoding="utf-8")
def replace_between(path,start,end,new):
    s=path.read_text(encoding="utf-8")
    if s.count(start)!=1 or s.count(end)!=1 or s.index(end)<s.index(start):
        raise SystemExit("MT2-029 unsafe region "+str(path))
    a=s.index(start);b=s.index(end,a)
    path.write_text(s[:a]+new+s[b:],encoding="utf-8")

# A proper arrow (not the repeat icon) for automatic NEXT.
drawable=root/"smarttubetv/src/stmobile/res/drawable"
shutil.copyfile(here/"assets/drawable/mars_short_next_029.xml",
                drawable/"mars_short_next_029.xml")

# All three visual columns share videoTop/videoHeight. Left card gets a symmetric
# 8dp top/bottom padding and ONE 10dp center gap, so preview tiles grow.
once(stage,"    private final View progress;",
     "    private final View progress;\n    private final View videoFrame;\n    private boolean videoFocused;")
once(stage,"    private final TextView dislikeCountLabel;\n","")
once(stage,"        View videoFrame = new View(activity);",
     "        videoFrame = new View(activity);")
once(stage,"        left.setPadding(dp(8), dp(12), dp(8), dp(12));",
     "        left.setPadding(dp(8), dp(8), dp(8), dp(8));")
once(stage,"screenHeight - dp(76), Gravity.LEFT | Gravity.CENTER_VERTICAL);\n        leftLp.leftMargin = dp(16);",
     "videoHeight, Gravity.TOP | Gravity.LEFT);\n        leftLp.leftMargin = dp(16);\n        leftLp.topMargin = videoTop;")
once(stage,"int previewImageWidth = Math.min(Math.round((videoLeft - dp(32)) * 0.41f),\n                Math.max(dp(78), Math.round((screenHeight - dp(115)) * 9f / 32f)));",
     "int previewImageWidth = Math.min(Math.round((videoLeft - dp(32)) * 0.42f),\n                Math.max(dp(78), Math.round((videoHeight - dp(34)) * 9f / 32f)));")
once(stage,"            TextView title = label(13, WHITE);",
     "            TextView title = label(12, WHITE);")
once(stage,"            TextView author = label(12, BLUE);",
     "            TextView author = label(11, BLUE);")
once(stage,"            rowLp.bottomMargin = dp(10);",
     "            rowLp.bottomMargin = i == 0 ? dp(10) : 0;")

# Slim toolbar. Only Repeat/Next, Like and Comment have numerical/short labels.
# The rest are self-describing via accessible contentDescription; D-pad focus is
# shown by a delicate turquoise rounded rectangle, not circular backgrounds.
replace_between(stage,"        // Compact round actions;",
                "        // A slim progress line",'''        // Icon-only TV controls, without the old heavy circles.
        rightScroll = new ScrollView(activity);
        rightScroll.setVerticalScrollBarEnabled(false);
        rightScroll.setFocusable(false);
        rightScroll.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        final int controlWidth = dp(54);
        FrameLayout.LayoutParams controlLp = new FrameLayout.LayoutParams(
                controlWidth, videoHeight, Gravity.TOP | Gravity.RIGHT);
        controlLp.topMargin = videoTop;
        controlLp.rightMargin = Math.max(dp(12),
                screenWidth - (videoLeft + videoWidth + dp(10) + controlWidth));
        root.addView(rightScroll, controlLp);
        LinearLayout controls = new LinearLayout(activity);
        controls.setOrientation(LinearLayout.VERTICAL);
        controls.setGravity(Gravity.CENTER_HORIZONTAL);
        controls.setPadding(dp(2), dp(3), dp(2), dp(3));
        rightScroll.addView(controls, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        clock = label(14, WHITE);
        clock.setGravity(Gravity.CENTER);
        controls.addView(clock, new LinearLayout.LayoutParams(-1, dp(27)));

        addSlimAction(controls, com.liskovsoft.smartyoutubetv2.tv.R.drawable.ic_player_repeat,
                "Weiter oder wiederholen", "Weiter",
                () -> { toggleAutoNext(); onAutoChange.run(); });
        addSlimAction(controls, com.liskovsoft.smartyoutubetv2.tv.R.drawable.ic_watch_thumb_up_outline,
                "Gefällt mir", "–", onLike);
        addSlimAction(controls, com.liskovsoft.smartyoutubetv2.tv.R.drawable.ic_watch_comment,
                "Kommentare", "Kommentare", onComments);
        addSlimAction(controls, com.liskovsoft.smartyoutubetv2.tv.R.drawable.mars_nav_profile_025,
                "Kanal öffnen", "", onChannel);
        addSlimAction(controls, com.liskovsoft.smartyoutubetv2.tv.R.drawable.ic_player_playlist_add,
                "Merken", "", onSave);
        addSlimAction(controls, com.liskovsoft.smartyoutubetv2.tv.R.drawable.mars_tv_ic_cc,
                "Untertitel", "", onCaptions);
        addSlimAction(controls, com.liskovsoft.smartyoutubetv2.tv.R.drawable.ic_player_speed,
                "Tempo", "", onSpeed);
        addSlimAction(controls, com.liskovsoft.smartyoutubetv2.tv.R.drawable.mars_nav_search_025,
                "Shorts durchsuchen", "", onSearch);
        addSlimAction(controls, android.R.drawable.ic_menu_close_clear_cancel,
                "Empfehlungsoptionen – Kein Interesse", "", onNoInterest);
        likeCountLabel = rightLabels.get(1);
        commentsCountLabel = rightLabels.get(2);
        autoButton = label(12, WHITE);
        elapsed = label(11, WHITE);
        elapsed.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        durationText = label(11, WHITE);
        durationText.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        LinearLayout timeBox = new LinearLayout(activity);
        timeBox.setOrientation(LinearLayout.HORIZONTAL);
        timeBox.setPadding(dp(2), 0, dp(2), 0);
        timeBox.addView(elapsed, new LinearLayout.LayoutParams(0, dp(18), 1f));
        timeBox.addView(durationText, new LinearLayout.LayoutParams(0, dp(18), 1f));
        FrameLayout.LayoutParams timesLp = new FrameLayout.LayoutParams(
                videoWidth - dp(24), dp(18), Gravity.TOP | Gravity.LEFT);
        timesLp.leftMargin = videoLeft + dp(12);
        timesLp.topMargin = videoTop + videoHeight + dp(10);
        root.addView(timeBox, timesLp);

''')
replace_between(stage,"    private void addRoundAction(",
                "    public void updateCounts(",'''    private void addSlimAction(LinearLayout parent, int icon, String description,
                               String caption, Runnable onClick) {
        LinearLayout row = new LinearLayout(activity);
        row.setGravity(Gravity.CENTER);
        row.setOrientation(LinearLayout.VERTICAL);
        ImageView button = new ImageView(activity);
        button.setImageResource(icon);
        button.setScaleType(ImageView.ScaleType.FIT_CENTER);
        button.setColorFilter(WHITE);
        button.setPadding(dp(4), dp(4), dp(4), dp(4));
        button.setContentDescription(description);
        button.setOnClickListener(v -> onClick.run());
        row.addView(button, new LinearLayout.LayoutParams(dp(28), dp(28)));
        TextView label = label(9, WHITE);
        label.setGravity(Gravity.CENTER);
        label.setSingleLine(true);
        label.setEllipsize(TextUtils.TruncateAt.END);
        label.setText(caption);
        if (!caption.isEmpty()) {
            row.addView(label, new LinearLayout.LayoutParams(dp(54), dp(13)));
        }
        parent.addView(row, new LinearLayout.LayoutParams(-1,
                caption.isEmpty() ? dp(39) : dp(46)));
        rightButtons.add(button);
        rightRows.add(row);
        rightLabels.add(label);
    }

    private GradientDrawable focusOutline(boolean focused) {
        GradientDrawable result = new GradientDrawable();
        result.setColor(android.graphics.Color.TRANSPARENT);
        result.setCornerRadius(dp(7));
        if (focused) result.setStroke(dp(1), 0xFF62DFFF);
        return result;
    }

    public void setVideoFocused(boolean focused) {
        if (videoFocused == focused) return;
        videoFocused = focused;
        GradientDrawable outline = new GradientDrawable();
        outline.setColor(android.graphics.Color.TRANSPARENT);
        outline.setCornerRadius(dp(18));
        outline.setStroke(dp(focused ? 2 : 1),
                focused ? 0xFF55E4F6 : 0xA673ABD4);
        videoFrame.setBackground(outline);
    }

''')
once(stage,'''        // YouTube does not publish official dislike totals; unknown stays unknown.
        dislikeCountLabel.setText(video == null || TextUtils.isEmpty(video.dislikeCount)
                ? "–" : video.dislikeCount);
''',"")
once(stage,'        autoButton.setText(enabled ? "↻  EIN" : "↻  AUS");',
     '        autoButton.setText(enabled ? "Weiter" : "Wiederholen");')
once(stage,'''        if (!rightLabels.isEmpty()) rightLabels.get(0).setText(enabled ? "Weiter" : "Wdh.");''',
     '''        if (!rightLabels.isEmpty()) rightLabels.get(0).setText(enabled ? "Weiter" : "Wdh.");
        if (!rightButtons.isEmpty()) {
            rightButtons.get(0).setImageResource(enabled
                    ? com.liskovsoft.smartyoutubetv2.tv.R.drawable.mars_short_next_029
                    : com.liskovsoft.smartyoutubetv2.tv.R.drawable.ic_player_repeat);
            rightButtons.get(0).setColorFilter(WHITE);
        }''')
once(stage,'''            rightButtons.get(i).setBackground(circle(selected));
            rightButtons.get(i).setColorFilter(selected ? BLUE : WHITE);''',
     '''            rightRows.get(i).setBackground(focusOutline(selected));
            rightButtons.get(i).setColorFilter(selected ? BLUE : WHITE);''')

# Media3 player remains untouched; show the same cyan focus border as preview
# rows when main video logically owns the remote focus.
once(shell,'''        shortsStage.setPreviewsFocused(shortsMode && focus == Focus.SHORTS_PREVIEWS);''',
     '''        shortsStage.setVideoFocused(shortsMode && focus == Focus.VIDEO
                && (!actions.commentsOpen() || shortsCommentsDetached));
        shortsStage.setPreviewsFocused(shortsMode && focus == Focus.SHORTS_PREVIEWS);''')
once(shell,'''    private boolean shortsAutoAdvanceArmed = true;''',
     '''    private boolean shortsAutoAdvanceArmed = true;
    private boolean shortsCommentsDetached;''')
once(shell,'''        default boolean closeSearch() { return false; }
        default void channel() { }''',
     '''        default boolean closeSearch() { return false; }
        default boolean focusComments() { return false; }
        default void channel() { }''')
# This early dispatcher grants the left arrow from a comment to the main player.
# Comments stay open. On right arrow from the focused video we can re-enter the
# original RecyclerView, including its sort/reply focus.
once(shell,'''        if (actions.commentsOpen()) {
            if (key == KeyEvent.KEYCODE_BACK || key == KeyEvent.KEYCODE_MENU) {
                if (action == KeyEvent.ACTION_DOWN) actions.closeComments();
                return true;
            }
            return false;
        }''',
     '''        if (actions.commentsOpen()) {
            if (key == KeyEvent.KEYCODE_BACK || key == KeyEvent.KEYCODE_MENU) {
                if (action == KeyEvent.ACTION_DOWN) {
                    actions.closeComments();
                    shortsCommentsDetached = false;
                }
                return true;
            }
            if (!shortsCommentsDetached) {
                if (key == KeyEvent.KEYCODE_DPAD_LEFT) {
                    if (action == KeyEvent.ACTION_DOWN) {
                        shortsCommentsDetached = true;
                        focus = Focus.VIDEO;
                        render();
                    }
                    return true;
                }
                return false;
            }
            if (key == KeyEvent.KEYCODE_DPAD_RIGHT && focus == Focus.VIDEO) {
                if (action == KeyEvent.ACTION_DOWN && actions.focusComments()) {
                    shortsCommentsDetached = false;
                    render();
                }
                return true;
            }
        }''')
once(shell,'''                } else if (shortsReactionPosition == 1) actions.like();
                else if (shortsReactionPosition == 2) actions.dislike();
                else if (shortsReactionPosition == 3) actions.comments();
                else if (shortsReactionPosition == 4) actions.channel();
                else if (shortsReactionPosition == 5) actions.save();
                else if (shortsReactionPosition == 6) actions.captions();
                else if (shortsReactionPosition == 7) actions.speed();
                else if (shortsReactionPosition == 8) actions.search();
                else if (shortsReactionPosition == 9) actions.noInterest();''',
     '''                } else if (shortsReactionPosition == 1) actions.like();
                else if (shortsReactionPosition == 2) {
                    shortsCommentsDetached = false;
                    actions.comments();
                } else if (shortsReactionPosition == 3) actions.channel();
                else if (shortsReactionPosition == 4) actions.save();
                else if (shortsReactionPosition == 5) actions.captions();
                else if (shortsReactionPosition == 6) actions.speed();
                else if (shortsReactionPosition == 7) actions.search();
                else if (shortsReactionPosition == 8) actions.noInterest();''')
# Keep the comments rail visible on a new short and refresh only its new video's
# data on demand; no eager fetching when the panel is closed.
once(comments,'''    private boolean mReleased;''',
     '''    private boolean mReleased;
    private boolean mMarsTvKeepOpen;''')
once(comments,'''    void onVideoChanged(@Nullable String videoId) {''',
     '''    void marsTvKeepOpen(boolean enabled) {
        mMarsTvKeepOpen = enabled;
    }

    void onVideoChanged(@Nullable String videoId) {''')
once(comments,'''        if (mLayout.isOpen() && !mSuspended && mLayout.getVisibility() == View.VISIBLE) {
            // The old video's comments stay in the sheet while it slides away; emptied once gone.''',
     '''        if (mMarsTvKeepOpen && mLayout.isOpen() && !mSuspended
                && mLayout.getVisibility() == View.VISIBLE) {
            // Keep TV pane visible while Shorts switches. Clear old rows now;
            // new comment keys for the next Short arrive via setSource().
            resetViews();
        } else if (mLayout.isOpen() && !mSuspended && mLayout.getVisibility() == View.VISIBLE) {
            // The old video's comments stay in the sheet while it slides away; emptied once gone.''')
# Locate setSource tail safely by insertion after the first source-derived feed
# creation, not the generic close callback.
once(comments,'''        if (mNewest == null && newestKey != null) {
            mNewest = new Feed(newestKey, false);
        }''',
     '''        if (mNewest == null && newestKey != null) {
            mNewest = new Feed(newestKey, false);
        }
        if (mMarsTvKeepOpen && mLayout.isOpen() && mTop != null
                && !mTop.loaded && !mTop.loading) {
            bindFeedToList(mTop, false);
            mLayout.getSheet().post(() -> maybeLoadMore(activePage()));
        }''')
# Compact comments in TV only. Preserve the upstream signed-in composer.
once(comments,'''        mSortLabel = layout.findViewById(R.id.comments_sort_label);''',
     '''        mSortLabel = layout.findViewById(R.id.comments_sort_label);
        if (com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(activity)) {
            ((TextView) layout.findViewById(R.id.comments_title)).setTextSize(16f);
            ((TextView) layout.findViewById(R.id.comments_title_replies)).setTextSize(16f);
            mCountView.setTextSize(12f);
            mSortLabel.setTextSize(12f);
        }''')
once(adapter,'''            default:
                return new CommentVH(inflater.inflate(R.layout.item_mobile_comment, parent, false),
                        viewType == TYPE_PARENT, mListener);''',
     '''            default: {
                View row = inflater.inflate(R.layout.item_mobile_comment, parent, false);
                if (com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(parent.getContext())) {
                    ((TextView) row.findViewById(R.id.comment_message)).setTextSize(13f);
                    ((TextView) row.findViewById(R.id.comment_meta)).setTextSize(11f);
                    ((TextView) row.findViewById(R.id.comment_pinned_label)).setTextSize(11f);
                    ((TextView) row.findViewById(R.id.comment_like_count)).setTextSize(11f);
                    ((TextView) row.findViewById(R.id.comment_like_count_next)).setTextSize(11f);
                    ((TextView) row.findViewById(R.id.comment_replies_label)).setTextSize(11f);
                }
                return new CommentVH(row, viewType == TYPE_PARENT, mListener);
            }''')
# Right-side TV comments are transparent and match the same top/bottom edges as
# central player. On phones the sheet remains untouched.
once(layout,'''        mMarsTvSideWidth = Math.max(0, px);
        mSheet.setTranslationY(0f);''',
     '''        mMarsTvSideWidth = Math.max(0, px);
        if (mMarsTvSideWidth > 0) {
            mSheet.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            mScrim.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        }
        mSheet.setTranslationY(0f);''')
once(playback,'''            int height = getResources().getDisplayMetrics().heightPixels;
            int videoWidth = MarsTvShortsStage.videoWidth(this);''',
     '''            int height = getResources().getDisplayMetrics().heightPixels;
            int videoWidth = MarsTvShortsStage.videoWidth(this);''')
once(playback,'''                    new android.widget.FrameLayout.LayoutParams(sideWidth,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.Gravity.RIGHT | android.view.Gravity.CENTER_VERTICAL);
            lp.rightMargin = (int) (10f * getResources().getDisplayMetrics().density);''',
     '''                    new android.widget.FrameLayout.LayoutParams(sideWidth,
                            height - (int) (54f * getResources().getDisplayMetrics().density),
                            android.view.Gravity.RIGHT | android.view.Gravity.TOP);
            lp.topMargin = (int) (22f * getResources().getDisplayMetrics().density);
            lp.rightMargin = (int) (10f * getResources().getDisplayMetrics().density);''')
once(playback,'''        onCommentsEntryClicked();
        if (pane != null) {''',
     '''        mCommentsPanel.marsTvKeepOpen(true);
        onCommentsEntryClicked();
        if (pane != null) {''')
once(playback,'''                    @Override public boolean closeComments() {
                        if (mCommentsPanel == null || !mCommentsPanel.isOpen()) return false;
                        mCommentsPanel.marsTvBack();
                        return true;
                    }''',
     '''                    @Override public boolean closeComments() {
                        if (mCommentsPanel == null || !mCommentsPanel.isOpen()) return false;
                        mCommentsPanel.marsTvBack();
                        return true;
                    }
                    @Override public boolean focusComments() {
                        if (mCommentsPanel == null || !mCommentsPanel.isOpen())
                            return false;
                        View list = findViewById(R.id.comments_list);
                        if (list != null) {
                            list.requestFocus();
                            return true;
                        }
                        return false;
                    }''')

# Search matches the 9:16 view height; no gradient banding. On Fire TV a
# microphone tap must not launch system-wide Alexa search (not app-owned).
once(search,'''        GradientDrawable glass = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xF50A2948, 0xFA071729});''',
     '''        GradientDrawable glass = new GradientDrawable();
        glass.setColor(0xE8113452);''')
once(search,'''            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    width, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.RIGHT);
            params.setMargins(0, dp(14), dp(12), dp(14));''',
     '''            int height = activity.getResources().getDisplayMetrics().heightPixels;
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    width, height - dp(54), Gravity.TOP | Gravity.RIGHT);
            params.setMargins(0, dp(22), dp(12), 0);''')
once(search,'''    private void startVoice() {
        try {''',
     '''    private void startVoice() {
        // The Fire TV Alexa key and mic are owned by Fire OS. Launching its
        // RecognizerIntent sends the user to the global Amazon search.
        if (android.os.Build.MANUFACTURER != null
                && android.os.Build.MANUFACTURER.toLowerCase(Locale.ROOT).contains("amazon")) {
            Toast.makeText(activity,
                    "Alexa-Mikrofon gehört zur Fire-TV-Suche. Bitte Text eingeben.",
                    Toast.LENGTH_LONG).show();
            input.requestFocus();
            return;
        }
        try {''')
once(search,'''                input.setText(words.get(0));
                input.setSelection(input.length());''',
     '''                String spoken = words.get(0).trim();
                if (spoken.toLowerCase(Locale.GERMAN).startsWith("text ")) {
                    spoken = spoken.substring(5).trim();
                }
                input.setText(spoken);
                input.setSelection(input.length());''')

gradle=(root/"smarttubetv/build.gradle").read_text(encoding="utf-8")
assert 'applicationId "de.equiummc.marstube2"' in gradle
assert 'versionCode 12029' in gradle
s=stage.read_text(encoding="utf-8")
assert "dislikeCountLabel" not in s and "addRoundAction(" not in s
assert "setVideoFocused(boolean focused)" in s
assert "mars_short_next_029" in s
assert "commentsCountLabel = rightLabels.get(2)" in s
assert "videoHeight, Gravity.TOP | Gravity.LEFT" in s
assert (drawable/"mars_short_next_029.xml").is_file()
assert "mMarsTvKeepOpen" in comments.read_text(encoding="utf-8")
assert "mSheet.setBackgroundColor(android.graphics.Color.TRANSPARENT)" in layout.read_text(encoding="utf-8")
print("MT2-029: compact Shorts rail, elapsed/total time, three aligned columns, TV comment navigation and flat search.")
