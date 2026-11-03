#!/usr/bin/env python3
"""MT2-003R: make isolated Android-TV Media3 lab a different Android package.

The test build must NEVER replace io.github.aleixrodriala.arc.debug, because its
debug signing cert changes between GitHub Actions runners. The old data and
12:13 resume record are intentionally preserved. Only the disposable pinned
NewTube checkout is modified, not the stable MarsTube repository app.
"""
from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit("usage: python3 patch_lab_identity.py <newtube-reference-root>")
root = Path(sys.argv[1]).resolve()
gradle = root / "smarttubetv/build.gradle"
manifest = root / "smarttubetv/src/stmobile/AndroidManifest.xml"
if not (root / "sabr-media3").is_dir() or not gradle.is_file() or not manifest.is_file():
    raise SystemExit("Not the pinned NewTube reference checkout")

def one(path, old, new):
    s = path.read_text(encoding="utf-8")
    if s.count(old) != 1:
        raise SystemExit(f"Cannot safely patch {path}: anchor count {s.count(old)}")
    path.write_text(s.replace(old, new, 1), encoding="utf-8")

# Existing debug buildType already adds ".debug" to flavor's applicationId.
# Final separate Android package: de.equiummc.marstube2.lab.debug
one(gradle, '            applicationId "io.github.aleixrodriala.arc"\n',
    '            applicationId "de.equiummc.marstube2.lab"\n')
one(manifest, 'android:label="MarsTube 2.0 Input Lab"',
    'android:label="MarsTube 2.0 Recovery Lab"')
print("MT2-003R side-by-side identity: de.equiummc.marstube2.lab.debug")
