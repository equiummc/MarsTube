#!/usr/bin/env python3
"""0.0.5: complete de-DE for 370 mobile UI messages (player/submenus/search/etc.).
Overlays the formerly empty values-de strings_mobile.xml. Fails closed on upstream
changes and validates formatter argument signatures.
"""
from pathlib import Path
import sys, re
from xml.sax.saxutils import escape
root = Path(sys.argv[1]).resolve()
lab = Path(__file__).resolve().parent
if not (root / "sabr-media3").is_dir():
    raise SystemExit("Not the disposable pinned NewTube reference checkout")
source = root / "smarttubetv/src/stmobile/res/values/strings_mobile.xml"
orig = source.read_text(encoding="utf-8")
rx = re.compile(r'<string\s+name="([^"]+)"[^>]*>([\s\S]*?)</string>')
strings = rx.findall(orig)
entries = []
for path in (lab / "locale_mobile_de_000_179.txt",
             lab / "locale_mobile_de_180_369.txt"):
    for ln in path.read_text(encoding="utf-8").splitlines():
        if not ln.strip():
            continue
        i, value = ln.split("|", 1)
        entries.append((int(i), value))
if len(strings) != 370 or len(entries) != 370:
    raise SystemExit(f"Unexpected mobile string count {len(strings)} vs {len(entries)}")
for idx, (num, translated) in enumerate(entries):
    if idx != num:
        raise SystemExit(f"Mobile translation order broken at {idx}: {num}")
    original_fmt = sorted(re.findall(r'%(?:\d+\$)?[dfs]', strings[idx][1]))
    translated_fmt = sorted(re.findall(r'%(?:\d+\$)?[dfs]', translated))
    if original_fmt != translated_fmt:
        raise SystemExit(f"Mobile translation formatter mismatch {strings[idx][0]}: "
                         f"{original_fmt} != {translated_fmt}")
    # Modern typographical apostrophes to avoid Android aapt quote escaping.
    entries[idx] = (num, translated.replace("'", "’"))
output = root / "smarttubetv/src/stmobile/res/values-de/strings_mobile.xml"
output.write_text(
    '<?xml version="1.0" encoding="utf-8"?>\n<resources>\n'
    + '\n'.join('    <string name="' + name + '">' + escape(text) + '</string>'
                for (name, _), (_, text) in zip(strings, entries))
    + '\n</resources>\n', encoding="utf-8")
print("MarsTube 0.0.5: 370 mobile labels fully localized for de-DE.")
