package com.newtube.mobile.ui.common;

/** Pure decision policy; zero work until a first decoded portrait frame exists. */
public final class MarsTvCurtainPolicy036 {
    private MarsTvCurtainPolicy036() {}
    public static boolean shouldReveal(int imageWidth, int imageHeight,
                                       int frameWidth, int frameHeight,
                                       boolean tvShorts) {
        return tvShorts && imageWidth > 0 && imageHeight > 0
                && frameWidth > 0 && frameHeight > 0
                && (float) imageWidth / imageHeight > 1.15f
                && (float) frameWidth / frameHeight <= 0.88f;
    }
}
