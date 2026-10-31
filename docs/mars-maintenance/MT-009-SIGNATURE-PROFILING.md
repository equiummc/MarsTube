# MT-009 – Identify the ~1.5-second format-transform bottleneck

**State:** diagnostic experiment, no performance fix claimed.
**Branch:** `feature/mt-009-signature-stage-diagnostics-20261031`
**Base:** `feature/mt-008-format-priority-diagnostics-20261031`.
Do not merge into `mars/stable` or change installed MT-008 until user approves testing.

## Evidence from Thomson (MT-008, 2026-10-31)

| Session | YouTube client ms | transformFormats ms | Format total ms | To first frame ms |
| --- | ---: | ---: | ---: | ---: |
| 20 | 665 | 1499 | 2199 | 3427 |
| 21 | 632 | 1644 | 2539 | 3915 |
| 22 | 432 | 1447 | 2127 | 3101 |

In these sessions `lock_wait_ms=0`, `potoken_ms=0`,
`auth_ms=0` and `fallback_ms=-1`. The client request is fast relative
to `transformFormats`; do not change network engines, HDR/codec selection,
quality presets or playback buffer to explain those 1.5 seconds.

The pinned submodule is `MediaServiceCore@40fec843eb24bc264e9c8b429fcee25068e230e2`.
`VideoInfoServiceBase.transformFormats()` invokes `decipherFormats()`, which
calls `AppService.bulkSigExtract()` (n-parameter/signature processing).
The TV client may select `TclPlayerDataExtractor` and
`TclChallengeProvider`, which create a V8 runtime, load the library/script,
execute the challenge, and release V8 per call. Another code path uses
`V8ChallengeProvider`. **This structure is a hypothesis, not proof
that V8 setup explains the measured delay.**

## New instrumented timing stages

CI and the local MT-009 test build first apply the unchanged MT-008
probe to the pinned submodule. Then `scripts/mars/mt009_probe_signature_stages.py`
adds **timing-only** stages without changing auth, token generation,
video URLs, signature/n results, JS engine state, locks, fallback, or playback:

- `MT-009 decipher_bulk_ms`, `format_entries`: bulk signature processing;
  `MT-009 decipher_token_apply_ms`: residual token application.
- `MT-009 extractor_lookup_ms`: cached extractor lookup and possible validation.
- `MT-009 tcl_player_code_ms`, `tcl_solve_ms`: TCL JS source load/solve.
- `MT-009 tcl_cache_lookup_ms`, `hit`: preprocessed solver cache.
- `MT-009 tcl_v8_native_create_ms`, `tcl_v8_load_script_ms`,
  `tcl_v8_boot_script_ms`, `tcl_v8_exec_ms`,
  `tcl_v8_shutdown_ms` and total `tcl_v8_init_ms`.
- If the regular web solver is selected: `MT-009 web_bulk_solve_ms`,
  `web_v8_native_create_ms`, `web_v8_load_script_ms`,
  `web_v8_boot_script_ms`, `web_v8_exec_ms`,
  `web_v8_shutdown_ms` and total `web_v8_init_ms`.
- `MT-009 tcl_json_ms` for solver output JSON decoding.

No video IDs, channel/user names, full URLs, auth headers, cookies, signature
values, player code or token values appear in any new MT-009 logs.

**Interpretation:** Stage times are *nested*; don't add the total
`decipher_bulk_ms` to its child measurements. Null/missing stages indicate
a different execution path, NOT an error. A V8 stage missing from a video
does not mean JavaScript was bypassed; inspect the extractor trace.

## Build & safety

The build is a signed **release-mode** Android build with diagnostics.
In the temporary working checkout only, run:

```powershell
python scripts/mars/mt008_probe_media_core.py
python scripts/mars/mt009_probe_signature_stages.py --check
python scripts/mars/mt009_probe_signature_stages.py
.\gradlew.bat --no-daemon :smarttubetv:assembleStstableRelease
```

Never commit the patched MediaServiceCore submodule content. Preserve
`.gitmodules` and gitlink SHA. Match signing certificate with the installed
APK. Do NOT uninstall MarsTube, change the package ID, or wipe user data.

CI verifies pinned-submodule anchors, Android-TV debug build, JVM unit tests,
and release-mode build. Actual hardware timing is unavailable in CI.

## Hardware test

1. Same Thomson, network, app account and video-quality preset as MT-008.
2. Play **at least five new Shorts** and **three normal videos**. Include
   immediate next/previous swaps to test cached and cold paths.
3. Capture logs: `adb -s 192.168.41.53:5555 logcat -d -t 30000 -v time -s "MarsTubePerf:I" "*:S"`.
4. Compare `MT-008 api transform_ms` to `MT-009 decipher_bulk_ms`,
   extractor lookup and the applicable Tcl/web V8 steps. Compute typical
   duration and at least worst-case values by video start.
5. Verify 20× DOWN / 10× UP Shorts, no normal video in Shorts, audio,
   subtitles, auth, SABR/HDR, no crashes, memory stability.
6. Only **after** the trace: propose a separate safe performance patch,
   with measured baseline and rollback. Reusing V8 across background
   threads is risky (same-thread / V8 Locker constraints); do not enable
   as a speculative tweak.

If the diagnostic release itself regresses playback, reinstall the
saved MT-008 APK with a matching signature. Never use `adb uninstall`.
