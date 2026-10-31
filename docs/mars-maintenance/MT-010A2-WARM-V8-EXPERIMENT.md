# MT-010A2 — one-thread TCL V8 warm-session experiment

**Experimental, not production. Base:** `feature/mt-010a-tcl-decoder-cache-20261031`.

## Why
Thomson MT-009 measured `tcl_v8_exec_ms` ~890–901 ms for 3 videos and
`tcl_v8_init_ms` ~120 ms each. The upstream `TclChallengeProvider`
initializes + destroys V8 for every new solve. Its JS
`tcl.solver.js` recompiles the same preprocessed TCL player source with
`new Function(preprocessedPlayer)()` on each call, even with script cache hits.

## A2 behavior (lab only)
- **Dedicated HandlerThread** serializes all TCL V8 calls; the runtime is
  created, used, and released on the same Android worker thread. No cross-thread
  sharing of a J2V8 instance. Waiting Rx workers obtain their result using a
  bounded FutureTask.
- **Warm V8 context:** reused for up to 45 seconds of idle time; idle cleanup
  releases it on that same worker. Logs `MT-010A2 v8_session_warm` and
  `v8_idle_release_ms`.
- **Single compiled function:** the existing TCL JS module caches *one*
  compiled function keyed by the entire exact preprocessed player source.
  A changed player source invalidates it and compiles the new version.
  This should reduce parsing for consecutive videos with the same player.
- **A1 decoder cache** still precedes this. A1's exact input hits bypass
  the solver. New inputs execute the worker's cached compiled function.
- **No auth bypass**, no change to signed URL format, no player/client
  fallbacks, no new media fetches, no extra persistent user data.
- **Fail-closed:** any exception returns the upstream solver's normal
  unresolved result; existing playback error and fallback logic remains.
  Timeouts may leave the worker busy until it completes; don't enable this
  without device acceptance and abort on error regressions.

## Verification
- CI applies MT-008, MT-009, A1 and then A2 source overlays to the **pinned**
  upstream MediaServiceCore SHA `40fec843eb24bc264e9c8b429fcee25068e230e2`.
  All require an exact source anchor, so changed upstream code fails closed.
- Node test runs the *original upstream* vs the *modified runtime JS* for
  synthetic pure challenge functions, repeats and changes source revisions.
  This does not prove real YouTube signature equivalence.
- Compile both debug and release; run common JVM tests; check PowerShell
  installer syntax. An actual locally signed APK remains uninstalled.

## Thomson acceptance before merging
1. Compare MT-009 baseline, A1 only and A2 for 10 new Shorts each.
2. Record `MT-010A decoder_hits`, `MT-010A2 v8_session_warm`,
   `MT-009 tcl_v8_exec_ms`, format_ms and first_frame, median/p95.
3. Verify signed streams actually play (audio/video/HDR/subtitles), no 403,
   stale player revisions, failure/retry loops or hidden crashes.
4. Try changing account, pause/background, rapid 20 swipes and a 60-second
   idle gap. Ensure context expires, memory settles and no cross-thread
   J2V8 access exceptions.
5. Compare memory on Thomson (~1.45GB RAM) and roll back immediately on
   sign-in or buffer regressions.

Do **not** merge or install on hardware on CI alone. No measured speedup
has been established for A1 or A2 yet. B's actual SABR sample handoff
remains a distinct blocked design gate in PR #12.
