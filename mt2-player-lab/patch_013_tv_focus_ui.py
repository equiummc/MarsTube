#!/usr/bin/env python3
"""MT2-013: Fire TV real-device corrections, after patch_012_tv_updates.py.

This patch intentionally keeps the same applicationId as 0.0.12 for the
eventual signed-update pair. Never publish an update signed by a random CI
debug key as though it were compatible with an earlier CI installation.
"""
from pathlib import Path
import sys
import os

if len(sys.argv) != 2:
    raise SystemExit("Usage: patch_013_tv_focus_ui.py <pinned-checkout>")
root = Path(sys.argv[1]).resolve()
mobile = root / "smarttubetv/src/stmobile"
tv = mobile / "java/com/newtube/mobile/ui"

def once(path, before, after):
    if not path.is_file():
        raise SystemExit(f"MT2-013 missing: {path}")
    text = path.read_text(encoding="utf-8")
    if text.count(before) != 1:
        raise SystemExit(f"MT2-013 fail closed {path}: anchor count {text.count(before)}: {before[:100]!r}")
    path.write_text(text.replace(before, after, 1), encoding="utf-8")

# Standalone 0.0.13 has an isolated package; the paired UPDATE LAB
# intentionally retains a single package for both 0.0.12 and 0.0.13.
# Both modes still require identical signing certificates for upgrades.
gradle = root / "smarttubetv/build.gradle"
manifest = mobile / "AndroidManifest.xml"
if os.environ.get("MT2_UPDATE_LAB", "") != "1":
    once(gradle, 'applicationId "de.equiummc.marstube2.firetest012"',
         'applicationId "de.equiummc.marstube2.firetest013"')
once(gradle, 'versionCode 12012', 'versionCode 12013')
once(gradle, 'versionName "0.0.12"', 'versionName "0.0.13"')
once(manifest, 'android:label="MarsTube 0.0.12"',
     'android:label="MarsTube 0.0.13"')

for filename, target in (
    ("MarsTvShell.java", tv / "playback/MarsTvShell.java"),
    ("MarsTvRail.java", tv / "common/MarsTvRail.java"),
):
    source = Path(__file__).resolve().parent / filename
    if not target.is_file() or not source.is_file():
        raise SystemExit(f"MT2-013 missing TV overlay {filename}")
    target.write_text(source.read_text(encoding="utf-8"), encoding="utf-8")

browse = tv / "browse/MobileBrowseActivity.java"
once(browse,
'''    // MT2-002 BEGIN: D-pad compatible temporary reference UI; no touch gestures required.
    private int mMarsTvSelection = 0;''',
'''    // MT2-002 BEGIN: D-pad compatible temporary reference UI; no touch gestures required.
    private int mMarsTvSelection = 0;

    // Focus transfer cannot work against an empty RecyclerView before the
    // asynchronous Shorts feed arrives; retry boundedly and don't use an
    // obsolete item index from the previous section.
    public void marsTvRestoreContentFocus() {
        if (!com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(this)) return;
        if (mYouShowing) {
            hideYouPanel();
        }
        mMarsTvSelection = 0;
        if (mContentGrid == null) return;
        mContentGrid.post(new Runnable() {
            int attempts;
            @Override public void run() {
                if (isFinishing() || isDestroyed() || mContentGrid == null) return;
                if (mAdapter != null && mAdapter.getItemCount() > 0
                        && mContentGrid.getVisibility() == View.VISIBLE) {
                    marsTvFocusCard(0);
                } else if (++attempts < 25) {
                    mContentGrid.postDelayed(this, 100);
                }
            }
        });
    }''')

once(browse,
'''    public void marsTvRoute(int destination) {
        android.content.Intent intent = new android.content.Intent();''',
'''    public void marsTvRoute(int destination) {
        if (destination == 8 || destination == 0 || destination == 2
                || destination == 3) {
            // Previous sections may have had a completely different grid.
            mMarsTvSelection = 0;
            if (mContentGrid != null) {
                mContentGrid.postDelayed(this::marsTvRestoreContentFocus, 120);
            }
        }
        android.content.Intent intent = new android.content.Intent();''')

# In the Home grid BACK goes to navigation; outside Home it returns to Home
# instead of leaving an inaccessible screen. Android takes over BACK on other
# activities (Settings, Search etc). The rail itself keeps its own BACK path.
once(browse,
'''        boolean mapped = key == KeyEvent.KEYCODE_DPAD_LEFT
                || key == KeyEvent.KEYCODE_DPAD_RIGHT''',
'''        boolean mapped = key == KeyEvent.KEYCODE_BACK
                || key == KeyEvent.KEYCODE_DPAD_LEFT
                || key == KeyEvent.KEYCODE_DPAD_RIGHT''')
once(browse,
'''        switch (key) {
            case KeyEvent.KEYCODE_DPAD_LEFT:''',
'''        switch (key) {
            case KeyEvent.KEYCODE_BACK:
                if (mCurrentSectionId != MediaGroup.TYPE_HOME) {
                    marsTvRoute(0);
                } else {
                    com.newtube.mobile.ui.common.MarsTvRail.focus(this);
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:''')

# Shorts tiles must have a deterministic transition from rail to grid.
# The old source updater can reload section asynchronously; don't consume
# OK on an invalid or absent ViewHolder forever.
once(browse,
'''                if (target != null) target.itemView.performClick();
                break;''',
'''                if (target != null) {
                    target.itemView.performClick();
                } else {
                    marsTvRestoreContentFocus();
                }
                break;''')

print("MT2-013: TV focus/back fixes and compact overlay applied; isolated 013 build or same-package signed update pair.")
