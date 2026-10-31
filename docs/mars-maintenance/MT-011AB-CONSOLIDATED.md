# MT-011AB — consolidated experiment: low-copy warm TCL + SABR preload gate

**Status: diagnostic/experimental only. This is NOT a completed SABR sample preloader.**

The 2026-10-31 Thomson data shows ~268ms spent re-encoding a cached preprocessed TCL player JS source into Gson JSON plus ~46ms building a large invocation string. The V8 worker queue/lock times are negligible. The existing MT-011B0 branch contains a fail-closed admission gate but does not fetch or transfer samples.

## Consolidated code

- Keep the previously tested MT-011A worker and TCL diagnostic overlays intact.
- MT-011AB adds a challenge-only invocation with fresh, properly encoded challenges on the dedicated V8 worker; it sends no cached player JS when the **entire exact** preprocessed source equals the source most recently accepted by the same live V8 instance. A changed source, cold runtime, or failed solver response uses the original full request. On idle release, clear worker-owned source identity.
- The JS handler rejects a challenge-only invocation unless a compiled function already exists. There is no direct JNI/HTTP/codec/auth modification.
- The original MT-010B preload safety gate and JUnit tests remain present. No speculative SABR networking is enabled; the guard must never report a media preload hit without media-sample adoption.

## Verification boundaries

CI: exact pinned submodule and source anchors, Node challenge-only/full-solver equivalence including source rotations, Android debug/release and unit tests. Device validation still needed: repeated/rotated signed YouTube n challenges, Shorts 20 rapid swipes, 45s idle, memory and 403 status.

If any player script identity is ambiguous, the new code falls back to the complete invocation. No speedup should be claimed until device timing.

**Open preloader blocker:** `VideoLoaderController` currently only prefetches format metadata, and `ExoPlayerController.openMediaSource()` prepares the single foreground `SimpleExoPlayer`. SABR chunk fetches depend on POST request state/counters and track choices. To deliver actual preloaded media samples to the foreground player requires a tested period/queue-ownership bridge or safe dual-instance handoff with bounded allocator/decoder use. A blind cache, second decoder, or generic Media3 preloader is unacceptable on the low-memory Thomson. Do not install as "full MT-011B" until media sample reuse is demonstrated.
