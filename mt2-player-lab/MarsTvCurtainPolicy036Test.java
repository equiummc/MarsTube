package com.newtube.mobile.ui.common;

import org.junit.Test;
import static org.junit.Assert.*;

/** Portrait thumbnail: no animation; landscape placeholder: center-open curtain. */
public final class MarsTvCurtainPolicy036Test {
    @Test public void nativePortraitPreviewHasNoExtraAnimation() {
        assertFalse(MarsTvCurtainPolicy036.shouldReveal(720,1280,720,1280,true));
    }
    @Test public void landscapePreviewToShortGetsCurtain() {
        assertTrue(MarsTvCurtainPolicy036.shouldReveal(1280,720,720,1280,true));
    }
    @Test public void normalVideoAndUnknownSizeDoNotAnimate() {
        assertFalse(MarsTvCurtainPolicy036.shouldReveal(1280,720,1280,720,true));
        assertFalse(MarsTvCurtainPolicy036.shouldReveal(1280,720,720,1280,false));
        assertFalse(MarsTvCurtainPolicy036.shouldReveal(0,0,720,1280,true));
    }
}
