package com.newtube.mobile.update;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.util.Log;
import androidx.annotation.Nullable;
import java.io.File;
import java.util.Arrays;

/**
 * Blocks accidental cross-app/cross-channel updates before handing an APK to
 * Android's user-confirmed installer. Android still performs the authoritative
 * cryptographic APK/signature verification.
 */
public final class MarsUpdateApkGuard {
    private static final String TAG = "MarsTubeUpdate";
    private static final long MAX_APK_BYTES = 280L * 1024L * 1024L;
    private static final long MIN_APK_BYTES = 1L * 1024L * 1024L;

    private MarsUpdateApkGuard() {}

    public static boolean isCompatible(Context context, @Nullable String apkPath,
                                       int expectedVersionCode) {
        if (apkPath == null || expectedVersionCode <= 0) return false;
        File apk = new File(apkPath);
        if (!apk.isFile() || apk.length() < MIN_APK_BYTES
                || apk.length() > MAX_APK_BYTES) {
            Log.w(TAG, "Reject update: APK absent or invalid size");
            return false;
        }

        PackageManager pm = context.getPackageManager();
        int flags = Build.VERSION.SDK_INT >= 28
                ? PackageManager.GET_SIGNING_CERTIFICATES
                : PackageManager.GET_SIGNATURES;
        try {
            PackageInfo installed = pm.getPackageInfo(context.getPackageName(), flags);
            PackageInfo candidate = pm.getPackageArchiveInfo(apk.getAbsolutePath(), flags);
            if (candidate == null
                    || !context.getPackageName().equals(candidate.packageName)) {
                Log.w(TAG, "Reject update: unrelated Android package");
                return false;
            }
            long currentCode = Build.VERSION.SDK_INT >= 28
                    ? installed.getLongVersionCode() : installed.versionCode;
            long nextCode = Build.VERSION.SDK_INT >= 28
                    ? candidate.getLongVersionCode() : candidate.versionCode;
            if (nextCode != expectedVersionCode || nextCode <= currentCode) {
                Log.w(TAG, "Reject update: mismatched or downgraded version");
                return false;
            }
            Signature[] current = signatures(installed);
            Signature[] next = signatures(candidate);
            if (current == null || current.length == 0 || next == null
                    || next.length != current.length || !Arrays.equals(current, next)) {
                Log.w(TAG, "Reject update: signing certificate differs");
                return false;
            }
            return true;
        } catch (Exception error) {
            // Fail closed. No APK path, certificate bytes, or URLs in logs.
            Log.w(TAG, "Reject update: package verification unavailable");
            return false;
        }
    }

    @Nullable
    private static Signature[] signatures(PackageInfo info) {
        if (Build.VERSION.SDK_INT >= 28) {
            if (info.signingInfo == null) return null;
            return info.signingInfo.getApkContentsSigners();
        }
        return info.signatures;
    }
}
