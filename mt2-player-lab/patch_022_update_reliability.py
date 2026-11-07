#!/usr/bin/env python3
"""MT2-022: independent live update feeds, select newest verified manifest.

Build-time overlay for the pinned NewTube + SharedModules source tree, applied
after patch_021. Does not affect the APK download, package ID, signature
preflight, user install confirmation, TV navigation or video player.
"""
from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit("Usage: patch_022_update_reliability.py <pinned-newtube-root>")
root = Path(sys.argv[1]).resolve()
assert (root / "sabr-media3").is_dir(), "Expected pinned Media3 source"

def once(path, old, new):
    if not path.is_file():
        raise SystemExit(f"MT2-022 missing {path}")
    src = path.read_text(encoding="utf-8")
    if src.count(old) != 1:
        raise SystemExit(f"MT2-022 unsafe anchor count={src.count(old)} in {path}: {old[:95]!r}")
    path.write_text(src.replace(old, new, 1), encoding="utf-8")

feed = root / "common/src/main/res/values/update_urls.xml"
once(feed, '''        <item>https://github.com/equiummc/MarsTube/releases/latest/download/marstube-tv.json</item>
''', '''        <item>https://github.com/equiummc/MarsTube/releases/latest/download/marstube-tv.json</item>
        <!-- Independent origin, same trusted MarsTube repo; updated after each signed release. -->
        <item>https://raw.githubusercontent.com/equiummc/MarsTube/mars/production-2/updates/stable.json</item>
''')

app = root / ("smarttubetv/src/stmobile/java/com/newtube/mobile/update/AppUpdates.java")
once(app, '''        final String trusted = "https://github.com/equiummc/MarsTube/"
                + "releases/latest/download/marstube-tv.json";
        if (urls.length != 1 || !trusted.equals(urls[0])) {
            Log.e(TAG, "Reject untrusted MarsTube update feed");
            return new String[0];
        }
        return urls;''',
'''        final String primary = "https://github.com/equiummc/MarsTube/"
                + "releases/latest/download/marstube-tv.json";
        final String mirror = "https://raw.githubusercontent.com/equiummc/MarsTube/"
                + "mars/production-2/updates/stable.json";
        if (urls.length != 2 || !primary.equals(urls[0]) || !mirror.equals(urls[1])) {
            Log.e(TAG, "Reject untrusted MarsTube update feed");
            return new String[0];
        }
        return urls;''')
once(app, '''    public void check() {''',
'''    public void check() {
        android.util.Log.i(TAG, "manual check tap installed=" + mInstalledName
                + " code=" + mInstalledCode + " phase=" + mPhase
                + " sources=" + mManifestUrls.length);''')

checker = root / ("SharedModules/appupdatechecker2/src/main/java/"
                  "com/liskovsoft/appupdatechecker2/core/AppVersionChecker.java")
once(checker, '''            for (Uri url : urls) {
                jo = getJSON(url);
                if (jo != null)
                    break;
            }

            return jo;''',
'''            // MT2-022: select the newest *valid* manifest, not the first HTTP 200.
            // The GitHub release asset can be blocked/stale on some Android TV
            // networks. The raw repository mirror uses a separate HTTPS origin.
            // Only the two audited MarsTube URLs are supplied by AppUpdates.
            int highestCode = Integer.MIN_VALUE;
            for (Uri url : urls) {
                JSONObject candidate = getJSON(url);
                if (candidate == null) {
                    android.util.Log.w(TAG, "manifest source failed host=" + url.getHost());
                    continue;
                }
                try {
                    UpdateManifest parsed = UpdateManifest.parse(candidate,
                            DeviceHelpers.getPrimaryAbi(),
                            LocaleUtility.getCurrentLanguage(mContext));
                    int candidateCode = parsed.latest().versionCode;
                    android.util.Log.i(TAG, "manifest source host=" + url.getHost()
                            + " latest=" + parsed.latest().versionName
                            + " code=" + candidateCode
                            + " installedCode=" + mCurrentAppVersion);
                    if (candidateCode > highestCode) {
                        jo = candidate;
                        highestCode = candidateCode;
                    }
                    // A genuinely newer version can be offered immediately.
                    // If this source says "up to date", always cross-check
                    // the independent mirror before telling the user so.
                    if (candidateCode > mCurrentAppVersion) break;
                } catch (JSONException e) {
                    mLastException = e;
                    android.util.Log.e(TAG, "invalid manifest from " + url.getHost(), e);
                }
            }
            android.util.Log.i(TAG, "manifest chosen code=" + highestCode
                    + " sources=" + urls.length);
            return jo;''')

assert "mars_check" in checker.read_text(encoding="utf-8")
assert "highestCode" in checker.read_text(encoding="utf-8")
assert "mManualRecheckAfterAuto" in app.read_text(encoding="utf-8")
assert "MarsUpdateApkGuard.isCompatible" in app.read_text(encoding="utf-8")
assert 'applicationId "de.equiummc.marstube2"' in (root / "smarttubetv/build.gradle").read_text(encoding="utf-8")
assert "raw.githubusercontent.com/equiummc/MarsTube" in feed.read_text(encoding="utf-8")
print("MT2-022: select freshest valid signed-release metadata across 2 trusted HTTPS origins.")
