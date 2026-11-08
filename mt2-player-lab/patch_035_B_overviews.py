#!/usr/bin/env python3
"""MT2-035 Punkt B: TV browse layout, subscriptions and media library.

Runs after MT2-034 A in the isolated pinned-source builder. Mobile unaffected.
Fail closed at upstream contract boundaries; never touch identity or signing.
"""
from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit("Usage: patch_035_B_overviews.py <isolated-source>")
root = Path(sys.argv[1])
here = Path(__file__).resolve().parent
tv = root / "smarttubetv/src/stmobile/java/com/newtube/mobile/ui"
browse = tv / "browse/MobileBrowseActivity.java"
cards = tv / "browse/VideoCardAdapter.java"
shell = tv / "playback/MarsTvShell.java"
gradle = root / "smarttubetv/build.gradle"
b_src = here / "MarsTvBrowseB035.java"
if not b_src.is_file() or not browse.is_file() or not cards.is_file() or not shell.is_file():
    raise SystemExit("MT2-035 B: source missing")
(tv / "browse/MarsTvBrowseB035.java").write_bytes(b_src.read_bytes())

def one(path, before, after):
    s = path.read_text(encoding="utf-8")
    n = s.count(before)
    if n != 1:
        raise SystemExit(f"MT2-035 B fail closed: {path.name} ({n} hits) {before[:120]!r}")
    path.write_text(s.replace(before, after, 1), encoding="utf-8")

def nreplace(path, before, after, expected):
    s = path.read_text(encoding="utf-8")
    if s.count(before) != expected:
        raise SystemExit(f"MT2-035 B fail closed: {path.name}: {s.count(before)} not {expected}")
    path.write_text(s.replace(before, after), encoding="utf-8")

# This page feature is scoped to TV. Leave all phone/card and mini-player services.
one(browse,
    "    private BrowseTopBar mTopBar;",
    "    private BrowseTopBar mTopBar;\n    @androidx.annotation.Nullable private MarsTvBrowseB035 mMarsTvB035;")
one(browse,
    """        bindViews();
        setupContentGrid();""",
    """        bindViews();
        marsTvInstallB035();
        setupContentGrid();""")
one(browse,
    "    private void bindViews() {",
    """    private void marsTvInstallB035() {
        if (!com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(this)) return;
        mMarsTvB035 = new MarsTvBrowseB035(this,
                (androidx.constraintlayout.widget.ConstraintLayout)
                    findViewById(R.id.mobile_browse_root),
                mContentSwipe, mContentGrid,
                this::marsTvApplyB035Filter, () -> marsTvFocusCard(0),
                this::marsTvLibraryB035);
    }

    private java.util.List<Video> marsTvB035Visible() {
        return mMarsTvB035 == null ? mCurrentVideos
                : mMarsTvB035.visible(mCurrentVideos, mCurrentSectionId);
    }

    private void marsTvApplyB035Filter() {
        if (mMarsTvB035 == null || mAdapter == null) return;
        mMarsTvSelection = 0;
        mAdapter.submitList(new java.util.ArrayList<>(marsTvB035Visible()));
        mContentGrid.scrollToPosition(0);
    }

    private void marsTvLibraryB035(String key) {
        int target = -1;
        if ("history".equals(key)) {
            target = MediaGroup.TYPE_HISTORY;
        } else if ("playlists".equals(key)) {
            target = MediaGroup.TYPE_USER_PLAYLISTS;
        } else {
            for (BrowseSection section : mSections) {
                if (!section.isEnabled()) continue;
                String title = section.getTitle() == null ? ""
                        : section.getTitle().toLowerCase(java.util.Locale.GERMANY);
                if ("music".equals(key) && (title.contains("musik")
                        || title.contains("music"))) {
                    target = section.getId();
                    break;
                }
                if ("later".equals(key) && (title.contains("später ansehen")
                        || title.contains("watch later"))) {
                    target = section.getId();
                    break;
                }
            }
            if (target < 0 && "later".equals(key)) {
                // Watch Later is often a playlist rather than a BrowseSection.
                target = MediaGroup.TYPE_USER_PLAYLISTS;
                android.widget.Toast.makeText(this,
                        "Später ansehen findest du unter Playlists",
                        android.widget.Toast.LENGTH_SHORT).show();
            }
        }
        if (target < 0) {
            android.widget.Toast.makeText(this,
                    "Bereich derzeit nicht verfügbar",
                    android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        boolean exists = false;
        for (BrowseSection section : mSections) {
            if (section.isEnabled() && section.getId() == target) {
                exists = true;
                break;
            }
        }
        if (!exists) {
            android.widget.Toast.makeText(this,
                    "Bereich erst nach Anmeldung verfügbar",
                    android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        mSectionFromYou = target != MediaGroup.TYPE_HISTORY;
        hideYouPanel();
        onSectionChosen(target);
        onYouPanelToggled();
        mContentGrid.post(() -> marsTvFocusCard(0));
    }

    private void bindViews() {""")
one(browse,
    """        boolean switched = mCurrentSectionId != sectionId;
        mCurrentSectionId = sectionId;
        paintCachedSnapshot(sectionId, switched);""",
    """        boolean switched = mCurrentSectionId != sectionId;
        mCurrentSectionId = sectionId;
        if (mMarsTvB035 != null) {
            mMarsTvB035.onSection(sectionId);
            updateGridSpanCount(computeSpanCount());
        }
        paintCachedSnapshot(sectionId, switched);""")
one(browse,
    """            boolean switched = mCurrentSectionId != section.getId();
            mCurrentSectionId = section.getId();
            paintCachedSnapshot(section.getId(), switched);""",
    """            boolean switched = mCurrentSectionId != section.getId();
            mCurrentSectionId = section.getId();
            if (mMarsTvB035 != null) {
                mMarsTvB035.onSection(mCurrentSectionId);
                updateGridSpanCount(computeSpanCount());
            }
            paintCachedSnapshot(section.getId(), switched);""")
one(browse,
    """    private void showYouPanel() {
        mYouShowing = true;""",
    """    private void showYouPanel() {
        mYouShowing = true;
        if (mMarsTvB035 != null) mMarsTvB035.setLibraryVisible(true);""")
one(browse,
    """    private void hideYouPanel() {
        mYouShowing = false;""",
    """    private void hideYouPanel() {
        mYouShowing = false;
        if (mMarsTvB035 != null) mMarsTvB035.setLibraryVisible(false);""")
one(browse,
    """        mLastPaginationTriggerCount = -1;
        mLastNearEndTriggerCount = -1;
        mContentGrid.animate().cancel();""",
    """        if (mMarsTvB035 != null) mMarsTvB035.onVideos(mCurrentVideos);
        mLastPaginationTriggerCount = -1;
        mLastNearEndTriggerCount = -1;
        mContentGrid.animate().cancel();""")
nreplace(browse,
    "mAdapter.submitList(new ArrayList<>(mCurrentVideos)",
    "mAdapter.submitList(new ArrayList<>(marsTvB035Visible())",
    5)
one(browse,
    "        List<Video> list = new ArrayList<>(mCurrentVideos);",
    "        List<Video> list = new ArrayList<>(marsTvB035Visible());")
one(browse,
    "            submitFeed(staleSwap);\n            checkFeedRunway();",
    "            if (mMarsTvB035 != null) mMarsTvB035.onVideos(mCurrentVideos);\n            submitFeed(staleSwap);\n            checkFeedRunway();")
one(browse,
    """        if (AppUpdates.instance(this).hasUpdate()) {
            addYouUpdateRow();
        }

        List<BrowseSection> navSections = selectNavSections();""",
    """        if (AppUpdates.instance(this).hasUpdate()) {
            addYouUpdateRow();
        }
        if (mMarsTvB035 != null) mMarsTvB035.insertLibraryTiles(mYouRows);

        List<BrowseSection> navSections = selectNavSections();""")
# Retain the user-selected Home tiles (4/6/9) and History setting, but
# enforce a compact two-pane Abos grid and the requested 3-wide Shorts overview.
one(browse,
    "                return com.newtube.mobile.player.MarsTvPlayerPrefs.shortsSpanCount(this);",
    "                return 3;")
one(browse,
    """            if (mCurrentSectionId == MediaGroup.TYPE_SUBSCRIPTIONS
                    && com.newtube.mobile.player.MarsTvPlayerPrefs.isSubscriptionsGrid(this)) {
                return 3;
            }""",
    """            if (mCurrentSectionId == MediaGroup.TYPE_SUBSCRIPTIONS) {
                return 2;
            }""")
one(browse,
    "        updateGridSpanCount(computeSpanCount(newConfig));",
    "        updateGridSpanCount(computeSpanCount(newConfig));\n"
    "        if (mMarsTvB035 != null) mMarsTvB035.onSection(mCurrentSectionId);")
one(browse,
    """        updateAccountRow();
        getWindow().getDecorView().postDelayed(mUpdateCheckIfDue, UPDATE_CHECK_DELAY_MS);""",
    """        updateAccountRow();
        if (mMarsTvB035 != null) {
            mMarsTvB035.resume();
            mMarsTvB035.onSection(mCurrentSectionId);
            mMarsTvB035.onVideos(mCurrentVideos);
        }
        getWindow().getDecorView().postDelayed(mUpdateCheckIfDue, UPDATE_CHECK_DELAY_MS);""")
one(browse,
    """        if (mPresenter != null) {
            mPresenter.onViewPaused();
        }

        getWindow().getDecorView().removeCallbacks(mUpdateCheckIfDue);""",
    """        if (mPresenter != null) {
            mPresenter.onViewPaused();
        }
        if (mMarsTvB035 != null) mMarsTvB035.pause();
        getWindow().getDecorView().removeCallbacks(mUpdateCheckIfDue);""")
one(browse,
    "        if (marsTvMiniPlayerKey(event)) return true;",
    """        if (mMarsTvB035 != null && mMarsTvB035.handleKey(event,
                mMarsTvSelection, mLayoutManager == null ? 1 : mLayoutManager.getSpanCount()))
            return true;
        if (marsTvMiniPlayerKey(event)) return true;""")
# Restore TV focus, existing R.id and playback semantics untouched.
one(cards,
    "            boolean shorts = video != null && video.isShorts;",
    """            boolean shorts = video != null && video.isShorts;
            // The outer home/shorts card is narrower within its grid cell.
            // Focus stays on the original click target; phones are untouched.
            int cardInset = Math.round((shorts ? 5f : 8f) * dm.density);
            itemView.setPaddingRelative(cardInset, dp10 / 3, cardInset, dp10 / 2);
            if (shorts) {
                // Fit two portrait rows in the TV overview, never stretch the source.
                int maxPerRow = Math.max(dp80, (dm.heightPixels
                        - Math.round(185f * dm.density)) / 2
                        - Math.round(52f * dm.density));
                width = Math.min(width, Math.max(dp78, Math.round(maxPerRow * 9f / 16f)));
            }""")
one(cards, "shorts ? 13f : 16f", "shorts ? 12.5f : 14.5f")
one(cards, "shorts ? 11f : 13f", "shorts ? 10.5f : 12f")

# The player date from 033 should also appear with the Shorts top/rail,
# but not permanently obscure the portrait video.
one(shell,
    """        clock.setVisibility(!pip && !shortsMode && !playerPanels.isOpen()
                && (panelVisible || focus == Focus.RAIL || infoOpen)""",
    """        clock.setVisibility(!pip && !playerPanels.isOpen()
                && ((shortsMode && focus == Focus.RAIL)
                    || (!shortsMode && (panelVisible || focus == Focus.RAIL || infoOpen)))""")

one(gradle, "versionCode 12034", "versionCode 12035")
one(gradle, 'versionName "0.0.34"', 'versionName "0.0.35"')
src = browse.read_text(encoding="utf-8")
assert "MarsTvBrowseB035" in src
assert "marsTvB035Visible()" in src
assert "insertLibraryTiles" in src
assert "onSection(sectionId)" in src
assert "mMarsTvB035.handleKey" in src
assert "return 3;" in src
assert 'applicationId "de.equiummc.marstube2"' in gradle.read_text(encoding="utf-8")
print("MT2-035 B: compact home, 3x2 Shorts overview, local de-DE date, two-column Abos, library quick cards.")
