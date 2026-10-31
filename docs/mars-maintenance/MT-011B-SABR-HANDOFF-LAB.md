# MT-011B0 — SABR sample handoff investigation (not a preloader)

**Branch base:** MT-011A1 diagnostic branch, retaining warm V8 and TCL stage instrumentation.
**Purpose:** start a separately reviewed, fail-closed implementation track for genuine media-segment reuse across Shorts swipes.

## Confirmed architecture

MarsTube uses legacy `SimpleExoPlayer`, `SabrMediaSource` and `DefaultSabrChunkSource`; the custom SABR chunk loader makes stateful POST requests. Media3's DefaultPreloadManager cannot be transplanted directly. Caching by URL or extracting bytes from one request to feed an independent player without preserving the full signed request and session state is unsafe.

## Implemented in B0

Ported the pure Java `ShortsSamplePreloadGate` and its JUnit tests from PR #12 onto the MT-011A1 branch. This is a **design safety boundary**, not an active media loader. CI runs its tests and preserves Android debug/release checks.

The gate explicitly requires matching current/next non-live Shorts, buffered and ready foreground playback, stable quality, and *proven* source/track and SABR request handoff; otherwise it denies preloading. Caps are one target, 4 MiB, 4s lookahead, 15s deadline.

## Mandatory next implementation phase

1. Trace the current Shorts navigation and candidate selection, including when next video ID becomes known.
2. Prove an actual next-video `SabrMediaSource` preparation and ownership transfer (or player-instance swap) compatible with the old ExoPlayer.
3. Bound real network/sample memory and cancel immediately on swipe, quality change, playback buffering or lifecycle exit.
4. Prove the media bytes are actually adopted by foreground playback, not downloaded twice.
5. Keep feature OFF by default, add stage timing and `short_prefetch_hit` only when a verifiable handoff occurs.
6. Run real Thomson tests (20+ swipes, login/quality/HDR, V8 warm/cold, RAM, 403, rollback).

**Do not install or advertise B0 as the requested MT-011B preloader.** It currently does not load media bytes, so it cannot improve first frame time. B0 source changes are retained for future work; no app update is justified.
