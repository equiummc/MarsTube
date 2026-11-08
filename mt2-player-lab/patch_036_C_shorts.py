#!/usr/bin/env python3
"""MT2-036 C: cumulative A+B+C TV Shorts discovery and preview fix.

Applied after 035 in disposable, pinned checkout. Fails closed on drift.
Media3, SABR, player SurfaceTexture, original app ID and signing stay untouched.
"""
from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit("Usage: patch_036_C_shorts.py <pinned-source>")
root = Path(sys.argv[1]).resolve()
here = Path(__file__).resolve().parent
tv = root / "smarttubetv/src/stmobile/java/com/newtube/mobile/ui"
browse = tv / "browse/MobileBrowseActivity.java"
playback = tv / "playback/MobilePlaybackActivity.java"
gradle = root / "smarttubetv/build.gradle"
if not (root / "sabr-media3").is_dir():
    raise SystemExit("MT2-036 C: Media3 checkout absent")

def once(path, before, after):
    text = path.read_text(encoding="utf-8")
    n = text.count(before)
    if n != 1:
        raise SystemExit("MT2-036 C FAIL CLOSED: %s hits=%s: %r" %
                         (path.name, n, before[:120]))
    path.write_text(text.replace(before, after, 1), encoding="utf-8")

sources = {
    "MarsShortsSession036.java": tv/"common/MarsShortsSession.java",
    "MarsShortsDiscovery036.java": tv/"common/MarsShortsDiscovery.java",
    "MarsTvInterest036.java": tv/"common/MarsTvInterest036.java",
    "MarsTvCurtainPolicy036.java": tv/"common/MarsTvCurtainPolicy036.java",
    "MarsTvCurtainPolicy036Test.java": root/"smarttubetv/src/test/java/com/newtube/mobile/ui/common/MarsTvCurtainPolicy036Test.java",
    "MarsShortsSessionTest036.java": root/"smarttubetv/src/test/java/com/newtube/mobile/ui/common/MarsShortsSessionTest.java",
}
for source, dest in sources.items():
    src = here/source
    if not src.is_file() or not dest.parent.is_dir():
        raise SystemExit("MT2-036 C: missing versioned file or destination: " + source)
    dest.write_bytes(src.read_bytes())

# Browse includes the existing normal-video near-end pagination. Use the local
# negative list to hide future tiles without adding network calls, a second
# adapter or an automatic video click.
once(browse,
'''    private java.util.List<Video> marsTvB035Visible() {
        return mMarsTvB035 == null ? mCurrentVideos
                : mMarsTvB035.visible(mCurrentVideos, mCurrentSectionId);
    }''',
'''    private java.util.List<Video> marsTvB035Visible() {
        java.util.List<Video> source = mMarsTvB035 == null ? mCurrentVideos
                : mMarsTvB035.visible(mCurrentVideos, mCurrentSectionId);
        if (!com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(this))
            return source;
        return com.newtube.mobile.ui.common.MarsTvInterest036.instance(this).filter(source);
    }''')
# TV 'Nicht interessiert' in the context menu: invoke the original YouTube
# feedback dialogue when it exists, while the persistent local list ensures
# feedback works even signed out. The normal related-grid is filtered on resume.
# Shorts rail 'Kein Interesse' directly invokes the local+optional remote action.
once(playback,
'''                    @Override public void noInterest() {
                        com.liskovsoft.smartyoutubetv2.common.app.presenters.dialogs.menu.VideoMenuPresenter
                                .instance(MobilePlaybackActivity.this).showMenu(getVideo());
                    }''',
'''                    @Override public void noInterest() { marsTvRejectCurrent036(); }''')
once(playback,
'''    private void marsTvOpenPlayerComments() {''',
'''    /** Immediate rejection on TV, regardless of login state.
     * YouTube's signed-in feedback token is attempted only if supplied.
     */
    private void marsTvRejectCurrent036() {
        Video rejected = getVideo();
        if (rejected == null || rejected.videoId == null) return;
        com.newtube.mobile.ui.common.MarsTvInterest036 interest =
                com.newtube.mobile.ui.common.MarsTvInterest036.instance(this);
        if (!interest.rejectLocal(rejected)) return;
        interest.sendSignedInFeedback(rejected);
        if (com.newtube.mobile.ui.common.MarsShortsSession.isActiveFor(rejected)) {
            com.newtube.mobile.ui.common.MarsShortsSession
                    .setExcluded(interest.snapshot());
            // Moves to next in the SAME Shorts session, or metadata paginates.
            // Do not call generic player recommendations or restart the decoder.
            marsTvStepShort(1);
        }
        android.widget.Toast.makeText(this,
                "Video lokal ausgeblendet", android.widget.Toast.LENGTH_SHORT).show();
    }

    private void marsTvOpenPlayerComments() {''')

# Only ask the existing cached thumbnail; do not load new network images while
# the Media3 stream starts. A native portrait source fills the portrait still.
# A landscape image stays at its genuine ratio until the first decoded frame.
once(playback,
'''        mStillVideoId = item.videoId;
        // A TV Short occupies a tall 9:16 PlayerView,''',
'''        marsTvCancelCurtain036();
        mStillVideoId = item.videoId;
        // A TV Short occupies a tall 9:16 PlayerView,''')
# In only the one Glide loader block, change the two center crops to FIT_CENTER
# for Shorts (preserve original aspect; portrait originals already fill).
src = playback.read_text(encoding="utf-8")
from_marker = '        String thumb = ClickbaitRemover.updateThumbnail(item, MainUIData.instance(this).getThumbQuality());'
to_marker = '''    }

    // ---------------------------------------------------------------------------------
    // NEWTUBE(net): image requests yield to the opening video'''
if src.count(from_marker) != 1 or src.count(to_marker) != 1:
    raise SystemExit("MT2-036 C loading still section moved")
start = src.index(from_marker)
end = src.index(to_marker, start)
segment = src[start:end]
if segment.count('.centerCrop()') != 2:
    raise SystemExit("MT2-036 C: cached still has unexpected crop count")
segment = segment.replace('.centerCrop()', '''.transform(shortsStill
                            ? new com.bumptech.glide.load.resource.bitmap.FitCenter()
                            : new com.bumptech.glide.load.resource.bitmap.CenterCrop())''')
src = src[:start] + segment + src[end:]
playback.write_text(src,encoding="utf-8")

# The black-mask curtain affects UI only, not playback. It begins AFTER first
# frame is proven ready and starts with a center opening; 440ms GPU transforms.
# If a genuine 9:16 thumbnail was available, leave the native fast reveal.
once(playback,
'''    private void hideVideoStill(boolean revealNewVideo, String lift) {
        mNewVideoStill = false;
        if (revealNewVideo) {
            boolean hidden = fadeOutLoadingStill(mVideoStill);''',
'''    private void hideVideoStill(boolean revealNewVideo, String lift) {
        mNewVideoStill = false;
        if (revealNewVideo) {
            if (marsTvCurtainForLandscapeStill036()) {
                releaseImageRequests("picture-visible");
                return;
            }
            boolean hidden = fadeOutLoadingStill(mVideoStill);''')
once(playback,
'''    private void liftLoadingStill(String lift) {''',
'''    private View mMarsCurtainLeft036;
    private View mMarsCurtainRight036;

    private void marsTvCancelCurtain036() {
        if (mMarsCurtainLeft036 != null) {
            mMarsCurtainLeft036.animate().cancel();
            if (mMarsCurtainLeft036.getParent() instanceof ViewGroup)
                ((ViewGroup)mMarsCurtainLeft036.getParent()).removeView(mMarsCurtainLeft036);
            mMarsCurtainLeft036 = null;
        }
        if (mMarsCurtainRight036 != null) {
            mMarsCurtainRight036.animate().cancel();
            if (mMarsCurtainRight036.getParent() instanceof ViewGroup)
                ((ViewGroup)mMarsCurtainRight036.getParent()).removeView(mMarsCurtainRight036);
            mMarsCurtainRight036 = null;
        }
    }

    private boolean marsTvCurtainForLandscapeStill036() {
        if (mVideoStill == null || mVideoStill.getVisibility() != View.VISIBLE
                || !com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(this)
                || !com.newtube.mobile.ui.common.MarsShortsSession.isActiveFor(getVideo())
                || mPlayer == null || mVideoStill.getDrawable() == null
                || !(mVideoStill.getParent() instanceof FrameLayout)
                || mVideoStill.getWidth() <= 0 || mVideoStill.getHeight() <= 0
                || mIsInPip) return false;
        android.graphics.drawable.Drawable photo = mVideoStill.getDrawable();
        VideoSize size = mPlayer.getVideoSize();
        if (!com.newtube.mobile.ui.common.MarsTvCurtainPolicy036.shouldReveal(
                photo.getIntrinsicWidth(), photo.getIntrinsicHeight(),
                size.width, size.height, true)) return false;
        FrameLayout host = (FrameLayout) mVideoStill.getParent();
        int half = (mVideoStill.getWidth() + 1) / 2;
        marsTvCancelCurtain036();
        View left = new View(this), right = new View(this);
        left.setBackgroundColor(Color.BLACK);
        right.setBackgroundColor(Color.BLACK);
        FrameLayout.LayoutParams ll = new FrameLayout.LayoutParams(half,
                ViewGroup.LayoutParams.MATCH_PARENT, Gravity.LEFT | Gravity.TOP);
        FrameLayout.LayoutParams rr = new FrameLayout.LayoutParams(half,
                ViewGroup.LayoutParams.MATCH_PARENT, Gravity.RIGHT | Gravity.TOP);
        // Animate masks without reallocating the player or forcing per-frame layouts.
        left.setPivotX(0f);
        right.setPivotX(half);
        left.setScaleX(0.68f); right.setScaleX(0.68f);
        host.addView(left, ll);
        host.addView(right, rr);
        mMarsCurtainLeft036 = left;
        mMarsCurtainRight036 = right;
        hideLoadingStillImmediately(mVideoStill);
        left.animate().scaleX(0f).setDuration(440L)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();
        right.animate().scaleX(0f).setDuration(440L)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .withEndAction(() -> {
                    if (mMarsCurtainRight036 == right) marsTvCancelCurtain036();
                }).start();
        return true;
    }

    private void liftLoadingStill(String lift) {''')

# A user returning from a video sees refreshed feed cards through the existing
# browse lifecycle; unchanged normal-video related feed continues via presenter.
once(gradle, "versionCode 12035", "versionCode 12036")
once(gradle, 'versionName "0.0.35"', 'versionName "0.0.36"')
assert 'applicationId "de.equiummc.marstube2"' in gradle.read_text(encoding="utf-8")
assert "MarsShortsDiscovery" in playback.read_text(encoding="utf-8")
assert "mMarsCurtainRight036" in playback.read_text(encoding="utf-8")
assert "MarsTvInterest036.instance(this).filter" in browse.read_text(encoding="utf-8")
print("MT2-036 C: cumulative ABC; negative feed filter, optional native feedback,"
      " bounded discovery, next/repeat unchanged, 9:16 preview/curtain with one Media3 player.")
