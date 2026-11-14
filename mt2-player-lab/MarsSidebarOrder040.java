package com.liskovsoft.smartyoutubetv2.common.app.presenters.service;

import java.util.Collections;
import java.util.List;
import java.util.function.ToIntFunction;

public final class MarsSidebarOrder040 {
    private MarsSidebarOrder040() { }
    /** Migrate the reported legacy slots only; retain every custom entry and boot section. */
    public static <T> boolean migrate(List<T> items, ToIntFunction<T> id, int home, int shorts) {
        if (items.size() > 4 && items.get(3) != null && items.get(4) != null
                && id.applyAsInt(items.get(3)) == home && id.applyAsInt(items.get(4)) == shorts) {
            Collections.swap(items,3,4);
            return true;
        }
        return false;
    }
}
