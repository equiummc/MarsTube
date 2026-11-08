#!/usr/bin/env python3
"""MT2-037: 100k profile-scoped SQLite rejects, Shorts and normal-video X.

Runs after successfully validated MT2-036 in isolated CI checkout.
Fails closed if any patch anchor changed. Preserves all player/cert/app data.
"""
from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit("Usage: patch_037_profile_reject.py <pinned-source>")
root = Path(sys.argv[1]).resolve()
here = Path(__file__).resolve().parent
tv = root/"smarttubetv/src/stmobile/java/com/newtube/mobile/ui"
b = tv/"browse/MobileBrowseActivity.java"
a = tv/"browse/VideoCardAdapter.java"
p = tv/"playback/MobilePlaybackActivity.java"
stage = tv/"playback/MarsTvShortsStage.java"
shell = tv/"playback/MarsTvShell.java"
dis = tv/"common/MarsShortsDiscovery.java"
panels = tv/"playback/MarsTvPlayerPanels034.java"
gradle = root/"smarttubetv/build.gradle"
def one(path, old, new):
    s = path.read_text(encoding="utf-8")
    hits = s.count(old)
    if hits != 1:
        raise SystemExit(f"MT2-037 FAIL CLOSED {path.name}: {hits} copies of {old[:85]!r}")
    path.write_text(s.replace(old,new,1),encoding="utf-8")

for name, destination in [
    ("MarsTvInterest037.java",tv/"common/MarsTvInterest037.java"),
    ("MarsTvInterestPolicy037.java",tv/"common/MarsTvInterestPolicy037.java"),
    ("MarsTvInterestPolicy037Test.java",
     root/"smarttubetv/src/test/java/com/newtube/mobile/ui/common/MarsTvInterestPolicy037Test.java")
]:
    src=here/name
    if not src.is_file() or not destination.parent.is_dir():
        raise SystemExit("MT2-037 missing repository source or target: "+name)
    destination.write_bytes(src.read_bytes())

# Replace OLD cross-profile list references, but never blindly rewrite them
# across the repository. Old 036 is left untouched for safe app-data migration.
one(b,
    "return com.newtube.mobile.ui.common.MarsTvInterest036.instance(this).filter(source);",
    "return com.newtube.mobile.ui.common.MarsTvInterest037.instance(this).filter(source);")
one(p,
    """        com.newtube.mobile.ui.common.MarsTvInterest036 interest =
                com.newtube.mobile.ui.common.MarsTvInterest036.instance(this);""",
    """        com.newtube.mobile.ui.common.MarsTvInterest037 interest =
                com.newtube.mobile.ui.common.MarsTvInterest037.instance(this);""")
one(p,
    """            com.newtube.mobile.ui.common.MarsShortsSession
                    .setExcluded(interest.snapshot());""",
    """            com.newtube.mobile.ui.common.MarsShortsSession
                    .removeRejected(rejected.videoId);""")

# A full 100,000-ID snapshot in memory is NOT acceptable for small TVs:
# discovery filters only the page currently being appended.
oldex = "MarsShortsSession.setExcluded(MarsTvInterest036.instance(context).snapshot());"
source = dis.read_text(encoding="utf-8")
if source.count(oldex) != 2:
    raise SystemExit("MT2-037 FAIL CLOSED: expected two old snapshot calls")
source = source.replace(oldex,"MarsShortsSession.setExcluded(java.util.Collections.emptySet());")
dis.write_text(source,encoding="utf-8")
one(dis,
    """        Set<String> excluded = new HashSet<>(seen);
        excluded.addAll(MarsTvInterest036.instance(context).snapshot());
        receivedFresh += MarsShortsSession.appendFresh(videos.getVideos(), excluded);""",
    """        // Query the profile-scoped SQLite index in batches; do not load 100k IDs.
        List<Video> eligible = MarsTvInterest037.instance(context).filter(videos.getVideos());
        receivedFresh += MarsShortsSession.appendFresh(eligible, new HashSet<>(seen));""")

# Player's More list: make the one-click rejection prominent as its first row.
one(panels,
    '''            case "Mehr":
            default:
                add("Kanal", "Kanal, Abonnement und Glocke", "page:Kanal");''',
    '''            case "Mehr":
            default:
                add("Nicht interessiert", "Dieses Video künftig ausblenden", "no_interest");
                add("Kanal", "Kanal, Abonnement und Glocke", "page:Kanal");''')
one(panels,
    '''                add("Nicht interessiert", "Empfehlungsoptionen öffnen", "no_interest");
                add("Vollbild", "Untermenü schließen", "fullscreen");''',
    '''                add("Vollbild", "Untermenü schließen", "fullscreen");''')
one(panels,
    '''        if (action.equals("info")) return android.R.drawable.ic_menu_info_details;''',
    '''        if (action.equals("no_interest")) return android.R.drawable.ic_menu_close_clear_cancel;
        if (action.equals("info")) return android.R.drawable.ic_menu_info_details;''')

# Real X in the TV normal-video grid (also shown on Shorts video tiles).
# The same reject handler immediately removes the card and preserves the D-pad
# selection, while the network feedback remains strictly optional.
one(a,
    '''    public interface OnVideoLongClickListener {
        boolean onVideoLongClick(Video video);
    }''',
    '''    public interface OnVideoLongClickListener {
        boolean onVideoLongClick(Video video);
    }
    public interface OnVideoRejectListener {
        void onVideoReject(Video video);
    }''')
one(a,
    '''    private final OnVideoLongClickListener mLongClickListener;''',
    '''    private final OnVideoLongClickListener mLongClickListener;
    private OnVideoRejectListener mRejectListener;''')
one(a,
    '''    private static final DiffUtil.ItemCallback<Video> DIFF_CALLBACK =''',
    '''    public void setRejectListener(OnVideoRejectListener listener) {
        mRejectListener = listener;
        if (getItemCount() > 0) notifyItemRangeChanged(0,getItemCount());
    }

    private static final DiffUtil.ItemCallback<Video> DIFF_CALLBACK =''')
one(a,
    '''            ((VideoViewHolder) holder).bind(getItem(position), mLongClickListener);
            ((VideoViewHolder) holder).bindChannelAvatar(mChannelClickListener);''',
    '''            ((VideoViewHolder) holder).bind(getItem(position), mLongClickListener);
            ((VideoViewHolder) holder).bindReject(mRejectListener);
            ((VideoViewHolder) holder).bindChannelAvatar(mChannelClickListener);''')
one(a,
    '''        private final View mOverflow;
        private final ImageView mAvatar;''',
    '''        private final View mOverflow;
        private final android.widget.ImageButton mReject;
        private final ImageView mAvatar;''')
one(a,
    '''            mOverflow = itemView.findViewById(R.id.video_overflow);
            mAvatar = itemView.findViewById(R.id.video_channel_avatar);''',
    '''            mOverflow = itemView.findViewById(R.id.video_overflow);
            // TV-only X on the thumbnail. Never create another VideoCard or
            // trigger its video-start callback when rejecting.
            android.widget.ImageButton reject = new android.widget.ImageButton(itemView.getContext());
            reject.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            reject.setColorFilter(android.graphics.Color.WHITE);
            reject.setContentDescription("Nicht interessiert");
            reject.setFocusable(true);
            reject.setClickable(true);
            int dp = Math.round(itemView.getResources().getDisplayMetrics().density);
            android.graphics.drawable.GradientDrawable bg =
                    new android.graphics.drawable.GradientDrawable();
            bg.setColor(0xCC082E50);
            bg.setCornerRadius(13f * dp);
            bg.setStroke(Math.max(1,dp),0xFF35BCF0);
            reject.setBackground(bg);
            reject.setPadding(dp*5,dp*5,dp*5,dp*5);
            reject.setVisibility(View.GONE);
            if (mThumbnailFrame instanceof FrameLayout) {
                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                        34*dp,34*dp,android.view.Gravity.TOP|android.view.Gravity.RIGHT);
                lp.setMargins(3*dp,3*dp,5*dp,0);
                ((FrameLayout)mThumbnailFrame).addView(reject,lp);
            }
            mReject = reject;
            mAvatar = itemView.findViewById(R.id.video_channel_avatar);''')
one(a,
    '''        public void bind(Video video, OnVideoLongClickListener longClickListener) {''',
    '''        public void bindReject(OnVideoRejectListener listener) {
            boolean available = listener != null && mVideo != null && mVideo.hasVideo()
                    && com.newtube.mobile.ui.common.MarsDeviceMode
                            .isTelevision(itemView.getContext());
            mReject.setVisibility(available ? View.VISIBLE : View.GONE);
            mReject.setContentDescription(available
                    ? "Nicht interessiert: " + (mVideo.getTitle() == null
                        ? "Video" : mVideo.getTitle()) : "Nicht interessiert");
            mReject.setOnClickListener(available
                    ? v -> { if (mVideo != null) listener.onVideoReject(mVideo); }
                    : null);
        }

        public void bind(Video video, OnVideoLongClickListener longClickListener) {''')

one(b,
    '''        mAdapter = new VideoCardAdapter(this::onVideoClicked, this::onVideoLongClicked);''',
    '''        mAdapter = new VideoCardAdapter(this::onVideoClicked, this::onVideoLongClicked);
        if (com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(this)) {
            mAdapter.setRejectListener(this::marsTvRejectGrid037);
        }''')
one(b,
    '''    private void onVideoClicked(Video video) {''',
    '''    /** Reject one normal video or Short from the grid without launching it. */
    private void marsTvRejectGrid037(Video video) {
        if (video == null || !video.hasVideo() || video.videoId == null) return;
        com.newtube.mobile.ui.common.MarsTvInterest037 store =
                com.newtube.mobile.ui.common.MarsTvInterest037.instance(this);
        if (!store.rejectLocal(video)) return;
        store.sendSignedInFeedback(video);
        if (mAdapter == null) return;
        int oldIndex = mMarsTvSelection;
        java.util.List<Video> refreshed = new java.util.ArrayList<>(marsTvB035Visible());
        mMarsTvSelection = Math.max(0, Math.min(oldIndex, refreshed.size() - 1));
        mAdapter.submitList(refreshed, () -> {
            if (!refreshed.isEmpty()) marsTvFocusCard(mMarsTvSelection);
        });
        android.widget.Toast.makeText(this,
                "Video für dieses Profil ausgeblendet",
                android.widget.Toast.LENGTH_SHORT).show();
    }

    private void onVideoClicked(Video video) {''')

# Normal player recommendation carousel also shares the same profile-scoped
# filter; otherwise a rejected normal video could stay in the six previews.
one(shell,
    '''        int limit = MarsTvPlayerPrefs.getCarouselPreviewCount(activity);
        List<Video> clean = new ArrayList<>();
        if (videos != null) {
            for (Video video : videos) {''',
    '''        int limit = MarsTvPlayerPrefs.getCarouselPreviewCount(activity);
        List<Video> clean = new ArrayList<>();
        List<Video> allowed = com.newtube.mobile.ui.common.MarsTvInterest037
                .instance(activity).filter(videos);
        if (allowed != null) {
            for (Video video : allowed) {''')
one(shell,
    '''    public void hideUi() {
        if (playerPanels.isOpen()) playerPanels.close();
        cancelHold();''',
    '''    /** Remove a rejected card immediately; no decoder/player restart. */
    public void rejectRecommendation(String videoId) {
        if (released || videoId == null) return;
        boolean removed = adapter.items.removeIf(v ->
                v != null && videoId.equals(v.videoId));
        if (removed) {
            cardPosition = Math.min(cardPosition, Math.max(0, adapter.items.size() - 1));
            adapter.notifyDataSetChanged();
            render();
        }
    }

    public void hideUi() {
        cancelHold();''')
one(p,
    '''        interest.sendSignedInFeedback(rejected);
        if (com.newtube.mobile.ui.common.MarsShortsSession.isActiveFor(rejected)) {''',
    '''        interest.sendSignedInFeedback(rejected);
        if (mMarsTvShell != null) mMarsTvShell.rejectRecommendation(rejected.videoId);
        if (com.newtube.mobile.ui.common.MarsShortsSession.isActiveFor(rejected)) {''')

# The first right-side Shorts action is the X, then Next/Repeat.
# Icon and D-pad indexes must be changed TOGETHER; drift fails this build.
auto = '''        addSlimAction(controls, com.liskovsoft.smartyoutubetv2.tv.R.drawable.ic_player_repeat,
                "Weiter oder wiederholen", "Weiter",
                () -> { toggleAutoNext(); onAutoChange.run(); });'''
reject = '''        addSlimAction(controls, android.R.drawable.ic_menu_close_clear_cancel,
                "Empfehlungsoptionen – Kein Interesse", "", onNoInterest);'''
one(stage,auto,reject+"\n"+auto)
one(stage,
    reject+'''
        likeCountLabel = rightLabels.get(1);''',
    '''        likeCountLabel = rightLabels.get(2);''')
one(stage,'''        commentsCountLabel = rightLabels.get(2);''',
          '''        commentsCountLabel = rightLabels.get(3);''')
one(stage,
    '''        if (!rightLabels.isEmpty()) rightLabels.get(0).setText(enabled ? "Weiter" : "Wdh.");''',
    '''        if (rightLabels.size() > 1) rightLabels.get(1).setText(enabled ? "Weiter" : "Wdh.");''')
source=stage.read_text(encoding="utf-8")
if source.count("rightButtons.get(0)") != 2:
    raise SystemExit("MT2-037 FAIL CLOSED: expected 2 auto icon references")
stage.write_text(source.replace("rightButtons.get(0)","rightButtons.get(1)"),encoding="utf-8")
# Final shell post-029: right rail has 9 actions, no dislike. One-to-one.
src=shell.read_text(encoding="utf-8")
old='''                if (shortsReactionPosition == 0) {
                    shortsStage.toggleAutoNext();
                    shortsAutoAdvanceArmed = true;
                } else if (shortsReactionPosition == 1) actions.like();
                else if (shortsReactionPosition == 2) {
                    shortsCommentsDetached = false;
                    actions.comments();
                } else if (shortsReactionPosition == 3) actions.channel();
                else if (shortsReactionPosition == 4) actions.save();
                else if (shortsReactionPosition == 5) actions.captions();
                else if (shortsReactionPosition == 6) actions.speed();
                else if (shortsReactionPosition == 7) actions.search();
                else if (shortsReactionPosition == 8) actions.noInterest();'''
new='''                if (shortsReactionPosition == 0) {
                    actions.noInterest();
                } else if (shortsReactionPosition == 1) {
                    shortsStage.toggleAutoNext();
                    shortsAutoAdvanceArmed = true;
                } else if (shortsReactionPosition == 2) actions.like();
                else if (shortsReactionPosition == 3) {
                    shortsCommentsDetached = false;
                    actions.comments();
                } else if (shortsReactionPosition == 4) actions.channel();
                else if (shortsReactionPosition == 5) actions.save();
                else if (shortsReactionPosition == 6) actions.captions();
                else if (shortsReactionPosition == 7) actions.speed();
                else if (shortsReactionPosition == 8) actions.search();'''
if src.count(old) != 1:
    raise SystemExit("MT2-037 FAIL CLOSED: Shorts control mapping changed")
shell.write_text(src.replace(old,new),encoding="utf-8")

one(gradle,"versionCode 12036","versionCode 12037")
one(gradle,'versionName "0.0.36"','versionName "0.0.37"')
assert "setRejectListener" in a.read_text(encoding="utf-8")
assert "activeProfileKey" in (tv/"common/MarsTvInterest037.java").read_text(encoding="utf-8")
assert "MarsTvInterest037.instance(context).filter" in dis.read_text(encoding="utf-8")
assert 'applicationId "de.equiummc.marstube2"' in gradle.read_text(encoding="utf-8")
print("MT2-037: profile-scoped SQLite 100k, normal-video grid X, Shorts X first,"
      " no full 100k snapshot and original signed Android application.")
