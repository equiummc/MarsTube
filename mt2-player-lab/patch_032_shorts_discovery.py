#!/usr/bin/env python3
"""MT2-032: selected Shorts start only on OK, paginated discovery and working Next.

This is TV-scoped. Never alter normal-video playback, stable BACK behavior,
APK signature or main Git checkout. Fails closed on drift from 0.0.31.
"""
from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit("Usage: patch_032_shorts_discovery.py <pinned-031-source>")
root = Path(sys.argv[1]).resolve()
here = Path(__file__).resolve().parent
tv = root / "smarttubetv/src/stmobile/java/com/newtube/mobile/ui"
if not (root / "sabr-media3").is_dir():
    raise SystemExit("MT2-032 requires pinned Media3/SABR source")


def once(path, before, after):
    text = path.read_text(encoding="utf-8")
    count = text.count(before)
    if count != 1:
        raise SystemExit(f"MT2-032 anchor error in {path.name}: {count}, {before[:110]!r}")
    path.write_text(text.replace(before, after, 1), encoding="utf-8")


session = here / "MarsShortsSession032.java"
discovery = here / "MarsShortsDiscovery032.java"
if not session.is_file() or not discovery.is_file():
    raise SystemExit("MT2-032: missing versioned discovery or session")
(tv / "common/MarsShortsSession.java").write_bytes(session.read_bytes())
(tv / "common/MarsShortsDiscovery.java").write_bytes(discovery.read_bytes())
tests = here / "MarsShortsSessionTest032.java"
test_dest = root / "smarttubetv/src/test/java/com/newtube/mobile/ui/common/MarsShortsSessionTest.java"
if not tests.is_file() or not test_dest.is_file():
    raise SystemExit("MT2-032: missing session unit tests")
test_dest.write_bytes(tests.read_bytes())

# No automatic click when the Shorts overview is entered OR when BACK returns.
# The old one-shot behavior also fired on a focused first card, confusing focus
# with user confirmation. Explicit onVideoClicked() remains the only launcher.
browse = tv / "browse/MobileBrowseActivity.java"
once(browse,
'''    private int mMarsTvExpectedFocusSection = -1;
    private boolean mMarsTvAutoOpenShorts;''',
'''    private int mMarsTvExpectedFocusSection = -1;''')
once(browse,
'''                    marsTvFocusCard(0);
                    mMarsTvExpectedFocusSection = -1;
                    if (mMarsTvAutoOpenShorts
                            && mCurrentSectionId == MediaGroup.TYPE_SHORTS) {
                        mMarsTvAutoOpenShorts = false;
                        for (Video first : new ArrayList<>(mCurrentVideos)) {
                            if (first != null && first.videoId != null && first.hasVideo()) {
                                onVideoClicked(first);
                                break;
                            }
                        }
                    }''',
'''                    // Focus only. Entering Shorts does not click or play a card.
                    marsTvFocusCard(0);
                    mMarsTvExpectedFocusSection = -1;''')
once(browse,
'''            mMarsTvAutoOpenShorts = destination == 8;
            mMarsTvExpectedFocusSection = destination == 8 ? MediaGroup.TYPE_SHORTS''',
'''            mMarsTvExpectedFocusSection = destination == 8 ? MediaGroup.TYPE_SHORTS''')

shell = tv / "playback/MarsTvShell.java"
once(shell,
'''        default Video currentVideo() { return null; }''',
'''        default Video currentVideo() { return null; }
        default void ensureShortsFeed() { }''')

# The original END handler was gated by hasNext(), so it could never fetch
# new pages at the current snapshot boundary. The Activity's scoped next()
# is now responsible for requesting one metadata page if needed.
once(shell,
'''                } else if (com.newtube.mobile.ui.common.MarsShortsSession.hasNext()) {
                    actions.next();
                } else {
                    // At the end of the fetched Shorts snapshot, do NOT jump to
                    // unrelated regular videos. The feed's end remains explicit.
                }''',
'''                } else {
                    // The scoped player next() handles a paginated refill at the
                    // end of the in-memory snapshot; never use regular suggestions.
                    actions.next();
                }''')
once(shell,
'''        boolean shorts = actions.isShortsPlayback();
        if (!shorts || exo == null) {''',
'''        boolean shorts = actions.isShortsPlayback();
        if (shorts) actions.ensureShortsFeed();
        if (!shorts || exo == null) {''')

playback = tv / "playback/MobilePlaybackActivity.java"
once(playback,
'''    @androidx.annotation.Nullable private MarsTvShell mMarsTvShell;''',
'''    @androidx.annotation.Nullable private MarsTvShell mMarsTvShell;
    @androidx.annotation.Nullable private com.newtube.mobile.ui.common.MarsShortsDiscovery mMarsTvDiscovery;

    private com.newtube.mobile.ui.common.MarsShortsDiscovery marsTvDiscovery() {
        if (mMarsTvDiscovery == null)
            mMarsTvDiscovery = new com.newtube.mobile.ui.common.MarsShortsDiscovery(
                    getApplicationContext());
        return mMarsTvDiscovery;
    }''')
once(playback,
'''                    @Override public Video currentVideo() { return getVideo(); }''',
'''                    @Override public Video currentVideo() { return getVideo(); }
                    @Override public void ensureShortsFeed() {
                        if (com.newtube.mobile.ui.common.MarsShortsSession
                                .isActiveFor(getVideo())) {
                            marsTvDiscovery().maybeLoad();
                        }
                    }''')
once(playback,
'''        if (target == null) {
            android.widget.Toast.makeText(this, "Kein weiterer Short in dieser Liste",
                    android.widget.Toast.LENGTH_SHORT).show();
        } else {
            onRelatedClicked(target);
        }
        return true;
    }

    private void onRelatedClicked(Video video) {''',
'''        if (target == null) {
            if (direction > 0) {
                // Same scoped session. One pending Next while we obtain a new
                // Shorts metadata page. No second player, no full-stream prefetch.
                marsTvDiscovery().requestNext(() -> {
                    if (isFinishing() || isDestroyed() || mPresenter == null
                            || !com.newtube.mobile.ui.common.MarsShortsSession
                                    .isActiveFor(getVideo())) return;
                    Video fresh = com.newtube.mobile.ui.common.MarsShortsSession
                            .step(getVideo(), 1);
                    if (fresh != null) onRelatedClicked(fresh);
                });
            }
        } else {
            onRelatedClicked(target);
        }
        return true;
    }

    private void onRelatedClicked(Video video) {''')
once(playback,
'''            maybeLiftStillAtFrame();
            onVideoShapeKnown();
        }
    };''',
'''            maybeLiftStillAtFrame();
            onVideoShapeKnown();
            // Record *rendered* Shorts only, not focused overview thumbnails.
            // Persist a bounded local history of 1,000 video IDs.
            if (com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(
                    MobilePlaybackActivity.this)
                    && com.newtube.mobile.ui.common.MarsShortsSession
                            .isActiveFor(getVideo())) {
                marsTvDiscovery().markWatched(getVideo());
            }
        }
    };''')
once(playback,
'''        if (mMarsShortsSearch != null) { mMarsShortsSearch.close(); mMarsShortsSearch = null; }
        if (mMarsTvShell != null) { mMarsTvShell.release(); mMarsTvShell = null; }''',
'''        if (mMarsShortsSearch != null) { mMarsShortsSearch.close(); mMarsShortsSearch = null; }
        if (mMarsTvDiscovery != null) { mMarsTvDiscovery.release(); mMarsTvDiscovery = null; }
        if (mMarsTvShell != null) { mMarsTvShell.release(); mMarsTvShell = null; }''')

gradle = root / "smarttubetv/build.gradle"
once(gradle, "versionCode 12031", "versionCode 12032")
once(gradle, 'versionName "0.0.31"', 'versionName "0.0.32"')
assert 'applicationId "de.equiummc.marstube2"' in gradle.read_text(encoding="utf-8")
assert "getShortsObserve()" in discovery.read_text(encoding="utf-8")
assert "continueGroupObserve(page)" in discovery.read_text(encoding="utf-8")
assert 'mMarsTvAutoOpenShorts' not in browse.read_text(encoding="utf-8")
assert 'mMarsTvDiscovery.release()' in playback.read_text(encoding="utf-8")
assert "if (actions.closeSearch()) return true;" in shell.read_text(encoding="utf-8")
print("MT2-032: overview selection restored, Shorts Next paginates, Loop repeats, Discovery seen IDs capped at 1,000.")
