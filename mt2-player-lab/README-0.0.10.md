# MarsTube 0.0.10 — TV player, settings and Shorts layout

Status: implementation and local build complete, device verification still open.

- Branch: `feature/mt2-010-player-settings-ui-20261104`
- Base: `11e0741a` (contains `MT2-010-ARBEITSAUFTRAG.md`)
- Work order: `mt2-player-lab/MT2-010-ARBEITSAUFTRAG.md`
- Overlay: `mt2-player-lab/patch_010_player_settings_ui.py`
- Pinned NewTube reference: `6f96a1b42bb9272935360d7e5aec34d35f2753f0`
- Local reference baseline after the 0.0.9 overlays: `1650bc7`

## Deliverable APK

| Item | Value |
| --- | --- |
| APK | `mt2-player-lab/artifacts/0.0.10/marstube-v0.0.10-firetv-player-settings-ui-debug.apk` |
| Size | `81,054,904` bytes (77.3 MiB) |
| SHA-256 | `cf390e4e117502f6af441e16e5a0a50f29db8c116698480d1b00d3aafc437eea` |
| Checksum file | `mt2-player-lab/artifacts/0.0.10/marstube-v0.0.10-firetv-player-settings-ui-debug.apk.sha256` |
| Package | `de.equiummc.marstube2.firetest010.debug` |
| Version | `versionCode 12010`, `versionName 0.0.10` |
| Label | `MarsTube 0.0.10` |
| SDK | minSdk 24, targetSdk 37, compileSdk 37 |
| ABIs | `arm64-v8a`, `armeabi-v7a` |
| Signature | V2, Android Debug certificate |

The APK is intentionally **not committed** (`*.apk` is ignored): it is a local
test artifact and the CI workflow publishes the same build as
`marstube-v0.0.10-firetv-player-settings-ui-debug`. The checksum is committed so
a downloaded or rebuilt APK can be compared.

## Compatibility with 0.0.9

- The application ID is new (`firetest010`), so 0.0.9
  (`de.equiummc.marstube2.firetest009.debug`) and its data stay installed.
- No uninstall, no in-place upgrade and no shared preferences file with 0.0.9.
- The TV preference store is `mars_tv_player_010`.
- Defaults preserve 0.0.9 behaviour: subtitles off, buffer Normal, home grid 4,
  quality-first start, Miniplayer enabled.
- Both APKs can be installed side by side. Installing the 0.0.10 APK does not
  touch the 0.0.9 package.

## Implemented changes

### TV player

- Dark-blue translucent controls, white/cyan vector glyphs, rounded focus
  treatment; no GPU blur and no heavy animation.
- Like/dislike on the left, large progress bar above the icon row.
- Resolution, codec and bitrate are shown only through the Info button; they are
  not permanently overlaid on the video.
- Info panel reports the protocol that is actually open, SABR fallback state and
  the current diagnostics.
- BACK closes Info first, then opens the left rail while video and audio keep
  playing. BACK on the rail closes it; a later BACK lets the player minimize.
  `MobilePlaybackActivity.handleBack()` forwards the same chain, so predictive
  back does not bypass it.
- OK short press toggles play/pause. OK long press starts a temporary 2× speed
  after 420 ms and restores the previous 1× state on release; it never enters a
  pause loop.

### Shorts

- D-pad up/down switches to previous/next Short while no control is focused;
  Media next/previous always switch; left/right still seek.
- Shorts grid uses three fully visible 9:16 cards on wide TVs and two on small
  viewports.
- TV Shorts cards use a portrait 9:16 override and high-resolution thumbnails
  (`oar2.jpg` with the original URL as fallback). No artificial blur-to-sharp
  upscale.
- The Shorts player keeps the present frame at 9:16, centred with side bars, and
  never uses `RESIZE_MODE_STRETCH`.

### Settings

TV root order:

1. `Video & Wiedergabe`
2. `Darstellung`
3. `Bedienung / Fernbedienung`
4. `Untertitel`
5. `Streaming & Netzwerk`
6. `Profil & Konto`
7. `Info`

Existing 0.0.9 pages (SponsorBlock, DeArrow, Backup, Advanced) are still linked
below the new sections, so no settings are lost.

- **Video & Wiedergabe:** max resolution (Auto/2160/1440/1080/720/480), max FPS
  (Auto/60/30), codec (Auto/H.264/VP9/AV1), HDR (Auto/off), bitrate limit
  (Auto/3/5/10/15/25 Mbit/s), buffer (small/Normal/large), start strategy
  (quality-first/fast-start). Every value is a preference or ceiling, not a
  guarantee that the stream offers that track.
- **Darstellung:** 4/6/9 home tiles, lower-carousel preview count 4/6/8/10,
  separate TV/phone layouts.
- **Bedienung / Fernbedienung:** documents D-pad, OK short/hold, BACK chain and
  the Miniplayer switch. Downloads are not offered on TV.
- **Untertitel:** off, forced translation subtitles only, or regular subtitles.
  Forced-only never falls back to arbitrary subtitles when no forced track
  exists. A manual non-default pick in the player still wins.
- **Streaming & Netzwerk:** SABR preference applies to the next video. Automatic
  fallback is allowed only after a real failure, and the actual protocol plus
  fallback state remain visible in the player Info panel. SABR is explicitly
  described as not being automatic data saving.
- **Profil & Konto:** profile grid, history preferably 3×3 on TV, subscriptions
  grid, and one-video-per-channel subscription filter.
- **Info:** version, diagnostics, GitHub link (`equiummc/MarsTube`), licenses and
  the explicit note that 0.0.10 is a separate test app.

### Browse, branding and phone behaviour

- Downloads are hidden from TV navigation, You routes and intents even if a
  legacy hidden menu stored a different state.
- The Miniplayer is hidden on TV when disabled; no permanently selectable empty
  function is left behind.
- The duplicate `MarsTube` wordmark in the TV top bar is hidden; the M logo stays.
- Phone portrait/landscape touch layouts and the shared player/settings logic
  remain separate; phone settings keep the legacy phone tree.

## Automated verification performed

All commands ran against the pinned reference checkout with the 0.0.9 overlays
applied in CI order, followed by `patch_010_player_settings_ui.py`.

| Check | Result |
| --- | --- |
| Overlay apply (fail-closed anchors) | PASS |
| `:sabr-media3:testDebugUnitTest` | 51 tests, 0 failures, 0 errors |
| `:smarttubetv:testStmobileDebugUnitTest` | 872 tests, 0 failures, 0 errors, 1 skipped |
| `:smarttubetv:assembleStmobileDebug` | PASS (`BUILD SUCCESSFUL`, 254 tasks) |
| APK package/version/label via `aapt` | PASS (`firetest010.debug`, `12010`, `0.0.10`, `MarsTube 0.0.10`) |
| APK signature via `apksigner` | PASS (V2 Android Debug) |
| Risky permissions | PASS (none of install-packages, delete-packages, overlay, record-audio) |
| Checksum | PASS (recorded above) |
| GitHub Actions | PASS, run [41927632833](https://github.com/equiummc/MarsTube/actions/runs/41927632833), 6m13s |
| CI artifact | `marstube-v0.0.10-firetv-player-settings-ui-debug`, 81,054,684 bytes, SHA-256 `4fc18f903d46136bb899ec436c7f4597549119233d4af85861c34a4ac62622f2` for that run |

Test report totals include the new `MarsTvPlayerPrefsTest` (8 Robolectric tests
covering defaults, start gate, buffer mapping, codec MIME preference, bitrate
units, home tile validation, Shorts span count and persistence).

The stale `StartupBandwidthMeterTest` expectation was updated from 4 Mbit/s to
10 Mbit/s because the 0.0.9 `patch_007_tv_quality.py` deliberately raised
`MAX_RECENT_SEED_BPS` to 10 Mbit/s.

## Device verification still open

No TV or phone was connected in this run, so the following are **not claimed as
passed** and must be checked on the real Thomson (~1.45 GB) and Fire TV (~2 GB)
devices:

- cold start time and memory pressure on both devices;
- normal video playback, D-pad jumps, OK hold 2× and safe restore to 1×;
- BACK at video, BACK with Info open and BACK inside dialogs;
- Shorts start/switch via D-pad and Media keys, and 9:16 rendering without
  stretching;
- bitrate ceilings and the actual selected track;
- SABR on/off across a video change, real-failure fallback and the protocol shown
  in Info;
- subtitle modes (off, forced-only with no forced track, regular);
- TV Downloads hidden and Miniplayer hidden when disabled;
- profile, history and subscriptions grids plus the channel filter;
- returning from every settings submenu without a focus trap.

CI run [41927632833](https://github.com/equiummc/MarsTube/actions/runs/41927632833)
passed and published the artifact named above. That CI APK has the same
package, version, label and ABI set as the local APK. Its checksum differs from
the local build because debug APKs carry build/signing metadata and the local
working tree uses CRLF; the committed overlay hash in the CI manifest
(`890ae9ca...`) matches the committed `patch_010_player_settings_ui.py`.

## Reproduce the build

```powershell
git clone https://github.com/aleixrodriala/newtube.git newtube-reference
git -C newtube-reference checkout 6f96a1b42bb9272935360d7e5aec34d35f2753f0
git -C newtube-reference submodule update --init --recursive

# Apply the 0.0.9 overlays in workflow order, then 0.0.10:
python mt2-player-lab/patch_tv_input.py newtube-reference
python mt2-player-lab/patch_sabr_recovery.py newtube-reference
python mt2-player-lab/patch_lab_identity.py newtube-reference
python mt2-player-lab/patch_tv_shell.py newtube-reference
python mt2-player-lab/patch_005_navigation.py newtube-reference
python mt2-player-lab/patch_005_localize.py newtube-reference
python mt2-player-lab/patch_005_mobile_de.py newtube-reference
python mt2-player-lab/patch_006_unified.py newtube-reference
python mt2-player-lab/patch_006_isolated_firetv.py newtube-reference
python mt2-player-lab/patch_007_tv_quality.py newtube-reference
python mt2-player-lab/patch_007_translate_common.py newtube-reference
python mt2-player-lab/patch_008_tv_player.py newtube-reference
python mt2-player-lab/patch_009_tv_shorts_ui.py newtube-reference
python mt2-player-lab/patch_010_player_settings_ui.py newtube-reference

cd newtube-reference
./gradlew :sabr-media3:testDebugUnitTest :smarttubetv:testStmobileDebugUnitTest :smarttubetv:assembleStmobileDebug
```

The debug APK is written to
`smarttubetv/build/outputs/apk/stmobile/debug/smarttubetv-stmobile-debug.apk`.

## Install next to 0.0.9

```powershell
adb install -r "mt2-player-lab\artifacts\0.0.10\marstube-v0.0.10-firetv-player-settings-ui-debug.apk"
```

`-r` reinstalls only this package. Do not use `adb uninstall` for the 0.0.9
package, and do not clear app data unless that is a deliberate test step.

## Known limitations

- Resolution, FPS, codec and bitrate settings are soft ceilings/preferences.
  Media3 may pick a lower track when the stream or decoder cannot provide the
  requested one.
- Media3 has no dedicated HDR constraint. HDR-off is best effort: the best SDR
  video track inside the same ceilings is pinned; an HDR-only source is left on
  Auto.
- SABR remains an opt-in transport. A fallback is visible in Info, but it is not
  a guarantee that SABR will work for every video or account.
- The APK is debug-signed and ARM-only. It is a test build, not a store release.
