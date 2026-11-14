#!/usr/bin/env python3
"""Issue #72: patch the real stmobile production path after 039. Fail on drift."""
from pathlib import Path
import shutil
import sys

root = Path(sys.argv[1]).resolve()
here = Path(__file__).resolve().parent
ui = root / 'smarttubetv/src/stmobile/java/com/newtube/mobile/ui'
browse = ui / 'browse/MobileBrowseActivity.java'
cards = ui / 'browse/VideoCardAdapter.java'
sidebar = root / 'common/src/main/java/com/liskovsoft/smartyoutubetv2/common/app/presenters/service/SidebarService.java'

def one(path, old, new):
    s = path.read_text(encoding='utf-8')
    if s.count(old) != 1:
        raise SystemExit(f'040 contract drift {path.name}: {s.count(old)} hits {old[:90]!r}')
    path.write_text(s.replace(old,new,1), encoding='utf-8')

def method(path, signature, body):
    s = path.read_text(encoding='utf-8')
    if s.count(signature) != 1: raise SystemExit('040 method drift: '+signature)
    start = s.index(signature)
    a = s.index('{',start)
    level = 1
    b = a+1
    while level:
        level += (s[b]=='{') - (s[b]=='}')
        b += 1
    path.write_text(s[:a+1]+'\n'+body+'\n    '+s[b-1:],encoding='utf-8')

for name in ['MarsTvGrid040','MarsTvGridPolicy040','MarsKnightRider040']:
    shutil.copyfile(here/(name+'.java'),ui/'browse'/(name+'.java'))
tests = root/'smarttubetv/src/test/java/com/newtube/mobile/ui/browse'
tests.mkdir(parents=True,exist_ok=True)
shutil.copyfile(here/'MarsTvGridPolicy040Test.java',tests/'MarsTvGridPolicy040Test.java')
shutil.copyfile(here/'MarsSidebarOrder040.java',sidebar.parent/'MarsSidebarOrder040.java')
# Run the pure sidebar migration regression in the existing app unit-test task.
shutil.copyfile(here/'MarsSidebarOrder040Test.java',tests/'MarsSidebarOrder040Test.java')

one(browse,'    private View mFeedSkeleton;', '''    private View mFeedSkeleton;
    private MarsKnightRider040 mMarsLoading040;
    private boolean mMarsProfilesOpen040;
    private int mMarsFocusEpoch040;''')
one(browse,'                this::marsTvLibraryB035);','''                this::marsTvLibraryB035);
        MarsTvGrid040.install(mContentGrid);
        mMarsLoading040 = new MarsKnightRider040(this);
        androidx.constraintlayout.widget.ConstraintLayout host = findViewById(R.id.mobile_browse_root);
        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams lp =
                new androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(0,
                    Math.max(3, Math.round(3*getResources().getDisplayMetrics().density)));
        lp.startToStart = R.id.mobile_content_swipe;
        lp.endToEnd = R.id.mobile_content_swipe;
        lp.topToTop = R.id.mobile_content_swipe;
        host.addView(mMarsLoading040,lp);
        mContentSwipe.setEnabled(false);''')
one(browse,'    private void setSkeletonVisible(boolean visible) {','''    private void setSkeletonVisible(boolean visible) {
        if (mMarsLoading040 != null) {
            mFeedSkeleton.setVisibility(View.GONE);
            mContentGrid.setAlpha(1f);
            mGridHiddenForSkeleton = false;
            return;
        }''')
one(browse,'            // Empty grid = first load: card-skeleton ghosts', '''            if (mMarsLoading040 != null) {
                setSkeletonVisible(false);
                mMarsLoading040.setLoading(show);
                mContentSwipe.setRefreshing(false);
                return;
            }
            // Empty grid = first load: card-skeleton ghosts''')
one(browse,'        boolean switched = mCurrentSectionId != sectionId;', '''        if (mMarsLoading040 != null) mMarsLoading040.setLoading(false);
        boolean switched = mCurrentSectionId != sectionId;''')
one(browse,'        if (mMarsTvB035 != null) mMarsTvB035.pause();','''        if (mMarsTvB035 != null) mMarsTvB035.pause();
        if (mMarsLoading040 != null) mMarsLoading040.setLoading(false);
        mMarsFocusEpoch040++;''')
one(browse,'    private void updateGridSpanCount(int spanCount) {','''    private void updateGridSpanCount(int spanCount) {
        if (mContentGrid != null) mContentGrid.setTag(R.id.mobile_content_grid, mCurrentSectionId);''')
method(browse,'    private int computeSpanCount(Configuration configuration)', '''        if (com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(this)) {
            if (mContentGrid != null) mContentGrid.setTag(R.id.mobile_content_grid,mCurrentSectionId);
            if (mCurrentSectionId == MediaGroup.TYPE_SHORTS) return 2;
            if (mCurrentSectionId == MediaGroup.TYPE_HOME || mCurrentSectionId == MediaGroup.TYPE_HISTORY
                    || mCurrentSectionId == MediaGroup.TYPE_LIVE || mCurrentSectionId == MediaGroup.TYPE_MUSIC
                    || mCurrentSectionId == MediaGroup.TYPE_SUBSCRIPTIONS)
                return com.newtube.mobile.player.MarsTvPlayerPrefs.getHomeTileCount(this) == 4 ? 2 : 3;
        }
        return com.newtube.mobile.ui.common.MobileGrid.computeSpanCount(configuration);''')
method(cards,'        private void marsTvBindShortsAspect(Video video)', '''            if (com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(itemView.getContext()))
                MarsTvGrid040.bind(itemView);''')
one(cards,'        return new VideoViewHolder(inflater.inflate(R.layout.item_video_card, parent, false), mClickListener);', '''        View card = inflater.inflate(R.layout.item_video_card, parent, false);
        card.setTag(R.id.mobile_content_grid,parent);
        return new VideoViewHolder(card, mClickListener);''')
# Re-apply after the avatar callback, which otherwise changes regular-card geometry.
one(cards,'            ((VideoViewHolder) holder).bindChannelAvatar(mChannelClickListener);','''            ((VideoViewHolder) holder).bindChannelAvatar(mChannelClickListener);
            if (com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(holder.itemView.getContext()))
                MarsTvGrid040.bind(holder.itemView);''')
one(cards,'                    .override(size[0], size[1])\n                    .centerCrop();','''                    .override(size[0], size[1])
                    .apply(com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(context)
                        ? com.bumptech.glide.request.RequestOptions.fitCenterTransform()
                        : com.bumptech.glide.request.RequestOptions.centerCropTransform());''')
one(cards,'                        .override(size[0], size[1])\n                        .centerCrop());','''                        .override(size[0], size[1])
                        .apply(com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(context)
                            ? com.bumptech.glide.request.RequestOptions.fitCenterTransform()
                            : com.bumptech.glide.request.RequestOptions.centerCropTransform()));''')
# Use a large portrait decode target rather than the old in-player preview size.
method(cards,'        private static int[] thumbnailSize(Context context, Video video)', '''            if (com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(context)
                    && video != null && video.isShorts) {
                int height = Math.max(1, context.getResources().getDisplayMetrics().heightPixels / 2);
                return new int[]{Math.max(1,height*9/16),height};
            }
            return new int[]{thumbWidth(context),sThumbH};''')

# Preserve previous values on disk; the new TV standard is six until the user
# explicitly selects a density again. The same preference then applies to all five pages.
prefs = root/'smarttubetv/src/stmobile/java/com/newtube/mobile/player/MarsTvPlayerPrefs.java'
one(prefs,'        int value = getInt(context, KEY_HOME_TILES, TILES_4);','''        if (!getBool(context,"mars040_grid_explicit",false)) return TILES_6;
        int value = getInt(context, KEY_HOME_TILES, TILES_6);''')
one(prefs,'        putInt(context, KEY_HOME_TILES, value);','''        putBool(context,"mars040_grid_explicit",true);
        putInt(context, KEY_HOME_TILES, value);''')
preftest=root/'smarttubetv/src/test/java/com/newtube/mobile/player/MarsTvPlayerPrefsTest.java'
# Only the first default expectation changes; rejection of invalid explicit values stays.
s=preftest.read_text()
assert s.count('assertEquals(MarsTvPlayerPrefs.TILES_4, MarsTvPlayerPrefs.getHomeTileCount(app));') == 2
preftest.write_text(s.replace('assertEquals(MarsTvPlayerPrefs.TILES_4, MarsTvPlayerPrefs.getHomeTileCount(app));',
            'assertEquals(MarsTvPlayerPrefs.TILES_6, MarsTvPlayerPrefs.getHomeTileCount(app));',1))

# Pinned upstream hard-codes Linux protoc; select the matching official binary for local PS7 builds.
one(root/'sabr-media3/build.gradle',
    "dependencies { sabrProtoc 'com.google.protobuf:protoc:3.22.3:linux-x86_64@exe' }",
    '''def protocPlatform = System.getProperty('os.name').toLowerCase().contains('windows')
        ? 'windows-x86_64' : 'linux-x86_64'
dependencies { sabrProtoc "com.google.protobuf:protoc:3.22.3:${protocPlatform}@exe" }''')
one(root/'smarttubetv/build.gradle',
    "    sabrProofProtoc 'com.google.protobuf:protoc:3.22.3:linux-x86_64@exe'",
    '''    sabrProofProtoc "com.google.protobuf:protoc:3.22.3:${System.getProperty('os.name').toLowerCase().contains('windows') ? 'windows-x86_64' : 'linux-x86_64'}@exe"''')

# Direct profile routing and cancellation of stale asynchronous card-focus requests.
one(browse,'''        } else if (destination == 6) {
            mBottomNav.post(() -> mBottomNav.setSelectedItemId(YOU_ITEM_ID));''', '''        } else if (destination == 6) {
            marsTvOpenProfiles040();''')
one(browse,'    private void marsTvAccountSelected038() {','''    private void marsTvOpenProfiles040() {
        if (mMarsProfilesOpen040) return;
        mMarsProfilesOpen040 = true;
        mMarsFocusEpoch040++;
        MarsTvProfiles038.show(this,this::marsTvAccountSelected038,() -> {
            mMarsProfilesOpen040 = false;
            com.newtube.mobile.ui.common.MarsTvRail.focus(this);
        });
    }

    private void marsTvAccountSelected038() {''')
s=browse.read_text(encoding='utf-8')
assert s.count('MarsTvProfiles038.show(this,this::marsTvAccountSelected038)') == 2
browse.write_text(s.replace('MarsTvProfiles038.show(this,this::marsTvAccountSelected038)','marsTvOpenProfiles040()'),encoding='utf-8')
one(browse,'        mContentGrid.post(new Runnable() {\n            int attempts;', '''        final int focusEpoch = mMarsFocusEpoch040;
        mContentGrid.post(new Runnable() {
            int attempts;''')
one(browse,'                // Only focus real cards from the newly selected section.', '''                if (mMarsProfilesOpen040 || focusEpoch != mMarsFocusEpoch040) return;
                // Only focus real cards from the newly selected section.''')
one(browse,'    public void marsTvRoute(int destination) {','''    public void marsTvRoute(int destination) {
        mMarsFocusEpoch040++;''')
one(browse,'''                    : destination == 3 ? MediaGroup.TYPE_HISTORY
                    : MediaGroup.TYPE_HOME;''', '''                    : destination == 3 ? MediaGroup.TYPE_HISTORY
                    : destination == 9 ? MediaGroup.TYPE_LIVE
                    : destination == 10 ? MediaGroup.TYPE_MUSIC
                    : MediaGroup.TYPE_HOME;''')
profile=ui/'browse/MarsTvProfiles038.java'
one(profile,'    public static void show(Activity activity, Runnable onChanged) {','''    public static void show(Activity activity, Runnable onChanged) {
        show(activity,onChanged,null);
    }
    public static void show(Activity activity, Runnable onChanged, Runnable onClosed) {''')
one(profile,'                YTSignInPresenter.instance(activity).start();','''                if (onClosed != null) onClosed.run();
                YTSignInPresenter.instance(activity).start();''')
one(profile,'            render(activity, accounts, onChanged);','            render(activity, accounts, onChanged, onClosed);')
one(profile,'                               Runnable onChanged) {','                               Runnable onChanged, Runnable onClosed) {')
one(profile,'        Dialog dialog = new Dialog(activity);','''        Dialog dialog = new Dialog(activity);
        dialog.setOnDismissListener(d -> { if (onClosed != null) onClosed.run(); });''')
one(profile,'        row.setOnClickListener(v -> onClick.run());','''        row.setOnClickListener(v -> onClick.run());
        row.setOnKeyListener((v,key,event) -> key == android.view.KeyEvent.KEYCODE_DPAD_LEFT
                || key == android.view.KeyEvent.KEYCODE_DPAD_RIGHT);''')
one(profile,'Aktives Profil bestimmt Abos, Empfehlungen und ausgeblendete Videos.',
            'Verfügbare YouTube-Identitäten. Weitere Kanalprofile müssen vom Anmeldedienst angeboten werden.')

model=ui/'common/MarsTvNavigationModel.java'
one(model,'''            new Entry("Startseite", 0),
            new Entry("Shorts", 8),''','''            new Entry("Shorts", 8),
            new Entry("Startseite", 0),''')
one(model,'public static final int HOME_INDEX = 3;','public static final int HOME_INDEX = 4;')
navtest=root/'smarttubetv/src/test/java/com/newtube/mobile/ui/common/MarsTvNavigationModelTest.java'
one(navtest,'assertEquals(3, MarsTvNavigationModel.HOME_INDEX);','''assertEquals(4, MarsTvNavigationModel.HOME_INDEX);
        assertEquals(8, entries[3].destination);''')
one(sidebar,'    private int mBootSectionId;','''    private int mBootSectionId;
    private boolean mMarsOrderMigrated040;''')
one(sidebar,'        mIsSettingsSectionEnabled = Helpers.parseBoolean(split, 2, true);','''        mIsSettingsSectionEnabled = Helpers.parseBoolean(split, 2, true);
        mMarsOrderMigrated040 = Helpers.parseBoolean(split, 3, false);''')
one(sidebar,'        cleanupPinnedItems();','''        cleanupPinnedItems();
        if (!mMarsOrderMigrated040) {
            MarsSidebarOrder040.migrate(mPinnedItems,this::getSectionId,
                    MediaGroup.TYPE_HOME,MediaGroup.TYPE_SHORTS);
            mMarsOrderMigrated040 = true;
            persistState();
        }''')
one(sidebar,'Helpers.mergeData(mPinnedItems, mBootSectionId, mIsSettingsSectionEnabled)',
            'Helpers.mergeData(mPinnedItems, mBootSectionId, mIsSettingsSectionEnabled, mMarsOrderMigrated040)')

# Proven hidden playback path: TV must not enter the phone audio-background mode
# when no visible in-app mini session exists. BACK within the visible player is untouched.
playback=ui/'playback/MobilePlaybackActivity.java'
one(playback,'        mIsStopped = true;','''        mIsStopped = true;
        if (com.newtube.mobile.ui.browse.MarsTvGridPolicy040.shouldPauseHidden(
                com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(this),mIsInPip,
                MiniPlayerBridge.isActive()) && mPlayer != null) mPlayer.pause();''')
# When the Browse mini host itself becomes hidden, do not leave its shared audio playing.
# Android's process visibility, rather than activity switches, distinguishes HOME
# from switching Browse -> player/settings and preserves the explicit in-app mini.
one(browse,'''    protected void onStop() {
        super.onStop();''','''    protected void onStop() {
        super.onStop();
        if (com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(this)
                && MiniPlayerBridge.isActive()) {
            android.app.ActivityManager.RunningAppProcessInfo state =
                    new android.app.ActivityManager.RunningAppProcessInfo();
            android.app.ActivityManager.getMyMemoryState(state);
            if (state.importance > android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE
                    && MiniPlayerBridge.getPlayer() != null) MiniPlayerBridge.getPlayer().pause();
        }''')
gradle=root/'smarttubetv/build.gradle'
one(gradle,'versionCode 12039','versionCode 12040')
one(gradle,'versionName "0.0.39"','versionName "0.0.40"')
assert "abiFilters 'armeabi-v7a'" in gradle.read_text(encoding='utf-8')
print('MT2-040: measured TV grids, content loader, real profile focus, targeted menu migration, hidden TV playback guard')
