#!/usr/bin/env python3
"""MT2-038: TV YouTube profile picker, real Live and Music destinations.
Apply after 037 on a disposable pinned-source checkout. Fail closed on drift.
"""
from pathlib import Path
import shutil
import sys

if len(sys.argv) != 2:
    raise SystemExit("Usage: patch_038_profiles_live_music.py <pinned-source>")
root=Path(sys.argv[1]).resolve()
here=Path(__file__).resolve().parent
tv=root/"smarttubetv/src/stmobile/java/com/newtube/mobile/ui"
browse=tv/"browse/MobileBrowseActivity.java"
model=tv/"common/MarsTvNavigationModel.java"
theme=tv/"common/MarsTvNavigationTheme.java"
test=root/"smarttubetv/src/test/java/com/newtube/mobile/ui/common/MarsTvNavigationModelTest.java"
gradle=root/"smarttubetv/build.gradle"

def once(path,old,new):
    s=path.read_text(encoding="utf-8")
    hits=s.count(old)
    if hits != 1:
        raise SystemExit("MT2-038 FAIL CLOSED %s: %d hits for %r" %
                         (path.name,hits,old[:120]))
    path.write_text(s.replace(old,new,1),encoding="utf-8")

for src,dest in [
    ("MarsTvProfiles038.java",tv/"browse/MarsTvProfiles038.java"),
    ("MarsTvNavigationModel038.java",model),
    ("MarsTvNavigationModelTest038.java",test),
]:
    from_file=here/src
    if not from_file.is_file() or not dest.parent.is_dir():
        raise SystemExit("MT2-038 required file missing: "+src)
    shutil.copyfile(from_file,dest)

drawables=root/"smarttubetv/src/stmobile/res/drawable"
for filename in ("mars_nav_live_038.xml","mars_nav_music_038.xml"):
    src=here/"assets/drawable"/filename
    if not src.is_file() or not drawables.is_dir():
        raise SystemExit("MT2-038 missing vector icon: "+filename)
    shutil.copyfile(src,drawables/filename)

# All main navigations use MarsTvNavigationModel.main(), including the player's
# rail. Do not fork UI lists or change the HOME_INDEX or playback back button.
once(theme,
'''            case 6: return R.drawable.mars_nav_profile_025;''',
'''            case 6: return R.drawable.mars_nav_profile_025;
            case 9: return R.drawable.mars_nav_live_038;
            case 10: return R.drawable.mars_nav_music_038;''')

# The pinned BrowsePresenter already registers actual TYPE_LIVE + TYPE_MUSIC
# and their real getLiveObserve/getMusicObserve endpoints. Route via the same
# onSectionChosen path used by all other browse sections; no dummy search.
once(browse,
'''        if (destination == 1) {
            SearchPresenter.instance(this).startSearch(null);''',
'''        if (destination == 9 || destination == 10) {
            final int realSection = destination == 9 ? MediaGroup.TYPE_LIVE
                    : MediaGroup.TYPE_MUSIC;
            mBottomNav.post(() -> {
                if (isFinishing() || isDestroyed()) return;
                hideYouPanel();
                mMarsTvSelection = 0;
                onSectionChosen(realSection);
                if (mContentGrid != null) {
                    mContentGrid.postDelayed(this::marsTvRestoreContentFocus,120);
                }
            });
            return;
        }
        if (destination == 1) {
            SearchPresenter.instance(this).startSearch(null);''')
once(browse,
'''        if (destination == 8 || destination == 0 || destination == 2
                || destination == 3) {''',
'''        if (destination == 8 || destination == 0 || destination == 2
                || destination == 3 || destination == 9 || destination == 10) {''')

# Keep You/Mediathek as the existing left-rail "Profil" entry; its account
# header and the new dedicated "Profil wechseln" row open the account chooser.
# Phone continues to use AccountsSheet, with no behavioural changes.
once(browse,
'''        accountRow.setOnClickListener(v -> AccountsSheet.show(this, this::updateAccountRow));''',
'''        accountRow.setOnClickListener(v -> {
            if (com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(this)) {
                MarsTvProfiles038.show(this,this::marsTvAccountSelected038);
            } else {
                AccountsSheet.show(this,this::updateAccountRow);
            }
        });''')
once(browse,
'''    private void updateAccountRow() {
        if (mYouAccountText == null) {''',
'''    /** Account selection is backed by the existing native YouTube Account ID.
     * Never reuse the previous profile's thumbnails or Shorts session.
     */
    private void marsTvAccountSelected038() {
        if (!com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(this)) return;
        com.newtube.mobile.ui.common.FeedCache.clear();
        com.newtube.mobile.ui.common.MarsShortsSession.clear();
        mCurrentVideos.clear();
        if (mAdapter != null) mAdapter.submitList(new java.util.ArrayList<>());
        hideYouPanel();
        updateAccountRow();
        mSectionFromYou = false;
        mBottomNav.post(() -> {
            if (isFinishing() || isDestroyed()) return;
            onSectionChosen(MediaGroup.TYPE_HOME);
            if (mPresenter != null) mPresenter.refresh(false);
            marsTvFocusCard(0);
        });
    }

    private void updateAccountRow() {
        if (mYouAccountText == null) {''')
once(browse,
'''        mYouRows.removeAllViews();
        mYouGridRow = null;
        mYouUpdateRow = null;''',
'''        mYouRows.removeAllViews();
        mYouGridRow = null;
        mYouUpdateRow = null;
        if (com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(this)) {
            addYouRow(R.drawable.ic_mobile_account, "YouTube-Profil wechseln", () ->
                    MarsTvProfiles038.show(this,this::marsTvAccountSelected038));
        }''')

once(gradle,"versionCode 12037","versionCode 12038")
once(gradle,'versionName "0.0.37"','versionName "0.0.38"')
assert 'applicationId "de.equiummc.marstube2"' in gradle.read_text(encoding="utf-8")
assert "new Entry(\"Live\", 9)" in model.read_text(encoding="utf-8")
assert "new Entry(\"Musik\", 10)" in model.read_text(encoding="utf-8")
assert "case 9: return R.drawable.mars_nav_live_038" in theme.read_text(encoding="utf-8")
assert "MarsTvProfiles038.show" in browse.read_text(encoding="utf-8")
assert "MediaGroup.TYPE_LIVE" in browse.read_text(encoding="utf-8")
assert "MediaGroup.TYPE_MUSIC" in browse.read_text(encoding="utf-8")
print("MT2-038: TV real profiles, Live, Music; identical signed app; original media services unchanged")
