# MT-011B5 – bounded adjacent Shorts metadata warm cache

**Goal:** preserve the observed MT-011B4 speed while improving repeated forward/backward Shorts navigation on the 1.45 GiB Thomson TV.

**User observation:** MT-011B4 was the fastest tested build. No direct RAM-pressure proof and no verified SABR preloaded sample adoption were supplied. Do not infer free RAM from total RAM.

## Changes vs MT-011B4

- Increase the existing in-memory **format metadata** LRU from **3 to 8 video IDs**; TTL **15s to 60s**, but each reuse still requires `MediaItemFormatInfo.isCacheActual()` and a playable response. The in-flight speculative request still expires after 15s. No video payloads stored in this LRU.
- On completion of the existing one-next-short metadata prefetch, immediately keep its verified metadata in the LRU; then, after 1.5s and **only while the current Short is actively playing**, request format information for one additional known item (`next + 2`) from the **same Shorts group**. No pagination, mixed suggestions, or independent speculative video decoding.
- Run speculative metadata requests serially: second-next begins only after the first succeeds. Do not make the second request if already cached. Cancel speculative actions on foreground navigation, account change, engine release, and exit. Discard stale callbacks.
- Backward swipes reuse the formats of recently watched Shorts already cached by the normal `loadFormatInfo` path.
- Leave the SABR media-sample mechanism, player selection, source identity, error handler, signing, 4MiB media candidate budget, and 15s SABR candidate lifetime unchanged from MT-011B4.

## Actual limits

8 short-format metadata entries are not 8 buffered videos. The legacy `ConcatenatingMediaSource` is still limited to one speculative next video, which might not preload its bytes early. Increasing full-video caches is deliberately **not** done here because measured SABR sample adoption, duplicate POST avoidance, Java heap headroom, and foreground rebuffer behavior have not been verified on this device.

## Acceptance

CI checks native debug/release, full common tests, parse validation, and the existing protected release installer. Added LRU tests confirm eight-item retention, eviction and 60s expiration. These do not prove on-device improvement.

On the physical TV, A/B compare the same Shorts sequence with previous MT-011B4 (10 forward, 10 backward, 10 rapid changes), first-frame median/p95 and no extra 403s, out-of-memory, frozen frames or rebuffer. Look for `format_route=warm_cache` from playback diagnostics and `MT-011B5 second_short_metadata_ready=true`. If reduced latency is not measurable or foreground suffers, revert the B5 test branch and retain MT-011B4.

**Deployment:** only after all checks green. `scripts/mars/Install-MT011B5-Release.ps1` uses separate worktree, backup of previous APK, signer comparison and `adb install -r` without data clearing. The main application branch and existing settings are left intact.
