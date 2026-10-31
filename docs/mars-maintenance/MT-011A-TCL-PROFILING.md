# MT-011A1 — TCL encode / worker wait profiling on Thomson

**Experimental diagnostic only** — based on MT-010A2 commit `18d4c43d97bd68627e013bbe5703d5fa288c08da`. Pinned MediaServiceCore SHA: `40fec843eb24bc264e9c8b429fcee25068e230e2`.

## Evidence and purpose

The real Thomson log of 2026-10-31 shows for warm sessions 16–18: `tcl_cache_lookup_ms=112–121`, then approximately 297–301ms until `tcl_v8_init_ms=0` (the latter measurement prints **after** init). `tcl_v8_exec_ms=316–356`, `transform_ms=742–775`. Session 19 (after expected 45s idle cleanup) has `v8_session_warm=false`, `tcl_v8_init_ms=123`, `tcl_v8_exec_ms=904`. The existing timings do **not** locate the ~300ms: it could be JSON serialization of cached preprocessed TCL JS, worker queueing, and/or other runtime setup.

Before touching the algorithm, add stage boundaries in `TclChallengeProvider.kt`:

| Log key (microseconds) | Meaning |
| --- | --- |
| `MT-011A tcl_request_build_us` | Build request map |
| `MT-011A tcl_json_encode_us` | Serialize exact request with Gson (includes cached player JavaScript) |
| `MT-011A tcl_stdin_build_us` | Concatenate JS invocation |
| `MT-011A tcl_worker_queue_us` | Enqueue FutureTask to worker start (includes dispatch) |
| `MT-011A tcl_worker_lock_us` | Acquire V8 guard (usually zero on single worker) |
| `MT-011A tcl_worker_setup_us` | Cancel old cleanup callback and read warm-state |

**No change** to signed n output, player revision matching, scripts, V8 timeout, cache sizes, authentication, codec, SABR, or idle release. Emits durations only (not player URLs, opaque n-params, tokens, or raw JS). `--check` requires each exact anchor and pinned submodule hash.

## Sequence

Build in an isolated worktree using `scripts/mars/Install-MT011A-Release.ps1` (after CI). MT-008, MT-009, MT-010A, MT-010A2 overlays apply in order, followed by MT-011A.

To compare on TV: clear logcat only, swipe 10 Shorts, run a video 60s, swipe once more, collect `MarsTubePerf` lines. Expect identical playback; no speed improvement claimed in a pure diagnostic build. Use the results to decide if a subsequent MT-011A2 source/marshalling cache is justified. Release only after verifying that any change preserves exact player-challenge correspondence and memory limits.

Do not merge into `mars/stable` or claim MT-011A performance gains without device validation.
