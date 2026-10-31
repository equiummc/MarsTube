# MT-011B6 — Four Shorts ahead, two bounded SABR media candidates

**Status:** Experimental branch. The user reported MT-011B4 as the fastest working build; leave it intact until MT-011B6 has proved real on-device benefit.

## What is actually cached

- **Up to 12 recently requested format-information entries**, up from 8, in process memory. The local age limit is **30 minutes** rather than 60 seconds. The YouTube format's independent `isCacheActual()` validity check remains mandatory on every reuse. An expired server signature is never resurrected by the longer local lifetime; the ordinary network route refreshes it.
- **Up to 4 known forward Shorts** can receive metadata preparation. One item is fetched first; the next three are requested sequentially, only when the current player is still playing, without unknown feed pagination or concurrent speculative `/player` requests. Older viewed Shorts retain entries for backtracking until LRU/expiry.
- **Two SABR candidate media sources**, each limited to **4 MiB actual transferred bytes**, with separate 15s expiry. Maximum speculative media allowance is 8 MiB, plus player buffers. They share the existing ExoPlayer queue — no second decoder or new streaming identity.
- During a successful first-candidate handoff, its second candidate remains queued and becomes the next available candidate. Foreground errors or low buffer cancel both. A queued source that has not actually transferred 64KiB cannot be adopted.
- Both candidate sources are still subject to legacy ExoPlayer's preparation/buffering behavior. Queuing a second source **does not prove it has fetched any media**. Success is only `preload_handoff_bytes > 0`, `preload_handoff_first_frame=true`, and continuous playback, with no duplicate or failed SABR requests. No performance claims solely from compiler results.

## Limits and caveats

- 4 MiB is a **cap per source**, not a guarantee that the first four seconds or a full video fit. Bitrate/resolution and segment size vary.
- The 30 minute metadata window is not a guarantee of 30 minutes' signature validity. Watching for hours is supported through normal format refresh if an entry is stale.
- The 15 second **media** candidate expiry is separate from the 30 minute **metadata** TTL, because stateful SABR POST sessions should not be held for hours without real-device evidence.
- Do not merge or publish as default until Thomson A/B checks show real media bytes for the second candidate, reasonable memory headroom, stable 403 rate, safe bidirectional swipes and first-frame times. If the legacy queue produces no second-candidate reads, explicitly report that and test a separate Media3-style loader architecture rather than pretending that the queue is sufficient.
- The experiment remains user opt-in and automatically disabled in-session after related errors. App package ID, current decoder/engine choice, device launcher, account, and preference storage are unchanged.

## Acceptance

1. Preserve the previous installed APK and existing settings; run CI debug + unit + release and protected installer syntax first.
2. Run 20 Shorts forward, 10 reverse, several very rapid double/swipe sequences, and a 2-minute dwell then swipe.
3. Inspect `MT-011B6 lookahead_ready_distance=2/3/4` and `preload_candidate_queued_slot=1/2`. Confirm media-transfer counters and decoded first frame; zero bytes means no actual preload.
4. Record process PSS, Java heap, available memory, buffer stalls, errors, median/p95 first-frame delay and exact 403 incidence compared with the same MT-011B4 sequence. If memory pressure or errors worsen, disable the experiment immediately.
5. Observe repeated pause/seek, title/quality changes, account switches and app exit. Do not change YouTube client or signed URL handling to mask stream failures.

**Code:** `feature/mt-011b6-four-ahead-two-media-20261031`.
