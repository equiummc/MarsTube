# MT-010B – Safety gate before any real SABR Shorts media preloading

**Stage:** an independently testable admission gate; **NOT** an enabled sample preloader.
Base is experimental MT-010A. No production code uses the gate yet.
This is intentional because no safe sample reuse path has been established in
legacy ExoPlayer's custom SABR implementation.

## Source inspection

MarsTube plays most examined Shorts using `SabrMediaSource`, with legacy
`SimpleExoPlayer` (not Media3). The real chunkloader is at
`exoplayer-amzn-2.10.6/library/sabr/src/main/java/com/google/android/exoplayer2/source/sabr/DefaultSabrChunkSource.java`.

SABR initialization and media segments use **HTTP POST** DataSpec requests
with `manifest.createVideoPlaybackAbrRequest(...)`, session/request counters,
per-track details and signed backend URLs. Blind URL-based caching, a second
HTTP reader, or copying MeeTube's Media3 DefaultPreloadManager may cause
wrong request-body/response matches, stale signed sessions, error loops, or
additional resource contention. No unsafe request caching is shipped.

## Implemented admission contract

`ShortsSamplePreloadGate` is a pure Java preflight that returns a typed
reason rather than opening a connection. All must hold before a *future*
sample manager can be activated:

- Opt-in enabled; current/next are known non-live Shorts
- Foreground player READY and sufficiently buffered
- Quality/track selection stable and matching
- Proven media source/sample ownership transfer into foreground player
- For SABR specifically, verified stateful request identity + handoff
- Hard design limits: one candidate, 4 MiB media memory,
  4 seconds sample head start, 15 second deadline

The default and every unsupported source fail closed. The source-read /
queued-sample transfer backend is **not connected**, so this branch alone
cannot speed up Shorts playback. Unit tests check the fail-closed decisions
and budget constants.

## What remains for genuine B

1. Implement or adopt a source-ownership/transfer bridge for
   `SabrMediaSource` + `SimpleExoPlayer`. Do not share a `MediaSource`
   concurrently between active and speculative owners without proven support.
2. Validate correct SABR session/request-body identities, signed URL expiry,
   auth/account changes, per-track selection and cancellation.
3. Bound actual allocator memory and network work by the gate limits.
   Pausing foreground buffering must cancel speculation immediately.
4. Exercise 20 Shorts, partial-ready swipes, login/quality switches and errors
   on Thomson; verify no extra freezes/rebuffers.
5. Compare A only, B only, A+B and baseline; preserve a signed rollback APK.
6. Enable for users only after confirmed real sample handoff and TTFF benefit.

**No claims of sample preloading, first-frame improvement, or production
readiness are made for this gate alone.**
