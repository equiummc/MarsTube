# MarsTube · Verbindliche Android-TV-Technik- und Kompatibilitätsregeln

Status: **Development policy accepted 2026-10-31**. This policy applies to
MarsTube's own Android TV flavor (`ststable`) and is **not** a public
release or a claim of device testing.

## 1. Operating-system support

- **Android 8.0 (API 26) and later** is the minimum **actively supported
  and regularly tested** Android / Fire OS Android-compatible runtime.
  Include Android 8.1 (API 27), Android 9+ and current Android TV.
- No promise of maintenance or compatibility for Android 7.x or below,
  or decade-old devices unable to handle current playback demands.
  Older devices may still run existing builds as long as their manifest
  permits this, but must not block sensible modernization.
- **Do not raise the shipped `minSdkVersion` yet.** The inherited
  `SharedModules/constants.gradle` currently configures
  `minSdkVersion = 17`. The build system may continue installing on
  older devices for now. Formal deprecation via `minSdkVersion 26`
  requires a separately reviewed release/migration decision, testing
  and clear release notes; this MT-001 change does not uninstall or
  exclude any existing device.
- Prevent regressions: any next release candidate must still be
  installable on **API 26**, and its `minSdkVersion` must not exceed 26.
  Fire OS devices must be tested by actual Android API level, not by
  Fire OS marketing version or device age. A given video resolution
  additionally depends on codec and hardware decoder.
- Minimum real-device smoke-test matrix: API 26 (or closest available
  genuine Android 8/Fire OS environment), a typical older Fire TV, and
  the Thomson Android 14 TV. Record which were actually tested.
  Emulator-only evidence is insufficient for playback quality claims.

## 2. Java and Android build system

- **JDK 17** is the proven GitHub Actions build environment.
- Keep `sourceCompatibility` and `targetCompatibility` at Java 8
  (`JavaVersion.VERSION_1_8`) for the TV app and common module until
  a concrete benefit and compatibility testing justify a separate
  migration.
- Keep the currently functioning Gradle 7.5 and Android Gradle Plugin
  7.4.2 build pair for now. Upgrade only for a specific verified need
  (security fix, required platform support, eliminated build failure,
  measurable build improvement). Test the entire dependency matrix
  before promoting it.
- Robolectric 4.11.1 is a **test-only compatibility fix** for JDK 17;
  it does not change the app's video engine or minimum runtime OS.
- Follow upstream SmartTube interfaces and source boundaries so
  `MediaServiceCore` and `SharedModules` remain safely mergeable.

## 3. Video player: keep the proven baseline

- Current playback engine is a **customized Amazon ExoPlayer 2.10.6
  lineage**, `exoplayer-amzn-2.10.6`, with additional DASH/HLS/SABR
  integration in `common/exoplayer/`, decoder-related adjustments
  and playback error handling. **Do not replace it by default.**
- The name `2.10.6` is not an objective benchmark of playback
  quality. Its customized transport paths, SmartTube integrations,
  and Android hardware decoding must be measured rather than
  dismissed solely due to version number.
- Player modernization is allowed when there is a **specific measured
  problem**: time to first frame, recurring buffering, frame drops,
  memory pressure, poor codec selection, error recovery, or a proven
  security/correctness issue. Do not add features or dependencies
  that disproportionately slow down low-RAM TVs.
- The repository already has `PlaybackStartupMetrics`, recording
  selection, format request/resolution, source preparation, player
  readiness, first frame and error phases **without logging video
  identifiers or account data**. Reuse this instrument before adding
  a separate performance subsystem.
- Benchmarks must compare a baseline and a candidate on the **same
  real device**, same network and same types of videos, including a
  cold-start first-play and repeated-play sample. Collect medians
  and error/regression counts; don't assert improvements from a
  single unpaired run.
- Explicit compatibility cases: low-RAM TV, remote-control seeking
  and playback, live streams, SDR/HDR as hardware permits, subtitles,
  SponsorBlock, audio synchronization, standby/resume, and fallbacks
  on unsupported codecs.
- Keep the existing player untouched when measurement shows no
  meaningful benefit. A full migration to Media3 requires an
  isolated research/experimental branch and comparative device tests;
  it is **not part of MT-001 or MT-002**.
- Modernizing the player will not add RAM or missing codec hardware
  to old television chipsets. Avoid pretending that software can
  overcome those physical constraints.

## 4. Release safeguards

- Every change must preserve the functional native Leanback TV
  navigation, stable `de.equiummc.mars.smarttube` package identity,
  Mars blue-glass UI and settings until tested otherwise.
- No forced update, reinstall, credential leak, changed app-signing
  identity, or user-data loss. APK signing and real-device upgrade
  must be proved before the updater is published.
- Device support guarantees apply **only after** a signed release
  passes the corresponding compatibility matrix; a successful
  GitHub debug build alone is insufficient.

Owner decision: prioritize safe, useful updates over chasing newest
language versions or replacing mature player components as a matter
of fashion.
