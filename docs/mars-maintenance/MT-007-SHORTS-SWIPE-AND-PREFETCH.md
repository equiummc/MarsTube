# MT-007 – Shorts swipe, isolated navigation and first-frame latency

Status: **experimental branch, device acceptance NOT performed**.
Base: `feature/mt-005-adaptive-diagnostics-20261031`. No stable branch touched.

## Evidence from Thomson Android 14 (31 October 2026)

The existing `MarsTubePerf` log measured the following six sessions, all with
`SabrMediaSource`:

| Session | Format lookup | First frame |
| --- | ---: | ---: |
| 1 | 12,998 ms | 19,114 ms |
| 2 | 10,396 ms | 12,283 ms |
| 3 | 4,797 ms | 6,741 ms |
| 4 | 4,219 ms | 6,918 ms |
| 5 | 4,852 ms | 6,989 ms |
| 6 | 4,690 ms | 7,029 ms |

The dominant delay is within the asynchronous `getFormatInfoObserve` request.
This log **does not distinguish** HTTP time, retries and response parsing.

Source review of pinned `MediaServiceCore` (`40fec843`):
`youtubeapi/.../service/internal/FormatInfoWrapper.kt` serializes calls via
` synchronized(lock) ` and may try a secondary (Innertube or legacy) provider
if the first says the video is unplayable. It also maintains a one-entry
format cache. This is **a possible cause of serialized/long lookups, not a
proven cause of these particular measurements**. A canceled prefetch may still
be executing inside that lock: verify UP/BACK and mixed-navigation latency
against the baseline before accepting prefetch. The
ExoPlayer initial playback buffer is still **2,500 ms** in this branch;
the separate MT-006 1,000-ms experiment is deliberately NOT merged.

## User experience implemented

- While a Short is playing with no menu/overlay: **DOWN next Short**,
  **UP previous Short**, **OK play/pause**, **BACK existing exit/menu behavior**.
- Horizontal arrows no longer invoke "quick skip" for Shorts. Ordinary video
  key bindings retain their existing settings.
- Navigation is scoped to playable `isShorts` entries in the **current
  `VideoGroup`**. Generic video queue / suggestions are not used.
- A bounded 12-item in-memory history supports reverse navigation.
- At the end of the loaded row, request the next page via the existing
  `continueGroupObserve` mechanism. The resulting candidate is checked
  for `isShorts`; missing pages, errors and non-Short candidates do not
  start ordinary videos. Duplicate remote presses during paging are ignored.
- A brief translated remote-control hint appears once per app process.

## Preload experiment

After the current Short has loaded and the next Short is known, request **one**
next Short's format info in the background, using the existing YouTube service.
- At most one pending request and one ready value in memory.
- The next navigation either **HIT**s the cached result or **JOIN**s the same
  in-flight request, avoiding an unnecessary duplicate fetch.
- Ready results are usable for **less than 15 seconds**; old values are dropped.
- A failed preload falls back to the regular format lookup.
- Changing to a different video, leaving Shorts, or releasing the engine
  cancels preparation. No persistent URLs, ID logs, credentials or disk cache.
- This is **not video-segment prebuffering** and cannot guarantee 1–2-second
  starts. YouTube/SABR source preparation and decoder time still apply.
- Existing `MarsTubePerf` timing remains, with additional
  `MT-007 short_prefetch_(start|ready|hit|join|failed)` events that have no IDs.

## Tests

Automated unit-test cases added:
- Next/previous selection only from Shorts (including mixed video rows).
- Twenty Short-to-Short movements, 19 reverse movements, and row boundaries.
- Missing groups, duplicates, unplayable items.
- Remote direction only when the Shorts view is unobstructed; no intercept
  of LEFT/RIGHT/BACK/OK.
- Cache expiration, missing cache, monotonic-clock guard.

Required GitHub Actions job:
`:smarttubetv:assembleStstableDebug` plus
`:common:testStstableDebugUnitTest`.

Real-device acceptance **still required**, including:
1. 20 Shorts UP/DOWN on Thomson, with fast double-presses and row pagination.
2. Confirm no ordinary video is launched from the Shorts swipe path.
3. Repeat 3+ Shorts with and without prefetch; record `MarsTubePerf` format
   time and total time, as well as preload HIT/JOIN rate and startup stalls.
4. Offline/slow Wi-Fi, page-load error, first/last row item, exit/re-entry.
5. Ordinary videos, 1080p60, real HDR content, audio, subtitles and standby.
6. Memory pressure on 1.45-GB-RAM TV; compare first-frame median and rebuffering.
7. Verify that shortened URLs, token expiry and network errors recover through
   the ordinary source. No release without this check.

Known limitations: The source can only prepare the **next known** Short. If a
new page has to be downloaded before the next candidate exists, or the YouTube
format request itself takes 4–13 seconds, the first unprepared transition can
still take several seconds. Data on the actual root cause inside
`MediaServiceCore` / YouTube's requests is not yet available.

### Android log collection (Thomson only)

```powershell
$adb = "$env:USERPROFILE\Android-Tools\platform-tools\adb.exe"
$tv = "192.168.41.53:5555"
& $adb -s $tv logcat -d -v time |
    Select-String "MarsTubePerf|MT-007 short_prefetch"
```

Do not switch the system display resolution, force HDR, change the ExoPlayer
codec priority, uninstall MarsTube, or merge this branch into `mars/stable`
as part of MT-007.
