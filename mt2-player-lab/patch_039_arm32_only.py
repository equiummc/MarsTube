#!/usr/bin/env python3
"""MT2-039: ARM32-only production APK. Applied after the pinned 038 patch chain.

Do not strip, modify, zipalign or re-sign an already signed APK. Configure
Android Gradle Plugin *before* assemble so all native libraries and the APK
are built, packaged and signed in one pass for Fire TV armeabi-v7a only.
"""
from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit("Usage: patch_039_arm32_only.py <pinned-newtube-checkout>")

root = Path(sys.argv[1]).resolve()
gradle = root / "smarttubetv/build.gradle"
if not gradle.is_file():
    raise SystemExit("MT2-039: Android app build.gradle missing")
text = gradle.read_text(encoding="utf-8")

def one(old, new):
    global text
    hits = text.count(old)
    if hits != 1:
        raise SystemExit(f"MT2-039 FAIL CLOSED: {hits} matches: {old[:120]!r}")
    text = text.replace(old, new, 1)

one("""        ndk {
            abiFilters 'armeabi-v7a', 'arm64-v8a'""",
    """        ndk {
            // MARS Fire TV test contract: ONLY 32-bit ARM, no bundled 64-bit libs.
            abiFilters 'armeabi-v7a'""")
one("""            include 'armeabi-v7a', 'arm64-v8a', 'x86' // v7a main build, x86 for WSL, v8a for Pixel Tablet (no 32 bit support)
            universalApk = true //generate an additional APK that contains all the ABIs""",
    """            include 'armeabi-v7a' // 32-bit-only Fire TV release
            universalApk = false // only ONE signed armeabi-v7a APK, no universal variant""")
one("versionCode 12038", "versionCode 12039")
one('versionName "0.0.38"', 'versionName "0.0.39"')

if "abiFilters 'arm64-v8a'" in text or "include 'arm64-v8a'" in text:
    raise SystemExit("MT2-039: unexpected remaining ARM64 ABI declaration")
if 'applicationId "de.equiummc.marstube2"' not in text:
    raise SystemExit("MT2-039: permanent signed package ID missing")
if "universalApk = false" not in text:
    raise SystemExit("MT2-039: ABI-only packaging not configured")

gradle.write_text(text, encoding="utf-8")
print("MT2-039: 32-bit armeabi-v7a ONLY, versionCode 12039, same permanent Android package")
