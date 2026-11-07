#!/usr/bin/env python3
"""Read-only APK size audit for locally built MarsTube 0.0.30 and 0.0.31.

Usage:
  python Audit-MarsTube-Apk.py E:\MarsTube\APK\MarsTube-0.0.30.apk E:\MarsTube\APK\MarsTube-0.0.31.apk

Reports COMPRESSED ZIP entry sizes (file-on-disk contributions) plus raw sizes,
which must not be confused with installed disk storage or runtime RAM use.
Does not extract, modify, upload or contact any service.
"""
from collections import defaultdict
from pathlib import Path
import sys
import zipfile

def mb(n):
    return f"{n / 1_000_000:,.2f}".replace(",", " ")

def audit(path):
    path = Path(path)
    if not path.is_file():
        print(f"NOT FOUND: {path}")
        return None
    data = defaultdict(lambda: [0, 0])
    native = defaultdict(lambda: [0, 0])
    native_files = defaultdict(int)
    with zipfile.ZipFile(path) as archive:
        for item in archive.infolist():
            name = item.filename
            if item.is_dir():
                continue
            if name.startswith("lib/"):
                category = "Native Bibliotheken"
                parts = name.split("/")
                if len(parts) >= 3:
                    abi = parts[1]
                    native[abi][0] += item.compress_size
                    native[abi][1] += item.file_size
                    native_files[parts[-1]] += item.compress_size
            elif name.startswith("classes") and name.endswith(".dex"):
                category = "DEX / Programmcode"
            elif name.startswith("res/") or name == "resources.arsc":
                category = "Android-Ressourcen"
            elif name.startswith("assets/"):
                category = "Assets"
            else:
                category = "Andere"
            data[category][0] += item.compress_size
            data[category][1] += item.file_size
    print("\n" + str(path))
    print(f"APK-Datei: {mb(path.stat().st_size)} MB ({path.stat().st_size:,} Byte)")
    print("Bereich                       ZIP MB      entpackt MB")
    for name, (packed, original) in sorted(data.items(), key=lambda x: -x[1][0]):
        print(f"{name:<28} {mb(packed):>8}      {mb(original):>8}")
    print("ABI                           ZIP MB      entpackt MB")
    for name, (packed, original) in sorted(native.items()):
        print(f"{name:<28} {mb(packed):>8}      {mb(original):>8}")
    print("Groesste .so-Dateien (komprimiert, summiert ueber ABIs):")
    for name, size in sorted(native_files.items(),key=lambda x:-x[1])[:12]:
        print(f"  {name:<37} {mb(size):>8} MB")
    return path.stat().st_size

if __name__ == "__main__":
    paths = sys.argv[1:]
    if not paths:
        paths=[r"E:\MarsTube\APK\MarsTube-0.0.30.apk",
               r"E:\MarsTube\APK\MarsTube-0.0.31.apk"]
    results=[audit(path) for path in paths]
    if len(results) >= 2 and results[0] is not None and results[1] is not None:
        delta=results[1]-results[0]
        print("\n0.0.31 gegen 0.0.30: " + f"{delta / 1_000_000:+.2f} MB "
              + f"({100*delta/results[0]:+.2f} %) komprimierte APK-Dateigroesse")
