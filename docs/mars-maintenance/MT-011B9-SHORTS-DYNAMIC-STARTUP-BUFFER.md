# MT-011B9 — Shorts-only dynamic fast start (experimental)

## Baseline
- MT-011B8: CI success 38 checks; no physical-TV acceptance yet.
- Thomson MT-011B6 live capture: median first frame 2.41s (39 completed), p95 ~4.03s; median format stage ~0.89s and player-ready stage ~1.3s. Prior spinner error was not reproduced.
- ExoPlayer source is the pinned vendored legacy `exoplayer-amzn-2.10.6`. In `ExoPlayerInitializer.createLoadControl`, its normal initial buffer threshold is **2500ms**, and rebuffer is **5000ms**.
- SABR queue currently offers up to two queued sources but an ExoPlayer `ConcatenatingMediaSource` does not guarantee eager sample download. A queued source alone is NOT a sample preload hit. Never claim near-instant/handoff success without on-device bytes+first frame.

## This revision
- Add `ShortsStartupBufferPolicy` for a **1000ms buffered media** initial playback threshold at normal playback speed. Playback speed is accounted for. Do not alter allocation, `minBufferMs`, `maxBufferMs`, steady-state loading, full-size videos, live streams, or rebuffer threshold.
- Add `ShortsAwareLoadControl`, a wrapper delegating every existing legacy `LoadControl` method to the original `DefaultLoadControl` except a Shorts-only OR fast-start condition in `shouldStartPlayback`.
- Bind shorts mode in `PlaybackFragment.setVideo`, before the next media source is prepared. The existing single `SimpleExoPlayer` and all SABR logic remain unchanged.
- If any early-started Short re-buffers within eight seconds, **disable fast start for the remainder of that player session**. This is protective, not a cure for existing sporadic loading stalls.
- Log `MT-011B9 shorts_fast_start buffered_ms=...` and `shorts_fast_start_auto_disabled=true`. Do not log signed playback URLs.
- Add pure-Java boundary tests in `ShortsStartupBufferPolicyTest`. No Codecs, cache limits, stream signatures or package/branding modifications.

## Acceptance
1. GitHub CI unit tests + Android debug/release APK + signer/safety checks all green.
2. Compare same 30+ Shorts and device conditions against B6 and B8 using `MarsTubePerf session first_frame`, routes, and `shorts_fast_start` labels. A fast-start log is not necessarily a reduction in first-frame time.
3. Monitor for player rebuffer, audio/video desync, repeated spinner, memory pressure, and startup latency. Confirm the fallback works after an early-start rebuffer.
4. Verify normal 4K videos, DASH, live, and all existing player modes unchanged.
5. If median/p95 do not improve measurably or rebuffer increases, revert the dynamic load-control change. No automatic release or production merge.

## Important limitation
The existing queue's actual downloaded SABR sample bytes remain unproven on this Thomson. B9 is a **bounded initial-buffer optimization**, not a claim of an implemented zero-copy pre-decoded sample handoff. Additional high-risk media-period architecture changes are deliberately postponed pending measurements.

## Device state
No APK installed, no device or app data modified remotely. Keep B6 as fallback. Installer `scripts/mars/Install-MT011B9-Release.ps1` uses backup/signature checks in a separate worktree.
