#!/usr/bin/env python3
"""Branding + basic German navigation gate for the native MarsTube source.

Run from repository root: python3 mt2-native/verify_mars_native.py
This deliberately validates the original Leanback TV sources, not the stmobile lab.
"""
from pathlib import Path
import struct
import xml.etree.ElementTree as ET

repo = Path(__file__).resolve().parents[1]
app = repo / "smarttubetv/src/main"
rootstrings = app / "res/values/strings.xml"
german = repo / "common/src/main/res/values-de/strings.xml"
manifest = app / "AndroidManifest.xml"
nativebrowse = app / "java/com/liskovsoft/smartyoutubetv2/tv/ui/browse/BrowseFragment.java"
nativeplayer = app / "java/com/liskovsoft/smartyoutubetv2/tv/ui/playback/PlaybackFragment.java"

def read_xml(path):
    return {e.attrib["name"]: "".join(e.itertext()).strip()
            for e in ET.parse(path).getroot().iter("string") if "name" in e.attrib}

main = read_xml(rootstrings)
de = read_xml(german)
assert main.get("app_name") == "MarsTube", "Native application label wrong"
assert main.get("browse_title") == "MarsTube", "Native browse title wrong"
expected = {
    "header_home": "Start",
    "title_search": "Suche",
    "header_subscriptions": "Abos",
    "header_history": "Verlauf",
    "header_settings": "Einstellungen",
}
for key, wanted in expected.items():
    assert de.get(key) == wanted, f"Native German menu invalid: {key}: {de.get(key)!r}"

for path in [
    app / "res/mipmap-nodpi/app_icon.png",
    app / "res/mipmap-nodpi/app_icon_alt.png",
    repo / "smarttubetv/src/ststable/res/mipmap-nodpi/app_icon.png",
    repo / "smarttubetv/src/ststable/res/mipmap-nodpi-v30/app_icon.png",
    repo / "smarttubetv/src/stbeta/res/mipmap-nodpi/app_icon.png",
    repo / "smarttubetv/src/stbeta/res/mipmap-nodpi-v30/app_icon.png",
    repo / "smarttubetv/src/stfdroid/res/mipmap-nodpi/app_icon.png",
    repo / "smarttubetv/src/stfdroid/res/mipmap-nodpi-v30/app_icon.png",
]:
    payload = path.read_bytes()
    assert payload[:8] == b"\x89PNG\r\n\x1a\n", f"Invalid M logo PNG: {path}"
    width, height = struct.unpack(">II", payload[16:24])
    assert width >= 90 and height >= 90, f"Unexpectedly small logo {width}x{height}: {path}"

txt = manifest.read_text(encoding="utf-8")
assert 'android:icon="@mipmap/app_icon"' in txt, "Native launcher icon not wired"
assert 'android.intent.category.LEANBACK_LAUNCHER' in txt, "Missing TV launcher"
assert "BrowseSupportFragment" in nativebrowse.read_text(encoding="utf-8")
assert nativeplayer.is_file(), "Native TV player missing"

print("PASS: native MarsTube app, M icon, German root menus and Android TV launcher.")
print("NOTE: this does NOT certify full localization, Back-stack or Media3 integration.")
