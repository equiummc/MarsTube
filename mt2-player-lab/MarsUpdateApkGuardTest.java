package com.newtube.mobile.update;

import android.app.Application;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.ConscryptMode;
import static org.junit.Assert.assertFalse;

/** Safe preflight must fail closed for broken or untrusted update inputs. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, manifest = Config.NONE, application = Application.class)
@ConscryptMode(ConscryptMode.Mode.OFF)
public final class MarsUpdateApkGuardTest {
    private final Application app = RuntimeEnvironment.getApplication();

    @Test
    public void cannotInstallWithoutAnApk() {
        assertFalse(MarsUpdateApkGuard.isCompatible(app, null, 12013));
    }

    @Test
    public void cannotInstallWithoutAValidTargetVersion() {
        assertFalse(MarsUpdateApkGuard.isCompatible(app, "/does/not/exist.apk", 0));
        assertFalse(MarsUpdateApkGuard.isCompatible(app, "/does/not/exist.apk", -1));
    }

    @Test
    public void missingApkFailsClosed() {
        assertFalse(MarsUpdateApkGuard.isCompatible(app,
                "/invalid/path/marstube-tv-0.0.13.apk", 12013));
    }
}
