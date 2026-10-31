package com.liskovsoft.smartyoutubetv2.common.app.models.playback.controllers;

/**
 * MT-010B safety boundary for a future genuine *sample* preloader.
 * This does not fetch media or create a second player.
 *
 * The current SABR loader makes state-dependent HTTP POST requests. Its media
 * samples cannot safely be reused by a generic URL-keyed GET cache.
 * Only a proven request-identity-preserving SABR handoff may enable it.
 */
public final class ShortsSamplePreloadGate {
    public static final int MAX_PRELOAD_BYTES = 4 * 1024 * 1024;
    public static final long MAX_PRELOAD_LEAD_MS = 4_000L;
    public static final long MAX_PRELOAD_DURATION_MS = 15_000L;
    public static final int MAX_TARGETS = 1;

    public enum Decision {
        ALLOW,
        SWITCH_OFF,
        NOT_SHORT_VOD,
        INVALID_NEXT,
        FOREGROUND_BUSY,
        QUALITY_MISMATCH,
        SABR_HANDOFF_NOT_PROVEN,
        SOURCE_HANDOFF_NOT_PROVEN
    }

    public static final class Input {
        public final boolean enabled;
        public final boolean currentShort;
        public final boolean nextShort;
        public final boolean live;
        public final boolean hasNext;
        public final boolean foregroundReady;
        public final boolean foregroundBuffered;
        public final boolean qualityMatches;
        public final boolean sabr;
        public final boolean sabrHandoffVerified;
        public final boolean sampleHandoffVerified;

        public Input(boolean enabled, boolean currentShort, boolean nextShort,
                boolean live, boolean hasNext, boolean foregroundReady,
                boolean foregroundBuffered, boolean qualityMatches,
                boolean sabr, boolean sabrHandoffVerified, boolean sampleHandoffVerified) {
            this.enabled = enabled;
            this.currentShort = currentShort;
            this.nextShort = nextShort;
            this.live = live;
            this.hasNext = hasNext;
            this.foregroundReady = foregroundReady;
            this.foregroundBuffered = foregroundBuffered;
            this.qualityMatches = qualityMatches;
            this.sabr = sabr;
            this.sabrHandoffVerified = sabrHandoffVerified;
            this.sampleHandoffVerified = sampleHandoffVerified;
        }
    }

    private ShortsSamplePreloadGate() { }

    public static Decision evaluate(Input input) {
        if (input == null || !input.enabled) return Decision.SWITCH_OFF;
        if (!input.currentShort || !input.nextShort || input.live) {
            return Decision.NOT_SHORT_VOD;
        }
        if (!input.hasNext) return Decision.INVALID_NEXT;
        if (!input.foregroundReady || !input.foregroundBuffered) {
            return Decision.FOREGROUND_BUSY;
        }
        if (!input.qualityMatches) return Decision.QUALITY_MISMATCH;
        if (input.sabr && !input.sabrHandoffVerified) {
            return Decision.SABR_HANDOFF_NOT_PROVEN;
        }
        if (!input.sampleHandoffVerified) {
            return Decision.SOURCE_HANDOFF_NOT_PROVEN;
        }
        return Decision.ALLOW;
    }
}
