package com.newtube.mobile.ui.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Pure, unit-tested limits and opaque profile keys for rejection storage. */
public final class MarsTvInterestPolicy037 {
    public static final int MAX_IDS = 100_000;
    private MarsTvInterestPolicy037() {}
    public static int overflow(long newCount) {
        return newCount <= MAX_IDS ? 0
                : (int)Math.min(Integer.MAX_VALUE, newCount - MAX_IDS);
    }
    public static String profileKey(String accountAndChannel) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(accountAndChannel.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder("profile:");
            final char[] HEX = "0123456789abcdef".toCharArray();
            for (byte b : digest) {
                out.append(HEX[(b >>> 4) & 15]).append(HEX[b & 15]);
            }
            return out.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
