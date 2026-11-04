# MarsTube 0.0.10 test APK

| Item | Value |
| --- | --- |
| File | `marstube-v0.0.10-firetv-player-settings-ui-debug.apk` |
| Size | `81,054,904` bytes |
| SHA-256 | `cf390e4e117502f6af441e16e5a0a50f29db8c116698480d1b00d3aafc437eea` |
| Package | `de.equiummc.marstube2.firetest010.debug` |
| Version | `12010` / `0.0.10` |
| Label | `MarsTube 0.0.10` |
| Signature | Android Debug (V2) |
| ABIs | `arm64-v8a`, `armeabi-v7a` |

This APK is a local test artifact and is ignored by `*.apk`; only this README
and the `.sha256` file are committed. The same build is published by the
`mt2-media3-sabr-lab` workflow as artifact
`marstube-v0.0.10-firetv-player-settings-ui-debug`.

CI run [41927632833](https://github.com/equiummc/MarsTube/actions/runs/41927632833)
passed. Its APK is 81,054,684 bytes with SHA-256
`4fc18f903d46136bb899ec436c7f4597549119233d4af85861c34a4ac62622f2`.
That checksum is specific to that CI build; the local APK above has a different
debug-build checksum with the same package, version, label and ABIs.

## Verify

```powershell
Get-FileHash .\marstube-v0.0.10-firetv-player-settings-ui-debug.apk -Algorithm SHA256
Get-Content .\marstube-v0.0.10-firetv-player-settings-ui-debug.apk.sha256
```

## Install next to 0.0.9

```powershell
adb install -r .\marstube-v0.0.10-firetv-player-settings-ui-debug.apk
```

The new application ID means 0.0.9 and its data stay installed. Do not
uninstall the 0.0.9 package for this test.

See `mt2-player-lab/README-0.0.10.md` for the change report, automated test
results and the device checks that are still open.
