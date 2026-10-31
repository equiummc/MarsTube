# MT-011B7 — Shorts playback stability after 3–4 switches

**Device observation:** MT-011B6 runs, but shows recurring black video after about 3–4 Shorts; backing out and re-entering restores playback. User reports no dramatic performance improvement over earlier build. Do **not** treat MT-011B6 as stable even though CI is green.

## Most likely regression candidates (not yet confirmed by logcat)
- One-to-two expansion of same-player SABR queued `MediaSource` instances in MT-011B6, each with a signed POST session and separate 4 MiB / 15 s budgets; asynchronous period removal/adoption or budget expiry can affect foreground.
- Longer chain of speculative metadata requests (next 2/3/4) competing with on-demand foreground video metadata fetch.
- Legacy `ConcatenatingMediaSource` does **not** guarantee next-period samples are eagerly loaded. Source queued != decoded samples actually ready.

## Conservative fix
- Keep longer **format metadata** LRU (12 entries, 30 minutes *local* maximum, `isCacheActual()` checked on reuse) and known next 1/2/3/4 lookahead, all metadata requests strictly serialized. Never hold four decoders.
- Revert B6's second SABR video period, restore MT-011B5's one-candidate queue and `MAX_TARGETS=1`. Foreground can proceed without preloading when the speculative queue is not ready.
- On every user-selected foreground format request, cancel the far-ahead speculative request/timer immediately, including when the first-next format observable is still a valid inflight request. This removes avoidable single-service lock contention on rapid swipes.
- Keep experimental media preload opt-in OFF by default, keep former working settings, do not alter signatures, streaming engine, package ID, player buffer values or YouTube endpoints.
- CI asserts branch-specific overlay anchors, runs existing tests, builds debug/release. This proves compilation only, not physical device fix.

## On-device validation
1. Capture the failure in B6 if possible via `adb logcat`; look for `MarsTubePerf` `preload_`, `format_route`, SABR IOExceptions, `onPlayerError`, HTTP 403 and memory signals.
2. Install the new test APK only with protected installer and same signer / backup. Compare 20 sequential swipes, ten reverses, 3–4 rapid taps and long watch.
3. Check no blank screen, player frame timing, no extra errors and reasonable memory.
4. If B7 still fails, switch OFF **only** experimental `Short-Videodaten vorladen (Test)`, restart app and test once. If this removes the issue, isolate SABR queue rather than increasing caching again. If not, investigate foreground/error and metadata/service requests.
5. Retain MT-011B4 as user-tested speed baseline until actual stability is proven.

**Do not merge into master or publish without successful physical-TV acceptance.**
