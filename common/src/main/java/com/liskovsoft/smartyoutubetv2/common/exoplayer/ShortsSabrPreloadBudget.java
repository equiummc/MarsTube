package com.liskovsoft.smartyoutubetv2.common.exoplayer;

import android.os.Handler;
import android.os.Looper;

import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.TransferListener;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Single-candidate SABR preload budget. Counts bytes downloaded by the queued
 * MediaSource's original DataSources; never copies/replays signed POST responses.
 * On adoption, the very same source/period continues with its original SABR
 * state and the limit is removed. Failure cancels ONLY speculative loading.
 */
public final class ShortsSabrPreloadBudget {
    public static final long MAX_BYTES = 4L * 1024 * 1024;
    public static final long MAX_AGE_MS = 15_000L;

    private final AtomicLong downloaded = new AtomicLong();
    private final Object reservationLock = new Object();
    private long reservedBytes;
    private final AtomicBoolean promoted = new AtomicBoolean();
    private final AtomicBoolean cancelled = new AtomicBoolean();
    private final AtomicBoolean notified = new AtomicBoolean();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Runnable onLimit;

    public ShortsSabrPreloadBudget(Runnable onLimit) {
        this.onLimit = onLimit;
    }

    public long bytes() {
        return downloaded.get();
    }

    public void promote() {
        promoted.set(true);
    }

    public void cancel() {
        cancelled.set(true);
    }

    private void checkAllowed() throws IOException {
        if (promoted.get()) return;
        if (cancelled.get() || downloaded.get() >= MAX_BYTES) {
            if (notified.compareAndSet(false, true)) main.post(onLimit);
            throw new IOException("MT-011B speculative SABR preload budget exhausted");
        }
    }

    public DataSource.Factory wrap(DataSource.Factory original) {
        return () -> new BudgetedDataSource(original.createDataSource());
    }

    private final class BudgetedDataSource implements DataSource {
        private final DataSource delegate;

        BudgetedDataSource(DataSource delegate) {
            this.delegate = delegate;
        }

        @Override
        public void addTransferListener(TransferListener transferListener) {
            delegate.addTransferListener(transferListener);
        }

        @Override
        public long open(DataSpec dataSpec) throws IOException {
            checkAllowed();
            return delegate.open(dataSpec);
        }

        @Override
        public int read(byte[] buffer, int offset, int readLength) throws IOException {
            checkAllowed();
            if (readLength == 0) return 0;
            int requested = readLength;
            boolean limited = !promoted.get();
            if (limited) {
                // Multiple SABR audio/video tracks may read concurrently. Reserve
                // before the I/O to make the shared 4MiB ceiling deterministic.
                synchronized (reservationLock) {
                    long remaining = MAX_BYTES - downloaded.get() - reservedBytes;
                    requested = (int) Math.min(requested, Math.max(0, remaining));
                    if (requested > 0) reservedBytes += requested;
                }
                if (requested == 0) {
                    if (notified.compareAndSet(false, true)) main.post(onLimit);
                    throw new IOException("MT-011B speculative byte cap reached");
                }
            }
            int count = 0;
            try {
                count = delegate.read(buffer, offset, requested);
                return count;
            } finally {
                if (limited) {
                    synchronized (reservationLock) {
                        reservedBytes -= requested;
                        if (count > 0) downloaded.addAndGet(count);
                    }
                }
            }
        }

        @Override
        public android.net.Uri getUri() {
            return delegate.getUri();
        }

        @Override
        public Map<String, List<String>> getResponseHeaders() {
            return delegate.getResponseHeaders();
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }
    }
}
