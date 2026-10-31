# MT-005 · Automatic stable video quality — experimental TV branch

Status: **experimental, opt-in, device validation pending.** Never promote
to the default TV release solely because CI passes.

## Intended user experience

One choice: **Automatic – best stable quality**. The player should pick
the highest device-supported format that is sustainable over the current
network. 4K is **preferred, not guaranteed**: display resolution alone
does not prove VP9/AV1 hardware decoding, 4K60 support, sufficient throughput,
or that the source even offers 4K.

The user should not need to understand AVC, VP9, AV1, fps or container
details. Manual quality selection must remain possible and authoritative.

## Existing SmartTube behavior found in source

- `PlayerData.getDefaultVideoFormat()` already uses
  `DeviceHelpers.isVP9ResolutionSupported(2160)` to choose an initial
  up-to-4K60 VP9 **preset**, and has a few device-specific overrides.
- Video selection in `RestoreTrackSelector` normally returns the
  SmartTube callback's **fixed track**, and its fallback explicitly
  passes `false` for adaptive selection. That is not full
  bandwidth-adaptive video quality, even if the UI labels a preset
  `Auto (max quality)`.
- PlaybackFragment already constructs the ExoPlayer selector with
  `new AdaptiveTrackSelection.Factory()`. We reuse this — do not
  replace the decoder, source factory or player engine.

## Experimental implementation

- Persist a separate `PlayerTweaksData` opt-in at appended preference
  index 62, default **false**. Old profiles and existing quality presets
  are not silently changed. Switching it on/off requests an app restart
  so existing fixed overrides are not mistaken for adaptive choices.
- Show the experimental option in the existing **Player Settings** menu
  in German and English. This is not yet a default for end users.
- When enabled and without a manual selection override,
  `RestoreTrackSelector.selectVideoTrack` delegates to ExoPlayer's
  built-in selection, using the *real* `enableAdaptiveTrackSelection`
  argument. This allows adaptive selection only when that source and
  track group support it; unsupported formats remain bounded by the
  renderer's actual capability flags.
- When disabled, preserve the original SmartTube callback and fixed
  video format behavior unchanged.
- A manual video-quality selection in the playback quality dialog
  disables the experimental mode, retaining manual control.
- We do **not** override HLS/DASH/SABR protocol handling, source-selection
  priorities, hardware codec declarations, buffer policies or device
  `minSdk`.
- A stream with a single video format, separated codec groups or no
  adaptive track support may remain fixed. Do not promise seamless
  in-place quality switches in every YouTube protocol.

## Device acceptance gate — not done yet

Test with **real** 1.5–2 GB RAM devices (Android 8+, Thomson Android 14,
Fire TV). Compare Auto OFF against ON, using the same streaming videos:

1. 4K30/4K60 (only where source and hardware support them),
   1080p60, ordinary SDR and HDR when supported.
2. 10 Mbit/s **measured available network throughput**, different
   from a displayed video bitrate. Test 5/10/20/40 Mbit/s conditions
   with a controlled bandwidth limit where available, including recovery.
3. Compare initial resolution, first-frame time, actual resolution
   changes, rebuffer time, stalls, frame drops and memory PSS.
4. Test live DASH/SABR/HLS, seek, audio, subtitle, HDR handling,
   SponsorBlock, standby/resume and manual override/restart.
5. Inspect selected codec and actual decoder support. Never force
   a track the renderer says is unsupported.
6. Roll back if any playback regression, repeated quality oscillation,
   live-transport failure or extra RAM pressure appears.

The player engine and network pipeline are intentionally untouched.
Some transport types may not supply multi-track ABR groups. First
confirm this limitation from real logs and playback.

## Upstream compatibility

The experimental hook changes only `RestoreTrackSelector`, a
`TrackSelectorManager` callback, one PlayerTweaksData field and
settings UI. These are upstream-tracked classes and therefore still
require comparison on every SmartTube merge. Do not sync or merge
the experiment into a stable branch until real-device acceptance.

## Current shipping state

- Built-in adaptive selection path: experimental branch implementation.
- Backend or Wi-Fi speed test: **not added**. ExoPlayer's estimated
  throughput is more meaningful than Wi-Fi link rate alone.
- UI default for all users: **not changed**.
- TV installation/release/update manifest: **not touched**.
- Device test and actual 4K behavior: **not yet verified**.
