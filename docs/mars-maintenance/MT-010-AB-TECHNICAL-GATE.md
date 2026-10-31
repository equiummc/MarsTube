# MT-010 A/B – split trials, no automatic production rollout

## Evidence and goals

Thomson MT-009 (16:20 local) showed for three consecutive videos:
- Signature transform: 1557 / 1495 / 1447 ms
- TCL JS V8 execute: 901 / 890 / 895 ms
- TCL V8 init: 119 / 125 / 119 ms
- TCL existing player-script cache: hit=true, lookup 190 / 129 / 111 ms
- First frame: 4161 / 5551 / 3105 ms

The TCL JS execution is expensive. Existing 'hit=true' means the preprocessed
player script was found, **not** that the output n challenge was memoized.
Player code and auth are unaffected by the proposed changes.

## A – experimental exact decoder-result memoization

Source overlay `scripts/mars/mt010a_decoder_cache.py`, applied in a separate
build checkout after MT-008/MT-009 probes to pinned MediaServiceCore.
Exact key = player URL + full original n parameter, in RAM only.
Only successful nonempty solver results stored, max 64, TTL 90 seconds,
monotonic clock. Known exact matches reused; misses still use the original
TCL/V8 solver, including original failure handling and player-specific script.
The native V8 lifetime and JS compilation are **not changed** in A1.
No tokens, decoded values, video IDs or keys appear in timing logs.
This is a **speculative optimization**. If successive videos use distinct
n parameters, cache hit rate may be zero, and cold-start speed will be unchanged.

Future A2 (separate review, not silently enabled): persistent single-thread V8
worker and compiled JS function reuse, only after output equivalence tests,
memory/idle eviction, account/player revision change and correct V8 thread ownership.
Never reuse mutable J2V8 instances across arbitrary Rx worker threads.

## B – verified feasibility boundary for first media segments

MarsTube uses the legacy `SimpleExoPlayer` and `SabrMediaSource`
(`exoplayer-amzn-2.10.6`), *not* Media3's `DefaultPreloadManager`.
`DefaultSabrChunkSource` constructs stateful POST requests whose payload,
SABR request number, signed URL, and backend stream context may change.
Therefore a generic cache prefetch using URL-only keys, a second network reader
or a Media3 preload plug-in cannot be assumed safe or reusable.

To count as B complete, a design must pass:
- Actual *decoded* next-video sample preload with reliable adoption by the
  **foreground** player (not merely format metadata or warm TCP sockets).
- One target, at most 4 MiB RAM, at most 4 seconds of sample lead,
  15-second fetch deadline, cancellation under foreground buffering/errors,
  account switching, manual selection, quality change or user swipe.
- Explicit key includes relevant SABR request semantics; no cross-session
  reuse of media or credentials. No bypass of auth, ads or age checks.
- Playback of a user-swiped next Short still works normally after a failed
  preload; no double playback and no extra foreground buffering.
- Unit, integration and real Thomson SABR acceptance before feature is enabled.
- Compare format_ms, player prepare and first_frame median/p95 with A only,
  B only, both on, both off. Do not claim speed based solely on CI.

**B cannot safely be declared implemented simply by copying MeeTube's
Media3-only classes.** The old player requires an audited bridge or migration.
This A branch deliberately does *not* issue speculative media POST requests.

## Release safety

Base MT-009 is unchanged. Neither branch is merged to stable. CI release APK
uses CI certificate; do not install that APK over locally signed app. Use
local release-mode build after signature verification and backed-up APK,
as with MT-009. No app-data removal or new package names.
