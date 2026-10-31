# MT-011B8 — Fast Shorts Metadata First, Foreground Safe

## Evidence (Thomson live log from 2026-10-31, local file MarsTube-Live-20261031-235324.txt)

- 41 format routes: **37 network**, **4 prefetch_inflight**, **0 warm_cache**, **0 prefetch_ready**.
- 39 successfully timed first frames: **median 2410 ms**, about **4028 ms p95** (observed order statistic).
- Median foreground format retrieval **~890 ms**, median player-prepared-to-ready **~1292 ms**. These costs are distinct and **cannot** both be eliminated by metadata caching.
- Existing next-Short job started **2,000 ms after onVideoLoaded**. Users commonly swiped before it could start or complete. No reproducible spinner failure captured in this log; do not infer that cache caused it.

## Changes

- Preserve the same installed MT-011B6 player / 12-metadata LRU / four-ahead scheduling / user-controlled two SABR media candidates (4MiB per candidate). Changes are isolated in the new test branch.
- Schedule next known Short's **format metadata** after **100 ms**, not 2,000 ms, following onVideoLoaded. Do not issue speculative request until foreground is actively playing; bounded retries after 200/350/650 ms, then give up.
- Enable metadata warmup when **either** original metadata-prefetch toggle or the separately opt-in experimental media-prefetch toggle is enabled. No forced enable for people who left both off.
- If current playback changes, stop queued requests and far-ahead work before the new on-demand format request. Preserve an already matching in-flight next-video request; do not discard its observable/stream just to restart it.
- Check originating and target IDs in asynchronous success callbacks; never reuse data for an unrelated swipe.
- On successful first next-format warmup, insert the verified item in the bounded cache and, only when user opted in to experimental SABR media preloading, offer it to the same-player media queue. Avoid duplicate code in the build overlay by keeping the first-next SABR offer in the checked-in controller.
- Trigger following metadata candidates after 300 ms, sequentially. No new decoders or extra simultaneous `/player` format calls.
- Log `MT-011B8 prefetch_scheduled_ms`, `prefetch_started`, `prefetch_wait_foreground_ms`, `prefetch_no_adjacent_short`, plus previous `format_route` and `first_frame` metrics. Instrumentation distinguishes unavailable adjacent Shorts from a timing miss.

## Acceptance and safety

1. CI: pinned V8 + SABR overlay check, new pure-Java prefetch-timing tests, common unit tests, Android debug and release, installer signer syntax. **CI green does not prove YouTube live sample reuse.**
2. Measure same 30+ Shorts navigation and A/B compare MT-011B6 to B8. Expect `format_route=warm_cache` or `prefetch_ready` to increase, not just `prefetch_inflight`.
3. Record median/p95 first-frame, buffer stalls, counts of spinner >15 s, actual SABR preload candidate bytes, 403, Java/native memory PSS. The user should have a visible speed gain and no new stalls before merge.
4. The old intermittent indefinite loading spinner existed **before** B6; diagnose independently. Avoid turning on a risky automatic watchdog until its failing phase is captured.
5. Keep B6 as device-working fallback. No changes to master or deployed TV here. Use `scripts/mars/Install-MT011B8-Release.ps1` only after full CI green; it backs up the APK and checks signer.

## Ceiling

Even with a 100%-effective metadata cache, the observed ~1.3 s player preparation/ready phase may remain. Reaching near-instant YouTube-like swipes would require actual sample/media handoff (and its live YouTube stream success), not merely adding cache entries.
