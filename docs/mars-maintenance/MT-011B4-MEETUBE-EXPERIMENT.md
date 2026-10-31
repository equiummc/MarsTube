# MT-011B4 — MeeTube-inspired experimental Shorts preload guard

**Status:** Source implementation and automatic tests only. Real YouTube SABR sample reuse on Thomson is NOT yet proven; do not publish as a production player or report a time saving until A/B testing confirms it.

## Decision and source comparison (2026-10-31)

- [MeeTube](https://github.com/yashoncode/meetube), Android phone Media3, uses `Media3NextPreloader` and `DefaultPreloadManager`. Its local H.264/AAC hardware fixture proved allocator/sample reuse. Its online acceptance using normal YouTube DASH returned HTTP 403 and no successful ready/hit; media preload remains debug opt-in.
- [SmarterTube](https://github.com/CodeSculptor/SmarterTube) shares SmartTube core, but did not supply a better verified SABR media preload implementation.
- [Legacy ExoPlayer queue behavior](https://github.com/google/ExoPlayer/issues/5621): adding a second source does not guarantee immediate preload. Next-period buffering starts when the first source approaches its end; therefore a queue alone is NOT proof of a preload hit.
- [Media3's ownership rule](https://developer.android.com/media/media3/exoplayer/preloading-media/preloadmanager/concepts): the manager and player must share the builder/looper/allocator. A direct port to MarsTube's legacy ExoPlayer and custom stateful SABR POST stream is not compatible.

## What this branch implements

- Preserve ExoPlayer and the existing SABR session/player settings; no Media3 wholesale replacement, second decoder, new token flow, change to client identities, or requests outside the stock pipeline.
- Separate, clearly marked **experimental Short media preload** switch in Player settings, OFF by default. It does not change the existing MT-008 metadata-prefetch switch. Enabling media preload permits its necessary metadata warmup even when the MT-008 switch is off.
- Only one known next short: current player READY, PLAY, at least 3s buffered; no streaming live media or DASH-mixed candidates; one attempt per candidate per current player session.
- Reuse the same `SabrMediaSource` queued on the foreground `ConcatenatingMediaSource`, with 4MiB network read limit and 15s lifetime. A 500ms watcher aborts speculative work when foreground is no longer playing, buffers less than 1.5s, or re-enters BUFFERING.
- A queue containing no bytes (or under 64KiB) is **never adopted**. It is cancelled before the old player is reset, then the standard open path takes over. Eligible adoption must match ID, real downloaded bytes, exact queue, size and age. Successful video decoding is logged separately at first frame.
- New `MT-011B4` logs: `preload_candidate_queued`, `preload_candidate_bytes`, `preload_discard_bytes`, `preload_cancel_foreground_priority`, `preload_handoff_bytes`, `preload_handoff_first_frame`, `preload_handoff_failed`.
- The `ShortsMediaHandoffPolicy` pure-Java tests assert foreground priority, stale/mismatched candidates, bytes and budget boundaries.
- Protected Thomson installer `scripts/mars/Install-MT011B4-Release.ps1`: independent worktree, pinned MediaServiceCore, backs up installed APK, compares signer before replacement, does not clear app data.

## Known limitations / NOT tested

- This **does not create a Media3 preloader**. Legacy `ConcatenatingMediaSource` may not preload the next period soon enough during rapid manual Shorts swipes.
- 64KiB downloaded does not guarantee independently reusable *decoded samples*; only the subsequent `preload_handoff_first_frame=true` plus sustained playback is acceptance evidence, and must be correlated with zero duplicate SABR POST reads.
- A failed speculative read can still bubble up from the shared ExoPlayer queue; never enable this experiment by default until real-device error handling has been verified.
- Code and CI verification cannot replace real device or Internet video proof. Installed APK and source tree on Windows must be checked before any deployment.

## Device acceptance gate (Thomson first)

1. Use the provided installer only after CI green, retaining original APK. Confirm the new switch is OFF: repeat 10 typical videos; normal and Shorts playback must match prior behavior.
2. Switch it ON, restart once, play a reproducible sequence of Shorts and 20 forward/10 backward swipes.
3. Require `preload_candidate_queued`, nonzero `preload_handoff_bytes`, `preload_handoff_first_frame=true`, continuous playback progress, and no increased rebuffer/error count. Merely seeing a queued candidate is NOT success.
4. Log first-frame latency, 403 counts, quality/HDR, RAM before and after for ON/OFF; distinguish cold and warm TCL engine.
5. Check quality change, pause, seeks, unstable network, full buffer, video cancellation, account switch, repeated swipes, player exit, and reboot.
6. On any unexpected black-screen or source error, disable the experimental setting and restart MarsTube. Do not change the user's working YouTube source/short settings to compensate.
7. Only promote if real transfer + repeatable first-frame benefit and no foreground regression. Otherwise revert the experiment and consider a separate Media3 port with its own tests, never an unreviewed migration.
