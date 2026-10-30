#!/usr/bin/env python3
"""Apply a small, fail-closed Mars UI/update overlay onto a SmartTube upstream checkout.
Do not hide upstream merge conflicts. Keeps upstream licenses and submodule pointers.
"""
from pathlib import Path
import argparse
import re
import sys

ap = argparse.ArgumentParser()
ap.add_argument('--repo', type=Path, required=True)
ap.add_argument('--owner', default='equiummc')
ap.add_argument('--version', default='32.68')
a = ap.parse_args()
root = a.repo.resolve()
if not (root / 'smarttubetv/build.gradle').is_file():
    sys.exit('ERROR: Not a SmartTube root')

def change(path, action):
    p = root / path
    if not p.is_file():
        raise RuntimeError('Missing upstream path: ' + path)
    orig = p.read_text(encoding='utf-8')
    new = action(orig)
    if new == orig:
        print('UNCHANGED', path)
    else:
        p.write_text(new, encoding='utf-8', newline='')
        print('PATCHED', path)

def replace_once(s, old, new, desc):
    if new in s:
        return s
    if s.count(old) != 1:
        raise RuntimeError(f'Upstream changed: expected one {desc}, saw {s.count(old)}')
    return s.replace(old, new, 1)

# Keep original source and copyright, but install our own separately signed package.
def package_mod(s):
    s = replace_once(s, 'applicationId "org.smarttube.stable"',
                     'applicationId "de.equiummc.mars.smarttube"', 'stable package')
    # Do not mirror original upstream versionCode; Mars releases advance independently.
    s = re.sub(r'(?m)^(\s*versionCode\s+)\d+\s*$', r'\g<1>100001', s, count=1)
    s = re.sub(r'(?m)^(\s*versionName\s+)"[^"]+"\s*$',
               r'\g<1>"Mars-' + a.version + '.1"', s, count=1)
    if 'versionCode 100001' not in s or f'versionName "Mars-{a.version}.1"' not in s:
        raise RuntimeError('Version fields not found; stop instead of releasing wrong package')
    return s
change('smarttubetv/build.gradle', package_mod)

# Our stable applicationId is not registered in the original Firebase project.
# Fail closed: no upstream Firebase plugin, Google Services JSON processing
# or Crashlytics SDK in our own stable APK. Keep this on every upstream merge.
def mars_firebase_off(s):
    old_plugin = '''// The first letter of the flavor name must be in uppercase
if (new File("${projectDir}/google-services.json").exists() && (getGradle().getStartParameter().getTaskRequests().toString().contains("Stbeta") ||
        getGradle().getStartParameter().getTaskRequests().toString().contains("Ststable"))) {
    // Google Services Gradle plugin (transforms google-services.json file)
    apply plugin: 'com.google.gms.google-services'

    // Apply the Crashlytics Gradle plugin
    apply plugin: 'com.google.firebase.crashlytics'
}'''
    new_plugin = '''// MARS_FORK_FIREBASE_DISABLED: This fork has a distinct Android applicationId.
// The upstream google-services.json is not registered for de.equiummc.mars.smarttube.
// Never apply upstream Google Services/Crashlytics plugins in our fork.
// An upstream merge must preserve this guard; no private Firebase config is required.'''
    s = replace_once(s, old_plugin, new_plugin, 'Google Services plugin block')
    old_sdk = '''    ststableImplementation('com.google.firebase:firebase-crashlytics:' + firebaseCrashlyticsVersion) {
        transitive = true
    }'''
    new_sdk = '''    // MARS_FORK_STABLE_CRASHLYTICS_DISABLED: no upstream Firebase SDK in our stable APK.'''
    return replace_once(s, old_sdk, new_sdk, 'stable Firebase Crashlytics SDK')
change('smarttubetv/build.gradle', mars_firebase_off)

# Developer signing material must never enter the public fork.
def secrets_ignore(s):
    additions = ['keystore.properties', '*.jks', '*.keystore']
    missing = [x for x in additions if x not in s.splitlines()]
    return s if not missing else s.rstrip('\n') + '\n' + ''.join(x + '\n' for x in missing)
change('.gitignore', secrets_ignore)

def text_mod(s):
    s = replace_once(s,
        '<string translatable="false" name="app_name"><![CDATA[SmartTube]]></string>',
        '<string translatable="false" name="app_name"><![CDATA[MarsTube]]></string>',
        'app label')
    s = replace_once(s,
        '<string translatable="false" name="browse_title"><![CDATA[SmartTube]]></string>',
        '<string translatable="false" name="browse_title"><![CDATA[MarsTube]]></string>',
        'browse label')
    return s
change('smarttubetv/src/main/res/values/strings.xml', text_mod)
change('smarttubetv/src/ststable/res/values/strings.xml', text_mod)

# Keep upstream flavors unchanged, rename only the Mars stable APK output.
def apk_brand(s):
    return replace_once(s, '            def project = "SmartTube"',
                        '            def project = variant.flavorName == "ststable" ? "MarsTube" : "SmartTube"',
                        'Mars stable APK file name')
change('smarttubetv/build.gradle', apk_brand)

# Mutate only real color resources; no wholesale theme replacement, keeping merges manageable.
def theme(s):
    pairs = {
       'fastlane_background': '#18313A',
       'fastlane_background_dark': '#111C25',
       'search_opaque': '#18313A',
       'card_default_background': '#18313A',
       'card_default_background_dark': '#111C25',
       'shelf_background': '#14191F',
       'shelf_background_dark': '#0E1115',
       'icon_background': '#27323D',
       'primary_dark': '#19303B',
       'primary': '#3C8393',
       'primary_light': '#B2DDE4',
    }
    for name, color in pairs.items():
        pattern = rf'(<color name="{re.escape(name)}">)[^<]+(</color>)'
        s2, n = re.subn(pattern, lambda m: m.group(1)+color+m.group(2), s)
        if n != 1:
            raise RuntimeError('Missing theme color: '+name)
        s = s2
    return s
change('smarttubetv/src/main/res/values/colors.xml', theme)

# Always use OUR release manifest. Never let the app download upstream signed APK.
manifest_url = f'https://raw.githubusercontent.com/{a.owner}/mars-smarttube/master/mars/updates/stable.json'
def update_url(_s):
    return '''<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string-array name="update_urls">
        <item>''' + manifest_url + '''</item>
    </string-array>
</resources>
'''
change('common/src/ststable/res/values/update_urls.xml', update_url)

# Make an update impossible to miss: the pinned "Update" section remains,
# and the update dialog is also offered when the app is in the foreground.
def visible_updates(s):
    old = '''        } else if (GeneralData.instance(getContext()).isOldUpdateNotificationsEnabled()) {
            showUpdateDialog(versionName, changelog, apkPath);
        } else {
            pinUpdateSection(versionName, changelog, apkPath);
        }'''
    new = '''        } else {
            // MARS OVERLAY: persistent Update navigation item plus prominent dialog.
            // Both use SmartTube's own update/download/install code, not another APK.
            pinUpdateSection(versionName, changelog, apkPath);
            showUpdateDialog(versionName, changelog, apkPath);
        }'''
    return replace_once(s, old, new, 'onUpdateFound branches')
change('common/src/main/java/com/liskovsoft/smartyoutubetv2/common/app/presenters/dialogs/AppUpdatePresenter.java', visible_updates)

# Never overwrite a previously reviewed update version list.
version_file = root / 'mars/updates/stable.json'
version_file.parent.mkdir(parents=True, exist_ok=True)
if not version_file.exists():
    import json
    version_file.write_text(json.dumps({
        'package': {'downloadUrl':'https://github.com/'+a.owner+'/mars-smarttube/releases'},
        'Mars-'+a.version+'.1': {'versionCode':100001,'changelog':['Erste Mars-Testversion; noch kein veroeffentlichtes APK-Update']}
    }, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
print('Overlay ready. No release APK created; upstream updates must be reviewed.')
