#!/usr/bin/env python3
"""MT2-020: force an uncached HTTPS manifest fetch before declaring TV up-to-date.

Only the pinned disposable newtube-reference tree is modified. The trusted
manifest host, APK signing checks and one permanent Android package stay intact.
"""
from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit("Usage: patch_020_update_freshness.py <pinned-checkout>")
root = Path(sys.argv[1]).resolve()
if not (root / "sabr-media3").is_dir():
    raise SystemExit("MT2-020: pinned Media3 checkout absent")

checker = root / ("SharedModules/appupdatechecker2/src/main/java/"
                  "com/liskovsoft/appupdatechecker2/core/AppVersionChecker.java")
app = root / ("smarttubetv/src/stmobile/java/com/newtube/mobile/"
              "update/AppUpdates.java")
if not checker.is_file() or not app.is_file():
    raise SystemExit("MT2-020 missing updater source")


def once(path: Path, old: str, new: str) -> None:
    source = path.read_text(encoding="utf-8")
    count = source.count(old)
    if count != 1:
        raise SystemExit(f"MT2-020 unsafe patch: {path}: anchor count={count}")
    path.write_text(source.replace(old, new, 1), encoding="utf-8")


# Android's existing update flow already limits the source to exactly the
# MarsTube-controlled latest/download/marstube-tv.json. Do NOT change that
# allowlist: a query argument only defeats stale CDN/proxy/OkHttp results.
# Updating the shared checker affects the metadata request ONLY; it does not
# rename the APK URL or disable any archive/signature guards.
once(checker,
     """                MyRequest request = new MyRequest(urlStr);
                long reqId = manager.enqueue(request);""",
     """                // MT2-020: every update check must fetch a fresh manifest.
                // The immutable source/allowlist is still validated by AppUpdates.
                Uri fresh = urlStr.buildUpon()
                        .appendQueryParameter("mars_check",
                                Long.toString(System.currentTimeMillis()) + "-"
                                + Long.toString(android.os.SystemClock.elapsedRealtimeNanos()))
                        .build();
                MyRequest request = new MyRequest(fresh);
                long reqId = manager.enqueue(request);""")

# The previous patch adds basic comparison logs, but a false "up to date"
# needs an explicit server version to explain which manifest was actually read.
once(app,
     '''        android.util.Log.i(TAG, "check result=up_to_date installedCode=" + mInstalledCode
                + " remoteCode=" + (info == null ? -1 : info.versionCode)
                + " remoteName=" + (info == null ? "unknown" : info.versionName));''',
     '''        Log.i(TAG, "check result=up_to_date installed=" + mInstalledName
                + " code=" + mInstalledCode
                + " manifestLatest=" + (info == null ? "unknown" : info.versionName)
                + " manifestCode=" + (info == null ? -1 : info.versionCode)
                + " check=uncached");''')

# A user pressing "Check" while a launch check is already running should not
# mistake that earlier (possibly pre-release) answer for a fresh user check.
once(app,
     '''    private boolean mAutoCheckInFlight;''',
     '''    private boolean mAutoCheckInFlight;
    private boolean mManualRecheckAfterAuto;''')
once(app,
     '''        if (mAutoCheckInFlight && SystemClock.elapsedRealtime() - mAutoCheckAtMs < AUTO_JOIN_MS) {
            return; // the automatic check's answer is this one's: one request, one answer
        }''',
     '''        if (mAutoCheckInFlight && SystemClock.elapsedRealtime() - mAutoCheckAtMs < AUTO_JOIN_MS) {
            mManualRecheckAfterAuto = true;
            return; // finish first request; then recheck with a new cache-busting URL
        }''')
once(app,
     '''    public void onUpToDate(UpdateInfo info) {
        android.util.Log.i''',
     '''    public void onUpToDate(UpdateInfo info) {
        if (mManualRecheckAfterAuto) {
            mManualRecheckAfterAuto = false;
            mAutoCheckInFlight = false;
            // Callback arrives after AppVersionChecker reset its in-flight state.
            // The second GET has a new nonce and cannot reuse the old manifest.
            mChecker.forceCheckForUpdates(mManifestUrls);
            return;
        }
        android.util.Log.i''')
once(app,
     '''    public void onUpdateAvailable(UpdateInfo info) {
        mUserCheck = false;''',
     '''    public void onUpdateAvailable(UpdateInfo info) {
        mManualRecheckAfterAuto = false;
        mUserCheck = false;''')
once(app,
     '''    public void onUpdateError(Exception error) {
        boolean userCheck = mUserCheck;''',
     '''    public void onUpdateError(Exception error) {
        if (mManualRecheckAfterAuto) {
            mManualRecheckAfterAuto = false;
            mAutoCheckInFlight = false;
            mChecker.forceCheckForUpdates(mManifestUrls);
            return;
        }
        boolean userCheck = mUserCheck;''')

stage = checker.read_text(encoding="utf-8")
updates = app.read_text(encoding="utf-8")
assert "mars_check" in stage
assert "getStreamForDownloadedFile" in stage
assert "mManualRecheckAfterAuto" in updates
assert "MarsUpdateApkGuard.isCompatible" in updates
assert "Reject untrusted MarsTube update feed" in updates
assert 'releases/latest/download/marstube-tv.json' in (root /
    "common/src/main/res/values/update_urls.xml").read_text(encoding="utf-8")
assert 'applicationId "de.equiummc.marstube2"' in (root /
    "smarttubetv/build.gradle").read_text(encoding="utf-8")
print("MT2-020: metadata GET cache bust, manual check no longer inherits old auto result.")
