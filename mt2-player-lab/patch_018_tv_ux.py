#!/usr/bin/env python3
"""MT2-018: Fire TV PiP containment, real user-triggered update refresh.

Apply after patch_017_shorts_stage.py to the pinned disposable source tree.
No change to the permanent package/signing identity.
"""
from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit("Usage: patch_018_tv_ux.py <pinned-checkout>")

root = Path(sys.argv[1]).resolve()
tv = root / "smarttubetv/src/stmobile/java/com/newtube/mobile/ui"
if not (root / "sabr-media3").is_dir():
    raise SystemExit("MT2-018: pinned Media3 checkout missing")

def once(path, before, after):
    if not path.is_file():
        raise SystemExit(f"MT2-018 missing: {path}")
    data = path.read_text(encoding="utf-8")
    n = data.count(before)
    if n != 1:
        raise SystemExit(f"MT2-018 anchor count {n}: {path}: {before[:90]!r}")
    path.write_text(data.replace(before, after, 1), encoding="utf-8")

playback = tv / "playback/MobilePlaybackActivity.java"

# TV remotes cannot reliably focus/expand an Android-system PiP on Fire TV.
# Keep the *in-app* mini-player, which already has a selectable TV rail/dock.
# Suppress ALL three automatic/manual system PiP entry paths on a television,
# but leave the original phone behavior untouched.
once(playback,
    '''    protected void onUserLeaveHint() {
        super.onUserLeaveHint();

        if (mSuppressAutoPip) {''',
    '''    protected void onUserLeaveHint() {
        super.onUserLeaveHint();

        if (com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(this)) {
            // Fire TV Home must not leave an unreachable pinned overlay.
            logPip("leave-skip reason=tv-no-system-pip");
            return;
        }
        if (mSuppressAutoPip) {''')

once(playback,
    '''    private void enterPipMode() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O''',
    '''    private void enterPipMode() {
        if (com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(this)) {
            logPip("enter-skip reason=tv-no-system-pip");
            return;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O''')

once(playback,
    '''    private boolean shouldAutoEnterPip() {
        return !mSuppressAutoPip''',
    '''    private boolean shouldAutoEnterPip() {
        return !com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(this)
                && !mSuppressAutoPip''')

updater = root / "smarttubetv/src/stmobile/java/com/newtube/mobile/update/AppUpdates.java"

# Settings -> "Auf Updates prüfen" is an explicit request: never present a
# 10-minute-old UP_TO_DATE answer as though the feed had just been checked.
once(updater,
    '''            case AVAILABLE:
            case UP_TO_DATE:
                if (SystemClock.elapsedRealtime() - mInfoAtMs < FRESH_MS) {
                    return;
                }
                break;''',
    '''            case AVAILABLE:
            case UP_TO_DATE:
                // A manual check always contacts the one trusted release feed.
                // Keep the download/ready phases guarded above.
                break;''')

once(updater,
    '''    public void onUpToDate(UpdateInfo info) {
        mUserCheck = false;''',
    '''    public void onUpToDate(UpdateInfo info) {
        android.util.Log.i(TAG, "check result=up_to_date installedCode=" + mInstalledCode
                + " remoteCode=" + (info == null ? -1 : info.versionCode)
                + " remoteName=" + (info == null ? "unknown" : info.versionName));
        // Guard against a shared-checker classification error when the manifest
        // itself did carry a higher versionCode.
        if (info != null && info.versionCode > mInstalledCode) {
            android.util.Log.w(TAG, "correcting stale up-to-date classification");
            onUpdateAvailable(info);
            return;
        }
        mUserCheck = false;''')

once(updater,
    '''        Log.d(TAG, "update available: " + info.versionName''',
    '''        Log.i(TAG, "update available: " + info.versionName''')

shell = (tv / "playback/MarsTvShell.java").read_text(encoding="utf-8")
stage = (tv / "playback/MarsTvShortsStage.java").read_text(encoding="utf-8")
session = (tv / "common/MarsShortsSession.java").read_text(encoding="utf-8")
checks = {
    "single stable package": 'applicationId "de.equiummc.marstube2"' in (root / "smarttubetv/build.gradle").read_text(encoding="utf-8"),
    "2 or 3 Shorts previews": ("for (int i = 0; i < 3; i++)" in stage
                             or "for (int i = 0; i < 2; i++)" in stage),
    "independent focus": "SHORTS_PREVIEWS" in shell,
    "same video session": "MarsShortsSession.select(selectedIndex)" in stage,
    "real rating actions": "actions::like, actions::dislike" in shell,
    "bottom scrub bar": "Gravity.RIGHT | Gravity.BOTTOM" in stage,
    "manual update check": "A manual check always contacts" in updater.read_text(encoding="utf-8"),
}
for label, passed in checks.items():
    if not passed:
        raise SystemExit(f"MT2-018 missing invariant: {label}")

print("MT2-018: no unreachable system PiP on TV, Shorts previews, D-pad rating/seek, live update check.")
