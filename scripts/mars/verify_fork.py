#!/usr/bin/env python3
"""MT-001 read-only guardrails for the MarsTube TV fork.

This check intentionally never modifies user data, builds, tags, or release feeds.
Functional playback and remote-control checks still require a real device.
"""
from __future__ import annotations

import argparse
import pathlib
import re
import sys
import xml.etree.ElementTree as ET


def check(root: pathlib.Path, require_submodules: bool) -> list[str]:
    failures = []

    def require(condition: bool, message: str) -> None:
        print(f"{'PASS' if condition else 'FAIL'}: {message}")
        if not condition:
            failures.append(message)

    def read(path: str) -> str:
        target = root / path
        try:
            return target.read_text(encoding="utf-8")
        except OSError:
            failures.append(f"missing source: {path}")
            return ""

    gradle = read("smarttubetv/build.gradle")
    common_gradle = read("common/build.gradle")
    browse = read("smarttubetv/src/main/java/com/liskovsoft/smartyoutubetv2/tv/ui/browse/BrowseFragment.java")
    styles = read("smarttubetv/src/main/res/values/styles.xml")
    scheme = read("common/src/main/java/com/liskovsoft/smartyoutubetv2/common/prefs/MainUIData.java")
    url_xml = read("common/src/ststable/res/values/update_urls.xml")
    gitmodules = read(".gitmodules")

    require(bool(re.search(r"ststable\s*\{\s*applicationId\s+\"de\.equiummc\.mars\.smarttube\"", gradle)),
            "ststable package identifier remains de.equiummc.mars.smarttube")
    match = re.search(r"versionCode\s+(\d+)", gradle)
    require(match is not None and int(match.group(1)) >= 100011,
            "monotonic Mars versionCode >= 100011 baseline")
    require(bool(re.search(r'versionName\s+"Mars-[^"]+"', gradle)),
            "independent Mars versionName is configured")
    require("setHeadersState(HEADERS_ENABLED)" in browse and "setupEventListeners()" in browse,
            "native Leanback navigation and event listeners retained")
    require("new MarsFixedRail" not in browse and "enforceMarsRailOnly" not in browse,
            "experimental secondary menu and legacy-header removal absent")
    require("R.string.color_scheme_mars_safe_glass" in browse and "setBadgeDrawable(null)" in browse,
            "no in-app M badge in the Mars glass theme")
    require('name="App.Theme.MarsSafeGlass.Browse"' in styles and
            '<item name="appLogo">@null</item>' in styles,
            "Mars glass browse theme retains no title badge")
    require("color_scheme_mars_safe_glass" in scheme,
            "blue glass color scheme remains registered")
    require("path = SharedModules" in gitmodules and "path = MediaServiceCore" in gitmodules,
            "both real Git submodules declared")
    require("sourceCompatibility JavaVersion.VERSION_1_8" in gradle and
            "targetCompatibility JavaVersion.VERSION_1_8" in gradle and
            "sourceCompatibility JavaVersion.VERSION_1_8" in common_gradle and
            "targetCompatibility JavaVersion.VERSION_1_8" in common_gradle,
            "Java 8 app source/target remains compatible with JDK 17 build")
    require("minSdkVersion project.properties.minSdkVersion" in gradle,
            "default app minSdk inherits SharedModules compatibility setting")
    # Android 8.0 = API 26. An inadvertent raise above API 26 would
    # silently lock out the agreed minimum supported device.
    stable_match = re.search(r"\bststable\s*\{([^}]*)\}", gradle, re.S)
    stable_block = stable_match.group(1) if stable_match else ""
    stable_override = re.search(r"minSdkVersion\s+(\d+)", stable_block)
    require(stable_match is not None and
            (stable_override is None or int(stable_override.group(1)) <= 26),
            "ststable flavor does not require Android newer than 8.0 (API 26)")

    try:
        tree = ET.fromstring(url_xml)
        urls = [e.text or "" for e in tree.findall(".//item")]
    except ET.ParseError:
        urls = []
    require(len(urls) == 1 and
            urls[0].startswith("https://raw.githubusercontent.com/equiummc/mars-smarttube/") and
            "yuliskov/SmartTube" not in urls[0],
            "stable updater configured for fork, not upstream")
    # MT-005: the experimental ABR hook must never become an unreviewed default
    # or interfere with SmartTube's original fixed-track fallback.
    if (root / "docs/mars-maintenance/MT-005-SMART-QUALITY-LAB.md").is_file():
        tweaks = read("common/src/main/java/com/liskovsoft/smartyoutubetv2/common/prefs/PlayerTweaksData.java")
        selector = read("common/src/main/java/com/liskovsoft/smartyoutubetv2/common/exoplayer/versions/selector/RestoreTrackSelector.java")
        require("mIsMarsAdaptiveQualityEnabled = Helpers.parseBoolean(split, 62, false)" in tweaks,
                "experimental adaptive quality defaults OFF for existing users")
        require("mIsMarsAdaptiveQualityEnabled" in tweaks and
                "isMarsAdaptiveQualityEnabled()" in selector and
                "enableAdaptiveTrackSelection" in selector and
                "super.selectVideoTrack(groups, formatSupports" in selector,
                "experimental ABR path uses existing ExoPlayer selector")
        require("selectVideoTrack: choose custom video processing" in selector,
                "legacy SmartTube fixed-quality path remains available")

    if require_submodules:
        for path in ("SharedModules/core_settings.gradle", "MediaServiceCore/core_settings.gradle",
                     "exoplayer-amzn-2.10.6/core_settings.gradle"):
            require((root / path).is_file(), f"Gradle dependency exists: {path}")
        upstream_constants = read("SharedModules/constants.gradle")
        inherited_min = re.search(r"^\s*minSdkVersion\s*=\s*(\d+)\b", upstream_constants, re.M)
        require(inherited_min is not None and int(inherited_min.group(1)) <= 26,
                "inherited minSdk keeps Android 8.0 (API 26) installable")
    else:
        print("INFO: submodule files not checked (pass --require-submodules after checkout).")

    # More than a compile test is necessary. This is not a TV end-to-end test.
    print("INFO: Android-TV navigation, playback, account login, and installation require device smoke tests.")
    return failures


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--repo", type=pathlib.Path, default=pathlib.Path(__file__).resolve().parents[2])
    parser.add_argument("--require-submodules", action="store_true")
    args = parser.parse_args()
    errors = check(args.repo.resolve(), args.require_submodules)
    if errors:
        print(f"MT-001: {len(errors)} fork guardrail(s) FAILED.")
        sys.exit(1)
    print("MT-001: static fork guardrails PASS.")
