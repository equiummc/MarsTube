package com.newtube.mobile.ui.common;

/** One canonical remote navigation model for TV pages, playback and Shorts. */
public final class MarsTvNavigationModel {
    private MarsTvNavigationModel() { }

    public static final class Entry {
        public final String label;
        public final int destination;
        private Entry(String label, int destination) {
            this.label = label;
            this.destination = destination;
        }
    }

    private static final Entry[] MAIN = {
            new Entry("Startseite", 0),     // Focusable M logo, not a decorative header.
            new Entry("Shorts", 8),
            new Entry("Suche", 1),
            new Entry("Abos", 2),
            new Entry("Verlauf", 3),
            new Entry("Profil", 6),
            new Entry("Einstellungen", 5)
    };

    public static Entry[] main() {
        return MAIN.clone();
    }

    public static int mainCount() {
        return MAIN.length;
    }

    /** Wrap both ways, never stop at the rail's top or bottom. */
    public static int wrap(int current, int step, int count) {
        if (count <= 0) return 0;
        return Math.floorMod(current + step, count);
    }

    /** Player has one contextual extra option only when Shorts are playing. */
    public static int playerCount(boolean shorts) {
        return MAIN.length + (shorts ? 1 : 0);
    }
}
