package com.newtube.mobile.ui.browse;
import org.junit.Test;
import static org.junit.Assert.*;

public class MarsTvGridPolicy040Test {
    @Test public void twoCompleteRowsFitEveryViewport() {
        for (int height : new int[]{480,720,960,1080,2160}) {
            int row = MarsTvGridPolicy040.rowHeight(height,16,12);
            assertTrue(row*2 <= height-28);
            assertTrue(height-28-row*2 < 2);
            for (int width : new int[]{220,360,560,1100}) {
                int image = MarsTvGridPolicy040.imageHeight(width,row,62,false);
                assertTrue(image+62 <= row);
                assertTrue(image*16/9 <= width);
                assertEquals(row-8,MarsTvGridPolicy040.imageHeight(width,row,8,true));
            }
        }
    }
    @Test public void tvBackgroundPausesButVisibleMiniAndPipRemain() {
        assertTrue(MarsTvGridPolicy040.shouldPauseHidden(true,false,false));
        assertFalse(MarsTvGridPolicy040.shouldPauseHidden(true,false,true));
        assertFalse(MarsTvGridPolicy040.shouldPauseHidden(true,true,false));
        assertFalse(MarsTvGridPolicy040.shouldPauseHidden(false,false,false));
    }
}
