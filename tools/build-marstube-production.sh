#!/usr/bin/env bash
# MARS-ZIEL: GitHub Actions / Ubuntu Bash
# MARS-PROJEKT: MarsTube
# MARS-VERSION: production-version.json
# MARS-AKTION: Permanentes Release-APK mit demselben Signierschluessel bauen
set -euo pipefail
cd "$(dirname "$0")/.."
root=newtube-reference
meta=mt2-player-lab/production-version.json
[ -f "$meta" ] || { echo 'Fehlender Versionsvertrag'; exit 1; }
for name in MARS_SIGNING_KEYSTORE_B64 MARS_SIGNING_STORE_PASS MARS_SIGNING_KEY_PASS MARS_SIGNING_KEY_ALIAS; do
  if [ -z "$(printenv "$name")" ]; then
    echo "::error::Dauerhafte GitHub-Signatur fehlt: $name" >&2
    exit 1
  fi
done
mkdir "$root"
git -C "$root" init
git -C "$root" remote add origin https://github.com/aleixrodriala/newtube.git
git -C "$root" fetch --depth 1 origin 6f96a1b42bb9272935360d7e5aec34d35f2753f0
git -C "$root" checkout --detach FETCH_HEAD
git -C "$root" submodule update --init --recursive --depth 1
[ "$(git -C "$root" rev-parse HEAD)" = '6f96a1b42bb9272935360d7e5aec34d35f2753f0' ]
yes | sdkmanager --licenses >/dev/null || true
sdkmanager --channel=3 'platforms;android-37.0' 'build-tools;37.0.0' 'ndk;21.0.6113669'
test -f "$ANDROID_HOME/platforms/android-37.0/android.jar"
if [ ! -e "$ANDROID_HOME/platforms/android-37" ]; then ln -s "$ANDROID_HOME/platforms/android-37.0" "$ANDROID_HOME/platforms/android-37"; fi
sed -i "s@'../SharedModules'@'./SharedModules'@g;s@'../MediaServiceCore'@'./MediaServiceCore'@g" "$root/settings.gradle"
chmod +x "$root/gradlew"
for patch in \
  patch_tv_input.py patch_sabr_recovery.py patch_lab_identity.py \
  patch_tv_shell.py patch_005_navigation.py patch_005_localize.py \
  patch_005_mobile_de.py patch_006_unified.py patch_006_isolated_firetv.py \
  patch_007_tv_quality.py patch_007_translate_common.py patch_008_tv_player.py \
  patch_009_tv_shorts_ui.py patch_010_player_settings_ui.py \
  patch_011_firetv_navigation.py patch_012_tv_updates.py \
  patch_013_tv_focus_ui.py patch_014_tv_shorts_mode.py \
  patch_015_universal_back.py patch_016_production.py \
  patch_017_shorts_stage.py patch_018_tv_ux.py \
  patch_019_shorts_three_column.py patch_020_update_freshness.py \
  patch_021_shorts_home_pause.py patch_022_update_reliability.py \
  patch_023_tv_settings.py patch_024_unified_navigation.py \
  patch_025_tv_ui.py patch_026_shorts_actions.py \
  patch_027_shorts_polish.py patch_028_shorts_undistorted_still.py patch_029_shorts_compact.py; do
  python3 "mt2-player-lab/$patch" "$root"
done
grep -Fq 'applicationId "de.equiummc.marstube2"' "$root/smarttubetv/build.gradle"
grep -Fq 'new MarsTvShortsStage' "$root/smarttubetv/src/stmobile/java/com/newtube/mobile/ui/playback/MarsTvShell.java"
grep -Fq 'mMarsTvUniversalBack.setEnabled(false)' "$root/smarttubetv/src/stmobile/java/com/newtube/mobile/ui/common/MobileActivity.java"
grep -Fq 'marsTvGoToSettingsRoot' "$root/smarttubetv/src/stmobile/java/com/newtube/mobile/ui/settings/MobileSettingsActivity.java"
grep -Fq 'Bei Ladeproblemen Qualität reduzieren' "$root/smarttubetv/src/stmobile/java/com/newtube/mobile/ui/settings/MarsTvSettingsPages.java"
grep -Fq 'no-system-pip' "$root/smarttubetv/src/stmobile/java/com/newtube/mobile/ui/playback/MobilePlaybackActivity.java"
grep -Fq 'SHORTS_PREVIEWS' "$root/smarttubetv/src/stmobile/java/com/newtube/mobile/ui/playback/MarsTvShell.java"
test -s "$root/smarttubetv/src/stmobile/java/com/newtube/mobile/ui/playback/MarsTvShortsStage.java"
grep -Fq 'android:icon="@drawable/mars_m_flat"' "$root/smarttubetv/src/stmobile/AndroidManifest.xml"
test -s "$root/smarttubetv/src/stmobile/res/drawable/mars_m_flat.xml"
grep -Fq 'MarsTvNavigationModel.wrap(index, -1' "$root/smarttubetv/src/stmobile/java/com/newtube/mobile/ui/common/MarsTvRail.java"
grep -Fq 'MarsTvNavigationModel.playerCount(shortsMode)' "$root/smarttubetv/src/stmobile/java/com/newtube/mobile/ui/playback/MarsTvShell.java"
grep -Fq 'SETTINGS_WIDTH_DP = WIDTH_DP' "$root/smarttubetv/src/stmobile/java/com/newtube/mobile/ui/common/MarsTvNavigationTheme.java"
grep -Fq 'MarsTvNavigationModel.HOME_INDEX' "$root/smarttubetv/src/stmobile/java/com/newtube/mobile/ui/playback/MarsTvShell.java"
grep -Fq 'mPlayerView.setClipToOutline(true)' "$root/smarttubetv/src/stmobile/java/com/newtube/mobile/ui/playback/MobilePlaybackActivity.java"
grep -Fq 'Math.round(22f * density)' "$root/smarttubetv/src/stmobile/java/com/newtube/mobile/ui/playback/MarsTvShortsStage.java"
grep -Fq 'label.setTranslationY(alignedTop);' "$root/smarttubetv/src/stmobile/java/com/newtube/mobile/ui/common/MarsTvRailLabel.java"
test -s "$root/smarttubetv/src/stmobile/res/drawable/mars_m_flat.xml"
grep -Fq 'releases/latest/download/marstube-tv.json' "$root/common/src/main/res/values/update_urls.xml"
grep -Fq 'raw.githubusercontent.com/equiummc/MarsTube/mars/production-2/updates/stable.json' "$root/common/src/main/res/values/update_urls.xml"
if grep -Fq 'aleixrodriala/newtube' "$root/common/src/main/res/values/update_urls.xml"; then echo '::error::Falscher Updater'; exit 1; fi
umask 077
mkdir -p "$RUNNER_TEMP/mars-signing"
printf '%s' "$MARS_SIGNING_KEYSTORE_B64" | base64 --decode > "$RUNNER_TEMP/mars-signing/release.jks"
test -s "$RUNNER_TEMP/mars-signing/release.jks"
keytool -list -keystore "$RUNNER_TEMP/mars-signing/release.jks" -storepass:env MARS_SIGNING_STORE_PASS -alias "$MARS_SIGNING_KEY_ALIAS" >/dev/null
printf 'storeFile=%s\nstorePassword=%s\nkeyAlias=%s\nkeyPassword=%s\n' \
  "$RUNNER_TEMP/mars-signing/release.jks" "$MARS_SIGNING_STORE_PASS" "$MARS_SIGNING_KEY_ALIAS" "$MARS_SIGNING_KEY_PASS" \
  > "$root/keystore.properties"
chmod 600 "$root/keystore.properties"
(cd "$root" && ./gradlew --no-daemon --max-workers=1 -Dorg.gradle.jvmargs=-Xmx4096m :sabr-media3:testDebugUnitTest :smarttubetv:assembleStmobileRelease --stacktrace)
mkdir -p dist
mapfile -t apks < <(find "$root/smarttubetv/build/outputs/apk" -type f -path '*stmobile*/release/*' -name '*.apk' | sort)
if [ "${#apks[@]}" -ne 1 ]; then printf '::error::Exactly 1 universal release APK expected, got %s\n' "${#apks[@]}"; printf '%s\n' "${apks[@]}"; exit 1; fi
cp "${apks[0]}" dist/MarsTube-production.apk
rm -f "$root/keystore.properties" "$RUNNER_TEMP/mars-signing/release.jks"
aapt="$ANDROID_HOME/build-tools/37.0.0/aapt"
signer="$ANDROID_HOME/build-tools/37.0.0/apksigner"
"$aapt" dump badging dist/MarsTube-production.apk > dist/badging.txt
"$signer" verify --print-certs dist/MarsTube-production.apk > dist/signature.txt
"$aapt" dump permissions dist/MarsTube-production.apk > dist/permissions.txt
grep -Fq 'android.permission.REQUEST_INSTALL_PACKAGES' dist/permissions.txt
for denied in REQUEST_DELETE_PACKAGES SYSTEM_ALERT_WINDOW RECORD_AUDIO; do
  if grep -Fq "android.permission.$denied" dist/permissions.txt; then echo "::error::Forbidden permission $denied"; exit 1; fi
done
python3 - <<'PY'
import json, hashlib
from pathlib import Path
meta=json.loads(Path('mt2-player-lab/production-version.json').read_text(encoding='utf-8'))
name=meta['versionName']; code=int(meta['versionCode']); app=meta['packageName']
assert app == 'de.equiummc.marstube2' and code>=12016
badge=Path('dist/badging.txt').read_text()
assert f"name='{app}'" in badge, 'Wrong package'
assert f"versionCode='{code}'" in badge, 'Wrong versionCode'
assert f"versionName='{name}'" in badge, 'Wrong versionName'
assert "application: label='MarsTube'" in badge, 'Wrong app label'
sig=Path('dist/signature.txt').read_text()
assert 'certificate SHA-256 digest:' in sig, 'APK not signed'
old=Path('dist/MarsTube-production.apk'); apk=old.with_name(f'MarsTube-{name}.apk'); old.rename(apk)
url=f'https://github.com/equiummc/MarsTube/releases/download/v{name}/{apk.name}'
manifest={'package':{'downloadUrlList':[url], 'downloadSize':apk.stat().st_size}, name:{'versionCode':code,'changelog':meta['changelog_de'],'changelog_de':meta['changelog_de']}}
Path('dist/marstube-tv.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
Path('dist/release-notes.md').write_text('# MarsTube '+name+'\n\n'+'\n'.join('- '+v for v in meta['changelog_de'])+'\n\nPermanente App-ID: '+app+'\n',encoding='utf-8')
Path('dist/SHA256SUMS').write_text(hashlib.sha256(apk.read_bytes()).hexdigest()+'  '+apk.name+'\n')
print('Signiertes dauerhaftes MarsTube-APK erfolgreich vorbereitet.')
PY
(cd dist && sha256sum -c SHA256SUMS)
