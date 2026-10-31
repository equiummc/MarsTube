package com.liskovsoft.smartyoutubetv2.common.app.models.playback.controllers;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Small, temporary playback-format cache for recently watched Shorts.
 * Nothing is written to disk. The caller must additionally validate
 * MediaItemFormatInfo.isCacheActual() when reusing signed stream metadata.
 */
public final class ShortsFormatWarmCache<T> {
    private final long ttlMs;
    private final int capacity;
    private final LinkedHashMap<String, Entry<T>> values =
            new LinkedHashMap<String, Entry<T>>(4, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Entry<T>> eldest) {
                    return size() > capacity;
                }
            };

    private static final class Entry<T> {
        final T value;
        final long atMs;
        Entry(T value, long atMs) {
            this.value = value;
            this.atMs = atMs;
        }
    }

    public ShortsFormatWarmCache(int capacity, long ttlMs) {
        if (capacity < 1 || ttlMs < 1) throw new IllegalArgumentException("invalid cache bounds");
        this.capacity = capacity;
        this.ttlMs = ttlMs;
    }

    public synchronized void put(String id, T value, long nowMs) {
        if (id != null && !id.isEmpty() && value != null) {
            values.put(id, new Entry<>(value, nowMs));
        }
    }

    public synchronized T get(String id, long nowMs) {
        if (id == null) return null;
        Entry<T> entry = values.get(id);
        if (entry == null) return null;
        if (nowMs < entry.atMs || nowMs - entry.atMs >= ttlMs) {
            values.remove(id);
            return null;
        }
        return entry.value;
    }

    public synchronized void remove(String id) {
        if (id != null) values.remove(id);
    }

    public synchronized void clear() {
        values.clear();
    }

    public synchronized int size() {
        return values.size();
    }
}
