# MT-007 — TV Shorts swipe and startup performance

Status: experimental, code implemented on an isolated branch; production rollout
and Thomson device acceptance are **not** complete.

## Baseline (Thomson Android 14, 2026-10-31)

Six real `MarsTubePerf` sessions using `SabrMediaSource`:

| Session | FormatInfo (ms) | First frame since selected (ms) |
| --- | ---: | ---: |
| 1 | 12,998 | 19,114 |
| 2 | 10,396 | 12,283 |
| 3 | 4,797 | 6,741 |
| 4 | 4,219 | 6,918 |
| 5 | 4,852 | 6,989 |
| 6 | 4,690 | 7,029 |

Most observed delay occurs before player preparation, within the external
`MediaItemService.getFormatInfoObserve()` call. That elapsed time cannot
yet be split reliably into server latency, retries, transport or parsing from
these application-level events. Do not assume the 2,500-ms ExoPlayer startup
buffer is the primary cause, and do not change it in MT-007.

## Input behavior

With a Short playing and no open controls/dialog:

- D-pad DOWN: next playable Short from the current group.
- D-pad UP: previous playable Short from the current group.
- OK: pause/resume.
- D-pad LEFT/RIGHT: no Shorts quick-skip; existing seek handling remains.
- BACK: existing player navigation.
- A first-time (per player-controller instance) translated hint shows the keys.

The dedicated selection policy skips non-Short entries, upcoming content and
live entries. It never falls through to the mixed general playlist, queue or
recommendations if the Shorts group has no candidate. A single bounded
continuation may load more items from the **same Shorts group**. If it fails
or returns no suitable Short, playback stays on the current Short. Late
callbacks after navigation are ignored. A 300-ms guard suppresses key-repeat
storms. Menus and child mode remain authoritative.

## Prefetch behavior and constraints

- After current playback begins, wait 2 seconds, then prepare at most one
  **known next Short's FORMAT INFORMATION** (not its entire media payload).
- A shared Rx replay/refCount observable prevents duplicate in-flight
  requests if the user swipes while the request is running. A completed
  result can be reused from the one-item memory cache.
- Retain the result for up to 15 seconds, never persist URLs or video IDs,
  clear at use, upon engine release, on account change, or controller finish.
- If the next Short is unknown, continue without speculative requests. Do
  not prefetch arbitrary recommended videos or skip to non-Short content.
- Original SABR, DASH, codec, HDR, video buffer and MT-005 auto-quality paths
  remain unchanged. MT-006's reduced initial buffer is a separate A/B test.

Prefetch effectiveness is **not proven** by CI. An observed
`MT-007 format_prefetch_hit` is necessary but not sufficient: compare actual
first-frame times, stalls, rebuffer counts and RAM on the physical TV.

## Verification

CI: Android TV native `:smarttubetv:assembleStstableDebug` plus
`:common:testStstableDebugUnitTest`. JVM Shorts policy tests include next,
previous, 20 consecutive forward and reverse swipes, end-of-group, missing
current, invalid direction, nulls, non-Short candidates and unavailable
candidates. CI does not simulate a remote control or real SABR requests.

Physical TV acceptance (required before merge or APK distribution):

1. Run 20 sequential Shorts swipes DOWN; confirm every next item is a Short.
2. Swipe UP 10 times; verify previous Short selection.
3. Exercise mixed recommendation rows, group ending and empty groups.
4. Press keys rapidly and during a network-delayed group continuation.
5. Confirm OK/Back/overlay, normal video seeking, child mode and playback
   controls still work.
6. Compare `MarsTubePerf` first-frame timing and prefetch hit rate on the
   same Shorts with MT-005 baseline. Target 1–2 seconds for already-prepared
   Shorts if feasible; do not claim that threshold until measured.
7. Check seek, playback progress, SABR, audio, subtitle, 1080p/4K inputs,
   SDR/HDR and memory stability. On failure, do not release this experiment.

No modifications to the TV's WM resolution or the Mars TV launcher.
