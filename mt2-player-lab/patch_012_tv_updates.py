#!/usr/bin/env python3
"""MT2-012: TV in-app update baseline, own feed, consent and APK guard.

Apply after patch_011_firetv_navigation.py in the disposable pinned source tree.
Never changes the old 0.0.9 / 0.0.10 / 0.0.11 installations.
"""
from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit("Usage: patch_012_tv_updates.py <pinned-reference-checkout>")
root = Path(sys.argv[1]).resolve()
mobile = root / "smarttubetv/src/stmobile"
tv = mobile / "java/com/newtube/mobile/ui"
if not (root / "sabr-media3").is_dir():
    raise SystemExit("MT2-012: pinned checkout with SABR Media3 required")


def once(path, old, new):
    if not path.is_file():
        raise SystemExit(f"MT2-012 missing: {path}")
    source = path.read_text(encoding="utf-8")
    count = source.count(old)
    if count != 1:
        raise SystemExit(f"MT2-012 fail closed: {path}: expected one anchor, got {count}: {old[:90]!r}")
    path.write_text(source.replace(old, new, 1), encoding="utf-8")


gradle = root / "smarttubetv/build.gradle"
manifest = mobile / "AndroidManifest.xml"
once(gradle, 'applicationId "de.equiummc.marstube2.firetest011"',
     'applicationId "de.equiummc.marstube2.firetest012"')
once(gradle, 'versionCode 12011', 'versionCode 12012')
once(gradle, 'versionName "0.0.11"', 'versionName "0.0.12"')
once(manifest, 'android:label="MarsTube 0.0.11"',
     'android:label="MarsTube 0.0.12"')

# Only the user-confirmed update route is allowed. Android manages the
# per-app "install unknown apps" consent. This is NOT INSTALL_PACKAGES.
once(manifest,
     '    <uses-permission android:name="android.permission.VIBRATE" />',
     '''    <uses-permission android:name="android.permission.VIBRATE" />
    <!-- MT2-012: only for explicitly confirmed in-app APK updates. -->
    <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />''')

# Trusted feed is MarsTube-controlled and currently intentionally absent
# until a matching signed release has been published. NEVER use NewTube feed.
feed = root / "common/src/main/res/values/update_urls.xml"
if not feed.is_file() or "aleixrodriala/newtube" not in feed.read_text(encoding="utf-8"):
    raise SystemExit("MT2-012: unknown upstream update source")
feed.write_text('''<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string-array name="update_urls">
        <!-- MT2-012: only own published stable-channel Android TV APKs. -->
        <item>https://github.com/equiummc/MarsTube/releases/latest/download/marstube-tv.json</item>
    </string-array>
</resources>
''', encoding="utf-8")

# The UI already has the user-driven update/download/progress/install screen.
# Replace the 0.0.11 no-op with the actual check and TV focus.
settings = tv / "settings/MarsTvSettingsPages.java"
once(settings,
'''        rows.add(SettingsRow.action("MarsTube-Updates",
                "Diese Testversion wird als APK lokal installiert.", page -> { }));''',
'''        rows.add(SettingsRow.action("Auf Updates prüfen",
                "MarsTube-Versionen vom eigenen GitHub-Release; Installation nur nach Bestätigung.",
                page -> com.newtube.mobile.ui.update.MobileUpdateActivity
                        .startCheck(page.requireContext())));''')

updater = tv / "update/AppUpdates.java"
once(updater,
'''    private static String[] manifestUrls(Context context) {
        String override = BuildConfig.DEBUG ? MobileMainApplication.getDebugSystemProperty("debug.arc.update_manifest") : "";

        if (!override.isEmpty()) {
            Log.w(TAG, "update manifest overridden (debug): " + override);
            return new String[] {override};
        }

        return context.getResources().getStringArray(R.array.update_urls);
    }''',
'''    private static String[] manifestUrls(Context context) {
        // MT2-012: block upstream/third-party sources even in debug builds.
        // No debug system-property override can redirect the installer.
        String[] urls = context.getResources().getStringArray(R.array.update_urls);
        final String trusted = "https://github.com/equiummc/MarsTube/"
                + "releases/latest/download/marstube-tv.json";
        if (urls.length != 1 || !trusted.equals(urls[0])) {
            Log.e(TAG, "Reject untrusted MarsTube update feed");
            return new String[0];
        }
        return urls;
    }''')

once(updater,
'''    public boolean install(Activity activity) {
        String path = mInfo != null ? mInfo.apkPath : null;
        Uri uri = path != null ? FileHelpers.getFileUri(activity, path) : null;''',
'''    public boolean install(Activity activity) {
        String path = mInfo != null ? mInfo.apkPath : null;
        // Even a valid remote manifest must never install a different app,
        // a downgrade, or an APK signed with a different certificate.
        if (!MarsUpdateApkGuard.isCompatible(activity, path,
                mInfo != null ? mInfo.versionCode : -1)) {
            Log.w(TAG, "MarsTube update APK verification failed");
            return false;
        }
        Uri uri = FileHelpers.getFileUri(activity, path);''')

once(updater,
'''        return mContext.getString(R.string.about_source_code_url) + "/releases/tag/v" + versionName;''',
'''        return "https://github.com/equiummc/MarsTube/releases/tag/v" + versionName;''')

guard = Path(__file__).resolve().parent / "MarsUpdateApkGuard.java"
dst = tv / "update/MarsUpdateApkGuard.java"
if not guard.is_file():
    raise SystemExit("MT2-012 missing MarsUpdateApkGuard.java")
dst.write_text(guard.read_text(encoding="utf-8"), encoding="utf-8")

# Exercise fail-closed behavior in the existing Robolectric TV unit-test task.
guard_test = Path(__file__).resolve().parent / "MarsUpdateApkGuardTest.java"
if not guard_test.is_file():
    raise SystemExit("MT2-012 missing MarsUpdateApkGuardTest.java")
test_dst = root / "smarttubetv/src/test/java/com/newtube/mobile/update/MarsUpdateApkGuardTest.java"
test_dst.parent.mkdir(parents=True, exist_ok=True)
test_dst.write_text(guard_test.read_text(encoding="utf-8"), encoding="utf-8")

sheet = tv / "update/MobileUpdateActivity.java"
once(sheet,
'''        render();
        mSheet.show();
    }''',
'''        render();
        mSheet.show();
        if (com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(this)) {
            // Explicit focus target for D-pad/OK, also when the sheet begins in
            // CHECKING (no buttons yet). The source NewTube sheet is touch-first.
            mPrimary.setFocusable(true);
            mSecondary.setFocusable(true);
            mPrimary.setMinHeight(dp(52));
            mSecondary.setMinHeight(dp(52));
            mPrimary.post(() -> {
                if (mPrimary.isShown()) mPrimary.requestFocus();
                else if (mSecondary.isShown()) mSecondary.requestFocus();
            });
        }
    }''')

once(sheet,
'''        bind(mSecondary, secondaryText, secondary);
        bind(mPrimary, primaryText, primary);
    }''',
'''        bind(mSecondary, secondaryText, secondary);
        bind(mPrimary, primaryText, primary);
        if (com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(this)) {
            // When CHECKING turns into READY/AVAILABLE the UI must remain operable.
            mPrimary.post(() -> {
                if (mSheet != null && mSheet.isShowing()
                        && !mPrimary.hasFocus() && !mSecondary.hasFocus()) {
                    if (mPrimary.isShown()) mPrimary.requestFocus();
                    else if (mSecondary.isShown()) mSecondary.requestFocus();
                }
            });
        }
    }''')

print("MT2-012 applied: own HTTPS release feed, Android install consent,")
print("version/package/signature preflight, TV update UI and separate v0.0.12 package.")
