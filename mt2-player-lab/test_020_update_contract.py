#!/usr/bin/env python3
"""Fast CI contract for the real production updater path (not a TV device test)."""
import json
from pathlib import Path

root = Path(__file__).resolve().parent.parent
src = root / "mt2-player-lab"
version = json.loads((src / "production-version.json").read_text(encoding="utf-8"))
assert version["versionName"] == "0.0.20"
assert version["versionCode"] == 12020
assert version["packageName"] == "de.equiummc.marstube2"

patch = (src / "patch_020_update_freshness.py").read_text(encoding="utf-8")
recipe = (root / "tools/build-marstube-production.sh").read_text(encoding="utf-8")
settings = (src / "MarsTvSettingsPages.java").read_text(encoding="utf-8")
assert "AppVersionChecker.java" in patch
assert 'appendQueryParameter("mars_check"' in patch
assert "mManualRecheckAfterAuto" in patch
assert "onUpToDate(UpdateInfo info)" in patch
assert "MarsUpdateApkGuard.isCompatible" in patch
assert "Reject untrusted MarsTube update feed" in patch
assert "patch_020_update_freshness.py" in recipe
assert "separate Test-App" not in settings

# Checker still uses the single official feed. No unverified dynamic domains.
trusted = "github.com/equiummc/MarsTube"
assert trusted in patch or trusted in (src / "patch_012_tv_updates.py").read_text(encoding="utf-8")
print("MT2-020 contract OK. Full Java/Gradle compile and TV update remain separate gates.")
