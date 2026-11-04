#!/usr/bin/env python3
"""Give MarsTube 0.0.6 a collision-free, disposable Fire TV test identity.

Applied ONLY after patch_006_unified.py in a pinned, temporary CI checkout.
This prevents replacing an installed beta app signed by a different CI key.
"""
from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit("Usage: patch_006_isolated_firetv.py <pinned-test-checkout>")
root = Path(sys.argv[1]).resolve()
gradle = root / "smarttubetv/build.gradle"
manifest = root / "smarttubetv/src/stmobile/AndroidManifest.xml"
if not all((root / "sabr-media3").is_dir() and p.is_file() for p in (gradle, manifest)):
    raise SystemExit("Fail closed: not the expected pinned test checkout")

def replace_once(path: Path, old: str, new: str) -> None:
    content = path.read_text(encoding="utf-8")
    if content.count(old) != 1:
        raise SystemExit(f"Fail closed: expected exactly one anchor in {path}: {old}")
    path.write_text(content.replace(old, new, 1), encoding="utf-8")

replace_once(gradle,
             'applicationId "de.equiummc.marstube2.beta"',
             'applicationId "de.equiummc.marstube2.firetest006"')
replace_once(manifest, 'android:label="MarsTube"',
             'android:label="MarsTube TV-Test 006"')
print("Fire TV test package: de.equiummc.marstube2.firetest006.debug")
print("Other MarsTube installations and their data remain unchanged")
