# MT2-033+ – TV UX modernization, Shorts Discovery and player behavior

Stand: 2026-11-08. Working branch based on proven MT2-032. Fire TV Cube first.
Do not change package name `de.equiummc.marstube2`, release signing,
Media3/SABR pipeline, BACK-to-rail contract, or installed app data.

## Screen reference and style
The user-supplied photos of the official YouTube TV interface are **UX references**,
not images/assets to copy. Maintain the independent MarsTube blue identity and
existing M logo. No brown filter or solid full-height blue background.

## Priority A – behavior (first MT2-033 implementation)
- [x] Player rail: restore prior focus when leaving the rail with RIGHT.
- [x] BACK twice: second BACK closes rail to its prior focus.
- [x] Shorts end: always dispatch scoped NEXT in the "Weiter" mode; MT2-032
  takes responsibility for fetching another metadata page when queue is empty.
- [x] Set underlying Media3 repeat mode (OFF for Weiter, ONE for Wiederholen)
  instead of relying on an UI-only toggle.
- [ ] Test end-of-feed with live network and repeated/paused/restarted playback
  on Fire TV; no generic regular-video fallback.
- [ ] Distinct active icons / labels for Weiter and Wiederholen; avoid counters.
- [ ] Discovery for normal recommendations: scoped pagination and bounded cache
  following Shorts design, retaining normal-video UX.
- [ ] Local `Nicht interessiert` negative list using video ID with immediate
  current-feed removal; do not falsely report server-side YouTube feedback.
- [ ] Confirm if a supported authenticated service API can submit official
  recommendation-feedback action; video rating (dislike) is not equivalent.

## 033 – integrated player submenu progress (current test patch)
- [x] Native right-side panels for Kanal, Kommentare, Merken, Einstellungen,
  and Mehr, with D-pad up/down/OK, left-to-close and BACK-to-close.
- [x] Same existing PlayerView docked as a left preview; no new decoder,
  second player, or off-screen Surface.
- [x] Actual subscriber toggle wired through existing onSubscribeTapped() and
  getButtonState(R.id.action_subscribe); requires account support.
- [x] Native audio-track picker only offered if alternate audio tracks exist.
- [x] Quality, captions, playback speed, playlist save and comments are routed
  through the existing Activity handlers, not fake local settings.
- [ ] Channel notification-bell state/action: channel page remains authoritative;
  this first-stage panel explicitly routes there instead of claiming a
  direct notification change.
- [ ] Unified comments pane resize/focus check on actual Fire TV.
- [ ] Ensure all new panes survive 20+ video/Shorts transitions without stutter,
  dropped focus or unexpected Media3 teardown.
- [ ] Only mark ready for installation after successful signed 033 CI checks.

## Priority B – player & loading
- [ ] Profile portrait thumbnail availability and effective displayed image URL,
  `mVideoStill`, `getCardImageUrl()`, ImageView scale type and frame bounds.
- [ ] Prefer true 9:16 thumbnail where actually available; do not stretch 16:9.
- [ ] Only when mismatch remains, masked 500 ms reveal on first decoded frame.
  No second decoder and no delay to playback; animation must be cancel-safe.
- [ ] Shrink only the lower seek/progress chrome; move interactive player
  icons above seek line as floating controls over a subtle blue gradient.
- [ ] Replace redundant around-Play transport icons with navigation to channel,
  favorites/save, comments, bell/subscription where supported.
- [ ] Side panel for channel, subscribed/notification state, comments, quality,
  audio and caption settings while one player remains active.
- [ ] Inspect available track formats before claiming 1080p Premium support.
  Premium access must reflect actual account, track availability and bitrate.

## Priority C – TV browse and visual structure
- [ ] Narrow detached navigation flyout label ("Startseite", etc.). Fix M/home
  flyout anchoring and label width; should be no wider than necessary for
  "Einstellungen", never appear randomly at upper-left.
- [ ] For normal Home keep six visible, smaller, legible cards with a reserved
  date/time area at the top right.
- [ ] For Shorts browse keep six narrower portrait cards, preserve focused
  preview before explicit OK and accessible remote navigation.
- [ ] Consistent local DE time & date on Home, Shorts browse, Shorts player
  and regular player when rail open. Player-chrome dates added in first 033
  stage; both browse headers require separate activity integration.
- [ ] Subscription page: left scrolling subscribed channel rail, thin divider,
  right video feed and working D-Pad transfer.
- [ ] Library page: compact tile shortcuts for Verlauf, Später ansehen,
  Playlists and Musik, retaining existing sections and accounts.
- [ ] All submenus use a smaller playing-video preview + right action column
  and safe restoration of focus when dismissed.

## CI and acceptance gates
- Build only from pinned upstream+Mars patches in isolated CI workspace.
- Run `:sabr-media3:testDebugUnitTest` and `:smarttubetv:testStmobileDebugUnitTest`.
- Check appID/versionCode/signature, universal ABI, SHA256SUMS and APK package.
- Keep test APK as CI-only artifact; **do not create normal Release** without
  Fire TV acceptance. Never uninstall or wipe user data to "fix" updates.
- Fire TV remote tests: 20+ Shorts with MORE discovery results; end-of-list,
  repeat/next, return from rail on all focuses, channel panel, normal video,
  navigation away/back, fast video switching; no decoder duplication.
- Check real transition frames (9:16), battery/performance, memory,
  black frames, changed URL errors and audio/track quality.
- On failure, no automated rollout to Thomson/Sony.

## Current implementation status
`patch_033_navigation_autonext.py` implements scoped behavior + player clock.
The extensive overlay redesign above is **not yet implemented**. No reliable
device-test claims before Fire TV measurements.
