# MT-008 — Format loading: foreground priority and timing diagnostics

**Status:** experiment, not production. Branch:
`feature/mt-008-format-priority-diagnostics-20261031`.
Base: `feature/mt-007-shorts-swipe-startup-20261031`.
Do **not** merge into `mars/stable` or replace a user APK without TV validation.

## Evidence

On the same Thomson Android 14 TV the official YouTube client starts Shorts
nearly instantly, while MT-007 still takes more than five seconds even though
vertical Shorts navigation now works and normal videos no longer appear by mistake.

Previously observed MarsTube sessions (SabrMediaSource): format-info resolution
12,998 / 10,396 / 4,797 / 4,219 / 4,852 / 4,690 ms; total to first frame
19,114 / 12,283 / 6,741 / 6,918 / 6,989 / 7,029 ms.

Pinned `MediaServiceCore` SHA: `40fec843eb24bc264e9c8b429fcee25068e230e2`.
The upstream `FormatInfoWrapper.getFormatInfo` serializes format fetches under
one lock; one-entry upstream cache, possible fallback client requests, auth
refresh and WebView-based PoToken generation also exist. Their **actual**
relative contributions on Thomson are NOT yet proven.

## Isolated MT-008 change

- **Foreground priority:** speculative next-Short format prefetch is now OFF
  by default. It can contend with the single upstream format lock; user requests
  do not need to wait for newly scheduled speculation. Nothing about token/auth
  generation or client ordering was bypassed or changed.
- **A/B setting:** player settings, MarsTube experimental quality section:
  `Prefetch next Short (experimental)` (German localized). Turn ON to compare
  with MT-007-style prefetch; OFF prioritizes explicit playback.
- **Recent-Short warm cache:** max 3 format entries for 15s in RAM, validated
  with `isCacheActual()` and `!isUnplayable()` on EVERY reuse; no disk writes,
  IDs or URLs in logs. Clear at account switch, engine release, finish, and on
  transition out of Shorts. Expired invalid entries removed. This can shorten
  backward/repeated Shorts but cannot eliminate the cold YouTube fetch.
- **Old request cancellation:** abort a speculative request targeting a
  different Short where Rx permits; reject a late result for a no-longer-selected
  Short. Do not assume an already-running upstream synchronous fetch is
  interrupted by Rx disposal.

## CI-only diagnostic probe

`scripts/mars/mt008_probe_media_core.py` checks the **exact pinned gitlink**
and instruments the checked-out submodule's working sources at CI build time.
It is **not a fork, merge, or new submodule commit** and does not modify
`.gitmodules`. It logs, under `MarsTubePerf`:

- `format_route=network|warm_cache|prefetch_inflight|prefetch_ready`
- `format_ms`, `first_frame total_ms` (MT-005 metrics kept)
- `MT-008 upstream lock_wait_ms` and `lock_held_ms`
- `MT-008 upstream primary_ms`, `fallback_ms` (-1 means unused)
- `MT-008 upstream auth_ms`, `potoken_ms` (required flag only)
- `MT-008 api client_attempt_ms`, `playable_ms`, `fixes_ms`,
  `transform_ms` (latency proxy including Retrofit transport/response parsing)

The inner network/JSON parsing split is **not yet separately observable**;
`client_attempt_ms` includes those phases together. Never claim it measures
network-only latency. The probe logs no video ID, account, query, URL, token,
headers, PoToken contents, or cookies. No auth bypass or codec adjustment.

The CI workflow applies the probe **only to MT-008 experimental builds**
after validating submodule identity. Ordinary builds without the probe retain
the same functional behavior, but do not contain these upstream phase logs.
Any future upstream submodule revision intentionally fails the probe and needs
manual review before building the experiment.

## Unit test / device acceptance gate

- CI: `:smarttubetv:assembleStstableDebug`,
  `:common:testStstableDebugUnitTest`, fork verification, pinned module check.
- JVM cache tests: lifetime boundary, LRU eviction, explicit removal, clear,
  nulls, clock rollback, invalid capacity/TTL. Existing Shorts swipe tests.
- Thomson TV: A/B prefetch OFF vs ON, 10 normal videos and at least
  20 next/previous Shorts, same account/network, cold and warm starts.
- Compare median AND p95 for `format_ms` and first-frame latency, and check
  `lock_wait_ms`, client-attempt count, HLS/translation timing and PoToken
  cost. Also watch rebuffer, crashes, memory, auth, captions and HDR/SDR.
- If no substantial real speed improvement or regressions occur, leave the
  speculative prefetch OFF and investigate the measured backend subphase.
- One user-controlled rollback: original MT-007 APK from the previously
  tested/debug-signed build, no `adb uninstall` or clearing app data.

**No verified TV performance improvement yet.** The unchanged upstream lock
still fundamentally limits concurrent cold-format requests.
